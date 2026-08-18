package com.thejoa703.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 페이징 응답 공용 Dto (Book, Sboard2 화면 목록에서 공용으로 사용)
 * - currentPage 는 1부터 시작합니다. (0부터 시작하는 Spring Pageable 과 화면단 페이지번호를 분리)
 */
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class PageResponseDto<T> {

	private List<T> content;      // 현재 페이지의 목록
	private int currentPage;      // 현재 페이지 (1부터 시작)
	private int pageSize;         // 한 페이지에 보여줄 개수 (기본 12)
	private long totalElements;   // 전체 데이터 개수
	private int totalPages;       // 전체 페이지수
}
