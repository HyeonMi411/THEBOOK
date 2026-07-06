//package com.the703.controller;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestMethod;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.multipart.MultipartFile;
//import org.springframework.web.servlet.mvc.support.RedirectAttributes;
//
//import com.the703.dto.BookDto;
//import com.the703.service.BookService;
//import com.the703.util.PagingUtil;
//
//@Controller
//public class BookController {  
//
//    @Autowired 
//    BookService service;
//
//    // ■ Oracle 페이징 (ROWNUM 기반)
//    @RequestMapping("/book/list")
//    public String list(Model model,
//                       @RequestParam(value="pstartno", defaultValue="1") int pstartno) {
//
//        // Oracle에서는 COUNT(*) 사용
//        int totalCnt = service.findAllCnt();
//
//        // Oracle 페이징 유틸 (ROWNUM 기반)
//        model.addAttribute("paging", new PagingUtil(totalCnt, pstartno));
//
//        // Oracle 페이징 쿼리에서 ROWNUM BETWEEN 사용
//        model.addAttribute("list", service.findAll(pstartno));
//
//        return "book/list";
//    }
//
//    // 글쓰기 폼
//    @RequestMapping(value="/book/write", method=RequestMethod.GET)
//    public String write() {
//        return "book/write";
//    }
//
//    // 글쓰기 기능 (Oracle에서는 SEQ 사용)
//    @PreAuthorize("isAuthenticated()")
//    @RequestMapping(value="/book/write", 
//                    method=RequestMethod.POST, 
//                    headers=("content-type=multipart/*"))
//    public String write_post(BookDto dto,
//                             @RequestParam("file") MultipartFile file,
//                             RedirectAttributes rttr) {
//
//        String result = "글쓰기 실패";
//
//        // Oracle에서는 INSERT 시 SEQ.NEXTVAL 사용
//        if(service.insert(dto, file) > 0) {
//            result = "글쓰기 성공";
//        }
//
//        rttr.addFlashAttribute("result", result);
//        return "redirect:/book/list";
//    }
//
//    // 상세보기
//    @RequestMapping("/book/detail")
//    public String detail(BookDto dto, Model model) {
//        model.addAttribute("book", service.detail(dto));
//        return "book/detail";
//    }
//
//    // 수정 폼
//    @RequestMapping(value="/book/edit", method=RequestMethod.GET)
//    public String edit(BookDto dto, Model model) {
//        model.addAttribute("book", service.editView(dto));
//        return "book/edit";
//    }
//
//    // 수정 기능
//    @RequestMapping(value="/book/edit", method=RequestMethod.POST)
//    public String edit_post(BookDto dto,
//                            @RequestParam("file") MultipartFile file,
//                            RedirectAttributes rttr) {
//
//        String result = "비밀번호 확인!";
//        if(service.edit(dto, file) > 0) {
//            result = "수정성공";
//        }
//
//        rttr.addFlashAttribute("result", result);
//        return "redirect:/book/detail?bookId=" + dto.getBookId();
//    }
//
//    // 삭제 기능
//    @RequestMapping(value="/book/delete", method=RequestMethod.GET)
//    public String delete_post(BookDto dto, RedirectAttributes rttr) {
//
//        String result = "비밀번호 확인!";
//        if(service.delete(dto) > 0) {
//            result = "삭제성공";
//        }
//
//        rttr.addFlashAttribute("result", result);
//        return "redirect:/book/list";
//    }
//}
