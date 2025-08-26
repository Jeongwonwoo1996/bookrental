package io.github.bookrentalteam.bookrental.common.exception;

/** 비즈니스 규칙 위반 예외 */
public class BusinessException extends RuntimeException {
	public BusinessException(String message) {
		super(message);
	}
}
