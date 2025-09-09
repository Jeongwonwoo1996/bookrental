package io.github.bookrentalteam.bookrental.service.impl;

import io.github.bookrentalteam.bookrental.common.exception.BusinessException;
import io.github.bookrentalteam.bookrental.common.exception.ValidationException;
import io.github.bookrentalteam.bookrental.common.security.Passwords;
import io.github.bookrentalteam.bookrental.domain.Member;
import io.github.bookrentalteam.bookrental.domain.Role;
import io.github.bookrentalteam.bookrental.repository.MemberRepository;
import io.github.bookrentalteam.bookrental.service.MemberService;

public class MemberServiceImpl implements MemberService {

	private final MemberRepository memberRepository; // 주입받는 저장소
	private Member currentUser = null; // 로그인 상태 저장 (콘솔 앱 기준)

	// 생성자에서 Repository 주입
	public MemberServiceImpl(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	@Override
	public Member signUp(String name, String email, String pw, Role role) {
		// --- 입력값 검증 ---
		if (name == null || name.isBlank()) {
			throw new ValidationException("이름은 필수입니다.");
		}
		if (email == null || email.isBlank()) {
			throw new ValidationException("이메일은 필수입니다.");
		}
		if (pw == null || pw.isBlank()) {
			throw new ValidationException("비밀번호는 필수입니다.");
		}

		String trimmedEmail = email.trim();

		// --- 이메일 중복 체크 ---
		// MemberRepository.findByEmail(...)이 Optional이 아니라 Member를 반환하도록 전환했다면 아래처럼
		// 사용합니다.
		// (Optional로 남겨두셨다면 .isPresent() / .orElse(null)로 바꿔 주세요)
		Member existing = memberRepository.findByEmail(trimmedEmail);
		if (existing != null) {
			throw new BusinessException("이미 등록된 이메일입니다.");
		}

		// --- 비밀번호 해싱 & 도메인 생성 ---
		String hashed = Passwords.hash(pw);
		Member m = new Member(name.trim(), trimmedEmail, hashed, role != null ? role : Role.USER);

		// --- 저장 ---
		memberRepository.save(m); // 반환값(long) 있으면 무시해도 무방

		return m;
	}

	@Override
	public Member login(String email, String pw) {
		if (email == null || email.isBlank()) {
			throw new ValidationException("이메일은 필수입니다.");
		}
		if (pw == null || pw.isBlank()) {
			throw new ValidationException("비밀번호는 필수입니다.");
		}

		Member m = memberRepository.findByEmail(email.trim());
		if (m == null || !m.authenticate(pw)) {
			throw new BusinessException("이메일 또는 비밀번호가 올바르지 않습니다.");
		}

		if (m.isSuspended()) {
			throw new BusinessException("대여 정지 중입니다. 종료일: " + m.getSuspendUntil());
		}

		currentUser = m;
		return m;
	}

	@Override
	public Member getCurrentUser() {
		return currentUser;
	}

	@Override
	public void logout() {
		currentUser = null;
	}
}
