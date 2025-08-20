package io.github.bookrentalteam.bookrental;

import java.util.UUID;

public record Member(UUID id, String name, String email, String passwordHash) {
    public Member {
        if (id == null) id = UUID.randomUUID();
        if (name == null || name.isBlank()) throw new IllegalArgumentException("이름은(는) 필수입니다.");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("이메일은(는) 필수입니다.");
        if (passwordHash == null || passwordHash.isBlank()) throw new IllegalArgumentException("비밀번호는(는) 필수입니다.");
    }

    public boolean authenticate(String rawPw) {
        return Passwords.matches(rawPw, passwordHash);
    }
}
