package io.github.bookrentalteam.bookrental;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class LibraryService {
    private final Map<UUID, Member> members = new LinkedHashMap<>();
    private final Map<UUID, Book> books = new LinkedHashMap<>();
    private final Map<UUID, Rental> rentals = new LinkedHashMap<>();

    public Member signUp(String name, String email, String pw) {
        if (members.values().stream().anyMatch(m -> m.email().equalsIgnoreCase(email))) {
            throw new IllegalStateException("이미 등록된 이메일입니다.");
        }
        String hashed = Passwords.hash(pw);
        Member m = new Member(UUID.randomUUID(), name, email, hashed);
        members.put(m.id(), m);
        return m;
    }

    public Member login(String email, String pw) {
        Optional<Member> om = members.values().stream()
                .filter(m -> m.email().equalsIgnoreCase(email))
                .findFirst();
        if (om.isEmpty() || !om.get().authenticate(pw)) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        return om.get();
    }

    public Book addBook(String isbn, String title, String author, int total) {
        if (books.values().stream().anyMatch(b -> b.isbn().equalsIgnoreCase(isbn))) {
            throw new IllegalStateException("이미 존재하는 ISBN입니다.");
        }
        if (total < 0) throw new IllegalArgumentException("총권수는 0 이상이어야 합니다.");
        Book b = new Book(UUID.randomUUID(), isbn, title, author, total, total);
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

    public UUID resolveBookId(String shortId) {
        for (UUID id : books.keySet()) {
            if (id.toString().startsWith(shortId)) return id;
        }
        throw new IllegalArgumentException("도서를 찾을 수 없습니다.");
    }
    public UUID resolveRentalId(String shortId) {
        for (UUID id : rentals.keySet()) {
            if (id.toString().startsWith(shortId)) return id;
        }
        throw new IllegalArgumentException("대여 건을 찾을 수 없습니다.");
    }

    public Rental rent(UUID bookId, UUID memberId) {
        Book b = books.get(bookId);
        if (b == null) throw new IllegalArgumentException("도서를 찾을 수 없습니다.");
        if (b.availableCopies() <= 0) throw new IllegalStateException("대여 가능 재고가 없습니다.");
        Book after = new Book(b.id(), b.isbn(), b.title(), b.author(), b.totalCopies(), b.availableCopies() - 1);
        books.put(after.id(), after);

        Rental r = new Rental(UUID.randomUUID(), bookId, memberId, LocalDate.now(), LocalDate.now().plusDays(14), null, RentalStatus.RENTED);
        rentals.put(r.id(), r);
        return r;
    }

    public Rental returnBook(UUID rentalId) {
        Rental rr = rentals.get(rentalId);
        if (rr == null) throw new IllegalArgumentException("대여 건을 찾을 수 없습니다.");
        Rental returned = rr.markReturned(LocalDate.now());
        rentals.put(returned.id(), returned);

        Book b = books.get(returned.bookId());
        Book after = new Book(b.id(), b.isbn(), b.title(), b.author(), b.totalCopies(), b.availableCopies() + 1);
        books.put(after.id(), after);

        return returned;
    }

    public List<Rental> rentalsByMember(UUID memberId) {
        return rentals.values().stream()
                .filter(r -> r.memberId().equals(memberId))
                .sorted(Comparator.comparing(Rental::rentedAt).reversed())
                .toList();
    }

    public Book getBook(UUID id) { return books.get(id); }
}
