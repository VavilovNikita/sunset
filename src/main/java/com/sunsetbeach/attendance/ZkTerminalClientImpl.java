package com.sunsetbeach.attendance;

import com.sunsetbeach.entity.AttendanceDeviceEntity;
import com.sunsetbeach.model.PunchDirection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * One path out of the ZKTeco wire protocol, for exactly one device model (K60, TCP/IP, port
 * 4370) - not a general multi-vendor client. Ported from the community's own best empirical
 * knowledge of this protocol (pyzk, MIT - github.com/fananimi/pyzk, and cross-checked against
 * zkteco4730-java and the PHP implementations the protocol's actually been reverse-engineered
 * against over years of real hardware, since ZKTeco itself has never published it), not
 * reimplemented from a spec - there isn't one. Only what a poll needs: connect, read the
 * attendance log, set the clock, disconnect.
 *
 * <p><b>Two things this class deliberately does NOT do:</b>
 * <ul>
 *   <li>Clear the device's log. There is no call anywhere in this class to {@code CMD_CLEAR_ATTLOG}
 *   (command 15) - some reference implementations expose it, this one doesn't even wire it up. The
 *   device is the only copy if {@link com.sunsetbeach.service.AttendanceService#ingestDevicePunch}
 *   ever has a bug, and it holds roughly two years of punches at this hotel's volume - there is no
 *   scenario where this system should be the reason that history disappears.</li>
 *   <li>Authenticate with a comm-key password ({@code CMD_AUTH}). If a device ever responds
 *   {@code CMD_ACK_UNAUTH}, {@link #poll} fails loudly with a clear message rather than attempting
 *   the password-scramble handshake pyzk implements - simpler to support once a real device
 *   actually needs it than to carry untested auth code for a case that may never come up.</li>
 * </ul>
 *
 * <p><b>Two things that could not be verified without the actual hardware</b> (there is no K60
 * to test against yet - see {@code AttendanceDevicePollServiceTests}' own fake-server tests for
 * what *is* covered without it):
 * <ul>
 *   <li>Which attendance-record layout the K60's own firmware sends - the protocol has at least
 *   three (8, 16, and 40 bytes per record; see {@link #parseAttendanceRecords}), and different
 *   firmware on the same model line has been observed sending different ones. This class detects
 *   the layout from the device's own {@code CMD_GET_FREE_SIZES} response (total bytes ÷ record
 *   count) rather than assuming one, and refuses - not guesses - a size matching none of the
 *   three known layouts.</li>
 *   <li>What the device's own per-record "punch" byte means. The convention used here (0 = check
 *   in, 1 = check out) is the one most ZK integrations report as the default when a terminal is
 *   in ordinary in/out mode, but it is a convention, not something this protocol documents
 *   anywhere - a device configured for break/overtime tracking uses more codes than that. Any
 *   code outside {0, 1} is refused, not guessed at as one or the other (see {@link
 *   #directionOf}) - <b>this needs confirming against the real K60 once it arrives</b>, most
 *   directly by punching it once in each direction and reading back what code each one produced.
 * </ul>
 *
 * <p><b>Windowed reads (a third thing unverified without hardware).</b> {@code
 * CMD_ATTLOG_TIME_RRQ} (10004) is not in pyzk (which only ever reads the whole log) but is real
 * enough to build on: it is the command a Java port of ZKTeco's own legacy {@code zkemkeeper}
 * Windows SDK exposes as {@code ReadTimeGLogData(dwMachineNumber, sTime, eTime)}, a documented
 * function name from ZKTeco's own official (if old) SDK, not something invented for that one
 * repository. It rides the exact same {@code CMD_PREPARE_BUFFER} envelope a full read does - see
 * {@link #readFullAttendanceLog} - with the command code and the two trailing int parameters
 * reinterpreted as start/end encoded times instead of {@code fct}/{@code ext}. What's genuinely
 * unverified is whether the K60's specific firmware honors it at all; {@link
 * #readWindowedAttendanceLog} treats anything other than an immediate-data or prepare-data
 * response to that command as "this firmware doesn't support it" and falls back to a full read in
 * the same call, never surfacing that as a poll failure - see that method's own javadoc. That
 * fallback is not silent past this class, though: {@link TerminalPollResult#windowedReadUnsupported}
 * carries the fact back to {@code AttendanceDevicePollService}, which persists it on the device row
 * - a K60 that turns out not to support the ranged command becomes a fact visible on the devices
 * screen, not a permanent, unremarked full-log read every five minutes.
 */
@Component
public class ZkTerminalClientImpl implements ZkTerminalClient {

    private static final Logger log = LoggerFactory.getLogger(ZkTerminalClientImpl.class);

    private static final int USHRT_MAX = 65535;

    private static final int CMD_ATTLOG_RRQ = 13;
    private static final int CMD_GET_FREE_SIZES = 50;
    private static final int CMD_SET_TIME = 202;
    private static final int CMD_CONNECT = 1000;
    private static final int CMD_EXIT = 1001;
    private static final int CMD_PREPARE_DATA = 1500;
    private static final int CMD_DATA = 1501;
    private static final int CMD_FREE_DATA = 1502;
    private static final int CMD_PREPARE_BUFFER = 1503;
    private static final int CMD_READ_BUFFER = 1504;
    /** ZKTeco's legacy zkemkeeper SDK calls this ReadTimeGLogData - see this class's own javadoc. */
    private static final int CMD_ATTLOG_TIME_RRQ = 10004;
    private static final int CMD_ACK_OK = 2000;
    private static final int CMD_ACK_UNAUTH = 2005;

    private static final int MACHINE_PREPARE_DATA_1 = 20560;
    private static final int MACHINE_PREPARE_DATA_2 = 32130;

    private static final int SOCKET_TIMEOUT_MS = 10_000;
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    /** Matches pyzk's own TCP chunk size (0xFFc0) - the largest single buffer read a device is asked for at once. */
    private static final int DEFAULT_MAX_CHUNK = 0xFFC0;
    /** Some firmware prepends this 8-byte sentinel before real 40-byte records - see parseAttendanceRecords. */
    private static final byte[] RECORD_40_SENTINEL = {(byte) 0xFF, '2', '5', '5', 0, 0, 0, 0};

    private final int maxChunk;

    public ZkTerminalClientImpl() {
        this(DEFAULT_MAX_CHUNK);
    }

    /** Package-private: lets a test force the multi-chunk CMD_READ_BUFFER path with a small fixture instead of needing tens of thousands of dummy records. */
    ZkTerminalClientImpl(int maxChunk) {
        this.maxChunk = maxChunk;
    }

    @Override
    public TerminalPollResult poll(AttendanceDeviceEntity device, LocalDateTime since, Integer knownRecordSize) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(device.getAddress(), device.getPort()), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);
            Session session = connect(socket);
            try {
                LocalDateTime now = LocalDateTime.now();
                int recordCount = readRecordCount(socket, session);
                TerminalPollResult result;
                if (recordCount == 0) {
                    result = new TerminalPollResult(List.of(), knownRecordSize, null);
                } else if (since == null) {
                    result = readFullAttendanceLog(socket, session, recordCount);
                } else {
                    if (knownRecordSize == null) {
                        throw new AttendanceDeviceException(
                                "A windowed read was requested without a previously-detected record size - this is a caller bug, "
                                        + "not something the terminal did");
                    }
                    result = readWindowedAttendanceLog(socket, session, since, now, knownRecordSize, recordCount, device.getName());
                }
                setClock(socket, session, now);
                return result;
            } finally {
                // Best-effort - a failed EXIT doesn't undo a successful read, and the socket close
                // (try-with-resources) releases the connection either way.
                try {
                    disconnect(socket, session);
                } catch (Exception e) {
                    log.debug("Disconnect from {} did not complete cleanly (harmless - closing the socket anyway)", device.getName(), e);
                }
            }
        } catch (IOException e) {
            throw new AttendanceDeviceException("Could not reach " + device.getName() + " at " + device.getAddress() + ":" + device.getPort(), e);
        }
    }

    private Session connect(Socket socket) throws IOException {
        Session session = new Session();
        Response response = sendCommand(socket, CMD_CONNECT, new byte[0], session);
        session.sessionId = response.sessionId();
        if (response.command() == CMD_ACK_UNAUTH) {
            throw new AttendanceDeviceException(
                    "This terminal requires a comm-key password - not supported (see ZkTerminalClientImpl's own javadoc)");
        }
        if (response.command() != CMD_ACK_OK) {
            throw new AttendanceDeviceException("Terminal rejected the connection (response code " + response.command() + ")");
        }
        return session;
    }

    private void disconnect(Socket socket, Session session) throws IOException {
        sendCommand(socket, CMD_EXIT, new byte[0], session);
    }

    /** CMD_GET_FREE_SIZES - a fixed-layout struct of 20 little-endian ints; the 9th (index 8) is the attendance record count. */
    private int readRecordCount(Socket socket, Session session) throws IOException {
        Response response = sendCommand(socket, CMD_GET_FREE_SIZES, new byte[0], session);
        requireOk(response, "read the device's own record counts");
        if (response.data().length < 80) {
            throw new AttendanceDeviceException("Unexpected CMD_GET_FREE_SIZES response (only " + response.data().length + " bytes)");
        }
        ByteBuffer buf = ByteBuffer.wrap(response.data()).order(ByteOrder.LITTLE_ENDIAN);
        int records = 0;
        for (int i = 0; i < 20; i++) {
            int value = buf.getInt();
            if (i == 8) {
                records = value;
            }
        }
        return records;
    }

    private void setClock(Socket socket, Session session, LocalDateTime now) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(encodeTime(now));
        Response response = sendCommand(socket, CMD_SET_TIME, buf.array(), session);
        requireOk(response, "set the device's clock");
    }

    /**
     * CMD_PREPARE_BUFFER + CMD_ATTLOG_RRQ, then either the data arrives immediately (small logs)
     * or the device says CMD_PREPARE_DATA and expects the client to pull it in chunks via
     * CMD_READ_BUFFER, released afterward with CMD_FREE_DATA - mirrors pyzk's read_with_buffer,
     * the community's own reverse-engineered protocol sequence for this. Unlike pyzk, each
     * individual chunk's own bytes are read here with a loop that blocks until the requested
     * length actually arrives (see {@link #readFully}) rather than trusting one {@code recv()}
     * call to return everything at once - that part is an ordinary TCP-correctness fix, not a
     * protocol difference.
     *
     * <p>The record layout size is detected here, not assumed or carried in from a previous poll
     * - this is the one read where {@code totalSize / recordCount} is guaranteed to land on a
     * clean 16/40/8, because both numbers describe the *same, complete* log. A windowed read's
     * returned {@code totalSize} describes only the filtered subset, so it can't be divided the
     * same way - see {@link #readWindowedAttendanceLog} and {@code AttendanceDeviceEntity
     * #attendanceRecordSize}'s own javadoc.
     */
    private TerminalPollResult readFullAttendanceLog(Socket socket, Session session, int recordCount) throws IOException {
        byte[] payload = buildPrepareBufferPayload(CMD_ATTLOG_RRQ, 0, 0);
        Response prepareResponse = sendCommand(socket, CMD_PREPARE_BUFFER, payload, session);
        if (prepareResponse.command() != CMD_DATA && prepareResponse.command() != CMD_PREPARE_DATA) {
            throw new AttendanceDeviceException("Terminal refused to read the attendance log (response code " + prepareResponse.command() + ")");
        }
        byte[] rawData = fetchBlob(socket, session, prepareResponse);
        if (rawData.length < 4) {
            return new TerminalPollResult(List.of(), null, null);
        }
        int totalSize = ByteBuffer.wrap(rawData, 0, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        byte[] records = Arrays.copyOfRange(rawData, 4, rawData.length);
        int recordSize = recordCount == 0 ? 0 : totalSize / recordCount;
        return new TerminalPollResult(parseAttendanceRecords(records, recordSize), recordSize, null);
    }

    /**
     * CMD_PREPARE_BUFFER with {@code CMD_ATTLOG_TIME_RRQ} in place of {@code CMD_ATTLOG_RRQ} and
     * the encoded {@code since}/{@code until} times where a full read sends {@code fct}/{@code
     * ext} - see this class's own javadoc for where that command comes from. Anything other than
     * an immediate-data or prepare-data response is treated as "this firmware doesn't support the
     * ranged command" - not a poll failure, and not thrown - and this method falls back to {@link
     * #readFullAttendanceLog} in the same call instead, which also re-detects the record size
     * fresh in case it had ever gone stale.
     */
    private TerminalPollResult readWindowedAttendanceLog(
            Socket socket, Session session, LocalDateTime since, LocalDateTime until, int knownRecordSize, int recordCount, String deviceName)
            throws IOException {
        byte[] payload = buildPrepareBufferPayload(CMD_ATTLOG_TIME_RRQ, encodeTime(since), encodeTime(until));
        Response prepareResponse = sendCommand(socket, CMD_PREPARE_BUFFER, payload, session);
        if (prepareResponse.command() != CMD_DATA && prepareResponse.command() != CMD_PREPARE_DATA) {
            log.info(
                    "{} did not accept a ranged attendance-log read (response code {}) - reading the full log this poll instead", deviceName,
                    prepareResponse.command());
            TerminalPollResult fallback = readFullAttendanceLog(socket, session, recordCount);
            return new TerminalPollResult(fallback.punches(), fallback.recordSize(), true);
        }
        byte[] rawData = fetchBlob(socket, session, prepareResponse);
        if (rawData.length < 4) {
            return new TerminalPollResult(List.of(), knownRecordSize, false);
        }
        byte[] records = Arrays.copyOfRange(rawData, 4, rawData.length);
        return new TerminalPollResult(parseAttendanceRecords(records, knownRecordSize), knownRecordSize, false);
    }

    private byte[] fetchBlob(Socket socket, Session session, Response prepareResponse) throws IOException {
        if (prepareResponse.command() == CMD_DATA) {
            return prepareResponse.data();
        }
        if (prepareResponse.data().length < 4) {
            throw new AttendanceDeviceException("Unexpected CMD_PREPARE_DATA response reading the attendance log");
        }
        int totalSize = ByteBuffer.wrap(prepareResponse.data()).order(ByteOrder.LITTLE_ENDIAN).getInt();
        return readBuffered(socket, session, totalSize);
    }

    private static byte[] buildPrepareBufferPayload(int command, int param1, int param2) {
        ByteBuffer buf = ByteBuffer.allocate(11).order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 1);
        buf.putShort((short) command);
        buf.putInt(param1);
        buf.putInt(param2);
        return buf.array();
    }

    private byte[] readBuffered(Socket socket, Session session, int totalSize) throws IOException {
        List<byte[]> chunks = new ArrayList<>();
        int start = 0;
        int remaining = totalSize;
        while (remaining > 0) {
            int chunkSize = Math.min(remaining, maxChunk);
            ByteBuffer request = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            request.putInt(start);
            request.putInt(chunkSize);
            Response response = sendCommand(socket, CMD_READ_BUFFER, request.array(), session);
            if (response.command() != CMD_DATA) {
                throw new AttendanceDeviceException("Terminal refused a chunk read at offset " + start + " (response code " + response.command() + ")");
            }
            chunks.add(response.data());
            start += chunkSize;
            remaining -= chunkSize;
        }
        sendCommand(socket, CMD_FREE_DATA, new byte[0], session);

        int size = chunks.stream().mapToInt(c -> c.length).sum();
        ByteBuffer combined = ByteBuffer.allocate(size);
        chunks.forEach(combined::put);
        return combined.array();
    }

    /**
     * The protocol has at least three attendance-record layouts (8, 16, or 40 bytes), which one a
     * given device sends is firmware-dependent and not knowable without asking it - detected here
     * from {@code totalSize / recordCount} the same way pyzk does, rather than assumed. A size
     * matching none of the three is refused, not parsed as the closest guess - see this class's
     * own javadoc for why that specifically needs confirming against the real K60.
     */
    private List<RawAttendancePunch> parseAttendanceRecords(byte[] data, int recordSize) {
        List<RawAttendancePunch> punches = new ArrayList<>();
        if (recordSize == 16) {
            ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            while (buf.remaining() >= 16) {
                long userId = buf.getInt() & 0xFFFFFFFFL;
                byte[] timeBytes = new byte[4];
                buf.get(timeBytes);
                int status = buf.get() & 0xFF;
                int punch = buf.get() & 0xFF;
                buf.getShort(); // reserved
                buf.getInt(); // workcode
                addIfMappable(punches, userId, timeBytes, punch, status);
            }
        } else if (recordSize == 40) {
            int offset = 0;
            if (data.length - offset >= RECORD_40_SENTINEL.length && startsWith(data, offset, RECORD_40_SENTINEL)) {
                offset += RECORD_40_SENTINEL.length;
            }
            ByteBuffer buf = ByteBuffer.wrap(data, offset, data.length - offset).order(ByteOrder.LITTLE_ENDIAN).slice();
            while (buf.remaining() >= 40) {
                buf.getShort(); // uid (machine-internal, not the enrollment number)
                byte[] userIdBytes = new byte[24];
                buf.get(userIdBytes);
                int status = buf.get() & 0xFF;
                byte[] timeBytes = new byte[4];
                buf.get(timeBytes);
                int punch = buf.get() & 0xFF;
                byte[] reserved = new byte[8];
                buf.get(reserved);
                String userIdString = new String(userIdBytes, StandardCharsets.US_ASCII).split(" ", 2)[0].trim();
                addIfMappable(punches, userIdString, timeBytes, punch, status);
            }
        } else if (recordSize == 8) {
            throw new AttendanceDeviceException(
                    "This terminal is sending the oldest (8-byte) attendance record format, which carries a machine-internal id instead of "
                            + "the enrollment number directly and isn't supported - see ZkTerminalClientImpl's own javadoc");
        } else {
            throw new AttendanceDeviceException("Unrecognised attendance record size (" + recordSize + " bytes) - refusing to guess a layout");
        }
        return punches;
    }

    private void addIfMappable(List<RawAttendancePunch> punches, long userId, byte[] timeBytes, int punchCode, int status) {
        addIfMappable(punches, String.valueOf(userId), timeBytes, punchCode, status);
    }

    private void addIfMappable(List<RawAttendancePunch> punches, String userIdString, byte[] timeBytes, int punchCode, int status) {
        Integer enrollmentNumber = parseEnrollmentNumber(userIdString);
        if (enrollmentNumber == null) {
            log.warn("Skipping an attendance record with a non-numeric enrollment number \"{}\"", userIdString);
            return;
        }
        PunchDirection direction = directionOf(punchCode);
        if (direction == null) {
            log.warn("Skipping an attendance record for enrollment number {} with an unrecognised punch code {} (status {})", enrollmentNumber,
                    punchCode, status);
            return;
        }
        punches.add(new RawAttendancePunch(enrollmentNumber, decodeTime(timeBytes), direction));
    }

    private static Integer parseEnrollmentNumber(String userIdString) {
        try {
            return Integer.valueOf(userIdString.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 0 = check in, 1 = check out - the common default-mode convention, not something this
     * protocol documents. Anything else (break/overtime codes some configurations use) is refused
     * rather than mapped to one of the two directions this system has - see this class's own
     * javadoc.
     */
    private static PunchDirection directionOf(int punchCode) {
        return switch (punchCode) {
            case 0 -> PunchDirection.IN;
            case 1 -> PunchDirection.OUT;
            default -> null;
        };
    }

    private static boolean startsWith(byte[] data, int offset, byte[] prefix) {
        for (int i = 0; i < prefix.length; i++) {
            if (data[offset + i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    /** Copied from zkemsdk.c's own DecodeTime, via pyzk - the device packs a timestamp into one little-endian u32. */
    private static LocalDateTime decodeTime(byte[] timeBytes) {
        long t = ByteBuffer.wrap(timeBytes).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFFFFL;
        int second = (int) (t % 60);
        t /= 60;
        int minute = (int) (t % 60);
        t /= 60;
        int hour = (int) (t % 24);
        t /= 24;
        int day = (int) (t % 31) + 1;
        t /= 31;
        int month = (int) (t % 12) + 1;
        t /= 12;
        int year = (int) t + 2000;
        return LocalDateTime.of(year, month, day, hour, minute, second);
    }

    /** The inverse of decodeTime - copied from zkemsdk.c's own EncodeTime, via pyzk. */
    private static int encodeTime(LocalDateTime t) {
        return (((t.getYear() % 100) * 12 * 31 + ((t.getMonthValue() - 1) * 31) + t.getDayOfMonth() - 1) * (24 * 60 * 60)
                + (t.getHour() * 60 + t.getMinute()) * 60 + t.getSecond());
    }

    private static void requireOk(Response response, String action) {
        if (response.command() != CMD_ACK_OK) {
            throw new AttendanceDeviceException("Terminal refused to " + action + " (response code " + response.command() + ")");
        }
    }

    // --- Packet framing (TCP top header, the 8-byte command header, and its checksum) ---
    // See this class's own javadoc: ported from pyzk's ZK class (__create_header/__create_checksum/
    // __create_tcp_top/__send_command), the community's own reverse-engineering of zkemsdk.c.

    private Response sendCommand(Socket socket, int command, byte[] commandData, Session session) throws IOException {
        byte[] header = createHeader(command, commandData, session);
        byte[] wrapped = wrapTcp(header);
        OutputStream out = socket.getOutputStream();
        out.write(wrapped);
        out.flush();

        InputStream in = socket.getInputStream();
        byte[] tcpTop = readFully(in, 8);
        ByteBuffer topBuf = ByteBuffer.wrap(tcpTop).order(ByteOrder.LITTLE_ENDIAN);
        int magic1 = topBuf.getShort() & 0xFFFF;
        int magic2 = topBuf.getShort() & 0xFFFF;
        int length = topBuf.getInt();
        if (magic1 != MACHINE_PREPARE_DATA_1 || magic2 != MACHINE_PREPARE_DATA_2 || length < 8) {
            throw new AttendanceDeviceException("Malformed response from terminal (bad TCP framing)");
        }
        byte[] body = readFully(in, length);
        ByteBuffer bodyBuf = ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN);
        int responseCommand = bodyBuf.getShort() & 0xFFFF;
        bodyBuf.getShort(); // checksum - not verified on responses (pyzk doesn't either)
        int sessionId = bodyBuf.getShort() & 0xFFFF;
        int replyId = bodyBuf.getShort() & 0xFFFF;
        session.replyId = replyId;
        byte[] data = Arrays.copyOfRange(body, 8, body.length);
        return new Response(responseCommand, sessionId, data);
    }

    private static byte[] readFully(InputStream in, int length) throws IOException {
        byte[] buf = new byte[length];
        int total = 0;
        while (total < length) {
            int read = in.read(buf, total, length - total);
            if (read < 0) {
                throw new IOException("Connection closed after " + total + " of " + length + " expected bytes");
            }
            total += read;
        }
        return buf;
    }

    private static byte[] wrapTcp(byte[] packet) {
        ByteBuffer result = ByteBuffer.allocate(8 + packet.length).order(ByteOrder.LITTLE_ENDIAN);
        result.putShort((short) MACHINE_PREPARE_DATA_1);
        result.putShort((short) MACHINE_PREPARE_DATA_2);
        result.putInt(packet.length);
        result.put(packet);
        return result.array();
    }

    /** command(u16) + checksum(u16, computed below) + sessionId(u16) + replyId(u16) + commandData - all little-endian. */
    private static byte[] createHeader(int command, byte[] commandData, Session session) {
        ByteBuffer withZeroChecksum = ByteBuffer.allocate(8 + commandData.length).order(ByteOrder.LITTLE_ENDIAN);
        withZeroChecksum.putShort((short) command);
        withZeroChecksum.putShort((short) 0);
        withZeroChecksum.putShort((short) session.sessionId);
        withZeroChecksum.putShort((short) session.replyId);
        withZeroChecksum.put(commandData);
        int checksum = checksum(withZeroChecksum.array());

        session.replyId++;
        if (session.replyId >= USHRT_MAX) {
            session.replyId -= USHRT_MAX;
        }

        ByteBuffer packet = ByteBuffer.allocate(8 + commandData.length).order(ByteOrder.LITTLE_ENDIAN);
        packet.putShort((short) command);
        packet.putShort((short) checksum);
        packet.putShort((short) session.sessionId);
        packet.putShort((short) session.replyId);
        packet.put(commandData);
        return packet.array();
    }

    /**
     * The ones-complement-style checksum zkemsdk.c itself uses (copied via pyzk's own
     * __create_checksum) - the "> USHRT_MAX, not >= 65536" arithmetic looks like an off-by-one
     * against a true 16-bit wraparound; it isn't a bug to fix here, it's what real terminals
     * expect.
     */
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

    private static final class Session {
        int sessionId;
        int replyId = USHRT_MAX - 1;
    }

    private record Response(int command, int sessionId, byte[] data) {
    }
}
