package com.sunsetbeach.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.AttendanceDeviceEntity;
import com.sunsetbeach.model.PunchDirection;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link ZkTerminalClientImpl} against a real local socket ({@link FakeZkTerminalServer}) - the
 * actual "seam" test for the wire protocol itself, as opposed to {@link
 * com.sunsetbeach.service.AttendanceDevicePollServiceTests}, which tests everything *above* the
 * client against {@link FakeZkTerminalClient} instead. This is where a transcription mistake in
 * the checksum, packet framing, or record parsing would actually be caught before real hardware
 * exists to catch it.
 */
class ZkTerminalClientImplTests {

    private static AttendanceDeviceEntity deviceAt(int port) {
        AttendanceDeviceEntity device = new AttendanceDeviceEntity();
        device.setName("Test Terminal");
        device.setAddress("127.0.0.1");
        device.setPort(port);
        device.setTimezone("Asia/Bangkok");
        return device;
    }

    /** Mirrors zkemsdk.c's own EncodeTime (via pyzk) - a second, independent copy for building test fixtures, not shared with the production code it's testing. */
    private static byte[] encodeTime(LocalDateTime t) {
        int encoded = ((t.getYear() % 100) * 12 * 31 + ((t.getMonthValue() - 1) * 31) + t.getDayOfMonth() - 1) * (24 * 60 * 60)
                + (t.getHour() * 60 + t.getMinute()) * 60 + t.getSecond();
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(encoded).array();
    }

    private static int encodeTimeAsInt(LocalDateTime t) {
        return ByteBuffer.wrap(encodeTime(t)).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    /**
     * Second, independent port of pyzk's own {@code make_commkey} - not shared with {@code
     * ZkTerminalClientImpl#makeCommKey}, the production code this exists to check. Transcribed
     * separately from the same pyzk source (zk/base.py) so a transcription mistake in the
     * production port has an independent implementation to be caught against, the same reasoning
     * behind this file's own independent {@link #encodeTime} copy.
     */
    private static byte[] expectedCommKey(long key, int sessionId, int ticks) {
        long k = 0;
        for (int i = 0; i < 32; i++) {
            k = (k << 1) | ((key >> i) & 1);
        }
        k = (k + sessionId) & 0xFFFFFFFFL;

        int b0 = (int) (k & 0xFF) ^ 'Z';
        int b1 = (int) ((k >> 8) & 0xFF) ^ 'K';
        int b2 = (int) ((k >> 16) & 0xFF) ^ 'S';
        int b3 = (int) ((k >> 24) & 0xFF) ^ 'O';

        int h0 = (b1 << 8) | b0;
        int h1 = (b3 << 8) | b2;

        int b = ticks & 0xFF;
        return new byte[] {(byte) ((h1 & 0xFF) ^ b), (byte) (((h1 >> 8) & 0xFF) ^ b), (byte) b, (byte) (((h0 >> 8) & 0xFF) ^ b)};
    }

    private static byte[] sixteenByteRecord(int enrollmentNumber, LocalDateTime timestamp, int punchCode) {
        ByteBuffer buf = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(enrollmentNumber);
        buf.put(encodeTime(timestamp));
        buf.put((byte) 1); // status (verification method) - irrelevant to parsing
        buf.put((byte) punchCode);
        buf.putShort((short) 0); // reserved
        buf.putInt(0); // workcode
        return buf.array();
    }

    private static byte[] fortyByteRecord(String enrollmentNumber, LocalDateTime timestamp, int punchCode) {
        ByteBuffer buf = ByteBuffer.allocate(40).order(ByteOrder.LITTLE_ENDIAN);
        buf.putShort((short) 1); // uid (machine-internal, ignored by the client)
        byte[] userIdField = new byte[24];
        byte[] userIdBytes = enrollmentNumber.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(userIdBytes, 0, userIdField, 0, userIdBytes.length);
        buf.put(userIdField);
        buf.put((byte) 1); // status
        buf.put(encodeTime(timestamp));
        buf.put((byte) punchCode);
        buf.put(new byte[8]); // reserved/space
        return buf.array();
    }

    @Test
    void poll_smallSixteenByteLog_parsesEnrollmentNumberTimestampAndDirection() throws Exception {
        LocalDateTime timestamp = LocalDateTime.of(2027, 8, 15, 9, 30, 0);
        byte[] records = sixteenByteRecord(123, timestamp, 0);
        try (FakeZkTerminalServer server = new FakeZkTerminalServer(records, 1, false)) {
            server.start();
            TerminalPollResult result = new ZkTerminalClientImpl().poll(deviceAt(server.port()), null, null);

            assertThat(result.punches()).hasSize(1);
            assertThat(result.punches().get(0).enrollmentNumber()).isEqualTo(123);
            assertThat(result.punches().get(0).deviceTimestamp()).isEqualTo(timestamp);
            assertThat(result.punches().get(0).direction()).isEqualTo(PunchDirection.IN);
            assertThat(result.recordSize()).isEqualTo(16);
            assertThat(server.receivedCommands()).containsExactly(1000, 50, 1503, 202, 1001);
        }
    }

    @Test
    void poll_fortyByteLog_withCheckOutCode_parsesDirectionAsOut() throws Exception {
        LocalDateTime timestamp = LocalDateTime.of(2027, 8, 15, 18, 0, 0);
        byte[] records = fortyByteRecord("456", timestamp, 1);
        try (FakeZkTerminalServer server = new FakeZkTerminalServer(records, 1, false)) {
            server.start();
            TerminalPollResult result = new ZkTerminalClientImpl().poll(deviceAt(server.port()), null, null);

            assertThat(result.punches()).hasSize(1);
            assertThat(result.punches().get(0).enrollmentNumber()).isEqualTo(456);
            assertThat(result.punches().get(0).direction()).isEqualTo(PunchDirection.OUT);
            assertThat(result.recordSize()).isEqualTo(40);
        }
    }

    @Test
    void poll_multipleRecords_returnsAllOfThem() throws Exception {
        LocalDateTime in = LocalDateTime.of(2027, 8, 16, 9, 0, 0);
        LocalDateTime out = LocalDateTime.of(2027, 8, 16, 17, 0, 0);
        byte[] records = concat(sixteenByteRecord(1, in, 0), sixteenByteRecord(1, out, 1));
        try (FakeZkTerminalServer server = new FakeZkTerminalServer(records, 2, false)) {
            server.start();
            List<RawAttendancePunch> punches = new ZkTerminalClientImpl().poll(deviceAt(server.port()), null, null).punches();

            assertThat(punches).hasSize(2);
            assertThat(punches.get(0).direction()).isEqualTo(PunchDirection.IN);
            assertThat(punches.get(1).direction()).isEqualTo(PunchDirection.OUT);
        }
    }

    /**
     * An unrecognised punch code (not 0 or 1) gets no direction guess, but the record itself is
     * still kept - direction is decided server-side from punch history regardless, so dropping the
     * record here would silently lose a real scan. See ZkTerminalClientImpl's own javadoc.
     */
    @Test
    void poll_unrecognisedPunchCode_isIncludedWithNullDirection() throws Exception {
        LocalDateTime timestamp = LocalDateTime.of(2027, 8, 17, 9, 0, 0);
        byte[] records = concat(sixteenByteRecord(1, timestamp, 4), sixteenByteRecord(2, timestamp, 0));
        try (FakeZkTerminalServer server = new FakeZkTerminalServer(records, 2, false)) {
            server.start();
            List<RawAttendancePunch> punches = new ZkTerminalClientImpl().poll(deviceAt(server.port()), null, null).punches();

            assertThat(punches).hasSize(2);
            assertThat(punches.get(0).enrollmentNumber()).isEqualTo(1);
            assertThat(punches.get(0).direction()).isNull();
            assertThat(punches.get(1).enrollmentNumber()).isEqualTo(2);
            assertThat(punches.get(1).direction()).isEqualTo(PunchDirection.IN);
        }
    }

    /** A non-numeric user id (some other data shape entirely) is refused, not guessed at. */
    @Test
    void poll_nonNumericEnrollmentNumber_skipsThatRecord() throws Exception {
        LocalDateTime timestamp = LocalDateTime.of(2027, 8, 18, 9, 0, 0);
        byte[] records = fortyByteRecord("not-a-number", timestamp, 0);
        try (FakeZkTerminalServer server = new FakeZkTerminalServer(records, 1, false)) {
            server.start();
            List<RawAttendancePunch> punches = new ZkTerminalClientImpl().poll(deviceAt(server.port()), null, null).punches();

            assertThat(punches).isEmpty();
        }
    }

    @Test
    void poll_unrecognisedRecordSize_refusesToGuess() throws Exception {
        byte[] garbage = new byte[24]; // matches none of the three known layouts (8/16/40)
        try (FakeZkTerminalServer server = new FakeZkTerminalServer(garbage, 1, false)) {
            server.start();
            assertThatThrownBy(() -> new ZkTerminalClientImpl().poll(deviceAt(server.port()), null, null))
                    .isInstanceOf(AttendanceDeviceException.class)
                    .hasMessageContaining("Unrecognised attendance record size");
        }
    }

    /** Forces CMD_PREPARE_DATA + multiple CMD_READ_BUFFER round trips instead of one immediate CMD_DATA - the path a real backlog after a device outage would take. */
    @Test
    void poll_forcedChunkedPath_stillReturnsEveryRecord() throws Exception {
        LocalDateTime t1 = LocalDateTime.of(2027, 8, 19, 9, 0, 0);
        LocalDateTime t2 = LocalDateTime.of(2027, 8, 19, 13, 0, 0);
        LocalDateTime t3 = LocalDateTime.of(2027, 8, 19, 16, 0, 0);
        byte[] records = concat(sixteenByteRecord(1, t1, 0), sixteenByteRecord(1, t2, 1), sixteenByteRecord(1, t3, 0));
        try (FakeZkTerminalServer server = new FakeZkTerminalServer(records, 3, true)) {
            server.start();
            // A tiny max chunk forces several CMD_READ_BUFFER round trips for 3*16=48 bytes of records.
            List<RawAttendancePunch> punches = new ZkTerminalClientImpl(10).poll(deviceAt(server.port()), null, null).punches();

            assertThat(punches).hasSize(3);
            assertThat(punches.get(2).deviceTimestamp()).isEqualTo(t3);
            assertThat(server.receivedCommands()).contains(1503, 1504, 1502);
            assertThat(server.receivedCommands().stream().filter(c -> c == 1504).count()).isGreaterThan(1);
        }
    }

    @Test
    void poll_setsTheDevicesClockToNow() throws Exception {
        byte[] records = new byte[0];
        try (FakeZkTerminalServer server = new FakeZkTerminalServer(records, 0, false)) {
            server.start();
            LocalDateTime before = LocalDateTime.now().minusMinutes(1);

            new ZkTerminalClientImpl().poll(deviceAt(server.port()), null, null);

            assertThat(server.lastSetTimeData()).isNotNull();
            int encoded = ByteBuffer.wrap(server.lastSetTimeData()).order(ByteOrder.LITTLE_ENDIAN).getInt();
            // Decode it back the same way the device would, and confirm it lands after "before" -
            // proving the client actually sent something close to "now", not a stale or fixed value.
            LocalDateTime decoded = decodeTime(encoded);
            assertThat(decoded).isAfterOrEqualTo(before);
        }
    }

    @Test
    void poll_noRecordsAtAll_returnsEmptyWithoutReadingTheLog() throws Exception {
        try (FakeZkTerminalServer server = new FakeZkTerminalServer(new byte[0], 0, false)) {
            server.start();
            TerminalPollResult result = new ZkTerminalClientImpl().poll(deviceAt(server.port()), null, null);

            assertThat(result.punches()).isEmpty();
            // recordCount 0 - the client should skip straight past PREPARE_BUFFER/ATTLOG entirely.
            assertThat(server.receivedCommands()).containsExactly(1000, 50, 202, 1001);
        }
    }

    @Test
    void poll_deviceUnreachable_throwsAttendanceDeviceException() {
        AttendanceDeviceEntity device = deviceAt(1); // nothing listens on port 1
        device.setAddress("127.0.0.1");

        assertThatThrownBy(() -> new ZkTerminalClientImpl().poll(device, null, null)).isInstanceOf(AttendanceDeviceException.class);
    }

    /** A device that never answers CMD_ACK_UNAUTH never sees a CMD_AUTH - the ordinary connect path is unaffected. */
    @Test
    void poll_deviceNeverRequiresAuth_neverSendsCommKeyHandshake() throws Exception {
        try (FakeZkTerminalServer server = new FakeZkTerminalServer(new byte[0], 0, false)) {
            server.start();
            new ZkTerminalClientImpl().poll(deviceAt(server.port()), null, null);

            assertThat(server.receivedCommands()).doesNotContain(1102);
        }
    }

    /**
     * The comm-key handshake path found live against a real K60: CMD_CONNECT answers
     * CMD_ACK_UNAUTH, the client replies with CMD_AUTH carrying the zero-key comm-key scramble,
     * and the device accepting it lets the poll proceed normally. Asserts the exact bytes sent,
     * not just that some CMD_AUTH was sent - see {@link #expectedCommKey} for why a loose
     * assertion wouldn't catch a subtly wrong byte-order or XOR-constant mistake.
     */
    @Test
    void poll_deviceRequiresAuth_sendsCorrectCommKeyAndSucceeds() throws Exception {
        LocalDateTime timestamp = LocalDateTime.of(2027, 8, 21, 9, 0, 0);
        byte[] records = sixteenByteRecord(1, timestamp, 0);
        try (FakeZkTerminalServer server = new FakeZkTerminalServer(records, 1, false).withUnauthConnect(true)) {
            server.start();
            TerminalPollResult result = new ZkTerminalClientImpl().poll(deviceAt(server.port()), null, null);

            assertThat(result.punches()).hasSize(1);
            assertThat(server.receivedCommands()).containsExactly(1000, 1102, 50, 1503, 202, 1001);
            assertThat(server.lastAuthData()).isEqualTo(expectedCommKey(0, server.sessionId(), 50));
        }
    }

    /**
     * A device that rejects the zero-key CMD_AUTH (a real non-zero comm-key configured on the
     * terminal) must fail with a message distinct from the old "not supported at all" wording -
     * this is now a genuine auth rejection, not an unimplemented feature.
     */
    @Test
    void poll_deviceRejectsAuth_throwsDistinctRejectionMessage() throws Exception {
        try (FakeZkTerminalServer server = new FakeZkTerminalServer(new byte[0], 0, false).withUnauthConnect(false)) {
            server.start();

            assertThatThrownBy(() -> new ZkTerminalClientImpl().poll(deviceAt(server.port()), null, null))
                    .isInstanceOf(AttendanceDeviceException.class)
                    .hasMessageContaining("rejected authentication")
                    .hasMessageContaining("non-zero comm-key");
        }
    }

    /**
     * The heart of the windowed-read feature: when a device honors CMD_ATTLOG_TIME_RRQ, the
     * client sends the encoded since/until times in the PREPARE_BUFFER payload and parses the
     * response using the *passed-in* record size, not one re-derived from CMD_GET_FREE_SIZES'
     * total count (which would be wrong for a subset - see ZkTerminalClientImpl's own javadoc).
     * The full-log record set is deliberately different from the ranged one, so a passing test
     * proves the ranged path actually ran, not that it silently fell back to the full log.
     */
    @Test
    void poll_windowedRead_sendsRangedCommandAndParsesWithKnownRecordSize() throws Exception {
        LocalDateTime fullLogPunch = LocalDateTime.of(2027, 8, 1, 9, 0, 0);
        LocalDateTime windowedPunch = LocalDateTime.of(2027, 8, 20, 14, 30, 0);
        byte[] fullLogRecords = sixteenByteRecord(1, fullLogPunch, 0);
        byte[] rangedRecords = sixteenByteRecord(2, windowedPunch, 1);
        LocalDateTime since = LocalDateTime.of(2027, 8, 20, 8, 0, 0);

        try (FakeZkTerminalServer server = new FakeZkTerminalServer(fullLogRecords, 1, false).withRangedRecords(rangedRecords)) {
            server.start();
            TerminalPollResult result = new ZkTerminalClientImpl().poll(deviceAt(server.port()), since, 16);

            assertThat(result.punches()).hasSize(1);
            assertThat(result.punches().get(0).enrollmentNumber()).isEqualTo(2);
            assertThat(result.punches().get(0).deviceTimestamp()).isEqualTo(windowedPunch);
            assertThat(result.punches().get(0).direction()).isEqualTo(PunchDirection.OUT);
            assertThat(result.recordSize()).isEqualTo(16);
            assertThat(server.lastRangeStartEncoded()).isEqualTo(encodeTimeAsInt(since));
            // The device honored the ranged command - this poll actually found out it's supported.
            assertThat(result.windowedReadUnsupported()).isFalse();
        }
    }

    /**
     * A device that doesn't honor the ranged command (older/different firmware) rejects it with
     * an error response rather than data - ZkTerminalClientImpl treats that as "unsupported", not
     * a poll failure, and reads the full log instead within the same call.
     */
    @Test
    void poll_windowedRead_deviceRejectsRangedCommand_fallsBackToFullRead() throws Exception {
        LocalDateTime fullLogPunch = LocalDateTime.of(2027, 8, 1, 9, 0, 0);
        byte[] fullLogRecords = sixteenByteRecord(9, fullLogPunch, 0);
        LocalDateTime since = LocalDateTime.of(2027, 8, 20, 8, 0, 0);

        // No .withRangedRecords(...) - the server rejects CMD_ATTLOG_TIME_RRQ with CMD_ACK_ERROR.
        try (FakeZkTerminalServer server = new FakeZkTerminalServer(fullLogRecords, 1, false)) {
            server.start();
            TerminalPollResult result = new ZkTerminalClientImpl().poll(deviceAt(server.port()), since, 16);

            assertThat(result.punches()).hasSize(1);
            assertThat(result.punches().get(0).enrollmentNumber()).isEqualTo(9);
            assertThat(result.punches().get(0).deviceTimestamp()).isEqualTo(fullLogPunch);
            // Freshly re-detected from the full read, not just echoed back.
            assertThat(result.recordSize()).isEqualTo(16);
            // The whole point of this test: the rejection must be visible on the result, not just
            // silently absorbed by the fallback - this is what AttendanceDevicePollService
            // persists onto the device row so the degradation shows up on the devices screen.
            assertThat(result.windowedReadUnsupported()).isTrue();
        }
    }

    @Test
    void poll_windowedRead_noRecordsInWindow_returnsEmptyWithKnownRecordSizeEchoedBack() throws Exception {
        byte[] fullLogRecords = sixteenByteRecord(1, LocalDateTime.of(2027, 8, 1, 9, 0, 0), 0);
        LocalDateTime since = LocalDateTime.of(2027, 8, 20, 8, 0, 0);

        try (FakeZkTerminalServer server = new FakeZkTerminalServer(fullLogRecords, 1, false).withRangedRecords(new byte[0])) {
            server.start();
            TerminalPollResult result = new ZkTerminalClientImpl().poll(deviceAt(server.port()), since, 40);

            assertThat(result.punches()).isEmpty();
            assertThat(result.recordSize()).isEqualTo(40);
            assertThat(result.windowedReadUnsupported()).isFalse();
        }
    }

    /**
     * A full read taken because there's no watermark yet (since=null) never attempted the
     * windowed command at all, so it must leave windowedReadUnsupported null ("didn't test it") -
     * never false, which would wrongly claim the device is known to support windowed reads.
     */
    @Test
    void poll_fullReadWithNoSince_leavesWindowedReadUnsupportedNull() throws Exception {
        byte[] records = sixteenByteRecord(1, LocalDateTime.of(2027, 8, 1, 9, 0, 0), 0);

        try (FakeZkTerminalServer server = new FakeZkTerminalServer(records, 1, false)) {
            server.start();
            TerminalPollResult result = new ZkTerminalClientImpl().poll(deviceAt(server.port()), null, null);

            assertThat(result.windowedReadUnsupported()).isNull();
        }
    }

    private static LocalDateTime decodeTime(int encoded) {
        long t = encoded & 0xFFFFFFFFL;
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

    private static byte[] concat(byte[]... arrays) {
        int total = 0;
        for (byte[] a : arrays) {
            total += a.length;
        }
        byte[] result = new byte[total];
        int offset = 0;
        for (byte[] a : arrays) {
            System.arraycopy(a, 0, result, offset, a.length);
            offset += a.length;
        }
        return result;
    }
}
