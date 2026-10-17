package com.sunsetbeach.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * The hooks business operations call to say "this happened". A notification accompanies an
 * operation, it never decides it: the work runs only after the operation's transaction has
 * committed (a rolled-back block announces nothing) and every failure is caught and logged here, so
 * a broken database write or an unreachable Telegram can't undo or fail the block that was created.
 */
@Component
public class NotificationEvents {

    private static final Logger log = LoggerFactory.getLogger(NotificationEvents.class);

    private final BlockNotificationService blockNotifications;

    public NotificationEvents(BlockNotificationService blockNotifications) {
        this.blockNotifications = blockNotifications;
    }

    /** A room block was just created (call inside the creating transaction). */
    public void blockCreated(String blockId) {
        afterCommit("block " + blockId, () -> blockNotifications.onBlockCreated(blockId));
    }

    private void afterCommit(String what, Runnable work) {
        Runnable safe = () -> {
            try {
                work.run();
            } catch (RuntimeException e) {
                log.error("Notification for {} failed - the operation itself is unaffected", what, e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safe.run();
                }
            });
        } else {
            safe.run();
        }
    }
}
