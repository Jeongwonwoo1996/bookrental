package io.github.bookrentalteam.bookrental.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import io.github.bookrentalteam.bookrental.common.exception.ValidationException;
import io.github.bookrentalteam.bookrental.common.security.Passwords;

public class Member {

	private Long id; // member_id (AUTO_INCREMENT)
	private String name;
	private String email;
	/** DB 컬럼명과 맞춤: 해시값 저장 전용 필드 */
	private String password; // hashed password
	private Role role; // USER / ADMIN
	private LocalDate suspendUntil; // 정지 종료일 (null이면 제재 없음)
	private LocalDateTime createdAt; // created_at
	private LocalDateTime updatedAt; // updated_at

	/** JDBC 매핑/리플렉션용 기본 생성자 */
	public Member() {
	}

	/** 신규 가입 시 생성자(해시를 이미 만들어 전달하는 형태) */
	public Member(String name, String email, String passwordHashed, Role role) {
		if (name == null || name.isBlank()) {
			throw new ValidationException("이름은 필수입니다.");
		}
		if (email == null || email.isBlank()) {
			throw new ValidationException("이메일은 필수입니다.");
		}
		if (passwordHashed == null || passwordHashed.isBlank()) {
			throw new ValidationException("비밀번호는 필수입니다.");
		}

		this.name = name.trim();
		this.email = email.trim();
		this.password = passwordHashed;
		this.role = (role != null) ? role : Role.USER;
	}

	// ===== 도메인 행위 =====
	/** 원문 비밀번호 검증 (저장값은 해시) */
	public boolean authenticate(String rawPw) {
		return Passwords.matches(rawPw, this.password);
	}

	/** 당일 포함 정지 여부 */
	public boolean isSuspended() {
		return suspendUntil != null && !suspendUntil.isBefore(LocalDate.now());
	}

	/** 정지 연장(누적) */
	public void suspend(int days) {
		if (days <= 0) {
			throw new ValidationException("정지 일수는 1일 이상이어야 합니다.");
		}
		LocalDate today = LocalDate.now();
		if (suspendUntil == null || suspendUntil.isBefore(today)) {
			suspendUntil = today.plusDays(days);
		} else {
			suspendUntil = suspendUntil.plusDays(days);
		}
	}

	// ===== Getter/Setter =====
	public Long getId() {
		return id;
	}

	/** DB INSERT 후 생성키 세팅용 */
	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		if (name == null || name.isBlank()) {
			throw new ValidationException("이름은 필수입니다.");
		}
		this.name = name.trim();
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		if (email == null || email.isBlank()) {
			throw new ValidationException("이메일은 필수입니다.");
		}
		this.email = email.trim();
	}

	/** 해시값 저장용 세터(원문 저장 금지) */
	public void setPasswordHashed(String passwordHashed) {
		if (passwordHashed == null || passwordHashed.isBlank()) {
			throw new ValidationException("비밀번호는 필수입니다.");
		}
		this.password = passwordHashed;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public LocalDate getSuspendUntil() {
		return suspendUntil;
	}

	public void setSuspendUntil(LocalDate suspendUntil) {
		this.suspendUntil = suspendUntil;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
