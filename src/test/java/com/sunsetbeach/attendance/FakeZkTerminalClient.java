package com.sunsetbeach.attendance;

import com.sunsetbeach.entity.AttendanceDeviceEntity;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The fake device {@code ZkTerminalClient}'s own javadoc promises: {@code
 * AttendanceDevicePollService} is built and tested entirely against this, with no real socket or
 * hardware anywhere. Each device gets its own queue of canned outcomes (a punch list, or an
 * exception to throw) so a test can script a whole poll cycle - including one device failing
 * while another succeeds - without any networking at all.
 */
public class FakeZkTerminalClient implements ZkTerminalClient {

    private final Map<String, Queue<Object>> outcomesByDeviceId = new ConcurrentHashMap<>();
    private final AtomicInteger pollCount = new AtomicInteger();

    /** Queues one poll's outcome for this device - a {@code List<RawAttendancePunch>} to return, or a {@code RuntimeException} to throw. */
    public void queue(String deviceId, Object outcome) {
        outcomesByDeviceId.computeIfAbsent(deviceId, id -> new ArrayDeque<>()).add(outcome);
    }

    public int pollCount() {
        return pollCount.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<RawAttendancePunch> poll(AttendanceDeviceEntity device) {
        pollCount.incrementAndGet();
        Queue<Object> queue = outcomesByDeviceId.get(device.getId());
        if (queue == null || queue.isEmpty()) {
            return List.of();
        }
        Object outcome = queue.poll();
        if (outcome instanceof RuntimeException e) {
            throw e;
        }
        return (List<RawAttendancePunch>) outcome;
    }
}
