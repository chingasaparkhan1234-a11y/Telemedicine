package kz.telemedicine.videocall;

public class VideoSession {
    public enum Status { INITIALIZING, ROOM_ACTIVE, TERMINATED_COMPENSATED }

    private final String sessionId;
    private final String appointmentRefId;
    private final String roomUrl;
    private Status status;

    public VideoSession(String sessionId, String appointmentRefId, String roomUrl) {
        this.sessionId = sessionId;
        this.appointmentRefId = appointmentRefId;
        this.roomUrl = roomUrl;
        this.status = Status.INITIALIZING;
    }

    public VideoSession(String sessionId, String appointmentRefId, String roomUrl, Status status) {
        this.sessionId = sessionId;
        this.appointmentRefId = appointmentRefId;
        this.roomUrl = roomUrl;
        this.status = status;
    }

    public void activate() {
        if (this.status != Status.INITIALIZING) {
            throw new IllegalStateException("Cannot activate session from status: " + status);
        }
        this.status = Status.ROOM_ACTIVE;
    }

    public void compensateTermination() {
        if (this.status != Status.ROOM_ACTIVE) {
            throw new IllegalStateException("Only ROOM_ACTIVE sessions can be compensated. Current: " + status);
        }
        this.status = Status.TERMINATED_COMPENSATED;
    }

    public String getSessionId() { return sessionId; }
    public String getAppointmentRefId() { return appointmentRefId; }
    public String getRoomUrl() { return roomUrl; }
    public Status getStatus() { return status; }
}