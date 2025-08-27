package io.github.bookrentalteam.bookrental;

import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

import io.github.bookrentalteam.bookrental.domain.Book;
import io.github.bookrentalteam.bookrental.domain.Member;
import io.github.bookrentalteam.bookrental.domain.Rental;
import io.github.bookrentalteam.bookrental.domain.RentalStatus;
import io.github.bookrentalteam.bookrental.domain.Role;
import io.github.bookrentalteam.bookrental.repository.BookRepository;
import io.github.bookrentalteam.bookrental.repository.MemberRepository;
import io.github.bookrentalteam.bookrental.repository.RentalRepository;
import io.github.bookrentalteam.bookrental.repository.impl.InMemoryBookRepository;
import io.github.bookrentalteam.bookrental.repository.impl.InMemoryMemberRepository;
import io.github.bookrentalteam.bookrental.repository.impl.InMemoryRentalRepository;
import io.github.bookrentalteam.bookrental.service.BookService;
import io.github.bookrentalteam.bookrental.service.MemberService;
import io.github.bookrentalteam.bookrental.service.RentalService;
import io.github.bookrentalteam.bookrental.service.impl.BookServiceImpl;
import io.github.bookrentalteam.bookrental.service.impl.MemberServiceImpl;
import io.github.bookrentalteam.bookrental.service.impl.RentalServiceImpl;

/**
 * 콘솔 테스트용 App - 회원가입, 로그인, 로그아웃 제공 - 도서/대여 기능은 [TODO] 표시만 남겨둠 (나중에 구현 예정) - 더미
 * 회원 데이터(seed) 포함
 */
public class App {

	private static final Scanner sc = new Scanner(System.in);

	// Repository 생성
	private static final MemberRepository memberRepository = new InMemoryMemberRepository();
	private static final BookRepository bookRepository = new InMemoryBookRepository();
	private static final RentalRepository rentalRepository = new InMemoryRentalRepository();

	// Service 생성 (의존성 주입)
	private static final MemberService memberService = new MemberServiceImpl(memberRepository);
	private static final BookService bookService = new BookServiceImpl(bookRepository);
	private static final RentalService rentalService = new RentalServiceImpl(rentalRepository, bookService);

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
				} else {
					showMainMenu();
					int sel = promptInt("선택");

					if (memberService.getCurrentUser().getRole() == Role.ADMIN) { // 관리자 메뉴
						switch (sel) {
						case 1 -> addBookFlow();
						case 2 -> listBooksFlow();
						case 3 -> searchBookFlow();
						case 4 -> rentBookFlow();
						case 5 -> returnBookFlow();
						case 6 -> extendRentalFlow();
						case 7 -> myRentalsFlow();
						case 0 -> logout();
						default -> System.out.println("[오류] 메뉴 번호를 다시 선택해주세요.");
						}
					} else { // 일반 사용자 메뉴
						switch (sel) {
						case 1 -> listBooksFlow();
						case 2 -> searchBookFlow();
						case 3 -> rentBookFlow();
						case 4 -> returnBookFlow();
						case 5 -> extendRentalFlow();
						case 6 -> myRentalsFlow();
						case 0 -> logout();
						default -> System.out.println("[오류] 메뉴 번호를 다시 선택해주세요.");
						}
					}
				}
			} catch (InputMismatchException e) {
				System.out.println("[오류] 숫자를 입력해주세요.");
			} catch (Exception e) {
				System.out.println("[오류] " + e.getMessage());
			}
		}
	}

	// 도서 대여
	private static void rentBookFlow() {
		System.out.println("[대여 가능한 도서 목록]");
		var books = bookService.listBooks();
		books.stream().filter(b -> b.getAvailableCopies() > 0)
				.forEach(b -> System.out.printf("ID=%d, 제목=%s, 저자=%s, 재고: 현재 대여 가능 권수=%d / 총 보유 권수=%d%n", b.getId(),
						b.getTitle(), b.getAuthor(), b.getAvailableCopies(), b.getTotalCopies()));

		System.out.print("대여할 도서 ID> ");
		long bookId = Long.parseLong(sc.nextLine().trim());
		Member current = memberService.getCurrentUser();
		Rental rental = rentalService.rentBook(bookId, current);
		System.out.println("[성공] 도서 대여 완료: rentalId=" + rental.getId());
	}

	// 도서 반납
	private static void returnBookFlow() {
		Member currentUser = memberService.getCurrentUser();
		System.out.println("[내 대여 목록]");
		var rentals = rentalService.getRentalsByMember(currentUser);
		rentals.stream().filter(r -> r.getStatus() == RentalStatus.RENTED).forEach(
				r -> System.out.printf("대여ID=%d, BookId=%d, 반납예정일=%s%n", r.getId(), r.getBookId(), r.getDueAt()));

		System.out.print("반납할 대여 ID> ");
		long rentalId = Long.parseLong(sc.nextLine().trim());
		Rental rental = rentalService.returnBook(rentalId);
		System.out.println("[성공] 도서 반납 완료: rentalId=" + rental.getId());
	}

	// 내 대여 목록
	private static void myRentalsFlow() {
		Member current = memberService.getCurrentUser();
		System.out.println("[내 대여 목록]");
		rentalService.getRentalsByMember(current).forEach(r -> {
			String returnedAt = (r.getReturnedAt() != null) ? r.getReturnedAt().toString() : "대여 진행중"; // ✅ 여기서 사용

			System.out.printf("대여ID=%d, BookID=%d, 상태=%s, 대여일=%s, 반납예정일=%s, 반납완료일=%s, 연장횟수=%d%n", r.getId(),
					r.getBookId(), r.getStatus(), r.getRentedAt(), r.getDueAt(), returnedAt, r.getExtensionCount());
		});
	}

	private static void extendRentalFlow() {
		Member current = memberService.getCurrentUser();
		System.out.println("[연장 가능한 대여 목록]");
		var rentals = rentalService.getRentalsByMember(current);
		rentals.stream().filter(r -> r.getStatus() == RentalStatus.RENTED).forEach(r -> {
			String returnedAt = (r.getReturnedAt() != null) ? r.getReturnedAt().toString() : "대여 진행중"; // ✅ 동일하게 적용

			System.out.printf("대여ID=%d, BookID=%d, 상태=%s, 대여일=%s, 반납예정일=%s, 반납완료일=%s, 연장횟수=%d%n", r.getId(),
					r.getBookId(), r.getStatus(), r.getRentedAt(), r.getDueAt(), returnedAt, r.getExtensionCount());
		});

		System.out.print("연장할 대여 ID> ");
		long rentalId = Long.parseLong(sc.nextLine().trim());

		try {
			Rental rental = rentalService.extendRental(rentalId);
			System.out.println("[성공] 대여 연장 완료: rentalId=" + rental.getId() + ", 반납예정일=" + rental.getDueAt());
		} catch (Exception e) {
			System.out.println("[오류] " + e.getMessage());
		}
	}

	// 책 리스트 출력 //
	private static void listBooksFlow() {
		// 서비스로부터 책 목록을 가져오기
		List<Book> books = bookService.listBooks();

		// 책 목록이 비어있는지 확인
		if (books.isEmpty()) {
			System.out.println("등록된 도서가 없습니다.");
		} else {
			// 목록에 있는 각 책의 정보를 출력
			for (Book book : books) {
				System.out.printf("ID: %d, 제목: %s, 저자: %s, 재고: 현재 대여 가능 권수=%d / 총 보유 권수=%d%n", book.getId(),
						book.getTitle(), book.getAuthor(), book.getAvailableCopies(), book.getTotalCopies());
			}
		}
	}

	// 책 추가 //
	private static void addBookFlow() {
		try {
			System.out.println("[도서 등록]");
			System.out.print("ISBN: ");
			String isbn = App.sc.nextLine().trim();
			System.out.print("제목: ");
			String title = App.sc.nextLine().trim();
			System.out.print("저자: ");
			String author = App.sc.nextLine().trim();
			System.out.print("보유 권수: ");
			int totalCopies = Integer.parseInt(App.sc.nextLine().trim());

			// App 클래스의 static 필드인 bookService에 직접 접근
			Book book = bookService.registerBook(isbn, title, author, totalCopies);
			System.out.printf("등록 완료 (ID: %d, 제목: %s)\n", book.getId(), book.getTitle());

		} catch (NumberFormatException e) {
			System.out.println("오류: 보유 권수는 숫자로 입력해야 합니다.");
		} catch (IllegalStateException e) {
			System.out.println("오류: " + e.getMessage());
		} catch (Exception e) {
			System.out.println("알 수 없는 오류가 발생했습니다: " + e.getMessage());
		}
	}

	// 책 검색 //
	private static void searchBookFlow() {
		System.out.println("\n--- 도서 검색 (제목, 저자 또는 ISBN) ---");
		System.out.print("검색어 입력: ");
		String keyword = App.sc.nextLine().trim();

		if (keyword.trim().isEmpty()) {
			System.out.println("검색어를 입력해주세요.");
			System.out.println("--------------------");
			return;
		}

		List<Book> foundBooks = bookService.searchBooks(keyword);

		if (foundBooks.isEmpty()) {
			System.out.printf("'%s'에 대한 검색 결과가 없습니다.\n", keyword);
		} else {
			System.out.printf("'%s' 검색 결과 (%d건)\n", keyword, foundBooks.size());
			for (Book book : foundBooks) {
				System.out.printf("ID: %d, 제목: %s, 저자: %s, ISBN: %s\n, 재고: 현재 대여 가능 권수=%d / 총 보유 권수=%d%n", book.getId(),
						book.getTitle(), book.getAuthor(), book.getIsbn(), book.getAvailableCopies(),
						book.getTotalCopies());
			}
		}
	}

	private static void showWelcome() {
		System.out.println("=== 도서 대여 시스템 ===");
		System.out.println("1) 회원가입   2) 로그인   3) 종료");
	}

	private static void showMainMenu() {
		Member currentUser = memberService.getCurrentUser();

		System.out.printf("=== 메인 메뉴 (로그인: %s, 권한: %s) ===%n", currentUser.getName(), currentUser.getRole());

		if (currentUser.getRole() == Role.ADMIN) {
			System.out
					.println("1) 도서 등록   2) 도서 목록   3) 도서 검색   4) 도서 대여   5) 도서 반납   6) 대여 연장   7) 내 대여 목록   0) 로그아웃");
		} else {
			System.out.println("1) 도서 목록   2) 도서 검색   3) 도서 대여   4) 도서 반납   5) 대여 연장   6) 내 대여 목록   0) 로그아웃");
		}
	}

	private static void signUpFlow() {
		System.out.println("[회원가입]");
		System.out.print("이름> ");
		String name = sc.nextLine().trim();
		System.out.print("이메일> ");
		String email = sc.nextLine().trim();
		System.out.print("비밀번호> ");
		String pw = sc.nextLine().trim();

		// 다음과 같은 형식으로 가입될 경우 ADMIN 권한 부여
		Role role = email.equalsIgnoreCase("admin@admin.com") ? Role.ADMIN : Role.USER;

		try {
			Member m = memberService.signUp(name, email, pw, role);
			System.out.println("[성공] 회원가입 완료: " + m.getName());
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
			System.out.println("[성공] 로그인: " + m.getName());
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

	private static void seed() {
		try {
			memberService.signUp("정원우", "wonwoo@test.com", "1234", Role.USER);
			memberService.signUp("김태영", "taeyoung@test.com", "1234", Role.USER);
			memberService.signUp("관리자", "admin@admin.com", "1234", Role.ADMIN);
		} catch (Exception ignore) {
			// 이미 등록된 경우는 무시
		}

		bookService.registerBook("978-89-7914-874-9", "자바의 정석", "남궁성", 5);
		bookService.registerBook("978-89-98142-35-3", "토비의 스프링 Vol.1", "이일민", 2);
		bookService.registerBook("978-89-98142-36-0", "토비의 스프링 Vol.2", "이일민", 2);

	}
}
