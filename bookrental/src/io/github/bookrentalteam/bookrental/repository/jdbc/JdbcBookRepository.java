package io.github.bookrentalteam.bookrental.repository.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import io.github.bookrentalteam.bookrental.config.ConnectionManager;
import io.github.bookrentalteam.bookrental.domain.Book;
import io.github.bookrentalteam.bookrental.repository.BookRepository;

public class JdbcBookRepository implements BookRepository {

	@Override
	public long save(Book book) {
		final String sql = """
				INSERT INTO book(isbn, title, author, total_copies, available_copies)
				VALUES (?, ?, ?, ?, ?)
				""";
		try (Connection conn = ConnectionManager.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, book.getIsbn());
			ps.setString(2, book.getTitle());
			ps.setString(3, book.getAuthor());
			ps.setInt(4, book.getTotalCopies());
			ps.setInt(5, book.getAvailableCopies());
			ps.executeUpdate();
			try (ResultSet rs = ps.getGeneratedKeys()) {
				if (rs.next()) {
					long id = rs.getLong(1);
					book.setId(id);
					return id;
				}
			}
			throw new SQLException("book PK 생성 실패");
		} catch (SQLException e) {
			throw new RuntimeException("JdbcBookRepository.save 실패", e);
		}
	}

	@Override
	public Book findById(long id) {
		final String sql = """
				SELECT book_id, isbn, title, author, total_copies, available_copies,
				       created_at, updated_at
				FROM book WHERE book_id=?
				""";
		try (Connection conn = ConnectionManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setLong(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? map(rs) : null;
			}
		} catch (SQLException e) {
			throw new RuntimeException("JdbcBookRepository.findById 실패", e);
		}
	}

	@Override
	public Book findByIsbn(String isbn) {
		final String sql = """
				SELECT book_id, isbn, title, author, total_copies, available_copies,
				       created_at, updated_at
				FROM book WHERE isbn=?
				""";
		try (Connection conn = ConnectionManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, isbn);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? map(rs) : null;
			}
		} catch (SQLException e) {
			throw new RuntimeException("JdbcBookRepository.findByIsbn 실패", e);
		}
	}

	@Override
	public List<Book> findAll() {
		final String sql = """
				SELECT book_id, isbn, title, author, total_copies, available_copies,
				       created_at, updated_at
				FROM book
				ORDER BY book_id DESC
				LIMIT 200
				""";
		List<Book> list = new ArrayList<>();
		try (Connection conn = ConnectionManager.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				list.add(map(rs));
			}
			return list;
		} catch (SQLException e) {
			throw new RuntimeException("JdbcBookRepository.findAll 실패", e);
		}
	}

	@Override
	public void updateCopies(long bookId, int total, int available) {
		final String sql = """
				UPDATE book SET total_copies=?, available_copies=? WHERE book_id=?
				""";
		try (Connection conn = ConnectionManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, total);
			ps.setInt(2, available);
			ps.setLong(3, bookId);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("JdbcBookRepository.updateCopies 실패", e);
		}
	}

	@Override
	public int decreaseAvailable(Connection conn, long bookId) {
		final String sql = """
				UPDATE book
				   SET available_copies = available_copies - 1
				 WHERE book_id = ?
				   AND available_copies > 0
				""";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setLong(1, bookId);
			return ps.executeUpdate(); // 1이면 성공, 0이면 재고 없음
		} catch (SQLException e) {
			throw new RuntimeException("JdbcBookRepository.decreaseAvailable 실패", e);
		}
	}

	@Override
	public int increaseAvailable(Connection conn, long bookId) {
		final String sql = """
				UPDATE book
				   SET available_copies = available_copies + 1
				 WHERE book_id = ?
				   AND available_copies < total_copies
				""";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setLong(1, bookId);
			return ps.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("JdbcBookRepository.increaseAvailable 실패", e);
		}
	}

	@Override
	public int lockAndGetAvailable(Connection conn, long bookId) {
		final String sql = "SELECT available_copies FROM book WHERE book_id=? FOR UPDATE";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setLong(1, bookId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getInt(1);
				}
				throw new RuntimeException("존재하지 않는 도서: " + bookId);
			}
		} catch (SQLException e) {
			throw new RuntimeException("JdbcBookRepository.lockAndGetAvailable 실패", e);
		}
	}

	private Book map(ResultSet rs) throws SQLException {
		Book b = new Book();
		b.setId(rs.getLong("book_id"));
		b.setIsbn(rs.getString("isbn"));
		b.setTitle(rs.getString("title"));
		b.setAuthor(rs.getString("author"));
		b.setTotalCopies(rs.getInt("total_copies"));
		b.setAvailableCopies(rs.getInt("available_copies"));
		b.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
		b.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
		return b;
	}
}
