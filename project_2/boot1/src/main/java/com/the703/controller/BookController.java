package com.the703.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.the703.api.ApiKakaoBook;
import com.the703.api.BookKakaoDto;
import com.the703.api.BookNlDto;
import com.the703.api.NlBookApiService;
import com.the703.dto.BookDto;
import com.the703.service.BookService;
import com.the703.util.UtilPaging;

@Controller
public class BookController {

    @Autowired private BookService service;
    @Autowired private ApiKakaoBook  kakaoApi; 
    @Autowired private NlBookApiService nlApi;   // ⭐ 국립중앙도서관 API만 사용

    //////////////////////////////////////////////////
    // localhost:8080/book/kakaoinsert?search=java
    @RequestMapping("/book/kakaoinsert")
	public String  kakaoinsert( @RequestParam String search) {		
    	// 1. kakao 가져오기 serach  관심사 자바 list 반복문 
    	List<BookKakaoDto>  result 	= kakaoApi.getBooks(  search );
    	// 2. 북 insert 
    	 service.insert(result); 
    	return "redirect:/book/list";
	}
    
	//////////////////////////////////////////////////
    
	//    @RequestMapping("/book/nlinsert")
	//	public int nlinsert( @RequestParam String search) {		
	//    	// 1. 국립중앙박물관 가져오기 serach  관심사 자바 list 반복문 
	//    	// 2. 북 insert 
	//	}
    
//    @GetMapping("/book/search/nl/category")
//    public String searchNlByCategory(@RequestParam String category,
//                                     @RequestParam(defaultValue="1") int page,
//                                     Model model) {
//
//        // 국립중앙도서관 API 검색 실행
//        List<BookNlDto> result = nlApi.search(category, page);
//
//        model.addAttribute("apiList", result);
//        model.addAttribute("keyword", category);
//        model.addAttribute("page", page);
//
//        return "book/search-nl";
//    }
    

    @GetMapping("/book/search/nl/category")
    public String searchNlCategory(
            @RequestParam String category,
            Model model) {

        List<BookNlDto> result = nlApi.search(category, 1);

        model.addAttribute("apiList", result);
        model.addAttribute("category", category);

        return "book/search-nl_list";
    }    
    
    @GetMapping("/book/search/nl/list")
    public String searchNlList(
            @RequestParam String category,
            Model model) {

        List<BookNlDto> result =
                nlApi.search(category, 1);

        result = result.stream()
                .filter(b ->
                    b.getKdc_name_1s() != null &&
                    b.getKdc_name_1s().contains(category))
                .toList();

        model.addAttribute("apiList", result);
        model.addAttribute("category", category);

        return "book/search-nl_list";
    }    

 // KDC 분류 클릭시 결과 페이지
//    @GetMapping("/book/search/nl/list")
//    public String searchNlList(
//            @RequestParam String category,
//            Model model) {
//
//        // category 를 검색어로 사용
//        List<BookNlDto> result = nlApi.search(category, 1);
//
//        model.addAttribute("apiList", result);
//        model.addAttribute("category", category);
//
//        return "book/search-nl_list";
//    }    


    
    // 📘 전체 리스트 (카테고리 필터)
    @RequestMapping("/book/list")
    public String list(Model model,
                       @RequestParam(value="pstartno", defaultValue="1") int pstartno,
                       @RequestParam(value="category", required=false) String category) {

        if (category != null && !category.isBlank()) {
            model.addAttribute("paging", new UtilPaging(service.findCategoryCnt(category), pstartno));
            model.addAttribute("list", service.findByCategory(category, pstartno));
            model.addAttribute("selectedCategory", category);
        } else {
            model.addAttribute("paging", new UtilPaging(service.findAllCnt(), pstartno));
            model.addAttribute("list", service.findAll(pstartno));
        }

        return "book/list";
    }

    // 🔍 국립중앙도서관 API 검색
    @GetMapping("/book/search/nl")
    public String searchNl(@RequestParam(required=false) String keyword,
                           @RequestParam(defaultValue="1") int page,
                           Model model) {

        if (keyword != null && !keyword.isBlank()) {

            // ⭐ 국립중앙도서관 API 호출
            List<BookNlDto> result = nlApi.search(keyword, page);
            model.addAttribute("apiList", result);

            // ⭐ 검색된 도서에서 KDC 분류명만 추출
            List<String> kdcList = result.stream()
                    .map(BookNlDto::getKdc_name_1s)
                    .filter(s -> s != null && !s.isBlank())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            model.addAttribute("kdcList", kdcList);
        }

        model.addAttribute("keyword", keyword);
        model.addAttribute("page", page);

        return "book/search-nl";
    }

    // 📘 글쓰기 화면
    @GetMapping("/book/write")
    public String write() {
        return "book/write";
    }

    // 📘 글쓰기 처리
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value="/book/write", headers=("content-type=multipart/*"))
    public String write_post(BookDto dto,
                             @RequestParam("file") MultipartFile file,
                             RedirectAttributes rttr) {

        String result = (service.insert(dto, file) > 0) ? "글쓰기 성공" : "글쓰기 실패";
        rttr.addFlashAttribute("result", result);

        return "redirect:/book/list";
    }

    // 📘 상세보기
    @RequestMapping("/book/detail")
    public String detail(BookDto dto, Model model) {
        model.addAttribute("book", service.detail(dto));
        return "book/detail";
    }

    // 📘 수정 화면
    @GetMapping("/book/edit")
    public String edit(BookDto dto, Model model) {
        model.addAttribute("book", service.editView(dto));
        return "book/edit";
    }

    // 📘 수정 처리
    @PostMapping("/book/edit")
    public String edit_post(BookDto dto,
                            @RequestParam("file") MultipartFile file,
                            RedirectAttributes rttr) {

        String result = (service.edit(dto, file) > 0) ? "수정 성공" : "수정 실패";
        rttr.addFlashAttribute("result", result);

        return "redirect:/book/detail?bookId=" + dto.getBookId();
    }

    // 📘 삭제
    @GetMapping("/book/delete")
    public String delete(BookDto dto, RedirectAttributes rttr) {

        String result = (service.delete(dto) > 0) ? "삭제 성공" : "삭제 실패";
        rttr.addFlashAttribute("result", result);

        return "redirect:/book/list";
    }

    // 🔍 제목 검색 (AJAX)
    @ResponseBody
    @GetMapping("/book/search")
    public List<BookDto> searchBooks(@RequestParam String keyword) {
        BookDto dto = new BookDto();
        dto.setSearchType(keyword);
        return service.searchBooks(dto);
    }

    // 🔽 국립중앙도서관 API → DB 저장
    @PostMapping("/book/save")
    @ResponseBody
    public String saveFromApi(@RequestBody BookDto dto) {

        dto.setBookCover("default.png");

        int result = service.insert(dto, null);

        return (result > 0) ? "SUCCESS" : "FAIL";
    }
}



//http://localhost:8080/book/list
//http://localhost:8080/book/search/nl
