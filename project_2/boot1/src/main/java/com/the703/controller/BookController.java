//package com.the703.controller;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.*;
//
//import org.springframework.web.multipart.MultipartFile;
//import org.springframework.web.servlet.mvc.support.RedirectAttributes;
//
//import com.the703.dto.BookDto;
//import com.the703.service.BookService;
//import com.the703.util.UtilPaging;
//
//import java.util.List;
//
//@Controller
//@RequestMapping("/book")
//public class BookController {
//
//    @Autowired
//    BookService service;
//
//    /** ■ Oracle 페이징 (OFFSET / FETCH) */
//    @GetMapping("/list")
//    public String list(Model model,
//                       @RequestParam(value="pstartno", defaultValue="1") int pstartno) {
//
//        int totalCnt = service.findAllCnt();
//
//        model.addAttribute("paging", new UtilPaging(totalCnt, pstartno));
//        model.addAttribute("list", service.findAll(pstartno));
//
//        return "book/list";
//    }
//
//    /** ■ 글쓰기 폼 */
//    @GetMapping("/write")
//    public String write() {
//        return "book/write";
//    }
//
//    /** ■ 글쓰기 처리 (Oracle SEQ 사용) */
//    @PreAuthorize("isAuthenticated()")
//    @PostMapping(value="/write", headers=("content-type=multipart/*"))
//    public String write_post(BookDto dto,
//                             @RequestParam("file") MultipartFile file,
//                             RedirectAttributes rttr) {
//
//        String result = "글쓰기 실패";
//
//        if (service.insert(dto, file) > 0) {
//            result = "글쓰기 성공";
//        }
//
//        rttr.addFlashAttribute("result", result);
//        return "redirect:/book/list";
//    }
//
//    /** ■ 상세보기 */
//    @GetMapping("/detail")
//    public String detail(BookDto dto, Model model) {
//        model.addAttribute("book", service.detail(dto));
//        return "book/detail";
//    }
//
//    /** ■ 수정 폼 */
//    @GetMapping("/edit")
//    public String edit(BookDto dto, Model model) {
//        model.addAttribute("book", service.editView(dto));
//        return "book/edit";
//    }
//
//    /** ■ 수정 처리 */
//    @PostMapping("/edit")
//    public String edit_post(BookDto dto,
//                            @RequestParam("file") MultipartFile file,
//                            RedirectAttributes rttr) {
//
//        String result = "수정 실패";
//
//        if (service.edit(dto, file) > 0) {
//            result = "수정 성공";
//        }
//
//        rttr.addFlashAttribute("result", result);
//        return "redirect:/book/detail?bookId=" + dto.getBookId();
//    }
//
//    /** ■ 삭제 */
//    @GetMapping("/delete")
//    public String delete(BookDto dto, RedirectAttributes rttr) {
//
//        String result = "삭제 실패";
//
//        if (service.delete(dto) > 0) {
//            result = "삭제 성공";
//        }
//
//        rttr.addFlashAttribute("result", result);
//        return "redirect:/book/list";
//    }
//
//    /* ---------------------------------------------------------
//       ■ AJAX 기능 추가 (검색 + 도서명 중복검사)
//       --------------------------------------------------------- */
//
//    /** ■ AJAX: 도서명 부분 검색 */
//    @ResponseBody
//    @GetMapping("/search")
//    public List<BookDto> search(@RequestParam("keyword") String keyword) {
//        return service.searchByTitle(keyword);
//    }
//
//    /** ■ AJAX: 도서명 중복 검사 */
//    @ResponseBody
//    @GetMapping("/check-title")
//    public boolean checkTitle(@RequestParam("title") String title) {
//        return service.isTitleDuplicate(title);
//    }
//}
//
//
//// http://localhost:8080/book/list
