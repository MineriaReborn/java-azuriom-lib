package fr.mineria.azauth;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AzAuth {

    private final String baseUrl;
    private final Gson gson;
    private boolean debug;

    private AzAuth(String baseUrl) {
        String site = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.baseUrl = site + "/api/auth";
        this.gson = new Gson();
    }

    public static AzAuth withBaseUrl(String baseUrl) {
        return new AzAuth(baseUrl);
    }

    public AzAuth debug(boolean debug) {
        this.debug = debug;
        return this;
    }

    // ======================= AUTHENTICATION =======================

    public User authenticate(String email, String password) throws IOException, AzAuthException {
        return authenticateInternal(email, password, null);
    }

    public User authenticate(String email, String password, String code2FA) throws IOException, AzAuthException {
        return authenticateInternal(email, password, code2FA);
    }

    private User authenticateInternal(String email, String password, String code2FA) throws IOException, AzAuthException {
        JsonObject req = new JsonObject();
        req.addProperty("email", email);
        req.addProperty("password", password);
        if (code2FA != null && !code2FA.trim().isEmpty()) {
            req.addProperty("code", code2FA);
        }
        JsonObject resp = post("/authenticate", req, true);
        return gson.fromJson(resp, User.class);
    }

    // ======================= VERIFICATION ========================

    public User verify(String accessToken) throws IOException, AzAuthException {
        JsonObject req = new JsonObject();
        req.addProperty("access_token", accessToken);
        JsonObject resp = post("/verify", req, true);
        return gson.fromJson(resp, User.class);
    }

    // ======================= LOGOUT =============================

    public void logout(String accessToken) throws IOException, AzAuthException {
        JsonObject req = new JsonObject();
        req.addProperty("access_token", accessToken);
        post("/logout", req, false);
    }

    // ======================= HTTP & JSON TOOLS =======================

    private JsonObject post(String path, JsonObject body, boolean expectJson) throws IOException, AzAuthException {
        String url = baseUrl + path;
        if (debug) System.out.println("[AzAuth] POST " + url + " | Body: " + body);

        URI uri = URI.create(url);
        URL urlObj = uri.toURL();
        HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
        
        try {
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(gson.toJson(body).getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
            String response = readAll(is);

            if (debug) System.out.println("[AzAuth] HTTP " + status + " | Response: " + response);

            if (status >= 200 && status < 300) {
                if (!expectJson) return null;
                if (response == null || response.trim().isEmpty()) return new JsonObject();
                return parseJsonObject(response);
            } else {
                JsonObject errJson = tryParseJson(response);
                String reason = errJson != null ? getString(errJson, "reason") : "";
                String message = errJson != null ? getString(errJson, "message") : response;
                String statusStr = errJson != null ? getString(errJson, "status") : "error";
                throw new AzAuthException(statusStr, reason, message, status);
            }
        } finally {
            conn.disconnect();
        }
    }

    private String readAll(InputStream is) throws IOException {
        if (is == null) return "";
        StringBuilder sb = new StringBuilder();
        try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            char[] buf = new char[2048];
            int n;
            while ((n = reader.read(buf)) > 0) sb.append(buf, 0, n);
        }
        return sb.toString();
    }

    private JsonObject parseJsonObject(String json) throws AzAuthException {
        try {
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            if (debug) e.printStackTrace();
            throw new AzAuthException("error", "json_parse", "Réponse JSON invalide : " + json, 0);
        }
    }

    private JsonObject tryParseJson(String json) {
        try {
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            if (debug) System.out.println("[AzAuth] (Non-JSON error): " + json);
            return null;
        }
    }

    private static String getString(JsonObject o, String key) {
        if (o == null || !o.has(key)) return null;
        JsonElement e = o.get(key);
        return e.isJsonNull() ? null : e.getAsString();
    }

    // ======================== USER MODEL ========================

    public static class User {
        @SerializedName("id")
        private int id;
        @SerializedName("username")
        private String username;
        @SerializedName("uuid")
        private String uuid;
        @SerializedName("email_verified")
        private boolean emailVerified;
        @SerializedName("money")
        private double money;
        @SerializedName("role")
        private Role role;
        @SerializedName("banned")
        private boolean banned;
        @SerializedName("created_at")
        private String createdAt;
        @SerializedName("access_token")
        private String accessToken;

        public int getId() { return id; }
        public String getUsername() { return username; }
        public String getUuid() { return uuid; }
        public boolean isEmailVerified() { return emailVerified; }
        public double getMoney() { return money; }
        public Role getRole() { return role; }
        public boolean isBanned() { return banned; }
        public String getCreatedAt() { return createdAt; }
        public String getAccessToken() { return accessToken; }

        @Override
        public String toString() {
            return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", uuid='" + uuid + '\'' +
                ", emailVerified=" + emailVerified +
                ", money=" + money +
                ", role=" + role +
                ", banned=" + banned +
                ", createdAt='" + createdAt + '\'' +
                ", accessToken='" + (accessToken != null ? "****" : null) + '\'' +
                '}';
        }

        public static class Role {
            @SerializedName("name")
            private String name;
            @SerializedName("color")
            private String color;
            public String getName() { return name; }
            public String getColor() { return color; }

            @Override
            public String toString() {
                return "Role{" +
                    "name='" + name + '\'' +
                    ", color='" + color + '\'' +
                    '}';
            }
        }
    }

    // ======================== EXCEPTION ========================

    public static class AzAuthException extends Exception {
        private static final long serialVersionUID = 1L;
        private final String status;
        private final String reason;
        private final String message;
        private final int httpStatus;

        public AzAuthException(String status, String reason, String message, int httpStatus) {
            super("[" + status + (reason == null || reason.isEmpty() ? "" : "/" + reason) + "] " + message + " (HTTP " + httpStatus + ")");
            this.status = status;
            this.reason = reason;
            this.message = message;
            this.httpStatus = httpStatus;
        }

        public String getStatus() { return status; }
        public String getReason() { return reason; }
        @Override public String getMessage() { return message; }
        public int getHttpStatus() { return httpStatus; }
    }
}