package com.thejoa703.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 스키마 자동 보정 러너
 * Hibernate 의 ddl-auto:update 는 "새 컬럼 추가"는 처리하지만, 이미 존재하는 컬럼의
 * 제약조건 완화(NOT NULL 해제)는 반영하지 못하는 경우가 있어서, 서버 기동 시점에
 * 실제 컬럼 상태를 확인해 필요하면 직접 고쳐줍니다.
 *
 * - BOOK.PUBLISH_DATE : 카카오/국립중앙도서관 자동수집 도서는 출판일 정보가 없거나
 *   파싱에 실패할 수 있어 null 로 저장합니다. 그러려면 이 컬럼이 NULL 을 허용해야 합니다.
 *
 * 이미 NULL 허용 상태면 아무것도 하지 않으므로(멱등성) 매번 실행돼도 안전하고,
 * 실패해도 애플리케이션 기동 자체는 막지 않습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaAutoFixRunner implements ApplicationRunner {

	private final DataSource dataSource;

	@Override
	public void run(ApplicationArguments args) {
		fixColumnNullableIfNeeded("BOOK", "PUBLISH_DATE");
	}

	private void fixColumnNullableIfNeeded(String tableName, String columnName) {
		try (Connection conn = dataSource.getConnection()) {
			DatabaseMetaData meta = conn.getMetaData();

			boolean isCurrentlyNotNull = false;
			boolean columnFound = false;
			try (ResultSet rs = meta.getColumns(null, null, tableName, columnName)) {
				if (rs.next()) {
					columnFound = true;
					isCurrentlyNotNull = "NO".equalsIgnoreCase(rs.getString("IS_NULLABLE"));
				}
			}

			if (!columnFound) {
				// 아직 테이블/컬럼 자체가 없는 최초 기동 시점 - Hibernate 가 이번 기동에서
				// 만들어줄 것이므로(NULL 허용으로), 여기서는 아무 것도 할 필요가 없습니다.
				return;
			}

			if (!isCurrentlyNotNull) {
				// 이미 NULL 허용 상태 - 할 일 없음 (재기동시 매번 여기로 빠짐, 정상)
				return;
			}

			try (Statement stmt = conn.createStatement()) {
				stmt.execute("ALTER TABLE " + tableName + " MODIFY (" + columnName + " NULL)");
				log.info("스키마 자동 보정 완료: {}.{} 컬럼을 NULL 허용으로 변경했습니다.", tableName, columnName);
			}

		} catch (Exception e) {
			// 스키마 보정 실패가 애플리케이션 기동 자체를 막지 않도록 예외를 삼킵니다.
			// (권한 부족, 방언 차이 등으로 실패하면 로그만 남기고, 필요시 수동으로 확인하면 됩니다)
			log.warn("스키마 자동 보정 중 문제가 발생했습니다 ({}.{}): {}", tableName, columnName, e.getMessage());
		}
	}
}
