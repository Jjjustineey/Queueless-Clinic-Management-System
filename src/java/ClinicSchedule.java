import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

public class ClinicSchedule {
    private final String operatingDays;
    private final LocalTime openingTime;
    private final LocalTime closingTime;
    private final String lunchBreak;
    private final String availableSlots;

    public ClinicSchedule() {
        this("Monday-Friday", LocalTime.of(8, 0), LocalTime.of(17, 0),
                "11:00-13:00", "08:00,09:00,10:00,13:00,14:00,15:00,16:00");
    }

    public ClinicSchedule(String operatingDays, LocalTime openingTime,
                          LocalTime closingTime, String lunchBreak,
                          String availableSlots) {
        if (operatingDays == null || operatingDays.isBlank()) throw new IllegalArgumentException("operatingDays must not be blank");
        if (openingTime == null || closingTime == null || !openingTime.isBefore(closingTime)) throw new IllegalArgumentException("Invalid clinic hours");
        if (lunchBreak == null || lunchBreak.isBlank()) throw new IllegalArgumentException("lunchBreak must not be blank");
        if (availableSlots == null || availableSlots.isBlank()) throw new IllegalArgumentException("availableSlots must not be blank");
        validateLunchBreak(lunchBreak);
        validateAvailableSlots(availableSlots, openingTime, closingTime, lunchBreak);
        this.operatingDays = operatingDays.trim();
        this.openingTime = openingTime;
        this.closingTime = closingTime;
        this.lunchBreak = lunchBreak.trim();
        this.availableSlots = availableSlots.trim();
    }

    public String getOperatingDays() { return operatingDays; }
    public LocalTime getOpeningTime() { return openingTime; }
    public LocalTime getClosingTime() { return closingTime; }
    public String getLunchBreak() { return lunchBreak; }
    public String getAvailableSlots() { return availableSlots; }

    public boolean isAvailable(LocalDate date, LocalTime time) {
        if (date == null || time == null || date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) return false;
        if (time.isBefore(openingTime) || time.plusHours(1).isAfter(closingTime)) return false;
        if (isDuringLunchBreak(time)) return false;
        return isConfiguredSlot(time);
    }

    public void validateAppointmentSlot(LocalDate date, LocalTime time) {
        if (!isAvailable(date, time)) {
            throw new IllegalArgumentException("Appointments are available Monday-Friday at 8:00, 9:00, 10:00, 13:00, 14:00, 15:00, or 16:00 only");
        }
    }

    private boolean isConfiguredSlot(LocalTime time) {
        for (String slot : availableSlots.split(",")) {
            if (LocalTime.parse(slot.trim()).equals(time)) return true;
        }
        return false;
    }

    private boolean isDuringLunchBreak(LocalTime time) {
        String[] parts = lunchBreak.split("-");
        LocalTime start = LocalTime.parse(parts[0].trim());
        LocalTime end = LocalTime.parse(parts[1].trim());
        return time.plusHours(1).isAfter(start) && time.isBefore(end);
    }

    private static void validateLunchBreak(String lunchBreak) {
        String[] parts = lunchBreak.split("-");
        if (parts.length != 2) throw new IllegalArgumentException("lunchBreak must use HH:mm-HH:mm format");
        try {
            LocalTime start = LocalTime.parse(parts[0].trim());
            LocalTime end = LocalTime.parse(parts[1].trim());
            if (!start.isBefore(end)) throw new IllegalArgumentException("lunchBreak must have an earlier start time");
        } catch (java.time.format.DateTimeParseException exception) {
            throw new IllegalArgumentException("lunchBreak must use HH:mm-HH:mm format", exception);
        }
    }

    private static void validateAvailableSlots(String availableSlots, LocalTime openingTime,
                                               LocalTime closingTime, String lunchBreak) {
        String[] lunch = lunchBreak.split("-");
        LocalTime lunchStart = LocalTime.parse(lunch[0].trim());
        LocalTime lunchEnd = LocalTime.parse(lunch[1].trim());
        for (String slotText : availableSlots.split(",")) {
            try {
                LocalTime slot = LocalTime.parse(slotText.trim());
                if (slot.isBefore(openingTime) || slot.plusHours(1).isAfter(closingTime)
                        || slot.plusHours(1).isAfter(lunchStart) && slot.isBefore(lunchEnd)) {
                    throw new IllegalArgumentException("availableSlots contains an invalid clinic slot: " + slotText.trim());
                }
            } catch (java.time.format.DateTimeParseException exception) {
                throw new IllegalArgumentException("availableSlots must contain comma-separated HH:mm values", exception);
            }
        }
    }
}
