package io.github.bookrentalteam.bookrental;

import java.util.UUID;

public record Book(UUID id, String isbn, String title, String author, int totalCopies, int availableCopies) {
    public Book {
        if (id == null) id = UUID.randomUUID();
        if (isbn == null || isbn.isBlank()) throw new IllegalArgumentException("ISBN은(는) 필수입니다.");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("제목은(는) 필수입니다.");
        if (author == null || author.isBlank()) throw new IllegalArgumentException("저자는(은) 필수입니다.");
        if (totalCopies < 0) throw new IllegalArgumentException("총권수는 0 이상이어야 합니다.");
        if (availableCopies < 0) throw new IllegalArgumentException("대여 가능 권수는 0 이상이어야 합니다.");
        if (availableCopies > totalCopies) throw new IllegalArgumentException("대여 가능 권수는 총권수를 초과할 수 없습니다.");
    }

    public Book increase(int n) {
        if (n < 1) throw new IllegalArgumentException("증가 수량은 1 이상이어야 합니다.");
        return new Book(id, isbn, title, author, totalCopies + n, availableCopies + n);
    }

    public Book decrease(int n) {
        if (n < 1) throw new IllegalArgumentException("감소 수량은 1 이상이어야 합니다.");
        if (availableCopies < n) throw new IllegalStateException("availableCopies가 부족하여 감축할 수 없습니다.");
        return new Book(id, isbn, title, author, totalCopies - n, availableCopies - n);
    }
}
