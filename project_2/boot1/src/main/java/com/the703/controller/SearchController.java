package com.the703.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.the703.api.MultiBookSearchService;
import com.the703.dto.BookDto;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/search")
public class SearchController {

    private final MultiBookSearchService multiSearch;

    // 통합 검색 화면
    @GetMapping("/all")
    public String searchAll(@RequestParam(required = false) String keyword,
                            Model model) {

        if (keyword != null && !keyword.isBlank()) {
            List<BookDto> all = multiSearch.searchAll(keyword);
            List<BookDto> nl = multiSearch.searchNl(keyword);
            List<BookDto> naver = multiSearch.searchNaver(keyword);
            List<BookDto> kakao = multiSearch.searchKakao(keyword);

            model.addAttribute("all", all);
            model.addAttribute("nl", nl);
            model.addAttribute("naver", naver);
            model.addAttribute("kakao", kakao);
        }

        model.addAttribute("keyword", keyword);

        return "search/multi-search";
    }
    
    @GetMapping("/search/nl")
    public String searchNl(@RequestParam String keyword, Model model) {

        List<BookDto> list = multiSearch.searchNlWithCover(keyword);

        model.addAttribute("list", list);
        model.addAttribute("keyword", keyword);

        return "search/nl-search";
    }

    
}
