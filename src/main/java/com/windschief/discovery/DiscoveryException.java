package com.windschief.discovery;

/** A safe, actionable message suitable for the task UI. Never includes remote URLs or tokens. */
public class DiscoveryException extends RuntimeException {
    public DiscoveryException(String message) { super(message); }
}
