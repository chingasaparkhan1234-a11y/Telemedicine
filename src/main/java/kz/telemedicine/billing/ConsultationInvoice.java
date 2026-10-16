package kz.telemedicine.billing;

public class ConsultationInvoice {
    public enum Status { PENDING, PAID, REFUNDED_COMPENSATED }

    private final String invoiceId;
    private final String patientId;
    private final double amount;
    private Status status;

    public ConsultationInvoice(String invoiceId, String patientId, double amount) {
        this.invoiceId = invoiceId;
        this.patientId = patientId;
        this.amount = amount;
        this.status = Status.PENDING;
    }

    public ConsultationInvoice(String invoiceId, String patientId, double amount, Status status) {
        this.invoiceId = invoiceId;
        this.patientId = patientId;
        this.amount = amount;
        this.status = status;
    }

    public void markPaid() {
        if (this.status != Status.PENDING) {
            throw new IllegalStateException("Invoice is not in PENDING status: " + status);
        }
        this.status = Status.PAID;
    }

    public void compensateRefund() {
        if (this.status != Status.PAID) {
            throw new IllegalStateException("Only PAID invoices can be refunded. Current: " + status);
        }
        this.status = Status.REFUNDED_COMPENSATED;
    }

    public String getInvoiceId() { return invoiceId; }
    public String getPatientId() { return patientId; }
    public double getAmount() { return amount; }
    public Status getStatus() { return status; }
}