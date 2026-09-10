package com.thejoa703.dto;

import java.time.LocalDateTime;

import com.thejoa703.entity.Sboard2;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class Sboard2Dto {

	// 공지사항 작성/수정 요청 Dto
	@Getter @Setter @AllArgsConstructor @NoArgsConstructor
	public static class Sboard2RequestDto {
		@NotBlank(message = "제목은 필수입니다.")
		private String btitle;

		@NotBlank(message = "내용은 필수입니다.")
		private String bcontent; // CLOB
	}

	// 공지사항 응답 Dto
	@Getter @Setter @NoArgsConstructor
	public static class Sboard2ResponseDto {
		private Long id;
		private String btitle;
		private String bcontent;
		private String bfile;
		private Integer bhit;
		private String bip;
		private LocalDateTime createdAt;
		private String userNickname; // 작성한 관리자 닉네임

		public static Sboard2ResponseDto from(Sboard2 board) {
			Sboard2ResponseDto dto = new Sboard2ResponseDto();
			dto.setId(board.getId());
			dto.setBtitle(board.getBtitle());
			dto.setBcontent(board.getBcontent());
			dto.setBfile(board.getBfile());
			dto.setBhit(board.getBhit());
			dto.setBip(board.getBip());
			dto.setCreatedAt(board.getCreatedAt());
			if (board.getUser() != null) { dto.setUserNickname(board.getUser().getNickname()); }
			return dto;
		}
	}
}
