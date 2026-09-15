package com.sunsetbeach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// EnableScheduling drives every @Scheduled background sweep in this app: PrintService's PENDING
// print-job retry, BookingExpiryService's unconfirmed-booking sweep, and
// AttendanceDevicePollService's fingerprint-terminal poll.
@SpringBootApplication
@EnableScheduling
public class SunsetApplication {

    public static void main(String[] args) {
        SpringApplication.run(SunsetApplication.class, args);
    }

}
