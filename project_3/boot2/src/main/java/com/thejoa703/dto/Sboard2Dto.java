package com.thejoa703.dto;

import java.time.LocalDateTime;

import com.thejoa703.entity.Sboard2;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class Sboard2Dto {

	// 공지글 작성/수정 요청 Dto (공지글 작성/수정/삭제는 관리자만 가능)
	@Getter @Setter @NoArgsConstructor @AllArgsConstructor
	public static class Sboard2RequestDto {
		@NotBlank
		private String btitle;     // 제목

		@NotBlank
		private String bcontent;   // 내용(긴 텍스트)

		// 레거시 호환용 글 비밀번호 컬럼(BPASS, NOT NULL) - 실제 작성/수정/삭제 권한은
		// adminUserId + ROLE_ADMIN 여부로 판단하므로 필수값은 아니며, 미입력시 서비스에서 빈 문자열로 채움
		private String bpass;
		// 첨부파일(bfile)은 MultipartFile 로 별도 전송
	}

	// 공지글 응답 Dto
	@Getter @Setter @NoArgsConstructor
	public static class Sboard2ResponseDto {
		private Long id;
		private String btitle;
		private String bcontent;
		private String bfile;
		private Integer bhit;
		private String bip;
		private LocalDateTime createdAt;

		// ★공지글을 작성한 관리자 정보
		private Long adminId;
		private String adminNickname;
		// ※ bpass(비밀번호)는 보안상 응답에 포함하지 않음

		public static Sboard2ResponseDto fromEntity(Sboard2 board) {
			Sboard2ResponseDto dto = new Sboard2ResponseDto();
			dto.setId(board.getId());
			dto.setBtitle(board.getBtitle());
			dto.setBcontent(board.getBcontent());
			dto.setBfile(board.getBfile());
			dto.setBhit(board.getBhit());
			dto.setBip(board.getBip());
			dto.setCreatedAt(board.getCreatedAt());
			if (board.getUser() != null) {
				dto.setAdminId(board.getUser().getId());
				dto.setAdminNickname(board.getUser().getNickname());
			}
			return dto;
		}
	}
}
