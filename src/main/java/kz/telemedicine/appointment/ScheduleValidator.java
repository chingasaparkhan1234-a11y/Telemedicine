package kz.telemedicine.appointment;

public class ScheduleValidator {
    public void validateSlot(int hourSlot) {
        if (hourSlot < 8 || hourSlot > 20) {
            throw new IllegalArgumentException("Consultation hours are strictly between 08:00 and 20:00. Provided: " + hourSlot);
        }
    }
}