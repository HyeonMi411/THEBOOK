package com.the703.api;

import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.the703.api.NlBookApiService;
import com.the703.api.ApiKakaoBook;
import com.the703.dto.BookDto;
import com.the703.service.BookService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BookScheduler {

    private final NlBookApiService nlService;
    private final ApiKakaoBook kakaoService;
    private final BookService bookService;

    /**
     * 매일 새벽 1시 자동 실행
     * cron = 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void autoImportBooks() {

        System.out.println("📚 [자동수집] 국립중앙도서관 도서 수집 시작");

        // 1) 국립중앙도서관 API 호출 (예: 인기 검색어 자동 수집)
        List<Map<String, Object>> apiList = nlService.search("베스트셀러", 1);

        int savedCount = 0;

        for (Map<String, Object> item : apiList) {

            BookDto dto = new BookDto();

            dto.setTitle((String) item.get("title_info"));
            dto.setAuthor((String) item.get("author_info"));
            dto.setPublisher((String) item.get("pub_info"));
            dto.setPublishDate((String) item.get("pub_year_info"));
            dto.setCategory("국립중앙도서관");
            dto.setDescription((String) item.get("subject_info"));

            // ⭐ ISBN 추출
            String isbn = (String) item.get("isbn");
            dto.setIsbn(isbn);

            // ⭐ 중복 검사 (제목 또는 ISBN)
            if (bookService.isDuplicate(dto)) {
                continue; // 이미 있는 책이면 저장 안 함
            }

            // ⭐ 카카오 API로 표지 자동 매칭
            if (isbn != null && !isbn.isBlank()) {
                String cover = kakaoService.getCoverByIsbn(isbn);
                dto.setBookCover(cover != null ? cover : "default.png");
            } else {
                dto.setBookCover("default.png");
            }

            // ⭐ DB 저장
            bookService.insert(dto, null);
            savedCount++;
        }

        System.out.println("📚 [자동수집] 저장 완료: " + savedCount + "건");
    }
}
