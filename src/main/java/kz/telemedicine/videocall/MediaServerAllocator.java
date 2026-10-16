package kz.telemedicine.videocall;

public class MediaServerAllocator {
    private boolean simulateOverload = false;

    public void setSimulateOverload(boolean simulateOverload) {
        this.simulateOverload = simulateOverload;
    }

    public String allocateRoom(String appointmentId) {
        if (simulateOverload) {
            throw new IllegalStateException("Media Server Error: 100% MCU Capacity reached. Cannot allocate video stream.");
        }
        return "https://telemed.health.kz/room/" + appointmentId;
    }
}