package com.medion.hardwarestore.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight in-memory key/value cache with per-entry TTL.
 *
 * <p>This is a deliberate stand-in for Redis: the payment flow needs a short-lived
 * store for M-Pesa checkout-to-transaction mappings and payment context, but adding
 * spring-boot-starter-data-redis would make a running Redis a hard boot dependency.
 * Swapping this class for a Redis-backed implementation later requires no changes to
 * callers - they depend only on the public methods here.</p>
 */
@Slf4j
@Service
public class PaymentCacheService {

    private record Entry(String value, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    /**
     * Store a value under the given key with a time-to-live.
     */
    public void put(String key, String value, Duration ttl) {
        store.put(key, new Entry(value, Instant.now().plus(ttl)));
    }

    /**
     * Retrieve a value if present and not expired.
     */
    public Optional<String> get(String key) {
        Entry entry = store.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.isExpired()) {
            store.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    /**
     * Remove a key from the cache.
     */
    public void evict(String key) {
        store.remove(key);
    }
}
