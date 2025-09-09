package io.github.bookrentalteam.bookrental.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DbTest {
	public static void main(String[] args) {
		try (Connection conn = ConnectionManager.getConnection();
				PreparedStatement ps = conn.prepareStatement("SELECT 1");
				ResultSet rs = ps.executeQuery()) {
			if (rs.next()) {
				System.out.println("DB 연결 OK: " + rs.getInt(1));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
