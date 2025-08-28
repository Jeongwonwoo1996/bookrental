package io.github.bookrentalteam.bookrental;

import java.lang.reflect.Field;
import java.time.LocalDate;
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

public class App {

	private static final Scanner sc = new Scanner(System.in);

	// ANSI 색상
	private static final String RESET = "\u001B[0m";
	private static final String RED = "\u001B[31m";
	private static final String GREEN = "\u001B[32m";
	private static final String YELLOW = "\u001B[33m";
	private static final String CYAN = "\u001B[36m";

	// Repository 생성
	private static final MemberRepository memberRepository = new InMemoryMemberRepository();
	private static final BookRepository bookRepository = new InMemoryBookRepository();
	private static final RentalRepository rentalRepository = new InMemoryRentalRepository();

	// Service 생성 (의존성 주입)
	private static final MemberService memberService = new MemberServiceImpl(memberRepository);
	private static final BookService bookService = new BookServiceImpl(bookRepository);
	private static final RentalService rentalService = new RentalServiceImpl(rentalRepository, memberRepository,
			bookService);

	public static void main(String[] args) {
		seed(); // 더미 회원 등록

		while (true) {
			try {
				if (memberService.getCurrentUser() == null) { // 로그인 안 된 상태
					showWelcome();
					int sel = promptInt("👉 메뉴 선택");

					switch (sel) {
					case 1 -> signUpFlow();
					case 2 -> loginFlow();
					case 3 -> {
						System.out.println(GREEN + "\n👋 이용해주셔서 감사합니다." + RESET);
						return;
					}
					default -> System.out.println(RED + "❌ [오류] 올바른 메뉴 번호를 입력해주세요." + RESET);
					}
				} else {
					showMainMenu();
					int sel = promptInt("👉 메뉴 선택");

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
						default -> System.out.println(RED + "❌ [오류] 올바른 메뉴 번호를 입력해주세요." + RESET);
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
						default -> System.out.println(RED + "❌ [오류] 올바른 메뉴 번호를 입력해주세요." + RESET);
						}
					}
				}
			} catch (InputMismatchException e) {
				System.out.println(RED + "❌ [오류] 숫자를 입력해주세요." + RESET);
			} catch (Exception e) {
				System.out.println(RED + "❌ [오류] " + e.getMessage() + RESET);
			}
		}
	}

	// 도서 대여
	private static void rentBookFlow() {
		Member current = memberService.getCurrentUser();
		var availableBooks = bookService.listBooks().stream().filter(b -> b.getAvailableCopies() > 0).toList();

		if (availableBooks.isEmpty()) {
			System.out.println(YELLOW + "⚠️ [안내] 현재 대여 가능한 도서가 없습니다." + RESET);
			return;
		}

		System.out.println(CYAN + "\n📖 [대여 가능한 도서 목록]" + RESET);
		availableBooks.forEach(b -> System.out.printf("  ▶ ID=%d | 제목=%s | 저자=%s | 재고=%d/%d%n", b.getId(), b.getTitle(),
				b.getAuthor(), b.getAvailableCopies(), b.getTotalCopies()));

		System.out.print("📌 대여할 도서 ID 입력> ");
		long bookId = Long.parseLong(sc.nextLine().trim());

		try {
			Rental rental = rentalService.rentBook(bookId, current);
			Book book = bookService.getBook(rental.getBookId());
			String bookTitle = (book != null) ? book.getTitle() : "(알 수 없음)";
			System.out.println(GREEN + "✅ [성공] '" + bookTitle + "' 도서 대여 완료!" + RESET);
		} catch (Exception e) {
			System.out.println(RED + "❌ [오류] " + e.getMessage() + RESET);
		}
	}

	// 도서 반납
	private static void returnBookFlow() {
		Member currentUser = memberService.getCurrentUser();
		var rentals = rentalService.getRentalsByMember(currentUser);
		var rentedBooks = rentals.stream().filter(r -> r.getStatus() == RentalStatus.RENTED).toList();

		if (rentedBooks.isEmpty()) {
			System.out.println(YELLOW + "⚠️ [안내] 반납할 도서가 없습니다." + RESET);
			return;
		}

		System.out.println(CYAN + "\n📚 [내 대여 목록]" + RESET);
		rentedBooks.forEach(r -> {
			Book book = bookService.getBook(r.getBookId());
			String bookTitle = (book != null) ? book.getTitle() : "(알 수 없음)";
			System.out.printf("  ▶ 대여ID=%d | 도서명=%s | 반납예정일=%s%n", r.getId(), bookTitle, r.getDueAt());
		});

		System.out.print("↩️ 반납할 대여 ID 입력> ");
		long rentalId = Long.parseLong(sc.nextLine().trim());

		try {
			Rental rental = rentalService.returnBook(rentalId);
			Book book = bookService.getBook(rental.getBookId());
			String bookTitle = (book != null) ? book.getTitle() : "(알 수 없음)";
			System.out.println(GREEN + "✅ [성공] '" + bookTitle + "' 도서 반납 완료!" + RESET);
		} catch (Exception e) {
			System.out.println(RED + "❌ [오류] " + e.getMessage() + RESET);
		}
	}

	// 대여 연장
	private static void extendRentalFlow() {
		Member current = memberService.getCurrentUser();
		var rentals = rentalService.getRentalsByMember(current);
		var extendable = rentals.stream().filter(r -> r.getStatus() == RentalStatus.RENTED).toList();

		if (extendable.isEmpty()) {
			System.out.println(YELLOW + "⚠️ [안내] 연장할 도서가 없습니다." + RESET);
			return;
		}

		System.out.println(CYAN + "\n🔄 [연장 가능한 대여 목록]" + RESET);
		extendable.forEach(r -> {
			String returnedAt = (r.getReturnedAt() != null) ? r.getReturnedAt().toString() : "대여 진행중";
			Book book = bookService.getBook(r.getBookId());
			String bookTitle = (book != null) ? book.getTitle() : "(알 수 없음)";
			System.out.printf("  ▶ 대여ID=%d | 도서명=%s | 상태=%s | 대여일=%s | 반납예정일=%s | 반납완료일=%s | 연장횟수=%d%n", r.getId(),
					bookTitle, r.getStatus(), r.getRentedAt(), r.getDueAt(), returnedAt, r.getExtensionCount());
		});

		System.out.print("🔄 연장할 대여 ID 입력> ");
		long rentalId = Long.parseLong(sc.nextLine().trim());

		try {
			Rental rental = rentalService.extendRental(rentalId);
			Book book = bookService.getBook(rental.getBookId());
			String bookTitle = (book != null) ? book.getTitle() : "(알 수 없음)";
			System.out.println(GREEN + "✅ [성공] '" + bookTitle + "' 대여 연장 완료! 새 반납예정일=" + rental.getDueAt() + RESET);
		} catch (Exception e) {
			System.out.println(RED + "❌ [오류] " + e.getMessage() + RESET);
		}
	}

	// 내 대여 목록
	private static void myRentalsFlow() {
		Member current = memberService.getCurrentUser();
		var rentals = rentalService.getRentalsByMember(current);

		if (rentals.isEmpty()) {
			System.out.println(YELLOW + "⚠️ [안내] 대여 중인 도서가 없습니다." + RESET);
			return;
		}

		System.out.println(CYAN + "\n📝 [내 대여 목록]" + RESET);
		rentals.forEach(r -> {
			String returnedAt = (r.getReturnedAt() != null) ? r.getReturnedAt().toString() : "대여 진행중";
			Book book = bookService.getBook(r.getBookId());
			String bookTitle = (book != null) ? book.getTitle() : "(알 수 없음)";
			System.out.printf("  ▶ 대여ID=%d | 도서명=%s | 상태=%s | 대여일=%s | 반납예정일=%s | 반납완료일=%s | 연장횟수=%d%n", r.getId(),
					bookTitle, r.getStatus(), r.getRentedAt(), r.getDueAt(), returnedAt, r.getExtensionCount());
		});
	}

	// 도서 목록
	private static void listBooksFlow() {
		List<Book> books = bookService.listBooks();

		if (books.isEmpty()) {
			System.out.println(YELLOW + "⚠️ 등록된 도서가 없습니다." + RESET);
		} else {
			System.out.println(CYAN + "\n📚 [도서 목록]" + RESET);
			books.forEach(b -> System.out.printf("  ▶ ID=%d | 제목=%s | 저자=%s | 재고=%d/%d%n", b.getId(), b.getTitle(),
					b.getAuthor(), b.getAvailableCopies(), b.getTotalCopies()));
		}
	}

	// 도서 등록
	private static void addBookFlow() {
		try {
			System.out.println(CYAN + "\n📕 [도서 등록]" + RESET);
			System.out.print("📖 ISBN 입력> ");
			String isbn = sc.nextLine().trim();
			System.out.print("📘 제목 입력> ");
			String title = sc.nextLine().trim();
			System.out.print("✍️ 저자 입력> ");
			String author = sc.nextLine().trim();
			System.out.print("📦 보유 권수 입력> ");
			int totalCopies = Integer.parseInt(sc.nextLine().trim());

			Book book = bookService.registerBook(isbn, title, author, totalCopies);
			System.out.printf(GREEN + "✅ [성공] 등록 완료! (ID=%d, 제목=%s)\n" + RESET, book.getId(), book.getTitle());
		} catch (NumberFormatException e) {
			System.out.println(RED + "❌ [오류] 보유 권수는 숫자로 입력해야 합니다." + RESET);
		} catch (Exception e) {
			System.out.println(RED + "❌ [오류] " + e.getMessage() + RESET);
		}
	}

	// 도서 검색
	private static void searchBookFlow() {
		System.out.println(CYAN + "\n🔍 [도서 검색]" + RESET);
		System.out.print("검색어 입력 (제목, 저자 또는 ISBN)> ");
		String keyword = sc.nextLine().trim();

		if (keyword.isEmpty()) {
			System.out.println(YELLOW + "⚠️ 검색어를 입력해주세요." + RESET);
			return;
		}

		List<Book> foundBooks = bookService.searchBooks(keyword);

		if (foundBooks.isEmpty()) {
			System.out.printf(YELLOW + "⚠️ '%s'에 대한 검색 결과가 없습니다.\n" + RESET, keyword);
		} else {
			System.out.printf(CYAN + "📖 '%s' 검색 결과 (%d건)\n" + RESET, keyword, foundBooks.size());
			foundBooks.forEach(b -> System.out.printf("  ▶ ID=%d | 제목=%s | 저자=%s | ISBN=%s | 재고=%d/%d%n", b.getId(),
					b.getTitle(), b.getAuthor(), b.getIsbn(), b.getAvailableCopies(), b.getTotalCopies()));
		}
	}

	private static void showWelcome() {
		System.out.println(CYAN + "======================================");
		System.out.println("        📚 도서 대여 시스템         ");
		System.out.println("======================================" + RESET);
		System.out.println("1) 📝 회원가입   2) 🔑 로그인   3) 🚪 종료");
	}

	private static void showMainMenu() {
		Member currentUser = memberService.getCurrentUser();

		System.out.println(CYAN + "\n======================================");
		System.out.printf(" 👤 로그인: %s  |  권한: %s%n", currentUser.getName(), currentUser.getRole());
		System.out.println("======================================" + RESET);

		if (currentUser.getRole() == Role.ADMIN) {
			System.out.println("1) 📕 도서 등록");
			System.out.println("2) 📚 도서 목록");
			System.out.println("3) 🔍 도서 검색");
			System.out.println("4) 📖 도서 대여");
			System.out.println("5) ↩️ 도서 반납");
			System.out.println("6) 🔄 대여 연장");
			System.out.println("7) 📝 내 대여 목록");
			System.out.println("0) 🚪 로그아웃");
		} else {
			System.out.println("1) 📚 도서 목록");
			System.out.println("2) 🔍 도서 검색");
			System.out.println("3) 📖 도서 대여");
			System.out.println("4) ↩️ 도서 반납");
			System.out.println("5) 🔄 대여 연장");
			System.out.println("6) 📝 내 대여 목록");
			System.out.println("0) 🚪 로그아웃");
		}
		System.out.println(CYAN + "======================================" + RESET);
	}

	// 회원가입
	private static void signUpFlow() {
		System.out.println(CYAN + "\n📝 [회원가입]" + RESET);
		System.out.print("👤 이름 입력> ");
		String name = sc.nextLine().trim();
		System.out.print("📧 이메일 입력> ");
		String email = sc.nextLine().trim();
		System.out.print("🔑 비밀번호 입력> ");
		String pw = sc.nextLine().trim();

		Role role = email.equalsIgnoreCase("admin@admin.com") ? Role.ADMIN : Role.USER;

		try {
			Member m = memberService.signUp(name, email, pw, role);
			System.out.println(GREEN + "✅ [성공] 회원가입 완료: " + m.getName() + RESET);
		} catch (Exception e) {
			System.out.println(RED + "❌ [오류] " + e.getMessage() + RESET);
		}
	}

	// 로그인
	private static void loginFlow() {
		System.out.println(CYAN + "\n🔑 [로그인]" + RESET);
		System.out.print("📧 이메일 입력> ");
		String email = sc.nextLine().trim();
		System.out.print("🔑 비밀번호 입력> ");
		String pw = sc.nextLine().trim();

		try {
			Member m = memberService.login(email, pw);
			System.out.println(GREEN + "✅ [성공] 로그인: " + m.getName() + RESET);
		} catch (Exception e) {
			System.out.println(RED + "❌ [오류] " + e.getMessage() + RESET);
		}
	}

	// 로그아웃
	private static void logout() {
		memberService.logout();
		System.out.println(YELLOW + "🚪 로그아웃 되었습니다." + RESET);
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

			Member overdueUser = memberService.signUp("연체회원", "overdue@test.com", "1234", Role.USER);

			bookService.registerBook("978-89-7914-874-9", "자바의 정석", "남궁성", 5);
			bookService.registerBook("978-89-98142-35-3", "토비의 스프링 Vol.1", "이일민", 2);
			bookService.registerBook("978-89-98142-36-0", "토비의 스프링 Vol.2", "이일민", 2);

			var overdueBook = bookService.registerBook("978-89-94492-00-1", "자바의 정석 4판", "남궁성", 1);
			Rental overdueRental = new Rental(overdueBook.getId(), overdueUser.getId());
			Field rentedAtField = Rental.class.getDeclaredField("rentedAt");
			Field dueAtField = Rental.class.getDeclaredField("dueAt");
			rentedAtField.setAccessible(true);
			dueAtField.setAccessible(true);

			LocalDate rentedAt = LocalDate.now().minusDays(20);
			rentedAtField.set(overdueRental, rentedAt);
			dueAtField.set(overdueRental, rentedAt.plusDays(14));

			overdueBook.rent();
			rentalRepository.save(overdueRental);

		} catch (Exception ignore) {
		}
	}
}
