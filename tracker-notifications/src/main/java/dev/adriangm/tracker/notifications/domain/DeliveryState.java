package dev.adriangm.tracker.notifications.domain;

public enum DeliveryState {
    PENDING,
    SENDING,
    SENT,
    REJECTED,
    UNCERTAIN,
    CANCELLED,
    EXPIRED
}
