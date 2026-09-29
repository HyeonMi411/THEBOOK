package com.thejoa703.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.thejoa703.dto.UtilDto.StoreDto;

/**
 * 오프라인 매장(픽업 지점) 목록 - 앱의 "매장 지도" 화면
 *
 * 포트폴리오용 예시 매장입니다. 실제 운영 시에는 STORE 테이블 + 관리자 등록 화면으로 옮기면 됩니다.
 * 좌표는 각 지역 대표 지점(역/광장 인근)의 대략적인 위치입니다.
 */
@Service
public class StoreLocationService {

	private static final List<StoreDto> STORES = List.of(
			new StoreDto(1L, "BookStore 강남점", "서울 강남구 강남대로 396 (강남역 인근)", "02-000-0001",
					"10:00 ~ 22:00", 37.4979, 127.0276),
			new StoreDto(2L, "BookStore 광화문점", "서울 종로구 세종대로 172 (광화문광장 인근)", "02-000-0002",
					"09:30 ~ 22:00", 37.5716, 126.9769),
			new StoreDto(3L, "BookStore 홍대점", "서울 마포구 양화로 160 (홍대입구역 인근)", "02-000-0003",
					"11:00 ~ 23:00", 37.5572, 126.9245),
			new StoreDto(4L, "BookStore 인천 송도점", "인천 연수구 컨벤시아대로 165 (센트럴파크 인근)", "032-000-0004",
					"10:30 ~ 21:30", 37.3925, 126.6390),
			new StoreDto(5L, "BookStore 부산 서면점", "부산 부산진구 중앙대로 730 (서면역 인근)", "051-000-0005",
					"10:00 ~ 22:00", 35.1578, 129.0600)
	);

	public List<StoreDto> getStores() {
		return STORES;
	}
}
