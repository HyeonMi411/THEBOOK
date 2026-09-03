package com.thejoa703.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 업로드 저장 서비스
 * ------------------------------------------------------------------
 * 원래는 Spring 전역 설정(application.yml 의 spring.servlet.multipart.max-file-size)
 * 으로 용량만 제한하고, 확장자·MIME 타입 검증이나 실제 파일 내용 확인이 전혀 없었습니다.
 * 이 상태로는 이론적으로 실행파일도 확장자만 바꿔서 업로드할 수 있었기 때문에 검증을
 * 추가했습니다. 용도가 서로 다른 두 가지 업로드를 분리했습니다.
 *
 * - uploadImage() : 프로필 사진(UserService), 도서 표지(BookService) 전용. 이미지
 *   파일만 허용하며, Content-Type 뿐 아니라 ImageIO 로 실제 디코딩까지 확인합니다
 *   (Content-Type 헤더는 클라이언트가 임의로 보낼 수 있는 값이라 그것만으로는
 *   신뢰할 수 없습니다 - 확장자만 .jpg 로 바꾼 실행파일 등을 이 단계에서 걸러냅니다).
 * - uploadDocument() : 공지사항 첨부파일(Sboard2Service) 전용. 이미지뿐 아니라 PDF
 *   등 문서 첨부가 정당한 용도라, 실행 가능한 위험 확장자만 명시적으로 차단하는
 *   블랙리스트 방식으로 검증합니다.
 *
 * 두 메서드 모두 저장 파일명에 원본 파일명을 전혀 사용하지 않고 UUID + 검증된
 * 확장자로만 새로 생성해서, 파일명에 "../" 등이 포함되어 uploads 폴더 바깥으로
 * 저장되는(Path Traversal) 문제 자체를 원천 차단합니다.
 * ------------------------------------------------------------------
 */
@Service
public class FileStorageService {

	private final Path root = Paths.get("uploads"); // 프로젝트 실행위치를 기준으로 uploads폴더 생성

	// 이미지 업로드(프로필 사진, 도서 표지) 전용 화이트리스트
	private static final List<String> IMAGE_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif", "webp");
	private static final List<String> IMAGE_CONTENT_TYPES =
			List.of("image/jpeg", "image/png", "image/gif", "image/webp");
	private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024; // 5MB

	// 공지사항 첨부파일 전용 - 이미지 + 문서. 실행 가능한 위험 확장자만 명시적으로 차단.
	private static final List<String> DOCUMENT_EXTENSIONS =
			List.of("jpg", "jpeg", "png", "gif", "webp", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "hwp", "txt", "zip");
	private static final long MAX_DOCUMENT_SIZE_BYTES = 10L * 1024 * 1024; // 10MB

	/** 프로필 사진 / 도서 표지 - 반드시 실제 이미지 파일이어야 합니다. */
	public String uploadImage(MultipartFile file) {
		validateCommon(file, MAX_IMAGE_SIZE_BYTES, "5MB");
		String extension = extractExtension(file.getOriginalFilename());
		if (!IMAGE_EXTENSIONS.contains(extension)) {
			throw new IllegalArgumentException(
					"허용되지 않는 파일 확장자입니다. (jpg, jpeg, png, gif, webp 만 업로드 가능)"
			);
		}
		String contentType = file.getContentType();
		if (contentType == null || !IMAGE_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
			throw new IllegalArgumentException("허용되지 않는 파일 형식입니다. 이미지 파일만 업로드 가능합니다.");
		}
		// Content-Type 은 위조될 수 있으므로, 실제로 이미지로 디코딩되는지 매직바이트
		// 기준으로 한 번 더 검증합니다.
		try {
			if (ImageIO.read(file.getInputStream()) == null) {
				throw new IllegalArgumentException("올바른 이미지 파일이 아닙니다.");
			}
		} catch (IOException e) {
			throw new IllegalArgumentException("이미지 파일을 읽을 수 없습니다.");
		}
		return store(file, extension);
	}

	/** 공지사항 첨부파일 - 이미지 또는 문서(PDF 등)를 허용합니다. */
	public String uploadDocument(MultipartFile file) {
		validateCommon(file, MAX_DOCUMENT_SIZE_BYTES, "10MB");
		String extension = extractExtension(file.getOriginalFilename());
		if (!DOCUMENT_EXTENSIONS.contains(extension)) {
			throw new IllegalArgumentException(
					"허용되지 않는 파일 확장자입니다. (이미지, pdf, doc(x), xls(x), ppt(x), hwp, txt, zip 만 업로드 가능)"
			);
		}
		return store(file, extension);
	}

	private String store(MultipartFile file, String extension) {
		try {
			if (!Files.exists(root)) { // 디렉토리 생성확인
				Files.createDirectories(root); // 중간경로까지 모두생성
			}
			// 원본 파일명은 저장에 전혀 사용하지 않습니다 - UUID + 검증된 확장자만으로
			// 새 파일명을 만들어서, 파일명을 통한 Path Traversal 자체가 불가능합니다.
			String filename = UUID.randomUUID() + "." + extension;
			Path target = root.resolve(filename); // uploads디렉토리안에서 filename붙여서 최종경로
			Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING); // 파일올리기
			return "uploads/" + filename; // uploads/파일
		} catch (IOException e) {
			throw new RuntimeException("파일 업로드 실패", e);
		}
	}

	private void validateCommon(MultipartFile file, long maxSizeBytes, String maxSizeLabel) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("업로드할 파일이 비어 있습니다.");
		}
		if (file.getSize() > maxSizeBytes) {
			throw new IllegalArgumentException("파일 용량은 " + maxSizeLabel + "를 초과할 수 없습니다.");
		}
	}

	private String extractExtension(String originalFilename) {
		if (originalFilename == null || !originalFilename.contains(".")) {
			throw new IllegalArgumentException("파일 확장자를 확인할 수 없습니다.");
		}
		String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
		// 확장자 문자열 자체에도 "../" 같은 경로 조작 문자가 섞여 들어올 수 있으니,
		// 영문자/숫자로만 구성된 짧은 확장자만 허용합니다.
		if (!ext.matches("[a-z0-9]{1,5}")) {
			throw new IllegalArgumentException("파일 확장자를 확인할 수 없습니다.");
		}
		return ext;
	}
}
