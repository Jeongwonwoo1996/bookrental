package io.github.bookrentalteam.bookrental.config;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * JDBC 커넥션 유틸. - resources/db.properties 로부터 접속정보 로드 - 우선순위: JVM 옵션(-Ddb.*) >
 * 환경변수(DB_*) > db.properties
 */
public class ConnectionManager {

	private static final String URL;
	private static final String USER;
	private static final String PASSWORD;

	static {
		try {
			// 1) properties 로드 (classpath: db.properties)
			Properties props = new Properties();
			try (InputStream in = ConnectionManager.class.getClassLoader().getResourceAsStream("db.properties")) {
				if (in != null) {
					props.load(in);
				}
			}

			// 2) 우선순위 적용
			String envUrl = System.getenv("DB_URL");
			String envUser = System.getenv("DB_USER");
			String envPwd = System.getenv("DB_PASSWORD");

			URL = firstNonBlank(System.getProperty("db.url"), envUrl, props.getProperty("db.url"));
			USER = firstNonBlank(System.getProperty("db.user"), envUser, props.getProperty("db.user"));
			PASSWORD = firstNonBlank(System.getProperty("db.password"), envPwd, props.getProperty("db.password"));

			// 3) 드라이버 로드 및 최소 검증
			Class.forName("com.mysql.cj.jdbc.Driver");
			if (URL == null || USER == null) {
				throw new IllegalStateException("DB 설정 누락: db.url 또는 db.user");
			}
		} catch (Exception e) {
			throw new RuntimeException("DB 설정 로드 실패", e);
		}
	}

	private static String firstNonBlank(String... values) {
		for (String v : values) {
			if (v != null && !v.isBlank()) {
				return v;
			}
		}
		return null;
	}

	/** 기본은 오토커밋 true. 트랜잭션 작업 시 conn.setAutoCommit(false)로 전환하여 사용하세요. */
	public static Connection getConnection() throws SQLException {
		return DriverManager.getConnection(URL, USER, PASSWORD);
	}
}
