package io.github.bookrentalteam.bookrental.common.security;

import org.mindrot.jbcrypt.BCrypt;

public final class Passwords {
	private static final int COST = 10;

	/** 비밀번호 해시(bcrypt) — jBCrypt는 기본적으로 $2a$ 형식으로 생성 */
	public static String hash(String rawPw) {
		return BCrypt.hashpw(rawPw, BCrypt.gensalt(COST));
	}

	/** 비밀번호 비교 — $2b$/$2y$를 $2a$로 정규화 후 비교 */
	public static boolean matches(String rawPw, String hashedPw) {
		if (hashedPw == null || hashedPw.isBlank()) {
			return false;
		}
		String normalized = hashedPw.replaceFirst("^\\$2[by]\\$", "\\$2a\\$");
		return BCrypt.checkpw(rawPw, normalized);
	}
}
