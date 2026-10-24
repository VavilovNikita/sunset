package com.sunsetbeach.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.junit.jupiter.api.Test;

class SlidingWindowBucketsTests {

    @Test
    void evictIdle_dropsKeysWithNothingInTheWindow_andTrimsTheRest() {
        ConcurrentHashMap<String, Deque<Instant>> buckets = new ConcurrentHashMap<>();
        Instant now = Instant.now();
        buckets.put("gone", new ConcurrentLinkedDeque<>(java.util.List.of(now.minus(Duration.ofHours(2)))));
        buckets.put("live", new ConcurrentLinkedDeque<>(java.util.List.of(now.minus(Duration.ofHours(2)), now.minus(Duration.ofMinutes(1)))));

        SlidingWindowBuckets.evictIdle(buckets, Duration.ofHours(1));

        assertThat(buckets).containsOnlyKeys("live");
        assertThat(buckets.get("live")).hasSize(1);
    }
}
