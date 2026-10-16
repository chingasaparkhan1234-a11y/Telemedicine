package kz.telemedicine.appointment;

public class Appointment {
    public enum Status { NEW, CONFIRMED, CANCELLED_COMPENSATED }

    private final String appointmentId;
    private final String patientId;
    private final String doctorId;
    private final int hourSlot;
    private Status status;

    public Appointment(String appointmentId, String patientId, String doctorId, int hourSlot) {
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.hourSlot = hourSlot;
        this.status = Status.NEW;
    }

    public Appointment(String appointmentId, String patientId, String doctorId, int hourSlot, Status status) {
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.hourSlot = hourSlot;
        this.status = status;
    }

    public void confirm() {
        if (this.status != Status.NEW) {
            throw new IllegalStateException("Cannot confirm appointment in status: " + status);
        }
        this.status = Status.CONFIRMED;
    }

    public void compensateCancellation() {
        if (this.status != Status.CONFIRMED) {
            throw new IllegalStateException("Only CONFIRMED appointments can be compensated. Current: " + status);
        }
        this.status = Status.CANCELLED_COMPENSATED;
    }

    public String getAppointmentId() { return appointmentId; }
    public String getPatientId() { return patientId; }
    public String getDoctorId() { return doctorId; }
    public int getHourSlot() { return hourSlot; }
    public Status getStatus() { return status; }
}