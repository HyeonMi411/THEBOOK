package com.thejoa703.dto;

import java.time.LocalDateTime;

import com.thejoa703.entity.Sboard2;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class Sboard2Dto {

    // 작성/수정 요청 Dto
    @Getter @Setter @AllArgsConstructor @NoArgsConstructor
    public static class Sboard2RequestDto {
        @NotBlank
        private String btitle;
        @NotBlank
        private String bcontent;
        @NotBlank
        private String bpass;      // 게시글 비밀번호 (수정/삭제시 검증)
        private String bfile;
    }

    // 응답 Dto
    @Getter @Setter @NoArgsConstructor
    public static class Sboard2ResponseDto {
        private Long   id;
        private String btitle;
        private String bcontent;
        private String bfile;
        private Integer bhit;
        private String bip;
        private LocalDateTime createdAt;
        private String userNickname;   // ManyToOne user 로부터 파생

        public static Sboard2ResponseDto from(Sboard2 board) {
            Sboard2ResponseDto dto = new Sboard2ResponseDto();
            dto.setId(board.getId());
            dto.setBtitle(board.getBtitle());
            dto.setBcontent(board.getBcontent());
            dto.setBfile(board.getBfile());
            dto.setBhit(board.getBhit());
            dto.setBip(board.getBip());
            dto.setCreatedAt(board.getCreatedAt());
            if (board.getUser() != null) {
                dto.setUserNickname(board.getUser().getNickname());
            }
            return dto;
        }

        public Sboard2ResponseDto(Sboard2 board) {
            this.id = board.getId();
            this.btitle = board.getBtitle();
            this.bcontent = board.getBcontent();
            this.bfile = board.getBfile();
            this.bhit = board.getBhit();
            this.bip = board.getBip();
            this.createdAt = board.getCreatedAt();
            if (board.getUser() != null) {
                this.userNickname = board.getUser().getNickname();
            }
        }
    }
}
