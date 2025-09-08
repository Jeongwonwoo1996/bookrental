package io.github.bookrentalteam.bookrental.repository.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import io.github.bookrentalteam.bookrental.domain.Rental;
import io.github.bookrentalteam.bookrental.domain.RentalStatus;
import io.github.bookrentalteam.bookrental.repository.RentalRepository;

public class JdbcRentalRepository implements RentalRepository {

	@Override
	public long save(Connection conn, Rental rental) {
		final String sql = """
				INSERT INTO rental (member_id, rented_at, rental_status)
				VALUES (?, ?, ?)
				""";
		try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			// 값 세팅 (rented_at은 객체에 없으면 now 사용)
			ps.setLong(1, rental.getMemberId());
			LocalDateTime rentedAt = rental.getRentedAt() != null ? rental.getRentedAt() : LocalDateTime.now();
			ps.setTimestamp(2, Timestamp.valueOf(rentedAt));
			ps.setString(3, (rental.getRentalStatus() != null ? rental.getRentalStatus() : RentalStatus.OPEN).name());

			ps.executeUpdate();

			try (ResultSet rs = ps.getGeneratedKeys()) {
				if (rs.next()) {
					long id = rs.getLong(1);
					rental.setId(id);
					// DB가 저장한 값을 객체에도 반영
					rental.setRentedAt(rentedAt);
					if (rental.getRentalStatus() == null) {
						rental.setRentalStatus(RentalStatus.OPEN);
					}
					return id;
				}
			}
			throw new SQLException("rental PK 생성 실패");
		} catch (SQLException e) {
			throw new RuntimeException("JdbcRentalRepository.save 실패", e);
		}
	}

	@Override
	public Rental findById(Connection conn, long rentalId) {
		final String sql = """
				SELECT rental_id, member_id, rented_at, rental_status, created_at, updated_at
				  FROM rental
				 WHERE rental_id = ?
				""";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setLong(1, rentalId);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? map(rs) : null;
			}
		} catch (SQLException e) {
			throw new RuntimeException("JdbcRentalRepository.findById 실패", e);
		}
	}

	@Override
	public void updateStatus(Connection conn, long rentalId, RentalStatus status) {
		final String sql = "UPDATE rental SET rental_status = ? WHERE rental_id = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, status.name());
			ps.setLong(2, rentalId);
			int updated = ps.executeUpdate();
			if (updated == 0) {
				throw new SQLException("대상 rental이 없습니다: id=" + rentalId);
			}
		} catch (SQLException e) {
			throw new RuntimeException("JdbcRentalRepository.updateStatus 실패", e);
		}
	}

	// ===== 공통 매핑 =====
	private Rental map(ResultSet rs) throws SQLException {
		Rental r = new Rental();
		r.setId(rs.getLong("rental_id"));
		r.setMemberId(rs.getLong("member_id"));
		Timestamp rented = rs.getTimestamp("rented_at");
		r.setRentedAt(rented != null ? rented.toLocalDateTime() : null);
		r.setRentalStatus(RentalStatus.valueOf(rs.getString("rental_status")));

		Timestamp c = rs.getTimestamp("created_at");
		if (c != null) {
			r.setCreatedAt(c.toLocalDateTime());
		}
		Timestamp u = rs.getTimestamp("updated_at");
		if (u != null) {
			r.setUpdatedAt(u.toLocalDateTime());
		}
		return r;
	}

	// JdbcRentalRepository 구현
	@Override
	public List<Rental> findByMemberId(Connection conn, long memberId) {
		final String sql = """
				SELECT rental_id, member_id, rented_at, rental_status, created_at, updated_at
				  FROM rental
				 WHERE member_id=?
				 ORDER BY rented_at DESC
				""";
		List<Rental> list = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setLong(1, memberId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					list.add(map(rs));
				}
			}
			return list;
		} catch (SQLException e) {
			throw new RuntimeException("JdbcRentalRepository.findByMemberId 실패", e);
		}
	}

}
