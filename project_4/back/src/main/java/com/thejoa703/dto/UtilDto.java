package com.thejoa703.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 외부 API 연동(날씨/지도/주소/뉴스/OCR/외부 도서검색) 응답 DTO 모음 */
public class UtilDto {

	/** 기상청 초단기실황 - 앱 홈 화면 날씨 카드 */
	@Getter @Setter @NoArgsConstructor
	public static class WeatherDto {
		private boolean available;       // false 면 키 미설정/호출 실패 → 앱은 카드 대신 안내문
		private String message;          // available=false 일 때 사유
		private Double temperature;      // T1H 기온(℃)
		private Integer humidity;        // REH 습도(%)
		private Double rainfall1h;       // RN1 1시간 강수량(mm)
		private Double windSpeed;        // WSD 풍속(m/s)
		private Integer precipitationType; // PTY 0 없음 1 비 2 비/눈 3 눈 5 빗방울 6 빗방울눈날림 7 눈날림
		private String precipitationText;
		private String baseDate;         // 발표 기준 yyyyMMdd
		private String baseTime;         // 발표 기준 HHmm
		private int nx;
		private int ny;
		private String readingTip;       // 날씨에 어울리는 한 줄 (앱 홈 문구)
	}

	/** 오프라인 매장(픽업 지점) - 지도 화면 마커 */
	@Getter @Setter @NoArgsConstructor @AllArgsConstructor
	public static class StoreDto {
		private Long id;
		private String name;
		private String address;
		private String phone;
		private String hours;
		private double lat;
		private double lng;
	}

	/** 주소 → 좌표 (카카오 로컬 API) */
	@Getter @Setter @NoArgsConstructor @AllArgsConstructor
	public static class GeocodeDto {
		private boolean found;
		private String address;
		private Double lat;
		private Double lng;
	}

	/** 도서 뉴스 (RSS 크롤링) */
	@Getter @Setter @NoArgsConstructor @AllArgsConstructor
	public static class NewsDto {
		private String title;
		private String link;
		private String source;
		private String publishedAt;
	}

	/** 표지/책 사진 OCR 결과 */
	@Getter @Setter @NoArgsConstructor @AllArgsConstructor
	public static class OcrResultDto {
		private List<String> lines;     // 인식된 줄 (위→아래)
		private String keyword;         // 도서 검색에 바로 쓸 추천 검색어 (가장 큰/긴 줄)
	}

	/** 외부 도서 통합검색 결과 한 권 (카카오/네이버/국립중앙도서관 공통 형태) */
	@Getter @Setter @NoArgsConstructor
	public static class ExternalBookDto {
		private String source;          // kakao | naver | nl
		private String title;
		private String authors;
		private String publisher;
		private String publishDate;     // yyyy-MM-dd 또는 yyyy (출처마다 다름)
		private String isbn;
		private String thumbnail;
		private String description;
		private Integer price;
		private String link;
		private boolean alreadyInStore; // 이미 쇼핑몰에 같은 제목이 등록돼 있는지
	}
}
