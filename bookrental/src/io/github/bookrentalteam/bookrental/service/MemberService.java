package io.github.bookrentalteam.bookrental.service;

import io.github.bookrentalteam.bookrental.domain.Member;
import io.github.bookrentalteam.bookrental.domain.Role;

public interface MemberService {
	/** 회원가입 */
	Member signUp(String name, String email, String password, Role role);

	/** 로그인 */
	Member login(String email, String password);

	/** 현재 로그인된 사용자 반환 */
	Member getCurrentUser();

	/** 로그아웃 */
	void logout();
}
