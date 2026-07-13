package com.the703.api;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.the703.dto.BookDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MultiBookSearchService {

    private final NlBookApiService nlService;
    private final ApiNaverBook naverService;
    private final ApiKakaoBook kakaoService;

    public List<BookDto> searchAll(String keyword) {

        List<BookDto> result = new ArrayList<>();

        // 국립중앙도서관
        result.addAll(nlService.search(keyword, 1));

        // 네이버
        result.addAll(naverService.getBooks(keyword));

        // 카카오
        result.addAll(kakaoService.getBooks(keyword));

        return result;
    }

    public List<BookDto> searchNl(String keyword) {
        return nlService.search(keyword, 1);
    }

    public List<BookDto> searchNaver(String keyword) {
        return naverService.getBooks(keyword);
    }

    public List<BookDto> searchKakao(String keyword) {
        return kakaoService.getBooks(keyword);
    }
    
    public List<BookDto> searchNlWithCover(String keyword) {

        List<BookDto> list = nlService.search(keyword, 1);

        for (BookDto dto : list) {
            matchCover(dto);   // ⭐ 카카오 → 네이버 → default 자동 적용
        }

        return list;
    }

    
    public BookDto matchCover(BookDto dto) {

        String isbn = dto.getIsbn();

        if (isbn == null || isbn.isBlank()) {
            dto.setBookCover("default.png");
            return dto;
        }

        // 1) 카카오 표지
        String kakaoCover = kakaoService.getCoverByIsbn(isbn);

        if (kakaoCover != null && !kakaoCover.isBlank()) {
            dto.setBookCover(kakaoCover);
            return dto;
        }

        // 2) 네이버 표지
        String naverCover = naverService.getCoverByIsbn(isbn);

        if (naverCover != null && !naverCover.isBlank()) {
            dto.setBookCover(naverCover);
            return dto;
        }

        // 3) 둘 다 없으면 기본 이미지
        dto.setBookCover("default.png");
        return dto;
    }
    
}
