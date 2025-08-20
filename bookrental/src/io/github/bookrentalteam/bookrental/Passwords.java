package io.github.bookrentalteam.bookrental;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * 비밀번호 해시/검증 유틸리티 (데모/학습용)
 *
 * ⚠️ 보안 주의
 * - 이 구현은 SHA-256 단방향 해시만 사용하며, 소금(salt)·후추(pepper)·반복(iteration)·메모리 하드닝이 없습니다.
 * - 따라서 실제 서비스의 사용자 비밀번호 저장/검증 용도로는 부적합합니다.
 * - 운영 환경에서는 반드시 BCrypt / PBKDF2 / scrypt / Argon2 등의 "비밀번호 전용" KDF를 사용하세요.
 *   (예: Spring Security PasswordEncoder, jBCrypt, BouncyCastle, argon2-jvm 등)
 *
 * 설계 메모
 * - 클래스는 package-private(기본 접근) 유틸리티로 두고 정적 메서드만 제공합니다.
 * - 해시는 소문자 16진수 문자열로 반환합니다.
 * - 예외는 간단히 RuntimeException으로 래핑(데모 용도).
 */
class Passwords {

    /**
     * 평문 비밀번호를 SHA-256으로 해싱해 16진수 문자열로 반환합니다.
     *
     * 처리 순서
     * 1) UTF-8로 바이트 변환
     * 2) MessageDigest(SHA-256)로 해시 생성
     * 3) 바이트 배열을 2자리 16진수 문자열(hex)로 변환
     *
     * @param raw 평문 비밀번호(널 허용 X)
     * @return 소문자 16진수 해시 문자열(길이 64)
     *
     * 보안 노트
     * - salt/iteration이 없으므로 무차별 대입/레인보우 테이블 공격에 취약합니다.
     * - 실제 서비스에서는 KDF를 사용하고, 가능한 한 서버 측 pepper(비밀 키)도 적용하세요.
     */
    static String hash(String raw) {
        try {
            // SHA-256 인스턴스 획득 (표준 알고리즘 이름)
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // 입력을 UTF-8 바이트로 변환 후 해시 계산
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));

            // 해시 바이트 → 소문자 16진수 문자열로 변환
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (Exception e) {
            // NoSuchAlgorithmException 등 발생 시 간단 래핑
            // (실서비스라면 로깅/에러 코드/복구 흐름을 설계하세요)
            throw new RuntimeException(e);
        }
    }

    /**
     * 평문 비밀번호가 주어진 해시와 일치하는지 검사합니다.
     *
     * 동작
     * - 입력(raw)을 동일 방식으로 해싱한 뒤 문자열 equals로 비교.
     *
     * @param raw  평문 비밀번호
     * @param hash 저장된 해시(SHA-256 16진수 문자열)
     * @return 일치 여부
     *
     * 보안 노트
     * - 문자열 equals는 타이밍 공격에 덜 안전할 수 있습니다.
     *   (상대적으로 안전한 비교를 원하면 MessageDigest.isEqual(byte[], byte[]) 사용 권장)
     * - 그러나 가장 중요한 점은 이 구현 자체가 "비밀번호 전용 KDF"가 아니라는 것입니다.
     *   운영 환경에서는 BCrypt/PBKDF2/scrypt/Argon2 기반 검증을 사용하세요.
     */
    static boolean matches(String raw, String hash) {
        return hash(raw).equals(hash);
        // 개선 예시(참고):
        // return MessageDigest.isEqual(hash(raw).getBytes(StandardCharsets.UTF_8),
        //                              hash.getBytes(StandardCharsets.UTF_8));
    }
}
