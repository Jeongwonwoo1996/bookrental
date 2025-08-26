package io.github.bookrentalteam.bookrental.service.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import io.github.bookrentalteam.bookrental.common.security.Passwords;
import io.github.bookrentalteam.bookrental.domain.Member;
import io.github.bookrentalteam.bookrental.domain.Role;
import io.github.bookrentalteam.bookrental.service.MemberService;

public class MemberServiceImpl implements MemberService {

	private final Map<Long, Member> members = new LinkedHashMap<>();
	private Member currentUser = null; // 로그인 상태 저장

	@Override
	public Member signUp(String name, String email, String pw, Role role) {
		if (members.values().stream().anyMatch(m -> m.email().equalsIgnoreCase(email))) {
			throw new IllegalStateException("이미 등록된 이메일입니다.");
		}
		String hashed = Passwords.hash(pw);
		Member m = new Member(name, email, hashed, role);
		members.put(m.id(), m);
		return m;
	}

	@Override
	public Member login(String email, String pw) {
		Member m = members.values().stream().filter(mem -> mem.email().equalsIgnoreCase(email))
				.filter(mem -> mem.authenticate(pw)).findFirst()
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
