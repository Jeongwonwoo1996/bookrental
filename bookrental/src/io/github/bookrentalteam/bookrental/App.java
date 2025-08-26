package io.github.bookrentalteam.bookrental;

import java.util.InputMismatchException;
import java.util.Scanner;

import io.github.bookrentalteam.bookrental.domain.Member;
import io.github.bookrentalteam.bookrental.domain.Role;
import io.github.bookrentalteam.bookrental.service.MemberService;
import io.github.bookrentalteam.bookrental.service.impl.MemberServiceImpl;

/**
 * 콘솔 테스트용 App - 회원가입, 로그인, 로그아웃만 제공 - 더미 회원 데이터(seed) 포함
 */
public class App {

	private static final Scanner sc = new Scanner(System.in);

	// 서비스 구현체
	private static final MemberService memberService = new MemberServiceImpl();

	public static void main(String[] args) {
		seed(); // 더미 회원 등록

		while (true) {
			try {
				if (memberService.getCurrentUser() == null) { // 로그인 안 된 상태
					showWelcome();
					int sel = promptInt("선택");
					switch (sel) {
					case 1 -> signUpFlow();
					case 2 -> loginFlow();
					case 3 -> {
						System.out.println("이용해주셔서 감사합니다.");
						return;
					}
					default -> System.out.println("[오류] 메뉴 번호를 다시 선택해주세요.");
					}
				} else { // 로그인 된 상태
					showMainMenu();
					int sel = promptInt("선택");
					switch (sel) {
					case 0 -> logout();
					default -> System.out.println("[오류] 메뉴 번호를 다시 선택해주세요.");
					}
				}
			} catch (InputMismatchException e) {
				System.out.println("[오류] 숫자를 입력해주세요.");
			} catch (Exception e) {
				System.out.println("[오류] " + e.getMessage());
			}
		}
	}

	private static void showWelcome() {
		System.out.println("=== 도서 대여 시스템 ===");
		System.out.println("1) 회원가입   2) 로그인   3) 종료");
	}

	private static void showMainMenu() {
		Member currentUser = memberService.getCurrentUser();
		System.out.printf("=== 메인 메뉴 (로그인: %s, 권한: %s) ===%n", currentUser.name(), currentUser.role());
		System.out.println("0) 로그아웃");
	}

	private static void signUpFlow() {
		System.out.println("[회원가입]");
		System.out.print("이름> ");
		String name = sc.nextLine().trim();
		System.out.print("이메일> ");
		String email = sc.nextLine().trim();
		System.out.print("비밀번호> ");
		String pw = sc.nextLine().trim();

		// 간단히 admin@admin.com 이면 ADMIN 권한
		Role role = email.equalsIgnoreCase("admin@admin.com") ? Role.ADMIN : Role.USER;

		try {
			Member m = memberService.signUp(name, email, pw, role);
			System.out.println("[성공] 회원가입 완료: " + m);
		} catch (Exception e) {
			System.out.println("[오류] " + e.getMessage());
		}
	}

	private static void loginFlow() {
		System.out.println("[로그인]");
		System.out.print("이메일> ");
		String email = sc.nextLine().trim();
		System.out.print("비밀번호> ");
		String pw = sc.nextLine().trim();

		try {
			Member m = memberService.login(email, pw);
			System.out.println("[성공] 로그인: " + m.name());
		} catch (Exception e) {
			System.out.println("[오류] " + e.getMessage());
		}
	}

	private static void logout() {
		memberService.logout();
		System.out.println("[안내] 로그아웃 되었습니다.");
	}

	private static int promptInt(String label) {
		System.out.print(label + "> ");
		String s = sc.nextLine().trim();
		return Integer.parseInt(s);
	}

	/**
	 * 더미 회원 데이터 등록
	 */
	private static void seed() {
		try {
			memberService.signUp("정원우", "wonwoo@test.com", "1234", Role.USER);
			memberService.signUp("김태영", "taeyoung@test.com", "1234", Role.USER);
			memberService.signUp("관리자", "admin@admin.com", "1234", Role.ADMIN);
		} catch (Exception ignore) {
			// 이미 등록된 경우는 무시
		}
	}
}
