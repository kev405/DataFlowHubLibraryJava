package com.practice.domain.transaction;

import java.time.LocalDate;
import java.util.UUID;

import com.practice.domain.user.User;

public record Transaction(
        UUID    id,
        User    user,
        double  amount,
        Status  status,
        LocalDate date) {

    public enum Status { VALID, PENDING, FAILED }
}
