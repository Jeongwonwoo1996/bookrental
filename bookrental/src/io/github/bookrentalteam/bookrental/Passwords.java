package io.github.bookrentalteam.bookrental;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

class Passwords {
    static String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    static boolean matches(String raw, String hash) { return hash(raw).equals(hash); }
}
