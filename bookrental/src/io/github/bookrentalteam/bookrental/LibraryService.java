package io.github.bookrentalteam.bookrental;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 도서 대여 시스템의 핵심 비즈니스 로직을 담당하는 서비스 계층.
 *
 * 설계 특징
 * - 간단한 과제/콘솔 앱을 가정하여 영속화 대신 메모리(Map) 저장 사용
 * - domain 레코드(예: Book, Rental, Member)가 불변(immutable)이라고 가정하고,
 *   상태 변경이 필요할 때는 "새 인스턴스 생성 후 교체" 방식 적용
 * - 순수 자바, 단일 스레드 콘솔 실행을 전제(동시성 제어 미적용)
 *
 * 자료구조 선택
 * - LinkedHashMap을 사용하여 삽입 순서가 보존되므로 목록 표시 시 일관된 정렬 제공
 *
 * 주의사항
 * - 실제 서비스/웹 환경에서는 동시성/트랜잭션/원자성 보장이 필요
 * - 이메일/ISBN 등은 간단 검증만 수행(형식/중복 정도). 실무에선 더 강력한 검증 권장
 */
public class LibraryService {

    /** 회원 저장소: memberId → Member */
    private final Map<UUID, Member> members = new LinkedHashMap<>();

    /** 도서 저장소: bookId → Book */
    private final Map<UUID, Book> books = new LinkedHashMap<>();

    /** 대여 저장소: rentalId → Rental */
    private final Map<UUID, Rental> rentals = new LinkedHashMap<>();

    // ---------------------------------------------------------
    // 회원 관련
    // ---------------------------------------------------------

    /**
     * 회원가입
     *
     * 동작
     * 1) 이메일 중복(대소문자 무시) 검사
     * 2) 비밀번호 해시(Passwords.hash) 후 저장
     * 3) UUID 발급하여 Member 생성 → members에 등록
     *
     * @param name  이름(널/공백 검증은 Member에서 책임지는 경우가 많음)
     * @param email 이메일(대소문자 구분 없이 중복 체크)
     * @param pw    원문 비밀번호(저장 전 해시)
     * @return 생성된 Member
     * @throws IllegalStateException 이미 등록된 이메일인 경우
     */
    public Member signUp(String name, String email, String pw) {
        // O(n) 검색: 과제 규모에선 충분. 인덱스가 필요할 만큼 크면 보조 맵(email → id) 고려
        if (members.values().stream().anyMatch(m -> m.email().equalsIgnoreCase(email))) {
            throw new IllegalStateException("이미 등록된 이메일입니다.");
        }

        // 비밀번호는 평문 저장 금지. 해시 후 저장(솔트/스트레치 등은 Passwords.hash 내부에 있다고 가정)
        String hashed = Passwords.hash(pw);

        Member m = new Member(UUID.randomUUID(), name, email, hashed);
        members.put(m.id(), m);
        return m;
    }

    /**
     * 로그인
     *
     * 동작
     * 1) 이메일로 회원 조회(대소문자 무시)
     * 2) Member.authenticate(pw)로 비밀번호 검증(해시 비교)
     *
     * @param email 이메일
     * @param pw    평문 비밀번호(내부에서 해시 비교)
     * @return 인증 성공한 Member
     * @throws IllegalArgumentException 이메일/비밀번호 불일치
     */
    public Member login(String email, String pw) {
        Optional<Member> om = members.values().stream()
                .filter(m -> m.email().equalsIgnoreCase(email))
                .findFirst();

        if (om.isEmpty() || !om.get().authenticate(pw)) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        return om.get();
    }

    // ---------------------------------------------------------
    // 도서 관련
    // ---------------------------------------------------------

    /**
     * 도서 등록
     *
     * 동작
     * 1) ISBN 중복 검사(대소문자 무시)
     * 2) 총권수(total) 유효성 검사(0 이상)
     * 3) Book 생성(available = total) 후 books에 저장
     *
     * @param isbn   국제 표준 도서 번호(간단 중복 검사)
     * @param title  제목
     * @param author 저자
     * @param total  총권수(0 이상)
     * @return 생성된 Book
     * @throws IllegalStateException    이미 존재하는 ISBN
     * @throws IllegalArgumentException total < 0
     */
    public Book addBook(String isbn, String title, String author, int total) {
        if (books.values().stream().anyMatch(b -> b.isbn().equalsIgnoreCase(isbn))) {
            throw new IllegalStateException("이미 존재하는 ISBN입니다.");
        }
        if (total < 0) throw new IllegalArgumentException("총권수는 0 이상이어야 합니다.");

        // Book 레코드는 불변이므로 생성 시 원하는 상태로 셋업
        Book b = new Book(UUID.randomUUID(), isbn, title, author, total, total);
        books.put(b.id(), b);
        return b;
    }

    /**
     * 도서 전체 목록
     *
     * @return 현재 저장된 모든 도서를 새 리스트로 반환(원본 보호)
     */
    public List<Book> listBooks() {
        return new ArrayList<>(books.values());
    }

    /**
     * 도서 검색
     *
     * 기준: 제목/저자/ISBN 부분 일치(대소문자 무시, Locale.ROOT 기준)
     * - Locale.ROOT 사용 이유: 지역(locale)에 따른 소문자 변환 이슈(예: 터키어 i 문제) 방지
     *
     * @param keyword 검색 키워드(널/공백은 호출 측에서 검증 가정)
     * @return 조건에 맞는 도서 리스트
     */
    public List<Book> searchBooks(String keyword) {
        String k = keyword.toLowerCase(Locale.ROOT);
        return books.values().stream().filter(b ->
                b.title().toLowerCase(Locale.ROOT).contains(k) ||
                b.author().toLowerCase(Locale.ROOT).contains(k) ||
                b.isbn().toLowerCase(Locale.ROOT).contains(k)
        ).collect(Collectors.toList());
    }

    /**
     * 화면에 표시된 "짧은 bookId(앞 몇 자리)"를 실제 UUID로 해석
     *
     * 구현
     * - books의 key(UUID)를 순회하며 문자열 시작(prefix) 비교로 매칭
     *
     * 주의
     * - 충돌 가능성: 같은 prefix를 공유하는 UUID가 있을 경우 첫 번째 것만 반환됨
     *   → 실무에선 "정확히 1건 매칭" 검증을 추가하거나, 최소 길이(예: 6~8자리) 강제 권장
     *
     * @param shortId UUID 문자열의 앞부분(화면 표시)
     * @return 일치하는 실제 UUID
     * @throws IllegalArgumentException 일치 항목 없음
     */
    public UUID resolveBookId(String shortId) {
        for (UUID id : books.keySet()) {
            if (id.toString().startsWith(shortId)) return id;
        }
        throw new IllegalArgumentException("도서를 찾을 수 없습니다.");
    }

    /**
     * 화면에 표시된 "짧은 rentalId(앞 몇 자리)"를 실제 UUID로 해석
     *
     * @param shortId UUID 문자열의 앞부분
     * @return 일치하는 실제 UUID
     * @throws IllegalArgumentException 일치 항목 없음
     * @see #resolveBookId(String)
     */
    public UUID resolveRentalId(String shortId) {
        for (UUID id : rentals.keySet()) {
            if (id.toString().startsWith(shortId)) return id;
        }
        throw new IllegalArgumentException("대여 건을 찾을 수 없습니다.");
    }

    // ---------------------------------------------------------
    // 대여/반납
    // ---------------------------------------------------------

    /**
     * 도서 대여
     *
     * 동작
     * 1) bookId로 도서를 조회(없으면 오류)
     * 2) 재고(availableCopies) 확인(없으면 오류)
     * 3) Book 불변 특성상, 재고 -1 적용한 "새 Book"을 생성하여 교체
     * 4) Rental 생성(대여일=오늘, 예정일=오늘+14일, 상태=RENTED) 후 저장
     *
     * 주의
     * - 대여기간(14일)은 하드코딩되어 있음(상수 추출 권장)
     * - 동시성 없는 콘솔 전제를 가정(멀티스레드 환경에선 원자성 보장 필요)
     *
     * @param bookId   대여할 도서 ID
     * @param memberId 대여자 회원 ID
     * @return 생성된 Rental
     * @throws IllegalArgumentException 도서가 없을 때
     * @throws IllegalStateException    재고가 없을 때
     */
    public Rental rent(UUID bookId, UUID memberId) {
        Book b = books.get(bookId);
        if (b == null) throw new IllegalArgumentException("도서를 찾을 수 없습니다.");
        if (b.availableCopies() <= 0) throw new IllegalStateException("대여 가능 재고가 없습니다.");

        // 불변 Book이므로 수량만 변경된 새 인스턴스로 교체
        Book after = new Book(b.id(), b.isbn(), b.title(), b.author(),
                              b.totalCopies(), b.availableCopies() - 1);
        books.put(after.id(), after);

        // 대여 기록 생성: 14일 기본 기한(추후 상수화/설정값 전환 가능)
        Rental r = new Rental(
                UUID.randomUUID(),
                bookId,
                memberId,
                LocalDate.now(),
                LocalDate.now().plusDays(14),
                null,
                RentalStatus.RENTED
        );
        rentals.put(r.id(), r);
        return r;
    }

    /**
     * 도서 반납
     *
     * 동작
     * 1) rentalId로 대여 기록 조회(없으면 오류)
     * 2) Rental 불변 가정: markReturned(오늘) 호출 시 "반환된 새 Rental"을 돌려준다고 가정하고 교체
     * 3) Book 조회 후 재고 +1 반영된 새 Book으로 교체
     *
     * 주의
     * - 이미 RETURNED 상태인 대여건에 대한 중복 반납 방어는 Rental.markReturned 내부/호출부에서 보장해야 함
     *
     * @param rentalId 반납할 대여 기록 ID
     * @return 반납 처리된 Rental(상태=RETURNED, returnDate 세팅)
     * @throws IllegalArgumentException 대여 기록 없음
     */
    public Rental returnBook(UUID rentalId) {
        Rental rr = rentals.get(rentalId);
        if (rr == null) throw new IllegalArgumentException("대여 건을 찾을 수 없습니다.");

        // Rental 또한 불변(레코드)이라고 가정: markReturned가 새 인스턴스를 반환
        Rental returned = rr.markReturned(LocalDate.now());
        rentals.put(returned.id(), returned);

        // 도서 재고 +1 (불변 Book 새 인스턴스로 교체)
        Book b = books.get(returned.bookId());
        Book after = new Book(b.id(), b.isbn(), b.title(), b.author(),
                              b.totalCopies(), b.availableCopies() + 1);
        books.put(after.id(), after);

        return returned;
    }

    // ---------------------------------------------------------
    // 조회 유틸
    // ---------------------------------------------------------

    /**
     * 특정 회원의 대여 이력 조회(현재/과거 포함)
     *
     * 정렬: 대여일(rentedAt) 내림차순(최근 내역 우선)
     * toList(): Java 16+의 수집기로, 불변 리스트를 반환(수정하려면 새 리스트 생성 필요)
     *
     * @param memberId 회원 ID
     * @return 해당 회원의 대여 기록 목록
     */
    public List<Rental> rentalsByMember(UUID memberId) {
        return rentals.values().stream()
                .filter(r -> r.memberId().equals(memberId))
                .sorted(Comparator.comparing(Rental::rentedAt).reversed())
                .toList();
    }

    /**
     * 단건 도서 조회
     *
     * @param id 도서 ID
     * @return Book(없으면 null)
     */
    public Book getBook(UUID id) { return books.get(id); }
}
