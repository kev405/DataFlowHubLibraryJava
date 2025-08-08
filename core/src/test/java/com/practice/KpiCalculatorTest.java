package com.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.practice.domain.transaction.Transaction;
import com.practice.domain.transaction.Transaction.Status;
import com.practice.domain.user.User;
import com.practice.domain.utils.enums.UserRole;
import com.practice.kpi.KpiCalculator;

class KpiCalculatorTest {

    private static final User ALICE = new User(
            UUID.randomUUID(), "Bob", "bob@example.com",
            UserRole.ADMIN, Instant.now());
    private static final User BOB   = new User(
            UUID.randomUUID(), "Ann", "ann@example.com",
            UserRole.ANALYST, Instant.now());

    private static List<Transaction> bigDataset() {
        List<Transaction> list = new ArrayList<>();
        IntStream.range(0, 120).forEach(i -> {
            User u = i % 2 == 0 ? ALICE : BOB;
            Status st = i % 10 == 0 ? Status.FAILED : Status.VALID;
            list.add(new Transaction(
                    UUID.randomUUID(), u,
                    10.0 + i, st,
                    LocalDate.of(2025, (i % 12) + 1, 15)));
        });
        return list;
    }

    @Test
    void totalAmountPerUser_returnsDescendingLinkedHashMap() {
        var totals = KpiCalculator.totalAmountPerUser(bigDataset());

        assertEquals(2, totals.size());
        // Bob debe tener total mayor (por la distribución del mock)
        Iterator<Double> it = totals.values().iterator();
        double first = it.next();
        double second = it.next();
        assertTrue(first >= second);

        // linked hash map preserva orden de iteración === orden de inserción
        assertTrue(totals instanceof LinkedHashMap);
    }

    @Test
    void avgByMonth_calculatesForEachMonth() {
        var avg = KpiCalculator.avgByMonth(bigDataset());
        assertEquals(12, avg.size());                     // 12 meses
    }

    @Test
    void countPerStatus_countsAllStatuses() {
        var cnt = KpiCalculator.countPerStatus(bigDataset());
        assertEquals(120, cnt.values().stream().mapToLong(Long::longValue).sum());
        assertTrue(cnt.get(Status.FAILED) > 0);
        assertTrue(cnt.get(Status.VALID) > 0);
    }
}
