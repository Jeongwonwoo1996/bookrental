package io.github.bookrentalteam.bookrental.common.exception;

/** 입력값 검증 실패 예외 */
public class ValidationException extends RuntimeException {
	public ValidationException(String message) {
		super(message);
	}
}
