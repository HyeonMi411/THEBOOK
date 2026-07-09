package com.the703.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.the703.dto.BookDto;
import com.the703.service.BookService;
import com.the703.util.UtilPaging;

@Controller
public class BookController {

    @Autowired
    private BookService service;

    // ---------------------------------------------------------
    // 📘 전체 리스트 (페이징)
    // ---------------------------------------------------------
    @RequestMapping("/book/list")
    public String list(Model model,
                       @RequestParam(value = "pstartno", defaultValue = "1") int pstartno) {

        model.addAttribute("paging", new UtilPaging(service.findAllCnt(), pstartno));
        model.addAttribute("list", service.findAll(pstartno));

        return "book/list";
    }


    // ---------------------------------------------------------
    // 📘 글쓰기 폼
    // ---------------------------------------------------------
    @RequestMapping(value = "/book/write", method = RequestMethod.GET)
    public String write() {
        return "book/write";
    }


    // ---------------------------------------------------------
    // 📘 글쓰기 기능
    // ---------------------------------------------------------
    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/book/write", method = RequestMethod.POST, headers = ("content-type=multipart/*"))
    public String write_post(BookDto dto,
                             @RequestParam("file") MultipartFile file,
                             RedirectAttributes rttr) {

        String result = "글쓰기 실패";

        if (service.insert(dto, file) > 0) {
            result = "글쓰기 성공";
        }

        rttr.addFlashAttribute("result", result);

        return "redirect:/book/list";
    }


    // ---------------------------------------------------------
    // 📘 상세보기
    // ---------------------------------------------------------
    @RequestMapping("/book/detail")
    public String detail(BookDto dto, Model model) {
        model.addAttribute("book", service.detail(dto));
        return "book/detail";
    }


    // ---------------------------------------------------------
    // 📘 수정 폼
    // ---------------------------------------------------------
    @RequestMapping(value = "/book/edit", method = RequestMethod.GET)
    public String edit(BookDto dto, Model model) {
        model.addAttribute("book", service.editView(dto));
        return "book/edit";
    }


    // ---------------------------------------------------------
    // 📘 수정 기능
    // ---------------------------------------------------------
    @RequestMapping(value = "/book/edit", method = RequestMethod.POST)
    public String edit_post(BookDto dto,
                            @RequestParam("file") MultipartFile file,
                            RedirectAttributes rttr) {

        String result = "수정 실패";

        if (service.edit(dto, file) > 0) {
            result = "수정 성공";
        }

        rttr.addFlashAttribute("result", result);

        return "redirect:/book/detail?bookId=" + dto.getBookId();
    }


    // ---------------------------------------------------------
    // 📘 삭제 기능
    // ---------------------------------------------------------
    @RequestMapping(value = "/book/delete", method = RequestMethod.GET)
    public String delete(BookDto dto, RedirectAttributes rttr) {

        String result = "삭제 실패";

        if (service.delete(dto) > 0) {
            result = "삭제 성공";
        }

        rttr.addFlashAttribute("result", result);

        return "redirect:/book/list";
    }


    // ---------------------------------------------------------
    // 🔍 카테고리 검색
    // ---------------------------------------------------------
//    @GetMapping("/book/category")
//    public String category(@RequestParam("category") String category, Model model) {
//
//        List<BookDto> list = service.findByCategory(category);
//
//        model.addAttribute("list", list);
//        model.addAttribute("category", category);
//
//        return "book/category";
//    }


    // ---------------------------------------------------------
    // 🔍 제목 검색 (AJAX)
    // ---------------------------------------------------------
//    @ResponseBody
//    @GetMapping("/book/search/title")
//    public List<BookDto> searchByTitle(@RequestParam("keyword") String keyword) {
//        return service.searchByTitle(keyword);
//    }


    // ---------------------------------------------------------
    // 🔍 통합 검색 (AJAX)
    // ---------------------------------------------------------
    @ResponseBody
    @GetMapping("/book/search")
    public List<BookDto> searchBooks(BookDto dto) {
        return service.searchBooks(dto);
    }


    // ---------------------------------------------------------
    // ⚠️ 도서명 중복검사 (AJAX)
    // ---------------------------------------------------------
//    @ResponseBody
//    @GetMapping("/book/check-title")
//    public BookDto checkTitleDuplicate(@RequestParam("title") String title) {
//
//        int count = service.checkTitleDuplicate(title);
//
//        BookDto result = new BookDto();
//        result.setTitle(title);
//        result.setDuplicate(count > 0);
//        result.setSuccess(true);
//
//        if (count > 0) {
//            result.setMessage("이미 존재하는 도서명입니다.");
//        } else {
//            result.setMessage("사용 가능한 도서명입니다.");
//        }
//
//        return result;
//    }
}
