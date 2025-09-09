package io.github.bookrentalteam.bookrental.repository.jdbc;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;

import io.github.bookrentalteam.bookrental.config.ConnectionManager;
import io.github.bookrentalteam.bookrental.domain.Member;
import io.github.bookrentalteam.bookrental.domain.Role;
import io.github.bookrentalteam.bookrental.repository.MemberRepository;

public class JdbcMemberRepository implements MemberRepository {

	@Override
	public long save(Member m) {
		final String sql = """
				INSERT INTO member(name, email, password, role, suspend_until)
				VALUES (?, ?, ?, ?, ?)
				""";
		try (Connection conn = ConnectionManager.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, m.getName());
			ps.setString(2, m.getEmail());
			ps.setString(3, m.getPasswordHashed());
			ps.setString(4, m.getRole().name());
			if (m.getSuspendUntil() != null) {
				ps.setDate(5, Date.valueOf(m.getSuspendUntil()));
			} else {
				ps.setNull(5, Types.DATE);
			}

			ps.executeUpdate();
			try (ResultSet rs = ps.getGeneratedKeys()) {
				if (rs.next()) {
					long id = rs.getLong(1);
					m.setId(id);
					return id;
				}
			}
			throw new SQLException("member PK 생성 실패");
		} catch (SQLException e) {
			throw new RuntimeException("JdbcMemberRepository.save 실패", e);
		}
	}

	@Override
	public Member findById(long id) {
		final String sql = """
				SELECT member_id, name, email, password, role, suspend_until, created_at, updated_at
				FROM member WHERE member_id=?
				""";
		try (Connection conn = ConnectionManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setLong(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? map(rs) : null;
			}
		} catch (SQLException e) {
			throw new RuntimeException("JdbcMemberRepository.findById 실패", e);
		}
	}

	@Override
	public Member findByEmail(String email) {
		final String sql = """
				SELECT member_id, name, email, password, role, suspend_until, created_at, updated_at
				FROM member WHERE email=?
				""";
		try (Connection conn = ConnectionManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, email);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? map(rs) : null;
			}
		} catch (SQLException e) {
			throw new RuntimeException("JdbcMemberRepository.findByEmail 실패", e);
		}
	}

	private Member map(ResultSet rs) throws SQLException {
		Member m = new Member();
		m.setId(rs.getLong("member_id"));
		m.setName(rs.getString("name"));
		m.setEmail(rs.getString("email"));
		m.setPasswordHashed(rs.getString("password")); // 저장은 해시
		m.setRole(Role.valueOf(rs.getString("role")));
		Date s = rs.getDate("suspend_until");
		m.setSuspendUntil(s != null ? s.toLocalDate() : null);
		m.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
		Timestamp up = rs.getTimestamp("updated_at");
		m.setUpdatedAt(up != null ? up.toLocalDateTime() : null);
		return m;
	}

	// JdbcMemberRepository 구현
	@Override
	public void updateSuspendUntil(Connection conn, long memberId, LocalDate suspendUntil) {
		final String sql = "UPDATE member SET suspend_until=? WHERE member_id=?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			if (suspendUntil != null) {
				ps.setDate(1, Date.valueOf(suspendUntil));
			} else {
				ps.setNull(1, java.sql.Types.DATE);
			}
			ps.setLong(2, memberId);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("JdbcMemberRepository.updateSuspendUntil 실패", e);
		}
	}

}
