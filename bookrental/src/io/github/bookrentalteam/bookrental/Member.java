package io.github.bookrentalteam.bookrental;

import java.util.UUID;

/**
 * Member 클래스는 도서 대여 시스템의 회원을 표현하는 불변(Immutable) 데이터 객체입니다.
 * - 회원은 고유한 UUID, 이름, 이메일, 비밀번호 해시값을 가집니다.
 * - record를 사용하여 간결하고 불변성을 보장합니다.
 */
public record Member(UUID id, String name, String email, String passwordHash) {

    /**
     * 생성자 (compact constructor)
     * - 객체 생성 시 필수적인 값 검증을 수행합니다.
     * - id가 null일 경우 자동으로 새로운 UUID를 생성합니다.
     * - name, email, passwordHash 값이 null 또는 공백일 경우 예외를 발생시킵니다.
     */
    public Member {
        // id가 null이면 새로운 UUID 자동 생성
        if (id == null) id = UUID.randomUUID();

        // 이름은 필수 값
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("이름은(는) 필수입니다.");
        }

        // 이메일은 필수 값
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일은(는) 필수입니다.");
        }

        // 비밀번호 해시는 필수 값
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("비밀번호는(는) 필수입니다.");
        }
    }

    /**
     * 사용자가 입력한 원본 비밀번호(rawPw)를 해시와 비교하여 인증 여부를 반환합니다.
     * 
     * @param rawPw 사용자가 입력한 비밀번호 (평문)
     * @return boolean 인증 성공 여부 (true: 인증 성공, false: 인증 실패)
     *
     * 내부적으로 Passwords.matches()를 호출하여
     * 입력값과 저장된 passwordHash를 비교합니다.
     */
    public boolean authenticate(String rawPw) {
        return Passwords.matches(rawPw, passwordHash);
    }
}
