package com.sunsetbeach.attendance;

import com.sunsetbeach.entity.AttendanceDeviceEntity;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The fake device {@code ZkTerminalClient}'s own javadoc promises: {@code
 * AttendanceDevicePollService} is built and tested entirely against this, with no real socket or
 * hardware anywhere. Each device gets its own queue of canned outcomes (a punch list, or an
 * exception to throw) so a test can script a whole poll cycle - including one device failing
 * while another succeeds - without any networking at all. What {@code since} was actually asked
 * for on each device's most recent call is recorded too, so a test can assert on the windowing
 * decision {@code AttendanceDevicePollService} made (full read vs. windowed from a watermark)
 * without needing any wire-level plumbing - that belongs to {@link
 * com.sunsetbeach.service.AttendanceDevicePollServiceTests}, not to {@link
 * ZkTerminalClientImplTests}, which exercises the real protocol instead.
 */
public class FakeZkTerminalClient implements ZkTerminalClient {

    private static final int DEFAULT_RECORD_SIZE = 16;

    private final Map<String, Queue<Object>> outcomesByDeviceId = new ConcurrentHashMap<>();
    private final Map<String, Optional<LocalDateTime>> lastSinceByDeviceId = new ConcurrentHashMap<>();
    private final AtomicInteger pollCount = new AtomicInteger();

    /** Queues one poll's outcome for this device - a {@code List<RawAttendancePunch>} to return, or a {@code RuntimeException} to throw. */
    public void queue(String deviceId, Object outcome) {
        outcomesByDeviceId.computeIfAbsent(deviceId, id -> new ArrayDeque<>()).add(outcome);
    }

    public int pollCount() {
        return pollCount.get();
    }

    /** The {@code since} this device's most recent {@link #poll} call was given - null means a full read was requested. */
    public LocalDateTime lastSinceRequested(String deviceId) {
        return lastSinceByDeviceId.getOrDefault(deviceId, Optional.empty()).orElse(null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public TerminalPollResult poll(AttendanceDeviceEntity device, LocalDateTime since, Integer knownRecordSize) {
        pollCount.incrementAndGet();
        lastSinceByDeviceId.put(device.getId(), Optional.ofNullable(since));
        Queue<Object> queue = outcomesByDeviceId.get(device.getId());
        if (queue == null || queue.isEmpty()) {
            return new TerminalPollResult(List.of(), knownRecordSize);
        }
        Object outcome = queue.poll();
        if (outcome instanceof RuntimeException e) {
            throw e;
        }
        List<RawAttendancePunch> punches = (List<RawAttendancePunch>) outcome;
        // The record size is a wire-protocol detail this fake has no concept of - a fixed
        // placeholder is enough for AttendanceDevicePollService to have something non-null to
        // persist, same as a real device's first full read would.
        return new TerminalPollResult(punches, since == null ? DEFAULT_RECORD_SIZE : knownRecordSize);
    }
}
