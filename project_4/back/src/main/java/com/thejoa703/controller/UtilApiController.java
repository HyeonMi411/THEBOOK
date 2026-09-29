package com.thejoa703.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.thejoa703.api.BookNewsService;
import com.thejoa703.api.ClovaOcrService;
import com.thejoa703.api.KakaoLocalService;
import com.thejoa703.api.KmaWeatherService;
import com.thejoa703.dto.UtilDto.GeocodeDto;
import com.thejoa703.dto.UtilDto.NewsDto;
import com.thejoa703.dto.UtilDto.OcrResultDto;
import com.thejoa703.dto.UtilDto.StoreDto;
import com.thejoa703.dto.UtilDto.WeatherDto;
import com.thejoa703.service.StoreLocationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 외부 API 연동 (boot1 수업의 ApiUtilController 를 앱용 REST API 로 정리)
 *
 * GET  /api/util/weather?lat=&lon=  기상청 초단기실황 (전체공개)
 * GET  /api/util/stores             오프라인 매장 목록 - 지도 마커 (전체공개)
 * GET  /api/util/geocode?address=   주소 → 좌표 (카카오 로컬) (전체공개)
 * GET  /api/util/book-news          도서 뉴스 (RSS 크롤링) (전체공개)
 * POST /api/util/ocr                책 표지 사진 → 글자 인식 → 추천 검색어 (로그인 필요 - 유료 API 보호)
 */
@Tag(name = "Util Api", description = "날씨 / 지도 / 주소 / 뉴스 / OCR")
@RestController
@RequestMapping("/api/util")
@RequiredArgsConstructor
public class UtilApiController {

	private final KmaWeatherService kmaWeatherService;
	private final StoreLocationService storeLocationService;
	private final KakaoLocalService kakaoLocalService;
	private final BookNewsService bookNewsService;
	private final ClovaOcrService clovaOcrService;

	@Operation(summary = "현재 날씨 (기상청 초단기실황)", description = "위도/경도를 기상청 격자로 변환해 가장 최근 관측값을 돌려줍니다. 기본값은 서울시청.")
	@GetMapping("/weather")
	public ResponseEntity<WeatherDto> weather(
			@Parameter(description = "위도") @RequestParam(name = "lat", defaultValue = "37.5665") double lat,
			@Parameter(description = "경도") @RequestParam(name = "lon", defaultValue = "126.9780") double lon
	) {
		return ResponseEntity.ok(kmaWeatherService.getWeather(lat, lon));
	}

	@Operation(summary = "오프라인 매장 목록 (지도 마커)")
	@GetMapping("/stores")
	public ResponseEntity<List<StoreDto>> stores() {
		return ResponseEntity.ok(storeLocationService.getStores());
	}

	@Operation(summary = "주소 → 좌표 (카카오 로컬 API)")
	@GetMapping("/geocode")
	public ResponseEntity<GeocodeDto> geocode(@RequestParam("address") String address) {
		if (address.length() > 200) {
			throw new IllegalArgumentException("주소가 너무 깁니다.");
		}
		return ResponseEntity.ok(kakaoLocalService.geocode(address));
	}

	@Operation(summary = "도서 뉴스 (RSS 크롤링, 30분마다 갱신)")
	@GetMapping("/book-news")
	public ResponseEntity<Map<String, Object>> bookNews() {
		List<NewsDto> items = bookNewsService.getLatest();
		String updated = bookNewsService.getLastUpdated();
		return ResponseEntity.ok(Map.of("items", items, "updatedAt", updated == null ? "" : updated));
	}

	@Operation(summary = "책 표지 사진으로 검색어 추출 (CLOVA OCR)", description = "JPG/PNG 5MB 이하. 인식된 줄과 추천 검색어를 돌려줍니다.")
	@PostMapping(value = "/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<OcrResultDto> ocr(@RequestPart("image") MultipartFile image) {
		return ResponseEntity.ok(clovaOcrService.recognize(image));
	}
}
