public class MedicalService {
    private final String serviceId;
    private final String name;
    private final String description;

    public MedicalService(String serviceId, String name, String description) {
        this.serviceId = required(serviceId, "serviceId");
        this.name = required(name, "name");
        this.description = required(description, "description");
    }

    public String getServiceId() { return serviceId; }
    public String getName() { return name; }
    public String getDescription() { return description; }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }
}
