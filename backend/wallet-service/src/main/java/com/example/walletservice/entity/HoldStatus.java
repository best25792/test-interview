package com.example.walletservice.entity;

public enum HoldStatus {
    ACTIVE,      // Hold is active and amount is reserved
    CAPTURED,    // Hold was captured (deducted from wallet)
    RELEASED,    // Hold was released (cancelled, refunded, or expired)
    EXPIRED      // Hold expired without being captured
}
