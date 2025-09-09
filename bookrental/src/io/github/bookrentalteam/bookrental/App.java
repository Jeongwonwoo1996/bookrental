package io.github.bookrentalteam.bookrental;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;

import io.github.bookrentalteam.bookrental.config.ConnectionManager;
import io.github.bookrentalteam.bookrental.domain.Book;
import io.github.bookrentalteam.bookrental.domain.Member;
import io.github.bookrentalteam.bookrental.domain.Rental;
import io.github.bookrentalteam.bookrental.domain.RentalDetail;
import io.github.bookrentalteam.bookrental.domain.Role;
import io.github.bookrentalteam.bookrental.repository.BookRepository;
import io.github.bookrentalteam.bookrental.repository.MemberRepository;
import io.github.bookrentalteam.bookrental.repository.RentalDetailRepository;
import io.github.bookrentalteam.bookrental.repository.RentalRepository;
import io.github.bookrentalteam.bookrental.repository.jdbc.JdbcBookRepository;
import io.github.bookrentalteam.bookrental.repository.jdbc.JdbcMemberRepository;
import io.github.bookrentalteam.bookrental.repository.jdbc.JdbcRentalDetailRepository;
import io.github.bookrentalteam.bookrental.repository.jdbc.JdbcRentalRepository;
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

	// ===== JDBC Repository 주입 =====
	private static final MemberRepository memberRepository = new JdbcMemberRepository();
	private static final BookRepository bookRepository = new JdbcBookRepository();
	private static final RentalRepository rentalRepository = new JdbcRentalRepository();
	private static final RentalDetailRepository rentalDetailRepository = new JdbcRentalDetailRepository();

	// ===== Service 주입 =====
	private static final MemberService memberService = new MemberServiceImpl(memberRepository);
	private static final BookService bookService = new BookServiceImpl(bookRepository);
	private static final RentalService rentalService = new RentalServiceImpl(rentalRepository, rentalDetailRepository,
			bookRepository, memberRepository);

	public static void main(String[] args) {
		while (true) {
			try {
				if (memberService.getCurrentUser() == null) { // 로그인 전
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
				} else { // 로그인 후
					showMainMenu();
					int sel = promptInt("👉 메뉴 선택");

					Member cu = memberService.getCurrentUser();
					boolean isSuspended = cu.getSuspendUntil() != null
							&& !cu.getSuspendUntil().isBefore(LocalDate.now());
					boolean hasOverdue = rentalService.existsOverdueByMember(cu.getId());

					if (cu.getRole() == Role.ADMIN) { // 관리자
						// 선택 즉시 차단 (ADMIN: 대여=4, 연장=6)
						if ((sel == 4 || sel == 6) && (isSuspended || hasOverdue)) {
							System.out.println(YELLOW + "⚠️ [안내] "
									+ (isSuspended ? ("대여 정지 상태입니다. " + cu.getSuspendUntil() + "까지 이용 불가")
											: "연체 중인 도서가 있어 대여/연장이 제한됩니다.")
									+ RESET);
							continue;
						}
						switch (sel) {
						case 1 -> addBookFlow();
						case 2 -> listBooksFlow();
						case 3 -> searchBookFlow();
						case 4 -> rentBooksFlow(); // 다건 대여
						case 5 -> returnBooksFlow(); // 다건 반납
						case 6 -> extendBooksFlow(); // 다건 연장
						case 7 -> myRentalsFlow();
						case 0 -> logout();
						default -> System.out.println(RED + "❌ [오류] 올바른 메뉴 번호를 입력해주세요." + RESET);
						}
					} else { // 일반 사용자
						// 선택 즉시 차단 (USER: 대여=3, 연장=5)
						if ((sel == 3 || sel == 5) && (isSuspended || hasOverdue)) {
							System.out.println(YELLOW + "⚠️ [안내] "
									+ (isSuspended ? ("대여 정지 상태입니다. " + cu.getSuspendUntil() + "까지 이용 불가")
											: "연체 중인 도서가 있어 대여/연장이 제한됩니다.")
									+ RESET);
							continue;
						}
						switch (sel) {
						case 1 -> listBooksFlow();
						case 2 -> searchBookFlow();
						case 3 -> rentBooksFlow();
						case 4 -> returnBooksFlow();
						case 5 -> extendBooksFlow();
						case 6 -> myRentalsFlow();
						case 0 -> logout();
						default -> System.out.println(RED + "❌ [오류] 올바른 메뉴 번호를 입력해주세요." + RESET);
						}
					}
				}
			} catch (NumberFormatException e) {
				System.out.println(RED + "❌ [오류] 숫자를 입력해주세요." + RESET);
			} catch (Exception e) {
				System.out.println(RED + "❌ [오류] " + (e.getMessage() != null ? e.getMessage() : e.toString()) + RESET);
			}
		}
	}

	// ==========================
	// 대여(다건)
	// ==========================
	// returnBooksFlow() 교체
	private static void rentBooksFlow() {
		Member current = memberService.getCurrentUser();

		// ✅ 메뉴 진입 즉시 차단: 대여 정지
		boolean isSuspended = current.getSuspendUntil() != null && !current.getSuspendUntil().isBefore(LocalDate.now());
		if (isSuspended) {
			System.out.println(YELLOW + "⚠️ [안내] 대여 정지 상태입니다. " + current.getSuspendUntil() + "까지 대여할 수 없습니다." + RESET);
			return;
		}

		// ✅ 메뉴 진입 즉시 차단: 연체 보유
		if (rentalService.existsOverdueByMember(current.getId())) {
			System.out.println(YELLOW + "⚠️ [안내] 연체 중인 도서가 있어 대여할 수 없습니다. 먼저 연체 도서를 반납해주세요." + RESET);
			return;
		}

		var availableBooks = bookService.listBooks().stream().filter(b -> b.getAvailableCopies() > 0).toList();

		if (availableBooks.isEmpty()) {
			System.out.println(YELLOW + "⚠️ [안내] 현재 대여 가능한 도서가 없습니다." + RESET);
			return;
		}

		System.out.println(CYAN + "\n📖 [대여 가능한 도서 목록]" + RESET);
		availableBooks.forEach(b -> System.out.printf("  ▶ ID=%d | 제목=%s | 저자=%s | 재고=%d/%d%n", b.getId(), b.getTitle(),
				b.getAuthor(), b.getAvailableCopies(), b.getTotalCopies()));

		System.out.print("📌 대여할 도서 ID들을 입력(쉼표로 구분, 예: 1,3,5)> ");
		List<Long> bookIds = parseIdList(sc.nextLine());

		try {
			Rental rentalHeader = rentalService.rentBooks(current, bookIds);
			System.out.println(GREEN + "✅ [성공] 대여 완료! (rentalId=" + rentalHeader.getId() + ")" + RESET);
		} catch (Exception e) {
			System.out.println(RED + "❌ [오류] " + e.getMessage() + RESET);
		}
	}

	// ==========================
	// 반납(다건)
	// ==========================
	// returnBooksFlow() 교체
	private static void returnBooksFlow() {
		Member current = memberService.getCurrentUser();
		var headers = rentalService.getRentalsByMember(current.getId());
		if (headers.isEmpty()) {
			System.out.println(YELLOW + "⚠️ [안내] 대여 내역이 없습니다." + RESET);
			return;
		}

		System.out.println(CYAN + "\n📚 [내 대여 내역]" + RESET);
		printRentalHeadersWithDetails(headers);

		// ✅ bookId만 입력
		System.out.print("↩️ 반납할 bookId들 입력(쉼표, 예: 2,5)> ");
		List<Long> bookIds = parseIdList(sc.nextLine());

		try {
			int count = rentalService.returnBooksByBookIds(current.getId(), bookIds);
			if (count == 0) {
				System.out.println(YELLOW + "ℹ️ 반납할 도서가 없거나 조건에 맞지 않습니다." + RESET);
			} else {
				System.out.println(GREEN + "✅ [성공] " + count + "권 반납 완료!" + RESET);
			}
		} catch (Exception e) {
			System.out.println(RED + "❌ [오류] " + e.getMessage() + RESET);
		}
	}

	// ==========================
	// 연장(다건)
	// ==========================
	// extendBooksFlow() 교체
	private static void extendBooksFlow() {
		Member current = memberService.getCurrentUser();
		// ✅ 진입 가드
		boolean isSuspended = current.getSuspendUntil() != null && !current.getSuspendUntil().isBefore(LocalDate.now());
		if (isSuspended) {
			System.out.println(YELLOW + "⚠️ [안내] 대여 정지 상태입니다. " + current.getSuspendUntil() + "까지 연장할 수 없습니다." + RESET);
			return;
		}
		if (rentalService.existsOverdueByMember(current.getId())) {
			System.out.println(YELLOW + "⚠️ [안내] 연체 중인 도서가 있어 연장할 수 없습니다. 먼저 연체 도서를 반납해주세요." + RESET);
			return;
		}

		var headers = rentalService.getRentalsByMember(current.getId());
		if (headers.isEmpty()) {
			System.out.println(YELLOW + "⚠️ [안내] 대여 내역이 없습니다." + RESET);
			return;
		}

		System.out.println(CYAN + "\n🔄 [연장 가능 내역 확인]" + RESET);
		printRentalHeadersWithDetails(headers);

		// ✅ bookId만 입력
		System.out.print("🔄 연장할 bookId들 입력(쉼표, 예: 1,3)> ");
		List<Long> bookIds = parseIdList(sc.nextLine());

		try {
			int updated = rentalService.extendBooksByBookIds(current.getId(), bookIds);
			if (updated == 0) {
				System.out.println(YELLOW + "ℹ️ 연장 가능한 대상이 없거나 조건을 만족하지 않습니다." + RESET);
			} else {
				System.out.println(GREEN + "✅ [성공] " + updated + "권 연장 완료!" + RESET);
			}
		} catch (Exception e) {
			System.out.println(RED + "❌ [오류] " + e.getMessage() + RESET);
		}
	}

	// ==========================
	// 내 대여 목록(헤더+상세 출력)
	// ==========================
	private static void myRentalsFlow() {
		Member current = memberService.getCurrentUser();
		var headers = rentalService.getRentalsByMember(current.getId());

		if (headers.isEmpty()) {
			System.out.println(YELLOW + "⚠️ [안내] 대여 내역이 없습니다." + RESET);
			return;
		}

		System.out.println(CYAN + "\n📝 [내 대여 목록]" + RESET);
		printRentalHeadersWithDetails(headers);
	}

	// ==========================
	// 도서 목록/등록/검색
	// ==========================
	private static void listBooksFlow() {
		var books = bookService.listBooks();

		if (books.isEmpty()) {
			System.out.println(YELLOW + "⚠️ 등록된 도서가 없습니다." + RESET);
		} else {
			System.out.println(CYAN + "\n📚 [도서 목록]" + RESET);
			books.forEach(b -> System.out.printf("  ▶ ID=%d | 제목=%s | 저자=%s | 재고=%d/%d%n", b.getId(), b.getTitle(),
					b.getAuthor(), b.getAvailableCopies(), b.getTotalCopies()));
		}
		;
	}

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

	private static void searchBookFlow() {
		System.out.println(CYAN + "\n🔍 [도서 검색]" + RESET);
		System.out.print("검색어 입력 (제목, 저자 또는 ISBN)> ");
		String keyword = sc.nextLine().trim();

		if (keyword.isEmpty()) {
			System.out.println(YELLOW + "⚠️ 검색어를 입력해주세요." + RESET);
			return;
		}

		var foundBooks = bookService.searchBooks(keyword);

		if (foundBooks.isEmpty()) {
			System.out.printf(YELLOW + "⚠️ '%s'에 대한 검색 결과가 없습니다.\n" + RESET, keyword);
		} else {
			System.out.printf(CYAN + "📖 '%s' 검색 결과 (%d건)\n" + RESET, keyword, foundBooks.size());
			foundBooks.forEach(b -> System.out.printf("  ▶ ID=%d | 제목=%s | 저자=%s | ISBN=%s | 재고=%d/%d%n", b.getId(),
					b.getTitle(), b.getAuthor(), b.getIsbn(), b.getAvailableCopies(), b.getTotalCopies()));
		}
	}

	// ==========================
	// 로그인/로그아웃/가입
	// ==========================
	private static void showWelcome() {
		System.out.println(CYAN + "======================================");
		System.out.println("        📚 도서 대여 시스템         ");
		System.out.println("======================================" + RESET);
		System.out.println("1) 📝 회원가입   2) 🔑 로그인   3) 🚪 종료");
	}

	private static void showMainMenu() {
		Member currentUser = memberService.getCurrentUser();

		System.out.println(CYAN + "======================================");
		System.out.printf(" 👤 로그인: %s  |  권한: %s%n", currentUser.getName(), currentUser.getRole());
		System.out.println("======================================" + RESET);

		// ✅ 상태 배너
		boolean isSuspended = currentUser.getSuspendUntil() != null
				&& !currentUser.getSuspendUntil().isBefore(LocalDate.now());
		boolean hasOverdue = rentalService.existsOverdueByMember(currentUser.getId());
		if (isSuspended) {
			System.out.println(YELLOW + "[안내] 대여 정지 상태: " + currentUser.getSuspendUntil() + " 까지 대여/연장 불가" + RESET);
		} else if (hasOverdue) {
			System.out.println(YELLOW + "[안내] 연체 중인 도서 보유: 대여/연장 메뉴 이용이 제한됩니다." + RESET);
		}

		if (currentUser.getRole() == Role.ADMIN) {
			System.out.println("1) 📕 도서 등록");
			System.out.println("2) 📚 도서 목록");
			System.out.println("3) 🔍 도서 검색");
			System.out.println("4) 📖 도서 대여(다건)" + (isSuspended || hasOverdue ? " 🔒(제한)" : ""));
			System.out.println("5) ↩️ 도서 반납(다건)");
			System.out.println("6) 🔄 대여 연장(다건)" + (isSuspended || hasOverdue ? " 🔒(제한)" : ""));
			System.out.println("7) 📝 내 대여 목록");
			System.out.println("0) 🚪 로그아웃");
		} else {
			System.out.println("1) 📚 도서 목록");
			System.out.println("2) 🔍 도서 검색");
			System.out.println("3) 📖 도서 대여(다건)" + (isSuspended || hasOverdue ? " 🔒(제한)" : ""));
			System.out.println("4) ↩️ 도서 반납(다건)");
			System.out.println("5) 🔄 대여 연장(다건)" + (isSuspended || hasOverdue ? " 🔒(제한)" : ""));
			System.out.println("6) 📝 내 대여 목록");
			System.out.println("0) 🚪 로그아웃");
		}
	}

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

	private static void loginFlow() {
		System.out.println(CYAN + "\n🔑 [로그인]" + RESET);
		System.out.print("📧 이메일 입력> ");
		String email = sc.nextLine().trim();
		System.out.print("🔑 비밀번호 입력> ");
		String pw = sc.nextLine().trim();

		try {
			Member m = memberService.login(email, pw);
			System.out.println(GREEN + "✅ [성공] 로그인: " + m.getName() + RESET);

			// ✅ 로그인 직후 안내 배너(차단 아님)
			if (m.getSuspendUntil() != null && !m.getSuspendUntil().isBefore(LocalDate.now())) {
				System.out.printf(YELLOW + "[안내] %s님은 %s까지 대여/연장이 제한됩니다.%n" + RESET, m.getName(), m.getSuspendUntil());
			}

		} catch (Exception e) {
			System.out.println(RED + "❌ [오류] " + e.getMessage() + RESET);
		}
	}

	private static void logout() {
		memberService.logout();
		System.out.println(YELLOW + "🚪 로그아웃 되었습니다." + RESET);
	}

	// ==========================
	// 유틸
	// ==========================
	private static int promptInt(String label) {
		System.out.print(label + "> ");
		String s = sc.nextLine().trim();
		return Integer.parseInt(s);
	}

	private static List<Long> parseIdList(String input) {
		if (input == null || input.isBlank()) {
			return List.of();
		}
		String[] tokens = input.split(",");
		List<Long> ids = new ArrayList<>();
		for (String t : tokens) {
			String s = t.trim();
			if (!s.isEmpty()) {
				ids.add(Long.parseLong(s));
			}
		}
		// 중복 제거 + 입력 순서 유지
		return new ArrayList<>(new LinkedHashSet<>(ids));
	}

	private static void printRentalHeadersWithDetails(List<Rental> headers) {
		try (Connection conn = ConnectionManager.getConnection()) {
			for (Rental r : headers) {
				System.out.printf("• rentalId=%d | memberId=%d | 상태=%s | 대여시각=%s%n", r.getId(), r.getMemberId(),
						r.getRentalStatus(), r.getRentedAt());
				var details = rentalDetailRepository.findByRentalId(conn, r.getId());
				if (details.isEmpty()) {
					System.out.println("   (상세 없음)");
				} else {
					for (RentalDetail d : details) {
						Book b = safeGetBook(d.getBookId());
						System.out.printf("   - bookId=%d | 제목=%s | 상태=%s | 예정일=%s | 반납일=%s | 연장=%d%n", d.getBookId(),
								b != null ? b.getTitle() : "(알 수 없음)", d.getDetailStatus(), d.getDueAt(),
								d.getReturnedAt() != null ? d.getReturnedAt() : "-", d.getExtensionCount());
					}
				}
			}
		} catch (Exception e) {
			System.out.println(RED + "❌ [오류] 대여 상세 조회 실패: " + e.getMessage() + RESET);
		}
	}

	private static List<RentalDetail> loadDetails(long rentalId) {
		try (Connection conn = ConnectionManager.getConnection()) {
			return rentalDetailRepository.findByRentalId(conn, rentalId);
		} catch (Exception e) {
			System.out.println(RED + "❌ [오류] 상세 로드 실패: " + e.getMessage() + RESET);
			return List.of();
		}
	}

	private static Book safeGetBook(long bookId) {
		try {
			return bookService.getBook(bookId);
		} catch (Exception ignore) {
			return null;
		}
	}
}
