package io.github.bookrentalteam.bookrental.domain;

import io.github.bookrentalteam.bookrental.common.exception.ValidationException;
import io.github.bookrentalteam.bookrental.common.security.Passwords;

/**
 * 회원 엔티티 (불변, auto-increment ID)
 */
public record Member(long id, String name, String email, String passwordHash, Role role) {
	private static long sequence = 0; // auto-increment

	public Member {
		if (name == null || name.isBlank()) {
			throw new ValidationException("이름은 필수입니다.");
		}
		if (email == null || email.isBlank()) {
			throw new ValidationException("이메일은 필수입니다.");
		}
		if (passwordHash == null || passwordHash.isBlank()) {
			throw new ValidationException("비밀번호는 필수입니다.");
		}
		if (role == null) {
			role = Role.USER;
		}
	}

	// 자동 ID 발급용 생성자
	public Member(String name, String email, String passwordHash, Role role) {
		this(++sequence, name, email, passwordHash, role);
	}

	public boolean authenticate(String rawPw) {
		return Passwords.matches(rawPw, passwordHash);
	}
}
