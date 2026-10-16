package kz.telemedicine.billing;

public class PaymentRulesEngine {
    private boolean simulateDecline = false;

    public void setSimulateDecline(boolean simulateDecline) {
        this.simulateDecline = simulateDecline;
    }

    public void processPayment(double amount, String cardNumber) {
        if (simulateDecline || (cardNumber != null && cardNumber.endsWith("0000"))) {
            throw new IllegalStateException("Payment Gateway Error: Card declined (Insufficient Funds / Card Blocked).");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }
    }
}