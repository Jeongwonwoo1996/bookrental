package io.github.bookrentalteam.bookrental;

import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

public class App {
	private static final Scanner sc = new Scanner(System.in);
	private static final LibraryService service = new LibraryService();
	private static Member currentUser = null;

	public static void main(String[] args) {
		seed();

		while (true) {
			try {
				if (currentUser == null) {
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
				} else {
					showMainMenu();
					int sel = promptInt("선택");

					if (currentUser.role() == Role.ADMIN) { // 관리자 메뉴
						switch (sel) {
						case 1 -> addBookFlow();
						case 2 -> listBooksFlow();
						case 3 -> searchFlow();
						case 4 -> rentFlow();
						case 5 -> returnFlow();
						case 6 -> myRentalsFlow();
						case 0 -> logout();
						default -> System.out.println("[오류] 메뉴 번호를 다시 선택해주세요.");
						}
					} else { // 일반 사용자 메뉴
						switch (sel) {
						case 1 -> listBooksFlow();
						case 2 -> searchFlow();
						case 3 -> rentFlow();
						case 4 -> returnFlow();
						case 5 -> myRentalsFlow();
						case 0 -> logout();
						default -> System.out.println("[오류] 메뉴 번호를 다시 선택해주세요.");
						}
					}
				}
			} catch (InputMismatchException e) {
				System.out.println("[오류] 숫자를 입력해주세요.");
			} catch (IllegalArgumentException | IllegalStateException e) {
				System.out.println("[오류] " + e.getMessage());
				pause();
			} catch (Exception e) {
				System.out.println("[오류] 예상치 못한 오류: " + e.getMessage());
				pause();
			}
		}
	}

	// -------------------- 메뉴 출력 --------------------
	private static void showWelcome() {
		System.out.println("=== 도서 대여 시스템 ===");
		System.out.println("1) 회원가입   2) 로그인   3) 종료");
	}

	private static void showMainMenu() {
		System.out.printf("=== 메인 메뉴 (로그인: %s, 권한: %s) ===%n", currentUser.name(), currentUser.role());

		if (currentUser.role() == Role.ADMIN) {
			System.out.println("1) 도서등록   2) 도서목록   3) 도서검색   4) 대여   5) 반납   6) 내 대여목록   0) 로그아웃");
		} else {
			System.out.println("1) 도서목록   2) 도서검색   3) 대여   4) 반납   5) 내 대여목록   0) 로그아웃");
		}
	}

	// -------------------- 기능 플로우 --------------------
	private static void signUpFlow() {
		System.out.println("[회원가입]");
		String name = promptNonEmpty("이름");
		String email = promptNonEmpty("이메일");

		if (!Patterns.isEmail(email)) {
			throw new IllegalArgumentException("형식이 올바르지 않습니다. (이메일)");
		}

		String pw = promptNonEmpty("비밀번호");

		// 관리자 계정 자동 부여 규칙
		Role role = email.equalsIgnoreCase("admin@admin.com") ? Role.ADMIN : Role.USER; // 이메일이 admin@admin.com과 같으면 관리자
																						// 아니면 사용자

		Member m = service.signUp(name, email, pw, role);
		System.out.println("[성공] 회원가입 완료 (memberId=" + m.id() + ", role=" + m.role() + ")");
		pause();
	}

	private static void loginFlow() {
		System.out.println("[로그인]");
		String email = promptNonEmpty("이메일");
		String pw = promptNonEmpty("비밀번호");

		Member m = service.login(email, pw);
		currentUser = m;
		System.out.println("[성공] 로그인되었습니다. (" + currentUser.name() + ", " + currentUser.role() + ")");
	}

	private static void addBookFlow() {
		System.out.println("[도서 등록] (0 입력 시 취소)");

		String isbn = promptNonEmpty("ISBN");
		if (isbn.equals("0")) {
			System.out.println("[안내] 도서 등록이 취소되었습니다.");
			return;
		}
		String title = promptNonEmpty("제목");
		if (title.equals("0")) {
			System.out.println("[안내] 도서 등록이 취소되었습니다.");
			return;
		}
		String author = promptNonEmpty("저자");
		if (author.equals("0")) {
			System.out.println("[안내] 도서 등록이 취소되었습니다.");
			return;
		}
		int total = promptInt("총권수 (0 입력 시 취소)");
		if (total == 0) {
			System.out.println("[안내] 도서 등록이 취소되었습니다.");
			return;
		}

		Book b = service.addBook(isbn, title, author, total);
		System.out.printf("[성공] 등록 완료: bookId=%d, 재고=%d/%d%n", b.id(), b.availableCopies(), b.totalCopies());
	}

	private static void listBooksFlow() {
		List<Book> list = service.listBooks();
		System.out.println("[도서 목록] 총 " + list.size() + "권");
		System.out.printf("%-4s %-20s %-20s %-16s %-8s%n", "ID", "ISBN", "제목", "저자", "재고/총");

		for (Book b : list) {
			System.out.printf("%-4d %-20s %-20s %-16s %d/%d%n", b.id(), b.isbn(), truncate(b.title(), 20),
					truncate(b.author(), 16), b.availableCopies(), b.totalCopies());
		}
		pause(); // 그대로 유지
	}

	private static void searchFlow() {
		System.out.println("[도서 검색] (0 입력 시 취소)");
		String keyword = promptNonEmpty("키워드(제목/저자/ISBN)");
		if (keyword.equals("0")) {
			System.out.println("[안내] 도서 검색이 취소되었습니다.");
			return;
		}

		List<Book> list = service.searchBooks(keyword);

		if (list.isEmpty()) {
			System.out.println("검색 결과가 없습니다.");
		} else {
			System.out.println("검색 결과(" + list.size() + "건)");
			System.out.printf("%-4s %-20s %-20s %-16s %-8s%n", "ID", "ISBN", "제목", "저자", "재고/총");
			for (Book b : list) {
				System.out.printf("%-4d %-20s %-20s %-16s %d/%d%n", b.id(), b.isbn(), truncate(b.title(), 20),
						truncate(b.author(), 16), b.availableCopies(), b.totalCopies());
			}
		}
		pause();
	}

	private static void rentFlow() {
		ensureLogin();
		System.out.println("[대여 가능한 도서 목록]");

		List<Book> list = service.listBooks().stream().filter(b -> b.availableCopies() > 0).toList();

		if (list.isEmpty()) {
			System.out.println("[안내] 현재 대여 가능한 도서가 없습니다.");
			pause();
			return;
		}

		System.out.printf("%-4s %-20s %-20s %-16s %-8s%n", "ID", "ISBN", "제목", "저자", "재고/총");
		for (Book b : list) {
			System.out.printf("%-4d %-20s %-20s %-16s %d/%d%n", b.id(), b.isbn(), truncate(b.title(), 20),
					truncate(b.author(), 16), b.availableCopies(), b.totalCopies());
		}

		System.out.println("0 입력 시 취소 후 메뉴로 돌아갑니다.");
		long bookId = promptInt("대여할 bookId 입력");
		if (bookId == 0) {
			System.out.println("[안내] 대여가 취소되었습니다.");
			return;
		}

		Rental r = service.rent(bookId, currentUser.id());
		Book after = service.getBook(bookId);

		System.out.printf("[성공] 대여 완료: rentalId=%d, 반납예정일=%s, 재고=%d/%d%n", r.id(), r.dueAt(), after.availableCopies(),
				after.totalCopies());
		pause();
	}

	private static void returnFlow() {
		ensureLogin();
		System.out.println("[내가 반납할 수 있는 도서 목록]");

		List<Rental> list = service.rentalsByMember(currentUser.id()).stream()
				.filter(r -> r.status() == RentalStatus.RENTED).toList();

		if (list.isEmpty()) {
			System.out.println("[안내] 반납할 수 있는 도서가 없습니다.");
			pause();
			return;
		}

		System.out.printf("%-4s %-20s %-18s %-12s %-12s %-10s%n", "ID", "책제목", "ISBN", "대여일", "예정일", "상태");

		for (Rental r : list) {
			Book b = service.getBook(r.bookId());
			System.out.printf("%-4d %-20s %-18s %-12s %-12s %-10s%n", r.id(),
					b != null ? truncate(b.title(), 20) : "(삭제됨)", b != null ? b.isbn() : "-", r.rentedAt(), r.dueAt(),
					r.status());
		}

		System.out.println("0 입력 시 취소 후 메뉴로 돌아갑니다.");
		long rentalId = promptInt("반납할 rentalId 입력");
		if (rentalId == 0) {
			System.out.println("[안내] 반납이 취소되었습니다.");
			return;
		}

		Rental r = service.returnBook(rentalId);
		Book after = service.getBook(r.bookId());

		System.out.printf("[성공] 반납 완료: status=%s, 재고=%d/%d%n", r.status(), after.availableCopies(),
				after.totalCopies());
		pause();
	}

	private static void myRentalsFlow() {
		ensureLogin();
		List<Rental> list = service.rentalsByMember(currentUser.id());

		if (list.isEmpty()) {
			System.out.println("[안내] 현재/과거 대여 이력이 없습니다.");
			pause();
			return;
		}

		System.out.println("[나의 대여 목록]");
		System.out.printf("%-4s %-20s %-18s %-12s %-12s %-10s%n", "ID", "책제목", "ISBN", "대여일", "예정일", "상태");

		for (Rental r : list) {
			Book b = service.getBook(r.bookId());
			String title = b != null ? truncate(b.title(), 20) : "(삭제됨)";
			String isbn = b != null ? truncate(b.isbn(), 18) : "-";

			System.out.printf("%-4d %-20s %-18s %-12s %-12s %-10s%n", r.id(), title, isbn, r.rentedAt(), r.dueAt(),
					r.status());
		}
		pause();
	}

	private static void logout() {
		currentUser = null;
		System.out.println("[안내] 로그아웃되었습니다.");
	}

	// -------------------- 공용 유틸 --------------------
	private static void ensureLogin() {
		if (currentUser == null) {
			throw new IllegalStateException("로그인 후 이용해주세요.");
		}
	}

	private static String promptNonEmpty(String label) {
		System.out.print(label + "> ");
		String s = sc.nextLine().trim();
		if (s.isEmpty()) {
			throw new IllegalArgumentException(label + "은(는) 필수입니다.");
		}
		return s;
	}

	private static int promptInt(String label) {
		System.out.print(label + "> ");
		String s = sc.nextLine().trim();
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			throw new InputMismatchException();
		}
	}

	private static void pause() {
		System.out.println("엔터를 누르면 메뉴로 돌아갑니다...");
		sc.nextLine();
	}

	private static String truncate(String s, int n) {
		if (s == null) {
			return "";
		}
		return s.length() <= n ? s : s.substring(0, n - 1) + "…";
	}

	// 초기값 //
	private static void seed() {
		try {
			service.signUp("정원우", "wonwoo@test.com", "1234", Role.USER);
			service.signUp("김태영", "taeyoung@test.com", "1234", Role.USER);
			service.signUp("관리자", "admin@test.com", "1234", Role.ADMIN);
		} catch (Exception ignore) {
		}

		service.addBook("978-89-7914-874-9", "자바의 정석", "남궁성", 5);
		service.addBook("978-89-98142-35-3", "토비의 스프링 Vol.1", "이일민", 2);
		service.addBook("978-89-98142-36-0", "토비의 스프링 Vol.2", "이일민", 2);
	}
}

// 정규식 // 
class Patterns {
	private static final java.util.regex.Pattern EMAIL = java.util.regex.Pattern
			.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

	static boolean isEmail(String s) {
		return EMAIL.matcher(s).matches();
	}
}
