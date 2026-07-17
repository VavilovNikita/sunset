package com.sunsetbeach.error;

/** Mirrors Prisma P2025 ("record not found") -> 404 {@code { error: "..." }}. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
