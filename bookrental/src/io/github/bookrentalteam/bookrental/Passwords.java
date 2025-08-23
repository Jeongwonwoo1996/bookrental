package io.github.bookrentalteam.bookrental;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 비밀번호 해시/검증 유틸
 * - 간단히 SHA-256 기반 (실무에서는 BCrypt/Scrypt 권장)
 */
public class Passwords {

    /** 비밀번호 해시 */
    public static String hash(String rawPw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(rawPw.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("해시 알고리즘 오류", e);
        }
    }

    /** 비밀번호 비교 */
    public static boolean matches(String rawPw, String hashedPw) {
        return hash(rawPw).equals(hashedPw);
    }
}
