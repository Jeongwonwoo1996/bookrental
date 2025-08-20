package io.github.bookrentalteam.bookrental;

import java.util.*;
// import java.time.*; // (참고) 현재 파일에서는 직접 사용하지 않음

/**
 * 콘솔 기반 도서 대여 시스템의 엔트리 포인트 클래스.
 *
 * 역할:
 * - 사용자 인터페이스(UI) 처리: 메뉴 출력, 입력, 결과 표시
 * - 예외 처리 및 흐름 제어 담당
 * - 실제 비즈니스 로직은 LibraryService에 위임
 *
 * 주요 기능:
 * - (로그인 전) 회원가입, 로그인, 종료
 * - (로그인 후) 도서 등록, 도서 목록, 도서 검색, 대여, 반납, 내 대여목록, 로그아웃
 */
public class App {
    /** 콘솔 입력을 위한 Scanner (프로그램 종료 시까지 공유) */
    private static final Scanner sc = new Scanner(System.in);

    /** 도서/회원/대여 관리 비즈니스 로직을 처리하는 서비스 */
    private static final LibraryService service = new LibraryService();

    /** 현재 로그인한 사용자. 로그인 전/로그아웃 시에는 null */
    private static Member currentUser = null;

    /**
     * 프로그램 실행 진입점 (main 메서드).
     * - seed()로 샘플 데이터 초기화
     * - 로그인 여부에 따라 다른 메뉴 루프 제공
     * - 예외 발생 시 사용자 친화적 메시지 출력 후 루프 지속
     */
    public static void main(String[] args) {
        seed(); // 테스트용 데이터 로딩

        while (true) {
            try {
                if (currentUser == null) {
                    // 로그인되지 않은 상태 → 환영/인증 메뉴
                    showWelcome();
                    int sel = promptInt("선택");
                    switch (sel) {
                        case 1 -> signUpFlow();   // 회원가입
                        case 2 -> loginFlow();    // 로그인
                        case 3 -> {               // 프로그램 종료
                            System.out.println("이용해주셔서 감사합니다.");
                            return;
                        }
                        default -> System.out.println("[오류] 메뉴 번호를 다시 선택해주세요.");
                    }
                } else {
                    // 로그인된 상태 → 메인 기능 메뉴
                    showMainMenu();
                    int sel = promptInt("선택");
                    switch (sel) {
                        case 1 -> addBookFlow();     // 도서 등록
                        case 2 -> listBooksFlow();   // 도서 목록
                        case 3 -> searchFlow();      // 도서 검색
                        case 4 -> rentFlow();        // 대여
                        case 5 -> returnFlow();      // 반납
                        case 6 -> myRentalsFlow();   // 내 대여목록
                        case 0 -> {                  // 로그아웃
                            currentUser = null;
                            System.out.println("[안내] 로그아웃되었습니다.");
                        }
                        default -> System.out.println("[오류] 메뉴 번호를 다시 선택해주세요.");
                    }
                }
            } catch (InputMismatchException e) {
                // 숫자가 아닌 값을 입력했을 때
                System.out.println("[오류] 숫자를 입력해주세요.");
            } catch (IllegalArgumentException | IllegalStateException e) {
                // 잘못된 인자/비즈니스 규칙 위반 등
                System.out.println("[오류] " + e.getMessage());
                pause();
            } catch (Exception e) {
                // 예상치 못한 시스템 예외
                System.out.println("[오류] 예상치 못한 오류: " + e.getMessage());
                pause();
            }
        }
    }

    /** 로그인 전: 환영/인증 메뉴 표시 */
    private static void showWelcome() {
        System.out.println("=== 도서 대여 시스템 ===");
        System.out.println("1) 회원가입   2) 로그인   3) 종료");
    }

    /** 로그인 후: 메인 메뉴 표시 */
    private static void showMainMenu() {
        System.out.printf("=== 메인 메뉴 (로그인: %s) ===%n", currentUser.name());
        System.out.println("1) 도서등록   2) 도서목록   3) 도서검색   4) 대여   5) 반납   6) 내 대여목록   0) 로그아웃");
    }

    /** 회원가입 처리 */
    private static void signUpFlow() {
        System.out.println("[회원가입]");
        String name = promptNonEmpty("이름");
        String email = promptNonEmpty("이메일");

        // 이메일 형식 검증
        if (!Patterns.isEmail(email)) {
            throw new IllegalArgumentException("형식이 올바르지 않습니다. (이메일)");
        }

        String pw = promptNonEmpty("비밀번호");
        Member m = service.signUp(name, email, pw);
        System.out.println("[성공] 회원가입 완료 (memberId=" + m.id() + ")");
        pause();
    }

    /** 로그인 처리 */
    private static void loginFlow() {
        System.out.println("[로그인]");
        String email = promptNonEmpty("이메일");
        String pw = promptNonEmpty("비밀번호");

        Member m = service.login(email, pw);
        currentUser = m;
        System.out.println("[성공] 로그인되었습니다. (" + currentUser.name() + ")");
    }

    /** 도서 등록 처리 */
    private static void addBookFlow() {
        System.out.println("[도서 등록]");
        String isbn = promptNonEmpty("ISBN");
        String title = promptNonEmpty("제목");
        String author = promptNonEmpty("저자");
        int total = promptInt("총권수");

        Book b = service.addBook(isbn, title, author, total);
        System.out.printf("[성공] 등록 완료: bookId=%s, 재고=%d/%d%n",
                b.id(), b.availableCopies(), b.totalCopies());
    }

    /** 도서 목록 출력 */
    private static void listBooksFlow() {
        List<Book> list = service.listBooks();
        System.out.println("[도서 목록] 총 " + list.size() + "권");
        System.out.printf("%-6s %-20s %-20s %-16s %-8s%n", "ID", "ISBN", "제목", "저자", "재고/총");

        for (Book b : list) {
            System.out.printf("%-6s %-20s %-20s %-16s %d/%d%n",
                    b.id().toString().substring(0, 6), // UUID 앞 6자리만 출력
                    b.isbn(),
                    truncate(b.title(), 20),
                    truncate(b.author(), 16),
                    b.availableCopies(), b.totalCopies());
        }
        pause();
    }

    /** 도서 검색 처리 */
    private static void searchFlow() {
        System.out.println("[도서 검색]");
        String keyword = promptNonEmpty("키워드(제목/저자/ISBN)");
        List<Book> list = service.searchBooks(keyword);

        if (list.isEmpty()) {
            System.out.println("검색 결과가 없습니다.");
        } else {
            System.out.println("검색 결과(" + list.size() + "건)");
            System.out.printf("%-6s %-20s %-20s %-16s %-8s%n", "ID", "ISBN", "제목", "저자", "재고/총");
            for (Book b : list) {
                System.out.printf("%-6s %-20s %-20s %-16s %d/%d%n",
                        b.id().toString().substring(0, 6),
                        b.isbn(),
                        truncate(b.title(), 20),
                        truncate(b.author(), 16),
                        b.availableCopies(), b.totalCopies());
            }
        }
        pause();
    }

    /** 도서 대여 처리 */
    private static void rentFlow() {
        ensureLogin();
        System.out.println("[대여]");

        // UUID 앞 6자리(쇼트ID) 입력받아 실제 UUID 매핑
        String shortId = promptNonEmpty("bookId 입력(목록/검색에서 ID 확인)");
        UUID bookId = service.resolveBookId(shortId);

        Rental r = service.rent(bookId, currentUser.id());
        Book after = service.getBook(bookId);

        System.out.printf("[성공] 대여 완료: rentalId=%s, 반납예정일=%s, 재고=%d/%d%n",
                r.id(), r.dueAt(), after.availableCopies(), after.totalCopies());
        pause();
    }

    /** 도서 반납 처리 */
    private static void returnFlow() {
        ensureLogin();
        System.out.println("[반납]");

        String shortId = promptNonEmpty("rentalId 입력");
        UUID rentalId = service.resolveRentalId(shortId);

        Rental r = service.returnBook(rentalId);
        Book after = service.getBook(r.bookId());

        System.out.printf("[성공] 반납 완료: status=%s, 재고=%d/%d%n",
                r.status(), after.availableCopies(), after.totalCopies());
        pause();
    }

    /** 로그인한 회원의 대여 내역 출력 */
    private static void myRentalsFlow() {
        ensureLogin();
        List<Rental> list = service.rentalsByMember(currentUser.id());

        if (list.isEmpty()) {
            System.out.println("[안내] 현재/과거 대여 이력이 없습니다.");
            pause();
            return;
        }

        System.out.println("[나의 대여 목록]");
        System.out.printf("%-6s %-20s %-18s %-12s %-12s %-10s%n",
                "ID", "책제목", "ISBN", "대여일", "예정일", "상태");

        for (Rental r : list) {
            Book b = service.getBook(r.bookId());
            String title = b != null ? truncate(b.title(), 20) : "(삭제됨)";
            String isbn = b != null ? truncate(b.isbn(), 18) : "-";

            System.out.printf("%-6s %-20s %-18s %-12s %-12s %-10s%n",
                    r.id().toString().substring(0, 6),
                    title, isbn,
                    r.rentedAt(), r.dueAt(), r.status());
        }
        pause();
    }

    /** 로그인 여부 확인 (없으면 예외 발생) */
    private static void ensureLogin() {
        if (currentUser == null) {
            throw new IllegalStateException("로그인 후 이용해주세요.");
        }
    }

    // -------------------- 공용 유틸 메서드 --------------------

    /** 공백 불가 문자열 입력 */
    private static String promptNonEmpty(String label) {
        System.out.print(label + "> ");
        String s = sc.nextLine().trim();
        if (s.isEmpty()) throw new IllegalArgumentException(label + "은(는) 필수입니다.");
        return s;
    }

    /** 정수 입력 (잘못 입력 시 InputMismatchException 발생) */
    private static int promptInt(String label) {
        System.out.print(label + "> ");
        String s = sc.nextLine().trim();
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new InputMismatchException();
        }
    }

    /** 일시 정지: 엔터 입력 대기 */
    private static void pause() {
        System.out.println("엔터를 누르면 메뉴로 돌아갑니다...");
        sc.nextLine();
    }

    /** 문자열 길이 제한 (길면 말줄임표 추가) */
    private static String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n - 1) + "…";
    }

    /** 초기 샘플 데이터 등록 */
    private static void seed() {
        try {
            service.signUp("정원우", "wonwoo@test.com", "test1234!");
            service.signUp("김태영", "taeyoung@test.com", "test1234!");
            service.signUp("관리자", "admin@admin.com", "admin1234!");
        } catch (Exception ignore) {
            // 이미 등록된 이메일이면 무시
        }

        // 샘플 도서 등록 (실제 ISBN 기반)
        service.addBook("978-89-7914-874-9", "자바의 정석", "남궁성", 5);
        service.addBook("978-89-98142-35-3", "토비의 스프링 Vol.1", "이일민", 2);
        service.addBook("978-89-98142-36-0", "토비의 스프링 Vol.2", "이일민", 2);
        service.addBook("978-89-94774-21-0", "모던 자바 인 액션", "라울-게브리얼 우르마", 3);
        service.addBook("978-89-98114-12-2", "스프링 인 액션", "크레이그 월즈", 4);
        service.addBook("978-89-98142-45-2", "이펙티브 자바 3판", "조슈아 블로크", 3);
        service.addBook("978-89-94774-31-9", "클린 코드", "로버트 C. 마틴", 2);
        service.addBook("978-89-98114-05-4", "클린 아키텍처", "로버트 C. 마틴", 1);
        service.addBook("978-89-491-3656-3", "Head First 디자인 패턴", "에릭 프리먼", 2);
        service.addBook("978-89-98114-08-5", "테스트 주도 개발", "켄트 벡", 0);
    }
}

/**
 * 정규식 유틸 클래스
 * - 현재는 이메일 형식 검증만 제공
 */
class Patterns {
    /** 간단한 이메일 정규식 */
    private static final java.util.regex.Pattern EMAIL =
            java.util.regex.Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    /** 이메일 형식 검증 */
    static boolean isEmail(String s) { return EMAIL.matcher(s).matches(); }
}
