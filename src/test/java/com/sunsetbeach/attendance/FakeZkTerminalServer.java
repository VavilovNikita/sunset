package com.sunsetbeach.attendance;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * A real local TCP server speaking just enough of the ZK wire protocol to drive {@link
 * ZkTerminalClientImpl} end-to-end - this is the actual "seam" test, as distinct from {@link
 * FakeZkTerminalClient} (which stands in for a whole device one layer up, at {@code
 * AttendanceDevicePollService}). It cross-checks the framing (magic bytes, lengths) and the
 * command sequence a poll sends, and independently confirms the attendance-record parsing by
 * constructing realistic record bytes and checking what {@link ZkTerminalClientImpl} makes of
 * them. It deliberately does *not* re-validate the outgoing checksum - see the comment on {@link
 * #readRequest} for the specific reason a naive recompute-and-compare doesn't apply to this
 * protocol.
 *
 * <p>Handles exactly the fixed sequence {@code ZkTerminalClientImpl#poll} always drives: CONNECT,
 * GET_FREE_SIZES, PREPARE_BUFFER (responding either with the data immediately, or PREPARE_DATA
 * followed by as many READ_BUFFER chunks as the script's chunk size demands, then FREE_DATA),
 * SET_TIME, EXIT. Records every command it receives and the raw bytes of the SET_TIME request, so
 * a test can assert on the exact sequence a poll sent.
 *
 * <p>A PREPARE_BUFFER request's own {@code fct} field (see {@code
 * ZkTerminalClientImpl#buildPrepareBufferPayload}) selects which log this server serves: {@code
 * CMD_ATTLOG_RRQ} always returns {@code attendanceRecords} (the constructor argument); {@code
 * CMD_ATTLOG_TIME_RRQ} (a windowed read) returns {@code rangedAttendanceRecords} if {@link
 * #withRangedRecords} configured one, or a bare {@code CMD_ACK_ERROR} otherwise - simulating
 * firmware that doesn't support the ranged command, to drive {@code
 * ZkTerminalClientImpl#readWindowedAttendanceLog}'s own fallback path. Either way, the encoded
 * start/end times sent are captured for a test to assert on.
 *
 * <p>{@link #withUnauthConnect} makes CMD_CONNECT answer CMD_ACK_UNAUTH instead of CMD_ACK_OK,
 * driving {@code ZkTerminalClientImpl#connect}'s comm-key handshake path - the client's CMD_AUTH
 * payload is captured via {@link #lastAuthData()} for a test to assert the exact scrambled bytes
 * against a hand-computed expectation, and the server's own answer to it (accept or reject) is
 * configurable via that same method's argument.
 */
class FakeZkTerminalServer implements AutoCloseable {

    private static final int USHRT_MAX = 65535;
    private static final int MACHINE_PREPARE_DATA_1 = 20560;
    private static final int MACHINE_PREPARE_DATA_2 = 32130;

    private static final int CMD_GET_FREE_SIZES = 50;
    private static final int CMD_SET_TIME = 202;
    private static final int CMD_CONNECT = 1000;
    private static final int CMD_EXIT = 1001;
    private static final int CMD_AUTH = 1102;
    private static final int CMD_ATTLOG_TIME_RRQ = 10004;
    private static final int CMD_PREPARE_DATA = 1500;
    private static final int CMD_DATA = 1501;
    private static final int CMD_FREE_DATA = 1502;
    private static final int CMD_PREPARE_BUFFER = 1503;
    private static final int CMD_READ_BUFFER = 1504;
    private static final int CMD_ACK_OK = 2000;
    private static final int CMD_ACK_ERROR = 2001;
    private static final int CMD_ACK_UNAUTH = 2005;

    private static final int SESSION_ID = 4242;

    private final ServerSocket serverSocket;
    private final byte[] attendanceRecords;
    private final int recordCount;
    private final boolean forceChunkedPath;
    private final List<Integer> receivedCommands = new ArrayList<>();
    private byte[] lastSetTimeData;
    private byte[] rangedAttendanceRecords;
    private Integer lastRangeStartEncoded;
    private Integer lastRangeEndEncoded;
    private byte[] pendingBlobRecords;
    private CompletableFuture<Void> serverTask;
    private boolean requireAuthHandshake;
    private boolean acceptAuthHandshake = true;
    private byte[] lastAuthData;

    /**
     * @param attendanceRecords the raw record bytes (post the 4-byte total-size header) to serve.
     * @param recordCount what CMD_GET_FREE_SIZES should claim, so the client can derive record size.
     * @param forceChunkedPath if true, respond to PREPARE_BUFFER with CMD_PREPARE_DATA (forcing the client's
     *     multi-chunk CMD_READ_BUFFER path) instead of returning the data immediately as CMD_DATA.
     */
    FakeZkTerminalServer(byte[] attendanceRecords, int recordCount, boolean forceChunkedPath) throws IOException {
        this.serverSocket = new ServerSocket(0);
        this.attendanceRecords = attendanceRecords;
        this.recordCount = recordCount;
        this.forceChunkedPath = forceChunkedPath;
    }

    /** Makes this server accept a windowed (CMD_ATTLOG_TIME_RRQ) read and serve these records for it, instead of rejecting it. */
    FakeZkTerminalServer withRangedRecords(byte[] rangedRecords) {
        this.rangedAttendanceRecords = rangedRecords;
        return this;
    }

    /**
     * Makes CMD_CONNECT answer CMD_ACK_UNAUTH instead of CMD_ACK_OK, driving
     * ZkTerminalClientImpl's comm-key handshake path. When {@code acceptHandshake} is true, the
     * server then answers the client's CMD_AUTH with CMD_ACK_OK regardless of payload (the payload
     * itself is captured for the test to assert on separately); when false, it answers CMD_AUTH
     * with CMD_ACK_ERROR, simulating a real non-zero comm-key rejecting the client's zero-key
     * attempt.
     */
    FakeZkTerminalServer withUnauthConnect(boolean acceptHandshake) {
        this.requireAuthHandshake = true;
        this.acceptAuthHandshake = acceptHandshake;
        return this;
    }

    int port() {
        return serverSocket.getLocalPort();
    }

    int sessionId() {
        return SESSION_ID;
    }

    byte[] lastAuthData() {
        return lastAuthData;
    }

    List<Integer> receivedCommands() {
        return List.copyOf(receivedCommands);
    }

    byte[] lastSetTimeData() {
        return lastSetTimeData;
    }

    Integer lastRangeStartEncoded() {
        return lastRangeStartEncoded;
    }

    Integer lastRangeEndEncoded() {
        return lastRangeEndEncoded;
    }

    void start() {
        serverTask = CompletableFuture.runAsync(this::serveOneConnection, Executors.newSingleThreadExecutor());
    }

    private void serveOneConnection() {
        try (Socket socket = serverSocket.accept()) {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            int sessionId = SESSION_ID;

            while (true) {
                Request request = readRequest(in);
                if (request == null) {
                    return; // client closed the connection
                }
                receivedCommands.add(request.command());

                switch (request.command()) {
                    case CMD_CONNECT -> writeResponse(
                            out, requireAuthHandshake ? CMD_ACK_UNAUTH : CMD_ACK_OK, sessionId, request.replyId(), new byte[0]);
                    case CMD_AUTH -> {
                        lastAuthData = request.data();
                        writeResponse(out, acceptAuthHandshake ? CMD_ACK_OK : CMD_ACK_ERROR, sessionId, request.replyId(), new byte[0]);
                    }
                    case CMD_GET_FREE_SIZES -> writeResponse(out, CMD_ACK_OK, sessionId, request.replyId(), freeSizesPayload());
                    case CMD_PREPARE_BUFFER -> handlePrepareBuffer(out, sessionId, request.replyId(), request.data());
                    case CMD_READ_BUFFER -> handleReadBuffer(out, sessionId, request.replyId(), request.data());
                    case CMD_FREE_DATA -> writeResponse(out, CMD_ACK_OK, sessionId, request.replyId(), new byte[0]);
                    case CMD_SET_TIME -> {
                        lastSetTimeData = request.data();
                        writeResponse(out, CMD_ACK_OK, sessionId, request.replyId(), new byte[0]);
                    }
                    case CMD_EXIT -> {
                        writeResponse(out, CMD_ACK_OK, sessionId, request.replyId(), new byte[0]);
                        return;
                    }
                    default -> {
                        return; // unexpected command - end the connection, the test will see a short read
                    }
                }
            }
        } catch (IOException e) {
            // Connection closed/reset once the client is done - not a failure of the fake server itself.
        }
    }

    private byte[] freeSizesPayload() {
        ByteBuffer buf = ByteBuffer.allocate(80).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < 20; i++) {
            buf.putInt(i == 8 ? recordCount : 0);
        }
        return buf.array();
    }

    /**
     * The blob a device sends is [4-byte "total_size"][record bytes...], and - confirmed by the
     * only way {@code ZkTerminalClientImpl#parseAttendanceRecords}' {@code totalSize / recordCount}
     * division can land on a clean 16/40/8 - that embedded "total_size" field describes the
     * record bytes alone, excluding its own 4 bytes. The *transfer* length (what CMD_PREPARE_DATA
     * declares, and what {@link #handleReadBuffer} paginates through) is the full blob including
     * that prefix - two different numbers, four bytes apart, easy to conflate (this fake server
     * did, the first time it was written - see the record-size assertions in
     * ZkTerminalClientImplTests, which is what caught it).
     */
    private void handlePrepareBuffer(OutputStream out, int sessionId, int replyId, byte[] requestData) throws IOException {
        ByteBuffer req = ByteBuffer.wrap(requestData).order(ByteOrder.LITTLE_ENDIAN);
        req.get(); // leading byte, always 1
        int fct = req.getShort() & 0xFFFF;
        int param1 = req.getInt();
        int param2 = req.getInt();

        if (fct == CMD_ATTLOG_TIME_RRQ) {
            lastRangeStartEncoded = param1;
            lastRangeEndEncoded = param2;
            if (rangedAttendanceRecords == null) {
                writeResponse(out, CMD_ACK_ERROR, sessionId, replyId, new byte[0]);
                return;
            }
            writeBufferedRecords(out, sessionId, replyId, rangedAttendanceRecords);
            return;
        }
        writeBufferedRecords(out, sessionId, replyId, attendanceRecords);
    }

    private void writeBufferedRecords(OutputStream out, int sessionId, int replyId, byte[] records) throws IOException {
        int transferLength = 4 + records.length;
        if (!forceChunkedPath) {
            ByteBuffer body = ByteBuffer.allocate(transferLength).order(ByteOrder.LITTLE_ENDIAN);
            body.putInt(records.length);
            body.put(records);
            writeResponse(out, CMD_DATA, sessionId, replyId, body.array());
        } else {
            pendingBlobRecords = records;
            ByteBuffer sizeOnly = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
            sizeOnly.putInt(transferLength);
            writeResponse(out, CMD_PREPARE_DATA, sessionId, replyId, sizeOnly.array());
        }
    }

    private void handleReadBuffer(OutputStream out, int sessionId, int replyId, byte[] requestData) throws IOException {
        byte[] records = pendingBlobRecords != null ? pendingBlobRecords : attendanceRecords;
        ByteBuffer req = ByteBuffer.wrap(requestData).order(ByteOrder.LITTLE_ENDIAN);
        int start = req.getInt();
        int size = req.getInt();
        ByteBuffer full = ByteBuffer.allocate(4 + records.length).order(ByteOrder.LITTLE_ENDIAN);
        full.putInt(records.length);
        full.put(records);
        byte[] fullBytes = full.array();
        byte[] chunk = new byte[Math.min(size, fullBytes.length - start)];
        System.arraycopy(fullBytes, start, chunk, 0, chunk.length);
        writeResponse(out, CMD_DATA, sessionId, replyId, chunk);
    }

    private record Request(int command, int replyId, byte[] data) {
    }

    private Request readRequest(InputStream in) throws IOException {
        byte[] tcpTop = readFully(in, 8);
        if (tcpTop == null) {
            return null;
        }
        ByteBuffer topBuf = ByteBuffer.wrap(tcpTop).order(ByteOrder.LITTLE_ENDIAN);
        int magic1 = topBuf.getShort() & 0xFFFF;
        int magic2 = topBuf.getShort() & 0xFFFF;
        int length = topBuf.getInt();
        if (magic1 != MACHINE_PREPARE_DATA_1 || magic2 != MACHINE_PREPARE_DATA_2) {
            throw new IOException("Bad TCP framing from client");
        }
        byte[] body = readFully(in, length);
        ByteBuffer bodyBuf = ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN);
        int command = bodyBuf.getShort() & 0xFFFF;
        bodyBuf.getShort(); // checksum - see the comment below on why this isn't re-validated
        int replyId = bodyBuf.getShort() & 0xFFFF;
        byte[] data = new byte[body.length - 8];
        System.arraycopy(body, 8, data, 0, data.length);

        // Deliberately not re-validating the checksum here: pyzk's own __create_header computes
        // it over the header with the *pre-increment* reply_id, then transmits the *incremented*
        // reply_id in that same field - the checksum a real device receives never actually
        // matches a naive recompute over the bytes as transmitted. ZkTerminalClientImpl ports that
        // exact sequence faithfully (see its own createHeader); a "recompute and compare" check
        // here would be testing this fake server's own guess at that quirk, not the client. The
        // checksum function itself (see checksum(byte[]) below, kept as a second, independently
        // transcribed copy) was verified by careful line-by-line comparison against pyzk's
        // __create_checksum instead - what this class actually cross-checks is the framing
        // (magic bytes, lengths, command sequencing) and the attendance-record parsing, which
        // don't depend on checksum validation either direction.
        return new Request(command, replyId, data);
    }

    private void writeResponse(OutputStream out, int command, int sessionId, int replyId, byte[] data) throws IOException {
        ByteBuffer header = ByteBuffer.allocate(8 + data.length).order(ByteOrder.LITTLE_ENDIAN);
        header.putShort((short) command);
        header.putShort((short) 0);
        header.putShort((short) sessionId);
        header.putShort((short) replyId);
        header.put(data);
        byte[] headerBytes = header.array();
        int checksum = checksum(headerBytes);
        ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN).putShort(2, (short) checksum);

        ByteBuffer wrapped = ByteBuffer.allocate(8 + headerBytes.length).order(ByteOrder.LITTLE_ENDIAN);
        wrapped.putShort((short) MACHINE_PREPARE_DATA_1);
        wrapped.putShort((short) MACHINE_PREPARE_DATA_2);
        wrapped.putInt(headerBytes.length);
        wrapped.put(headerBytes);
        out.write(wrapped.array());
        out.flush();
    }

    private static byte[] readFully(InputStream in, int length) throws IOException {
        byte[] buf = new byte[length];
        int total = 0;
        while (total < length) {
            int read = in.read(buf, total, length - total);
            if (read < 0) {
                return total == 0 ? null : Arrays.copyOf(buf, total);
            }
            total += read;
        }
        return buf;
    }

    /** Second, independent port of pyzk's __create_checksum - see this class's own javadoc for why it's not shared with ZkTerminalClientImpl. */
    private static int checksum(byte[] p) {
        long sum = 0;
        int i = 0;
        int remaining = p.length;
        while (remaining > 1) {
            int lo = p[i] & 0xFF;
            int hi = p[i + 1] & 0xFF;
            sum += lo | (hi << 8);
            if (sum > USHRT_MAX) {
                sum -= USHRT_MAX;
            }
            i += 2;
            remaining -= 2;
        }
        if (remaining > 0) {
            sum += p[i] & 0xFF;
        }
        while (sum > USHRT_MAX) {
            sum -= USHRT_MAX;
        }
        sum = ~sum;
        while (sum < 0) {
            sum += USHRT_MAX;
        }
        return (int) (sum & 0xFFFF);
    }

    @Override
    public void close() throws IOException {
        serverSocket.close();
        if (serverTask != null) {
            serverTask.cancel(true);
        }
    }
}
