package io.github.bookrentalteam.bookrental.service.impl;

import io.github.bookrentalteam.bookrental.common.security.Passwords;
import io.github.bookrentalteam.bookrental.domain.Member;
import io.github.bookrentalteam.bookrental.domain.Role;
import io.github.bookrentalteam.bookrental.repository.MemberRepository;
import io.github.bookrentalteam.bookrental.service.MemberService;

public class MemberServiceImpl implements MemberService {

	private final MemberRepository memberRepository; // 주입받는 저장소
	private Member currentUser = null; // 로그인 상태 저장

	// 생성자에서 Repository 주입
	public MemberServiceImpl(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	@Override
	public Member signUp(String name, String email, String pw, Role role) {
		// 이메일 중복 체크
		if (memberRepository.findByEmail(email).isPresent()) {
			throw new IllegalStateException("이미 등록된 이메일입니다.");
		}

		// 비밀번호 해싱
		String hashed = Passwords.hash(pw);
		Member m = new Member(name, email, hashed, role);

		// 저장
		memberRepository.save(m);
		return m;
	}

	@Override
	public Member login(String email, String pw) {
		Member m = memberRepository.findByEmail(email).filter(mem -> mem.authenticate(pw))
				.orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

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
