package io.github.bookrentalteam.bookrental.common.validation;

import java.util.regex.Pattern;

/**
 * 정규식 유틸 클래스
 */
public class Patterns {
    private static final Pattern EMAIL =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public static boolean isEmail(String s) {
        return EMAIL.matcher(s).matches();
    }
}
