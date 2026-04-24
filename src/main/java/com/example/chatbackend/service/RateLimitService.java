package com.example.chatbackend.service;

import com.example.chatbackend.config.ChatProperties;
import com.example.chatbackend.exception.RateLimitExceededException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private final int requestsPerMinute;
    private final Clock clock;
    private final Map<String, UserBucket> buckets = new ConcurrentHashMap<>();

    @Autowired
    public RateLimitService(ChatProperties properties) {
        this(properties, Clock.systemUTC());
    }

    RateLimitService(ChatProperties properties, Clock clock) {
        this.requestsPerMinute = properties.getRateLimit().getRequestsPerMinute();
        this.clock = clock;
    }

    public void checkAllowed(String userId) {
        if (requestsPerMinute <= 0) {
            return;
        }

        UserBucket bucket = buckets.computeIfAbsent(userId, ignored -> new UserBucket(requestsPerMinute, clock.instant()));
        if (!bucket.tryConsume(clock.instant(), requestsPerMinute)) {
            throw new RateLimitExceededException("Too many chat requests. Please retry later.");
        }
    }

    private static final class UserBucket {
        private int tokens;
        private Instant lastRefill;

        private UserBucket(int tokens, Instant lastRefill) {
            this.tokens = tokens;
            this.lastRefill = lastRefill;
        }

        private synchronized boolean tryConsume(Instant now, int capacity) {
            if (now.minusSeconds(60).isAfter(lastRefill) || now.minusSeconds(60).equals(lastRefill)) {
                tokens = capacity;
                lastRefill = now;
            }

            if (tokens <= 0) {
                return false;
            }

            tokens--;
            return true;
        }
    }
}
