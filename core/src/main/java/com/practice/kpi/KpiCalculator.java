package com.practice.kpi;

import java.util.*;
import java.util.stream.*;

import com.practice.domain.transaction.Transaction;
import com.practice.domain.user.User;

public final class KpiCalculator {

    private KpiCalculator() { }

    /**
     * Calculates total amount per user for VALID transactions and
     * returns a LinkedHashMap sorted by descending total.
     */
    public static Map<User, Double> totalAmountPerUser(List<Transaction> txs) {

        // 1. filter     2. groupBy user + sum     3. sort desc     4. collect to LinkedHashMap
        return txs.stream()
                  .filter(t -> t.status() == Transaction.Status.VALID)
                  .collect(Collectors.groupingBy(
                          Transaction::user,
                          Collectors.summingDouble(Transaction::amount)))
                  .entrySet()
                  .stream()
                  .sorted(Map.Entry.<User, Double>comparingByValue().reversed())
                  .collect(Collectors.toMap(
                          Map.Entry::getKey,
                          Map.Entry::getValue,
                          (a, b) -> a,
                          LinkedHashMap::new));
    }

    /* --- métodos extra para practicar operaciones ---------------------- */

    /** Average amount per month (YYYY-MM) for VALID transactions. */
    public static Map<String, Double> avgByMonth(List<Transaction> txs) {
        return txs.stream()
                  .filter(t -> t.status() == Transaction.Status.VALID)
                  .collect(Collectors.groupingBy(
                          t -> t.date().withDayOfMonth(1).toString(),      // 2025-08-01
                          Collectors.averagingDouble(Transaction::amount)));
    }

    /** Count of transactions per status. */
    public static Map<Transaction.Status, Long> countPerStatus(List<Transaction> txs) {
        return txs.stream()
                  .collect(Collectors.groupingBy(Transaction::status, Collectors.counting()));
    }
}
