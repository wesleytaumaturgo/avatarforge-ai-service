package com.taumaturgo.domain.models;

public enum ProcessingStatus {
    PENDING,
    PROCESSING,
    DONE,
    FAILED;

    public boolean isTerminal() {
        return this == DONE || this == FAILED;
    }
}
