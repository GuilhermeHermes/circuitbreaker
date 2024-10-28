package com.guilhermehermes.circuitbreaker.dependente;

class ServiceResponse {
    private final String message;
    private final boolean success;
    private final int remainingCalls;

    public ServiceResponse(String message, boolean success, int remainingCalls) {
        this.message = message;
        this.success = success;
        this.remainingCalls = remainingCalls;
    }

    // Getters
    public String getMessage() { return message; }
    public boolean isSuccess() { return success; }
    public int getRemainingCalls() { return remainingCalls; }
}
