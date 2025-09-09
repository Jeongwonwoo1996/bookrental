package io.github.bookrentalteam.bookrental.domain;

/**
 * 대여 상태
 */
public enum RentalStatus {
	OPEN, // 대여 중 (현재 대여가 진행중인 상태)
	CLOSED // 대여 완료 (모든 대여가 완료된 상태)
}
