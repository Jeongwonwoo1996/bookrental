package io.github.bookrentalteam.bookrental;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 비즈니스 로직 서비스
 */
public class LibraryService {

    private final Map<Long, Member> members = new LinkedHashMap<>();
    private final Map<Long, Book> books = new LinkedHashMap<>();
    private final Map<Long, Rental> rentals = new LinkedHashMap<>();

    // -------------------- 회원 --------------------
    public Member signUp(String name, String email, String pw, Role role) {
        if (members.values().stream().anyMatch(m -> m.email().equalsIgnoreCase(email))) {
            throw new IllegalStateException("이미 등록된 이메일입니다.");
        }
        String hashed = Passwords.hash(pw);
        Member m = new Member(name, email, hashed, role);
        members.put(m.id(), m);
        return m;
    }

    public Member login(String email, String pw) {
        return members.values().stream()
                .filter(m -> m.email().equalsIgnoreCase(email))
                .filter(m -> m.authenticate(pw))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));
    }

    // -------------------- 도서 --------------------
    public Book addBook(String isbn, String title, String author, int total) {
        if (books.values().stream().anyMatch(b -> b.isbn().equalsIgnoreCase(isbn))) {
            throw new IllegalStateException("이미 존재하는 ISBN입니다.");
        }
        Book b = new Book(isbn, title, author, total);
        books.put(b.id(), b);
        return b;
    }

    public List<Book> listBooks() {
        return new ArrayList<>(books.values());
    }

    public List<Book> searchBooks(String keyword) {
        String k = keyword.toLowerCase(Locale.ROOT);
        return books.values().stream().filter(b ->
                b.title().toLowerCase(Locale.ROOT).contains(k) ||
                b.author().toLowerCase(Locale.ROOT).contains(k) ||
                b.isbn().toLowerCase(Locale.ROOT).contains(k)
        ).collect(Collectors.toList());
    }

    public Book getBook(long id) {
        return books.get(id);
    }

    // -------------------- 대여 / 반납 --------------------
    public Rental rent(long bookId, long memberId) {
        // 연체 체크
        rentals.values().stream()
                .filter(r -> r.memberId() == memberId && r.isOverdue())
                .findAny()
                .ifPresent(r -> {
                    throw new IllegalStateException("연체 중이므로 대여 불가 (연체 일수: " + r.overdueDays() + "일)");
                });

        Book b = books.get(bookId);
        if (b == null) throw new IllegalArgumentException("도서를 찾을 수 없습니다.");
        if (b.availableCopies() <= 0) throw new IllegalStateException("대여 가능 재고가 없습니다.");

        Book after = new Book(b.id(), b.isbn(), b.title(), b.author(), b.totalCopies(), b.availableCopies() - 1);
        books.put(after.id(), after);

        Rental r = new Rental(bookId, memberId);
        rentals.put(r.id(), r);
        return r;
    }

    public Rental returnBook(long rentalId) {
        Rental rr = rentals.get(rentalId);
        if (rr == null) throw new IllegalArgumentException("대여 건을 찾을 수 없습니다.");

        Rental returned = rr.markReturned(LocalDate.now());
        rentals.put(returned.id(), returned);

        Book b = books.get(returned.bookId());
        Book after = new Book(b.id(), b.isbn(), b.title(), b.author(), b.totalCopies(), b.availableCopies() + 1);
        books.put(after.id(), after);

        return returned;
    }

    public List<Rental> rentalsByMember(long memberId) {
        return rentals.values().stream()
                .filter(r -> r.memberId() == memberId)
                .sorted(Comparator.comparing(Rental::rentedAt).reversed())
                .toList();
    }
}
