package com.windschief.spotify;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@Interceptor
@RetrySpotify
@Priority(Interceptor.Priority.APPLICATION)
public class SpotifyRetryInterceptor {
    private static final int MAX_RETRIES = 3;
    private static final Duration DEFAULT_RETRY_AFTER = Duration.ofSeconds(5);

    @AroundInvoke
    public Object handleRetry(InvocationContext context) throws Exception {
        int attempts = 0;
        WebApplicationException lastException = null;

        while (attempts < MAX_RETRIES) {
            try {
                return context.proceed();
            } catch (WebApplicationException e) {
                lastException = e;

                if (e.getResponse().getStatus() == 429) {
                    attempts++;

                    Duration retryAfter = getRetryAfterDuration(e.getResponse());

                    if (attempts < MAX_RETRIES) {
                        long delayMs = retryAfter.toMillis() * (long) Math.pow(2, attempts - 1);
                        sleep(delayMs);
                        continue;
                    }
                }

                throw e;
            }
        }

        throw lastException;
    }

    private Duration getRetryAfterDuration(Response response) {
        String retryAfter = response.getHeaderString("Retry-After");
        if (retryAfter != null) {
            try {
                return Duration.ofSeconds(Long.parseLong(retryAfter));
            } catch (NumberFormatException e) {
                return DEFAULT_RETRY_AFTER;
            }
        }
        return DEFAULT_RETRY_AFTER;
    }

    private void sleep(long milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Retry interrupted", e);
        }
    }
}
