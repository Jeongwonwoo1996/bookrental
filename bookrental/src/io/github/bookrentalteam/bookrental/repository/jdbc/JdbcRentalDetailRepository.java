package io.github.bookrentalteam.bookrental.repository.jdbc;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import io.github.bookrentalteam.bookrental.domain.DetailStatus;
import io.github.bookrentalteam.bookrental.domain.RentalDetail;
import io.github.bookrentalteam.bookrental.repository.RentalDetailRepository;

public class JdbcRentalDetailRepository implements RentalDetailRepository {

	@Override
	public void saveAll(Connection conn, long rentalId, List<RentalDetail> details) {
		final String sql = """
				INSERT INTO rental_detail
				  (rental_id, book_id, due_at, returned_at, detail_status, extension_count)
				VALUES (?, ?, ?, ?, ?, ?)
				""";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			for (RentalDetail d : details) {
				ps.setLong(1, rentalId);
				ps.setLong(2, d.getBookId());
				ps.setDate(3, Date.valueOf(d.getDueAt()));
				if (d.getReturnedAt() != null) {
					ps.setDate(4, Date.valueOf(d.getReturnedAt()));
				} else {
					ps.setNull(4, Types.DATE);
				}
				ps.setString(5, d.getDetailStatus().name());
				ps.setInt(6, d.getExtensionCount());
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (SQLException e) {
			throw new RuntimeException("JdbcRentalDetailRepository.saveAll 실패", e);
		}
	}

	@Override
	public List<RentalDetail> findByRentalId(Connection conn, long rentalId) {
		final String sql = """
				SELECT rental_id, book_id, due_at, returned_at, detail_status, extension_count,
				       created_at, updated_at
				  FROM rental_detail
				 WHERE rental_id = ?
				 ORDER BY book_id
				""";
		List<RentalDetail> list = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setLong(1, rentalId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					list.add(map(rs));
				}
			}
			return list;
		} catch (SQLException e) {
			throw new RuntimeException("JdbcRentalDetailRepository.findByRentalId 실패", e);
		}
	}

	@Override
	public int updateStatusBatch(Connection conn, long rentalId, List<Long> bookIds, DetailStatus status,
			LocalDate returnedAt) {
		final String sql = """
				UPDATE rental_detail
				   SET detail_status = ?, returned_at = ?
				 WHERE rental_id = ? AND book_id = ?
				""";
		int updatedTotal = 0;
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			boolean setReturnDate = (status == DetailStatus.RETURNED);
			Date ret = setReturnDate ? Date.valueOf(returnedAt != null ? returnedAt : LocalDate.now()) : null;

			for (Long bookId : bookIds) {
				ps.setString(1, status.name());
				if (setReturnDate) {
					ps.setDate(2, ret);
				} else {
					ps.setNull(2, Types.DATE); // RETURNED가 아니면 returned_at 비움
				}
				ps.setLong(3, rentalId);
				ps.setLong(4, bookId);
				ps.addBatch();
			}
			int[] counts = ps.executeBatch();
			updatedTotal = sumBatchCounts(counts);
		} catch (SQLException e) {
			throw new RuntimeException("JdbcRentalDetailRepository.updateStatusBatch 실패", e);
		}
		return updatedTotal;
	}

	@Override
	public int extendBatch7d(Connection conn, long rentalId, List<Long> bookIds) {
		// 연장 규칙 강제:
		// - detail_status = 'RENTED'
		// - extension_count < 1
		// - due_at >= CURDATE() (연체가 아니어야)
		final String sql = """
				UPDATE rental_detail
				   SET due_at = DATE_ADD(due_at, INTERVAL 7 DAY),
				       extension_count = extension_count + 1
				 WHERE rental_id = ?
				   AND book_id = ?
				   AND detail_status = 'RENTED'
				   AND extension_count < 1
				   AND due_at >= CURDATE()
				""";
		int updatedTotal = 0;
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			for (Long bookId : bookIds) {
				ps.setLong(1, rentalId);
				ps.setLong(2, bookId);
				ps.addBatch();
			}
			int[] counts = ps.executeBatch();
			updatedTotal = sumBatchCounts(counts);
		} catch (SQLException e) {
			throw new RuntimeException("JdbcRentalDetailRepository.extendBatch7d 실패", e);
		}
		return updatedTotal;
	}

	@Override
	public int countActive(Connection conn, long rentalId) {
		final String sql = """
				SELECT COUNT(*)
				  FROM rental_detail
				 WHERE rental_id = ?
				   AND detail_status IN ('RENTED','OVERDUE','LOST')
				""";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setLong(1, rentalId);
			try (ResultSet rs = ps.executeQuery()) {
				rs.next();
				return rs.getInt(1);
			}
		} catch (SQLException e) {
			throw new RuntimeException("JdbcRentalDetailRepository.countActive 실패", e);
		}
	}

	@Override
	public int countReturned(Connection conn, long rentalId) {
		final String sql = """
				SELECT COUNT(*)
				  FROM rental_detail
				 WHERE rental_id = ?
				   AND detail_status = 'RETURNED'
				""";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setLong(1, rentalId);
			try (ResultSet rs = ps.executeQuery()) {
				rs.next();
				return rs.getInt(1);
			}
		} catch (SQLException e) {
			throw new RuntimeException("JdbcRentalDetailRepository.countReturned 실패", e);
		}
	}

	// ===== 내부 유틸 =====
	private RentalDetail map(ResultSet rs) throws SQLException {
		RentalDetail d = new RentalDetail();
		d.setRentalId(rs.getLong("rental_id"));
		d.setBookId(rs.getLong("book_id"));
		d.setDueAt(rs.getDate("due_at").toLocalDate());

		Date ret = rs.getDate("returned_at");
		d.setReturnedAt(ret != null ? ret.toLocalDate() : null);

		d.setDetailStatus(DetailStatus.valueOf(rs.getString("detail_status")));
		d.setExtensionCount(rs.getInt("extension_count"));
		return d;
	}

	private int sumBatchCounts(int[] counts) {
		int sum = 0;
		if (counts == null) {
			return 0;
		}
		for (int c : counts) {
			// SUCCESS_NO_INFO(-2)면 정확한 수는 모르지만 "성공"으로 간주하여 1씩 올려도 되고,
			// 여기서는 0 이상만 더합니다.
			if (c > 0) {
				sum += c;
			}
		}
		return sum;
	}

	@Override
	public List<Long> findActiveBookIdsByMemberAndBookIds(Connection conn, long memberId, List<Long> bookIds) {
		if (bookIds == null || bookIds.isEmpty()) {
			return List.of();
		}

		String placeholders = String.join(",", java.util.Collections.nCopies(bookIds.size(), "?"));
		final String sql = """
				SELECT DISTINCT rd.book_id
				  FROM rental_detail rd
				  JOIN rental r ON r.rental_id = rd.rental_id
				 WHERE r.member_id = ?
				   AND rd.book_id IN (""" + placeholders + """
				   )
				   AND rd.detail_status IN ('RENTED','OVERDUE','LOST')
				""";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			int idx = 1;
			ps.setLong(idx++, memberId);
			for (Long id : bookIds) {
				ps.setLong(idx++, id);
			}
			try (ResultSet rs = ps.executeQuery()) {
				List<Long> result = new ArrayList<>();
				while (rs.next()) {
					result.add(rs.getLong(1));
				}
				return result;
			}
		} catch (SQLException e) {
			throw new RuntimeException("JdbcRentalDetailRepository.findActiveBookIdsByMemberAndBookIds 실패", e);
		}
	}
}
