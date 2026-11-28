
import com.sun.net.httpserver.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.spec.KeySpec;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Main {

    static final Path ROOT = Paths.get(System.getProperty("user.dir")).toAbsolutePath();

    static final Path DATA = Paths.get(
        System.getProperty("data.path", ROOT.resolve("data/store.json").toString())
    ).toAbsolutePath().normalize();

    static final int PORT = parsePort(System.getProperty("port", "8080"));

    static final Object DB_LOCK = new Object();
    static final SecureRandom RANDOM = new SecureRandom();

    static final int PBKDF2_ITERATIONS = 120000;
    static final int PBKDF2_KEY_LENGTH = 256;

    static Map<String, Object> db = new LinkedHashMap<>();
    static final Map<String, String> sessions = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    static Map<String, Object> map(Object o) {
        return o instanceof Map
            ? (Map<String, Object>) o
            : new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    static List<Object> list(Object o) {
        return o instanceof List
            ? (List<Object>) o
            : new ArrayList<>();
    }

    static String s(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    static Map<String, Object> obj(Object... a) {
        Map<String, Object> m = new LinkedHashMap<>();

        for (int i = 0; i < a.length; i += 2) {
            m.put((String) a[i], a[i + 1]);
        }

        return m;
    }

    static synchronized void save() {
        try {
            ensureDatabaseShape();

            Files.createDirectories(DATA.getParent());

            Path tmp = DATA.resolveSibling(DATA.getFileName() + ".tmp");

            Files.writeString(
                tmp,
                Json.stringify(db),
                StandardCharsets.UTF_8
            );

            try {
                Files.move(
                    tmp,
                    DATA,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(
                    tmp,
                    DATA,
                    StandardCopyOption.REPLACE_EXISTING
                );
            }

        } catch (IOException e) {
            throw new IllegalStateException("Could not save database", e);
        }
    }

    static synchronized void load() {
        try {
            Files.createDirectories(DATA.getParent());

            if (Files.exists(DATA)) {
                Object parsed = Json.parse(
                    Files.readString(DATA, StandardCharsets.UTF_8)
                );

                if (!(parsed instanceof Map)) {
                    throw new IllegalArgumentException(
                        "Database root must be an object"
                    );
                }

                db = map(parsed);
            }

            ensureDatabaseShape();

        } catch (Exception e) {
            System.err.println(
                "Database could not be loaded: " + e.getMessage()
            );

            if (Files.exists(DATA)) {
                try {
                    Files.copy(
                        DATA,
                        DATA.resolveSibling(
                            "store.json.corrupt-" + System.currentTimeMillis()
                        ),
                        StandardCopyOption.COPY_ATTRIBUTES
                    );
                } catch (IOException ignored) {
                }
            }

            db = new LinkedHashMap<>();
            ensureDatabaseShape();
        }

        if (list(db.get("users")).isEmpty()) {
            addUser(
                "Clinic Administrator",
                "admin",
                "admin@clinic.local",
                "09170000000",
                "admin123",
                "staff"
            );

            addUser(
                "Dr. Santos",
                "doctor",
                "doctor@clinic.local",
                "09170000001",
                "doctor123",
                "staff"
            );

            save();
        }
    }

    static String hash(String x) {
        try {
            byte[] salt = new byte[16];
            RANDOM.nextBytes(salt);

            return "pbkdf2$"
                + PBKDF2_ITERATIONS
                + "$"
                + Base64.getEncoder().encodeToString(salt)
                + "$"
                + Base64.getEncoder().encodeToString(
                    pbkdf2(
                        x,
                        salt,
                        PBKDF2_ITERATIONS,
                        PBKDF2_KEY_LENGTH
                    )
                );

        } catch (Exception e) {
            throw new IllegalStateException(
                "Password hashing unavailable",
                e
            );
        }
    }

    static synchronized Map<String, Object> addUser(
        String name,
        String username,
        String email,
        String mobile,
        String pass,
        String role
    ) {
        Map<String, Object> u = obj(
            "id", UUID.randomUUID().toString(),
            "fullName", name,
            "username", username,
            "email", email,
            "mobile", mobile,
            "password", hash(pass),
            "role", role,
            "createdAt", Instant.now().toString()
        );

        list(db.get("users")).add(u);

        return u;
    }

    static void activity(String msg) {
        list(db.get("activities")).add(
            0,
            obj(
                "message", msg,
                "time", LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("h:mm a")
                )
            )
        );

        List<Object> a = list(db.get("activities"));

        while (a.size() > 30) {
            a.remove(a.size() - 1);
        }
    }

    static void send(
        HttpExchange x,
        int status,
        Object body
    ) throws IOException {

        byte[] b = Json.stringify(body).getBytes(StandardCharsets.UTF_8);

        x.getResponseHeaders().set(
            "Content-Type",
            "application/json; charset=utf-8"
        );

        x.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        x.getResponseHeaders().set(
            "Access-Control-Allow-Headers",
            "Content-Type"
        );

        x.getResponseHeaders().set(
            "Access-Control-Allow-Methods",
            "GET,POST,PUT,DELETE,OPTIONS"
        );

        x.sendResponseHeaders(status, b.length);
        x.getResponseBody().write(b);
        x.close();
    }

    static Map<String, Object> body(HttpExchange x) throws IOException {
        String raw = new String(
            x.getRequestBody().readAllBytes(),
            StandardCharsets.UTF_8
        ).trim();

        if (
            raw.isBlank()
            || !raw.startsWith("{")
            || !raw.endsWith("}")
        ) {
            throw new IllegalArgumentException(
                "Malformed JSON request."
            );
        }

        Object parsed = Json.parse(raw);

        if (!(parsed instanceof Map)) {
            throw new IllegalArgumentException(
                "Malformed JSON request."
            );
        }

        return map(parsed);
    }

    static String cookie(HttpExchange x, String key) {
        String c = x.getRequestHeaders().getFirst("Cookie");

        if (c != null) {
            for (String p : c.split(";")) {
                String[] kv = p.trim().split("=", 2);

                if (kv.length == 2 && kv[0].equals(key)) {
                    return kv[1];
                }
            }
        }

        return "";
    }

    static Map<String, Object> user(HttpExchange x) {
        String token = cookie(x, "ql_session");

        if (token.isBlank()) {
            return null;
        }

        String id = sessions.get(token);

        return id == null ? null : findUser(id);
    }

    static Map<String, Object> findUser(String id) {
        for (Object o : list(db.get("users"))) {
            Map<String, Object> u = map(o);

            if (s(u.get("id")).equals(id)) {
                return u;
            }
        }

        return null;
    }

    static Map<String, Object> safeUser(Map<String, Object> u) {
        return obj(
            "id", u.get("id"),
            "fullName", u.get("fullName"),
            "username", u.get("username"),
            "email", u.get("email"),
            "mobile", u.get("mobile"),
            "role", u.get("role")
        );
    }

    static boolean staff(Map<String, Object> u) {
        return u != null && s(u.get("role")).equals("staff");
    }

    static Map<String, Object> appointmentById(String id) {
        for (Object o : list(db.get("appointments"))) {
            Map<String, Object> a = map(o);

            if (s(a.get("id")).equals(id)) {
                return a;
            }
        }

        return null;
    }

    static String date(Object o) {
        return s(o);
    }

    static boolean validSlot(String d, String t) {
        try {
            LocalDate day = LocalDate.parse(d);
            LocalTime time = LocalTime.parse(t);

            return day.getDayOfWeek().getValue() <= 5
                && List.of(
                    LocalTime.of(8, 0),
                    LocalTime.of(9, 0),
                    LocalTime.of(10, 0),
                    LocalTime.of(13, 0),
                    LocalTime.of(14, 0),
                    LocalTime.of(15, 0),
                    LocalTime.of(16, 0)
                ).contains(time);

        } catch (DateTimeParseException e) {
            return false;
        }
    }

    static synchronized void handle(HttpExchange x) throws IOException {
        String path = x.getRequestURI().getPath();
        String method = x.getRequestMethod();

        if (method.equals("OPTIONS")) {
            send(x, 204, obj());
            return;
        }

        try {
            if (path.startsWith("/api/")) {
                Map<String, Object> b =
                    method.equals("GET")
                    ? new LinkedHashMap<>()
                    : body(x);

                Map<String, Object> u = findUser(
                    sessions.get(cookie(x, "ql_session"))
                );

                if (
                    (path.equals("/api/auth/signup") && !method.equals("POST"))
                    || (path.equals("/api/auth/login") && !method.equals("POST"))
                    || (path.equals("/api/auth/logout") && !method.equals("POST"))
                    || (path.equals("/api/auth/me") && !method.equals("GET"))
                    || (path.equals("/api/auth/forgot") && !method.equals("POST"))
                    || (path.equals("/api/auth/reset") && !method.equals("POST"))
                    || (
                        path.equals("/api/patient/profile")
                        && !List.of("GET", "POST").contains(method)
                    )
                    || (
                        path.equals("/api/appointments")
                        && !List.of("GET", "POST").contains(method)
                    )
                    || (
                        path.equals("/api/appointments/cancel")
                        && !method.equals("POST")
                    )
                    || (
                        path.equals("/api/staff/dashboard")
                        && !method.equals("GET")
                    )
                    || (
                        path.equals("/api/staff/queue")
                        && !method.equals("GET")
                    )
                    || (
                        path.equals("/api/staff/action")
                        && !method.equals("POST")
                    )
                    || (
                        path.equals("/api/staff/walkin")
                        && !method.equals("POST")
                    )
                    || (
                        path.equals("/api/staff/patients")
                        && !method.equals("GET")
                    )
                ) {
                    send(x, 405, obj("error", "Method not allowed."));
                    return;
                }

                // SIGNUP
                if (
                    path.equals("/api/auth/signup")
                    && method.equals("POST")
                ) {
                    String role = "patient";

                    String fullName = s(b.get("fullName")).trim();
                    String username = s(b.get("username")).trim();
                    String email = s(b.get("email"))
                        .trim()
                        .toLowerCase(Locale.ROOT);

                    String mobile = s(b.get("mobile")).trim();
                    String password = s(b.get("password"));
                    String confirmation = s(b.get("confirmPassword"));

                    validateAccountFields(
                        fullName,
                        username,
                        email,
                        mobile,
                        password,
                        confirmation
                    );

                    for (Object z : list(db.get("users"))) {
                        Map<String, Object> v = map(z);

                        if (
                            email.equalsIgnoreCase(s(v.get("email")))
                            || mobile.equals(s(v.get("mobile")))
                            || username.equalsIgnoreCase(s(v.get("username")))
                        ) {
                            send(
                                x,
                                409,
                                obj(
                                    "error",
                                    "Email, mobile number, or username is already registered."
                                )
                            );
                            return;
                        }
                    }

                    Map<String, Object> nu = addUser(
                        fullName,
                        username,
                        email,
                        mobile,
                        password,
                        role
                    );

                    String token = UUID.randomUUID().toString();
                    sessions.put(token, s(nu.get("id")));

                    x.getResponseHeaders().add(
                        "Set-Cookie",
                        "ql_session=" + token
                            + "; Path=/; HttpOnly; SameSite=Lax"
                    );

                    save();

                    send(x, 200, obj("user", safeUser(nu)));
                    return;
                }

                // LOGIN
                if (
                    path.equals("/api/auth/login")
                    && method.equals("POST")
                ) {
                    String identity = s(b.get("identity")).toLowerCase();
                    String role = s(b.get("role"));

                    Map<String, Object> found = null;

                    for (Object z : list(db.get("users"))) {
                        Map<String, Object> v = map(z);

                        if (
                            (
                                s(v.get("email")).equalsIgnoreCase(identity)
                                || s(v.get("mobile")).equals(identity)
                                || s(v.get("username")).equalsIgnoreCase(identity)
                            )
                            && verifyPassword(v, s(b.get("password")))
                            && s(v.get("role")).equals(role)
                        ) {
                            found = v;
                            break;
                        }
                    }

                    if (found == null) {
                        send(
                            x,
                            401,
                            obj("error", "Incorrect login details or account type.")
                        );
                        return;
                    }

                    String token = UUID.randomUUID().toString();

                    sessions.put(token, s(found.get("id")));
                    save();

                    x.getResponseHeaders().add(
                        "Set-Cookie",
                        "ql_session=" + token
                            + "; Path=/; HttpOnly; SameSite=Lax"
                    );

                    send(x, 200, obj("user", safeUser(found)));
                    return;
                }

                // LOGOUT
                if (path.equals("/api/auth/logout")) {
                    String token = cookie(x, "ql_session");

                    sessions.remove(token);

                    x.getResponseHeaders().add(
                        "Set-Cookie",
                        "ql_session=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax"
                    );

                    send(x, 200, obj("ok", true));
                    return;
                }

                // CURRENT USER
                if (path.equals("/api/auth/me")) {
                    send(
                        x,
                        200,
                        obj("user", u == null ? null : safeUser(u))
                    );
                    return;
                }

                // FORGOT PASSWORD
                if (
                    path.equals("/api/auth/forgot")
                    && method.equals("POST")
                ) {
                    String id = s(b.get("identity"));
                    boolean exists = false;

                    for (Object z : list(db.get("users"))) {
                        Map<String, Object> v = map(z);

                        if (
                            s(v.get("email")).equalsIgnoreCase(id)
                            || s(v.get("mobile")).equals(id)
                        ) {
                            exists = true;
                        }
                    }

                    String code = String.format(
                        "%06d",
                        RANDOM.nextInt(1000000)
                    );

                    if (exists) {
                        db.put("reset_" + id, code);
                        save();
                    }

                    send(
                        x,
                        200,
                        obj(
                            "message",
                            "If the account exists, a verification code has been generated.",
                            "demoCode",
                            exists ? code : null
                        )
                    );
                    return;
                }

                // RESET PASSWORD
                if (
                    path.equals("/api/auth/reset")
                    && method.equals("POST")
                ) {
                    String id = s(b.get("identity"));
                    String code = s(b.get("code"));

                    if (!code.equals(s(db.get("reset_" + id)))) {
                        send(
                            x,
                            400,
                            obj("error", "Invalid verification code.")
                        );
                        return;
                    }

                    String newPassword = s(b.get("password"));

                    if (newPassword.length() < 6) {
                        send(
                            x,
                            400,
                            obj(
                                "error",
                                "Password must contain at least 6 characters."
                            )
                        );
                        return;
                    }

                    for (Object z : list(db.get("users"))) {
                        Map<String, Object> v = map(z);

                        if (
                            s(v.get("email")).equalsIgnoreCase(id)
                            || s(v.get("mobile")).equals(id)
                        ) {
                            v.put("password", hash(newPassword));
                        }
                    }

                    db.remove("reset_" + id);
                    save();

                    send(x, 200, obj("ok", true));
                    return;
                }

                if (u == null) {
                    send(
                        x,
                        401,
                        obj("error", "Please log in to continue.")
                    );
                    return;
                }

                // PATIENT PROFILE - SAVE
                if (
                    path.equals("/api/patient/profile")
                    && method.equals("POST")
                ) {
                    if (!s(u.get("role")).equals("patient")) {
                        send(x, 403, obj("error", "Patient access only."));
                        return;
                    }

                    int age = integerField(b, "age", 0, 130);
                    String sex = required(b, "sex");
                    String category = required(b, "category");

                    u.put("age", age);
                    u.put("sex", sex);
                    u.put("category", category);
                    u.put("registered", true);

                    save();

                    send(
                        x,
                        200,
                        obj(
                            "user", safeUser(u),
                            "profile",
                            obj(
                                "age", u.get("age"),
                                "sex", u.get("sex"),
                                "category", u.get("category")
                            )
                        )
                    );
                    return;
                }

                // PATIENT PROFILE - GET
                if (
                    path.equals("/api/patient/profile")
                    && method.equals("GET")
                ) {
                    if (!s(u.get("role")).equals("patient")) {
                        send(x, 403, obj("error", "Patient access only."));
                        return;
                    }

                    send(
                        x,
                        200,
                        obj(
                            "user", safeUser(u),
                            "profile",
                            obj(
                                "age", u.get("age"),
                                "sex", u.get("sex"),
                                "category", u.get("category")
                            ),
                            "registered",
                            Boolean.TRUE.equals(u.get("registered"))
                        )
                    );
                    return;
                }

                // BOOK APPOINTMENT
                if (
                    path.equals("/api/appointments")
                    && method.equals("POST")
                ) {
                    if (!s(u.get("role")).equals("patient")) {
                        send(x, 403, obj("error", "Patient access only."));
                        return;
                    }

                    if (!Boolean.TRUE.equals(u.get("registered"))) {
                        send(
                            x,
                            400,
                            obj("error", "Complete patient registration first.")
                        );
                        return;
                    }

                    String d = s(b.get("date"));
                    String t = s(b.get("time"));
                    String service = s(b.get("service"));

                    if (!validSlot(d, t)) {
                        send(
                            x,
                            400,
                            obj(
                                "error",
                                "Choose Monday to Friday, 8:00 AM-5:00 PM. 11:00 AM-12:00 PM is lunch break."
                            )
                        );
                        return;
                    }

                    for (Object z : list(db.get("appointments"))) {
                        Map<String, Object> a = map(z);

                        if (
                            d.equals(s(a.get("date")))
                            && t.equals(s(a.get("time")))
                            && !s(a.get("status")).equals("Cancelled")
                            && !s(a.get("status")).equals("No-Show")
                            && !s(a.get("status")).equals("Re-Assigned")
                        ) {
                            send(
                                x,
                                409,
                                obj(
                                    "error",
                                    "That appointment slot is no longer available. Please choose another time."
                                )
                            );
                            return;
                        }
                    }

                    if (service.isBlank()) {
                        send(
                            x,
                            400,
                            obj("error", "Select a medical service.")
                        );
                        return;
                    }

                    int q = ((Number) db.get("nextQueue")).intValue();
                    db.put("nextQueue", q + 1);

                    Map<String, Object> a = obj(
                        "id", UUID.randomUUID().toString(),
                        "patientId", u.get("id"),
                        "patientName", u.get("fullName"),
                        "category", u.get("category"),
                        "age", u.get("age"),
                        "sex", u.get("sex"),
                        "mobile", u.get("mobile"),
                        "email", u.get("email"),
                        "service", service,
                        "date", d,
                        "time", t,
                        "queue", q,
                        "status", "Waiting",
                        "doctor", "To be assigned",
                        "createdAt", Instant.now().toString(),
                        "walkIn", false
                    );

                    list(db.get("appointments")).add(a);

                    activity(
                        "Q" + q + " appointment booked by " + u.get("fullName")
                    );

                    save();

                    send(x, 200, obj("appointment", a));
                    return;
                }

                // GET APPOINTMENTS
                if (
                    path.equals("/api/appointments")
                    && method.equals("GET")
                ) {
                    List<Object> out = new ArrayList<>();

                    for (Object z : list(db.get("appointments"))) {
                        Map<String, Object> a = map(z);

                        if (
                            staff(u)
                            || s(a.get("patientId")).equals(s(u.get("id")))
                        ) {
                            out.add(a);
                        }
                    }

                    send(x, 200, obj("appointments", out));
                    return;
                }

                // CANCEL APPOINTMENT
                if (
                    path.equals("/api/appointments/cancel")
                    && method.equals("POST")
                ) {
                    Map<String, Object> a = appointmentById(s(b.get("id")));

                    if (
                        a == null
                        || (
                            !staff(u)
                            && !s(a.get("patientId")).equals(s(u.get("id")))
                        )
                    ) {
                        send(x, 404, obj("error", "Appointment not found."));
                        return;
                    }

                    if (!s(a.get("status")).equals("Waiting")) {
                        send(
                            x,
                            400,
                            obj("error", "Only waiting appointments can be cancelled.")
                        );
                        return;
                    }

                    a.put("status", "Cancelled");
                    a.put("cancelledAt", Instant.now().toString());

                    activity("Q" + a.get("queue") + " cancelled");

                    save();

                    send(x, 200, obj("appointment", a));
                    return;
                }

                // STAFF DASHBOARD
                if (path.equals("/api/staff/dashboard")) {
                    if (!staff(u)) {
                        send(x, 403, obj("error", "Staff access only."));
                        return;
                    }

                    List<Object> ap = list(db.get("appointments"));

                    LocalDate today = LocalDate.now();

                    int todays = 0;
                    int in = 0;
                    int completed = 0;

                    Map<String, Integer> counts = new LinkedHashMap<>();

                    counts.put("No-Show", 0);
                    counts.put("Cancelled", 0);
                    counts.put("Completed", 0);
                    counts.put("Checking", 0);
                    counts.put("Waiting", 0);
                    counts.put("Re-Assigned", 0);

                    for (Object z : ap) {
                        Map<String, Object> a = map(z);

                        if (today.toString().equals(s(a.get("date")))) {
                            todays++;

                            String st = s(a.get("status"));

                            counts.put(
                                st,
                                counts.getOrDefault(st, 0) + 1
                            );

                            if (
                                st.equals("Waiting")
                                || st.equals("Checking")
                            ) {
                                in++;
                            }

                            if (st.equals("Completed")) {
                                completed++;
                            }
                        }
                    }

                    long totalPatients = list(db.get("users"))
                        .stream()
                        .filter(
                            z -> s(map(z).get("role")).equals("patient")
                        )
                        .count();

                    List<Object> activities = list(db.get("activities"));

                    send(
                        x,
                        200,
                        obj(
                            "totalPatients", totalPatients,
                            "todaysAppointments", todays,
                            "inQueue", in,
                            "completedToday", completed,
                            "counts", counts,
                            "activities",
                            activities.subList(
                                0,
                                Math.min(8, activities.size())
                            )
                        )
                    );
                    return;
                }

                // STAFF QUEUE
                if (
                    path.equals("/api/staff/queue")
                    && method.equals("GET")
                ) {
                    if (!staff(u)) {
                        send(x, 403, obj("error", "Staff access only."));
                        return;
                    }

                    send(
                        x,
                        200,
                        obj("appointments", list(db.get("appointments")))
                    );
                    return;
                }

                // STAFF APPOINTMENT ACTIONS
                if (
                    path.equals("/api/staff/action")
                    && method.equals("POST")
                ) {
                    if (!staff(u)) {
                        send(x, 403, obj("error", "Staff access only."));
                        return;
                    }

                    Map<String, Object> a =
                        appointmentById(s(b.get("id")));

                    String action = s(b.get("action"));

                    if (a == null) {
                        send(x, 404, obj("error", "Appointment not found."));
                        return;
                    }

                    String st = s(a.get("status"));

                    if (
                        action.equals("checking")
                        && st.equals("Waiting")
                    ) {
                        a.put("status", "Checking");

                    } else if (
                        action.equals("complete")
                        && st.equals("Checking")
                    ) {
                        a.put("status", "Completed");

                    } else if (
                        action.equals("no-show")
                        && st.equals("Waiting")
                    ) {
                        a.put("status", "No-Show");

                    } else if (
                        action.equals("reassign")
                        && (
                            st.equals("Cancelled")
                            || st.equals("No-Show")
                        )
                    ) {
                        String patient = s(b.get("patientName"));

                        if (patient.isBlank()) {
                            send(
                                x,
                                400,
                                obj(
                                    "error",
                                    "Enter the walk-in patient's full name."
                                )
                            );
                            return;
                        }

                        int q = ((Number) db.get("nextQueue")).intValue();
                        db.put("nextQueue", q + 1);

                        Map<String, Object> nw = obj(
                            "id", UUID.randomUUID().toString(),
                            "patientId", "walkin-" + UUID.randomUUID(),
                            "patientName", patient,
                            "category", s(b.get("category")),
                            "age", s(b.get("age")),
                            "sex", s(b.get("sex")),
                            "mobile", s(b.get("mobile")),
                            "email", "",
                            "service", a.get("service"),
                            "date", a.get("date"),
                            "time", a.get("time"),
                            "queue", q,
                            "status", "Waiting",
                            "doctor", "To be assigned",
                            "createdAt", Instant.now().toString(),
                            "walkIn", true,
                            "reassignedFrom", a.get("id")
                        );

                        list(db.get("appointments")).add(nw);

                        a.put("reassignedTo", nw.get("id"));
                        a.put("status", "Re-Assigned");

                        activity(
                            "Released slot Q"
                            + a.get("queue")
                            + " reassigned to walk-in Q"
                            + q
                        );

                        save();

                        send(
                            x,
                            200,
                            obj(
                                "appointment", a,
                                "newAppointment", nw
                            )
                        );
                        return;

                    } else {
                        send(
                            x,
                            400,
                            obj(
                                "error",
                                "That action is not allowed for the current appointment status."
                            )
                        );
                        return;
                    }

                    activity(
                        "Q" + a.get("queue")
                        + " updated to "
                        + a.get("status")
                    );

                    save();

                    send(x, 200, obj("appointment", a));
                    return;
                }

                // WALK-IN REGISTRATION
                if (
                    path.equals("/api/staff/walkin")
                    && method.equals("POST")
                ) {
                    if (!staff(u)) {
                        send(x, 403, obj("error", "Staff access only."));
                        return;
                    }

                    String name = s(b.get("fullName"));

                    if (name.isBlank()) {
                        send(
                            x,
                            400,
                            obj("error", "Patient name is required.")
                        );
                        return;
                    }

                    int q = ((Number) db.get("nextQueue")).intValue();
                    db.put("nextQueue", q + 1);

                    Map<String, Object> a = obj(
                        "id", UUID.randomUUID().toString(),
                        "patientId", "walkin-" + UUID.randomUUID(),
                        "patientName", name,
                        "category", s(b.get("category")),
                        "age", s(b.get("age")),
                        "sex", s(b.get("sex")),
                        "mobile", s(b.get("mobile")),
                        "email", "",
                        "service", s(b.get("service")),
                        "date", LocalDate.now().toString(),
                        "time", LocalTime.now().toString(),
                        "queue", q,
                        "status", "Waiting",
                        "doctor", "To be assigned",
                        "createdAt", Instant.now().toString(),
                        "walkIn", true
                    );

                    list(db.get("appointments")).add(a);

                    activity(
                        "Walk-in patient "
                        + name
                        + " added as Q"
                        + q
                    );

                    save();

                    send(x, 200, obj("appointment", a));
                    return;
                }

                // STAFF PATIENT LIST
                if (
                    path.equals("/api/staff/patients")
                    && method.equals("GET")
                ) {
                    if (!staff(u)) {
                        send(x, 403, obj("error", "Staff access only."));
                        return;
                    }

                    List<Object> p = new ArrayList<>();

                    for (Object z : list(db.get("users"))) {
                        Map<String, Object> v = map(z);

                        if (s(v.get("role")).equals("patient")) {
                            p.add(
                                obj(
                                    "id", v.get("id"),
                                    "fullName", v.get("fullName"),
                                    "age", v.get("age"),
                                    "sex", v.get("sex"),
                                    "mobile", v.get("mobile"),
                                    "email", v.get("email"),
                                    "category", v.get("category")
                                )
                            );
                        }
                    }

                    send(x, 200, obj("patients", p));
                    return;
                }

                // STAFF VIEW PATIENT HISTORY
                if (
                    path.startsWith("/api/staff/patient/")
                    && method.equals("GET")
                ) {
                    if (!staff(u)) {
                        send(x, 403, obj("error", "Staff access only."));
                        return;
                    }

                    String id = path.substring(
                        "/api/staff/patient/".length()
                    );

                    Map<String, Object> p = findUser(id);

                    if (p == null) {
                        send(x, 404, obj("error", "Patient not found."));
                        return;
                    }

                    List<Object> hist = new ArrayList<>();

                    for (Object z : list(db.get("appointments"))) {
                        if (s(map(z).get("patientId")).equals(id)) {
                            hist.add(z);
                        }
                    }

                    send(
                        x,
                        200,
                        obj(
                            "patient",
                            obj(
                                "id", p.get("id"),
                                "fullName", p.get("fullName"),
                                "mobile", p.get("mobile"),
                                "email", p.get("email"),
                                "age", p.get("age"),
                                "sex", p.get("sex"),
                                "category", p.get("category")
                            ),
                            "history", hist
                        )
                    );
                    return;
                }

                send(
                    x,
                    404,
                    obj("error", "API endpoint not found.")
                );
                return;
            }

            // STATIC FILES
            String rel = path.equals("/")
                ? "index.html"
                : path.substring(1);

            Path publicRoot = ROOT.resolve("src/web").normalize();
            Path f = publicRoot.resolve(rel).normalize();

            if (
                !f.startsWith(publicRoot)
                || !Files.exists(f)
                || Files.isDirectory(f)
            ) {
                send(x, 404, obj("error", "Not found"));
                return;
            }

            String ct;

            if (rel.endsWith(".css")) {
                ct = "text/css";
            } else if (rel.endsWith(".js")) {
                ct = "application/javascript";
            } else if (rel.endsWith(".svg")) {
                ct = "image/svg+xml";
            } else if (rel.endsWith(".png")) {
                ct = "image/png";
            } else if (rel.endsWith(".jpg") || rel.endsWith(".jpeg")) {
                ct = "image/jpeg";
            } else if (rel.endsWith(".webp")) {
                ct = "image/webp";
            } else {
                ct = "text/html";
            }

            byte[] bytes = Files.readAllBytes(f);

            x.getResponseHeaders().set(
                "Content-Type",
                ct + "; charset=utf-8"
            );

            x.sendResponseHeaders(200, bytes.length);
            x.getResponseBody().write(bytes);
            x.close();

        } catch (IllegalArgumentException e) {
            send(x, 400, obj("error", e.getMessage()));

        } catch (Exception e) {
            e.printStackTrace();
            send(x, 500, obj("error", "Server error."));
        }
    }

    // START SERVER - AUTOMATIC PORT SELECTION
    public static void main(String[] args) throws Exception {
        load();

        HttpServer server = null;
        int actualPort = PORT;

        for (int i = 0; i <= 20; i++) {
            int tryPort = PORT + i;

            if (tryPort > 65535) {
                break;
            }

            try {
                server = HttpServer.create(
                    new InetSocketAddress(tryPort),
                    0
                );

                actualPort = tryPort;
                break;

            } catch (BindException e) {
                System.out.println(
                    "Port " + tryPort
                    + " is already in use. Trying another port..."
                );
            }
        }

        if (server == null) {
            throw new IOException(
                "No available port found. Please close another running server."
            );
        }

        server.createContext("/", Main::handle);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.out.println();
        System.out.println("======================================");
        System.out.println("          QUEUELESS CLINIC");
        System.out.println("======================================");
        System.out.println("Server started successfully!");
        System.out.println(
            "Website: http://localhost:" + actualPort
        );
        System.out.println("--------------------------------------");
        System.out.println("Demo staff accounts:");
        System.out.println(
            "Admin: admin@clinic.local / admin123"
        );
        System.out.println(
            "Doctor: doctor@clinic.local / doctor123"
        );
        System.out.println("======================================");
        System.out.println();
    }

    static void ensureDatabaseShape() {
        if (!(db.get("users") instanceof List)) {
            db.put("users", new ArrayList<>());
        }

        if (!(db.get("appointments") instanceof List)) {
            db.put("appointments", new ArrayList<>());
        }

        if (!(db.get("activities") instanceof List)) {
            db.put("activities", new ArrayList<>());
        }

        if (!(db.get("nextQueue") instanceof Number)) {
            db.put("nextQueue", 1);
        }
    }

    static String required(
        Map<String, Object> b,
        String key
    ) {
        String value = s(b.get(key)).trim();

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                key + " is required."
            );
        }

        return value;
    }

    static void validateAccountFields(
        String fullName,
        String username,
        String email,
        String mobile,
        String password,
        String confirmation
    ) {
        if (
            fullName.isBlank()
            || username.isBlank()
            || email.isBlank()
            || mobile.isBlank()
            || password.length() < 6
            || !password.equals(confirmation)
        ) {
            throw new IllegalArgumentException(
                "Please complete all fields. Passwords must match and contain at least 6 characters."
            );
        }

        if (!fullName.matches("[\\p{L} .'-]{2,100}")) {
            throw new IllegalArgumentException(
                "Enter a valid full name."
            );
        }

        if (!username.matches("[A-Za-z0-9_.-]{3,30}")) {
            throw new IllegalArgumentException(
                "Username must be 3-30 letters, numbers, dots, underscores, or hyphens."
            );
        }

        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException(
                "Enter a valid email address."
            );
        }

        if (!mobile.matches("[0-9+() -]{7,20}")) {
            throw new IllegalArgumentException(
                "Enter a valid mobile number."
            );
        }
    }

    static int integerField(
        Map<String, Object> b,
        String key,
        int min,
        int max
    ) {
        try {
            Object raw = b.get(key);

            int value = raw instanceof Number
                ? ((Number) raw).intValue()
                : Integer.parseInt(s(raw));

            if (
                raw instanceof Number
                && ((Number) raw).doubleValue() != value
            ) {
                throw new IllegalArgumentException(
                    "Invalid " + key + "."
                );
            }

            if (value < min || value > max) {
                throw new IllegalArgumentException(
                    "Invalid " + key + "."
                );
            }

            return value;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                "Invalid " + key + "."
            );
        }
    }

    static byte[] pbkdf2(
        String password,
        byte[] salt,
        int iterations,
        int bits
    ) throws GeneralSecurityException {

        KeySpec spec = new PBEKeySpec(
            password.toCharArray(),
            salt,
            iterations,
            bits
        );

        return SecretKeyFactory
            .getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec)
            .getEncoded();
    }

    static boolean verifyPassword(
        Map<String, Object> user,
        String password
    ) {
        String stored = s(user.get("password"));

        if (stored.startsWith("pbkdf2$")) {
            try {
                String[] p = stored.split("\\$", -1);

                if (p.length != 4) {
                    return false;
                }

                byte[] salt = Base64.getDecoder().decode(p[2]);
                byte[] expected = Base64.getDecoder().decode(p[3]);

                return MessageDigest.isEqual(
                    expected,
                    pbkdf2(
                        password,
                        salt,
                        Integer.parseInt(p[1]),
                        expected.length * 8
                    )
                );

            } catch (Exception e) {
                return false;
            }
        }

        boolean valid = MessageDigest.isEqual(
            stored.getBytes(StandardCharsets.UTF_8),
            legacyHash(password).getBytes(StandardCharsets.UTF_8)
        );

        if (valid) {
            user.put("password", hash(password));
        }

        return valid;
    }

    static String legacyHash(String x) {
        try {
            return Base64.getEncoder().encodeToString(
                MessageDigest
                    .getInstance("SHA-256")
                    .digest(x.getBytes(StandardCharsets.UTF_8))
            );

        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    static int parsePort(String value) {
        try {
            int port = Integer.parseInt(value);

            return port >= 1 && port <= 65535
                ? port
                : 8080;

        } catch (NumberFormatException e) {
            return 8080;
        }
    }

    // JSON HANDLING
    static class Json {

        static String stringify(Object o) {
            if (o == null) {
                return "null";
            }

            if (o instanceof String || o instanceof Character) {
                return "\"" + esc(String.valueOf(o)) + "\"";
            }

            if (o instanceof Number || o instanceof Boolean) {
                return o.toString();
            }

            if (o instanceof Map) {
                StringJoiner j = new StringJoiner(",", "{", "}");

                ((Map<?, ?>) o).forEach(
                    (k, v) -> j.add(
                        stringify(String.valueOf(k))
                        + ":"
                        + stringify(v)
                    )
                );

                return j.toString();
            }

            if (o instanceof Iterable) {
                StringJoiner j = new StringJoiner(",", "[", "]");

                for (Object v : (Iterable<?>) o) {
                    j.add(stringify(v));
                }

                return j.toString();
            }

            return stringify(String.valueOf(o));
        }

        static String esc(String s) {
            return s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        }

        static Object parse(String s) {
            return new Parser(s).value();
        }

        static class Parser {

            String s;
            int i = 0;

            Parser(String s) {
                this.s = s;
            }

            void ws() {
                while (
                    i < s.length()
                    && Character.isWhitespace(s.charAt(i))
                ) {
                    i++;
                }
            }

            Object value() {
                ws();

                if (i >= s.length()) {
                    return null;
                }

                char c = s.charAt(i);

                if (c == '{') {
                    return object();
                }

                if (c == '[') {
                    return array();
                }

                if (c == '"') {
                    return str();
                }

                if (s.startsWith("true", i)) {
                    i += 4;
                    return true;
                }

                if (s.startsWith("false", i)) {
                    i += 5;
                    return false;
                }

                if (s.startsWith("null", i)) {
                    i += 4;
                    return null;
                }

                int st = i;

                while (
                    i < s.length()
                    && "-0123456789.eE".indexOf(s.charAt(i)) >= 0
                ) {
                    i++;
                }

                try {
                    return Double.valueOf(s.substring(st, i));
                } catch (Exception e) {
                    return null;
                }
            }

            Map<String, Object> object() {
                Map<String, Object> m = new LinkedHashMap<>();

                i++;
                ws();

                while (
                    i < s.length()
                    && s.charAt(i) != '}'
                ) {
                    String k = str();

                    ws();
                    i++;

                    Object v = value();

                    m.put(k, v);

                    ws();

                    if (
                        i < s.length()
                        && s.charAt(i) == ','
                    ) {
                        i++;
                        ws();
                    } else {
                        break;
                    }
                }

                if (i < s.length()) {
                    i++;
                }

                return m;
            }

            List<Object> array() {
                List<Object> a = new ArrayList<>();

                i++;
                ws();

                while (
                    i < s.length()
                    && s.charAt(i) != ']'
                ) {
                    a.add(value());

                    ws();

                    if (
                        i < s.length()
                        && s.charAt(i) == ','
                    ) {
                        i++;
                        ws();
                    } else {
                        break;
                    }
                }

                if (i < s.length()) {
                    i++;
                }

                return a;
            }

            String str() {
                i++;

                StringBuilder b = new StringBuilder();

                while (i < s.length()) {
                    char c = s.charAt(i++);

                    if (c == '"') {
                        break;
                    }

                    if (c == '\\' && i < s.length()) {
                        char n = s.charAt(i++);

                        switch (n) {
                            case 'n':
                                b.append('\n');
                                break;

                            case 'r':
                                b.append('\r');
                                break;

                            case 't':
                                b.append('\t');
                                break;

                            default:
                                b.append(n);
                        }

                    } else {
                        b.append(c);
                    }
                }

                return b.toString();
            }
        }
    }
}
