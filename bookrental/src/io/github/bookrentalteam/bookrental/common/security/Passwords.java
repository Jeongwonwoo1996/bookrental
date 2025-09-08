package io.github.bookrentalteam.bookrental.common.security;

import org.mindrot.jbcrypt.BCrypt;

public final class Passwords {
	private static final int COST = 10; // 작업비용(라운드)

	/** 비밀번호 해시(bcrypt) */
	public static String hash(String rawPw) {
		return BCrypt.hashpw(rawPw, BCrypt.gensalt(COST));
	}

	/** 비밀번호 비교(bcrypt) */
	public static boolean matches(String rawPw, String hashedPw) {
		return hashedPw != null && !hashedPw.isBlank() && BCrypt.checkpw(rawPw, hashedPw);
	}
}
