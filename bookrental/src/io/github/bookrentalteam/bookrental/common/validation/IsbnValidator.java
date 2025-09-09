package io.github.bookrentalteam.bookrental.common.validation;

public final class IsbnValidator {
	private IsbnValidator() {
	}

	public static boolean isValid(String raw) {
		if (raw == null) {
			return false;
		}
		String s = raw.replaceAll("[^0-9Xx]", ""); // 하이픈/공백 제거
		return isValid10(s) || isValid13(s);
	}

	private static boolean isValid10(String s) {
		if (s.length() != 10) {
			return false;
		}
		int sum = 0;
		for (int i = 0; i < 9; i++) {
			char c = s.charAt(i);
			if (!Character.isDigit(c)) {
				return false;
			}
			sum += (10 - i) * (c - '0');
		}
		char last = s.charAt(9);
		int val = (last == 'X' || last == 'x') ? 10 : (Character.isDigit(last) ? last - '0' : -1);
		if (val < 0) {
			return false;
		}
		sum += val;
		return sum % 11 == 0;
	}

	private static boolean isValid13(String s) {
		if (s.length() != 13) {
			return false;
		}
		for (int i = 0; i < 13; i++) {
			if (!Character.isDigit(s.charAt(i))) {
				return false;
			}
		}
		int sum = 0;
		for (int i = 0; i < 12; i++) {
			int d = s.charAt(i) - '0';
			sum += (i % 2 == 0) ? d : d * 3;
		}
		int check = (10 - (sum % 10)) % 10;
		return check == (s.charAt(12) - '0');
	}
}