package io.github.bookrentalteam.bookrental.domain;

import java.time.LocalDate;

import io.github.bookrentalteam.bookrental.common.exception.ValidationException;
import io.github.bookrentalteam.bookrental.common.security.Passwords;

public class Member {
	private static long sequence = 0;

	private Long id;
	private String name;
	private String email;
	private String passwordHash;
	private Role role;
	private LocalDate suspendUntil; // 대여 정지 종료일(null이면 제재 없음)

	public Member(String name, String email, String passwordHash, Role role) {
		if (name == null || name.isBlank()) {
			throw new ValidationException("이름은 필수입니다.");
		}
		if (email == null || email.isBlank()) {
			throw new ValidationException("이메일은 필수입니다.");
		}
		if (passwordHash == null || passwordHash.isBlank()) {
			throw new ValidationException("비밀번호는 필수입니다.");
		}

		this.id = ++sequence;
		this.name = name;
		this.email = email;
		this.passwordHash = passwordHash;
		this.role = (role != null) ? role : Role.USER;
	}

	// getter
	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public Role getRole() {
		return role;
	}

	public LocalDate getSuspendUntil() {
		return suspendUntil;
	}

	// 비즈니스 로직
	public boolean authenticate(String rawPw) {
		return Passwords.matches(rawPw, passwordHash);
	}

	public boolean isSuspended() {
		return suspendUntil != null && suspendUntil.isAfter(LocalDate.now());
	}

	public void suspend(int days) {
		if (suspendUntil == null || suspendUntil.isBefore(LocalDate.now())) {
			suspendUntil = LocalDate.now().plusDays(days);
		} else {
			suspendUntil = suspendUntil.plusDays(days); // 기존 정지에 누적
		}

	}
}