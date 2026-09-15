package com.thejoa703.llmrag;

import java.io.InputStream;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import lombok.extern.slf4j.Slf4j;

/**
 * 서버 기동 시 src/main/resources/docs/company.pdf 를 미리 읽어서
 * AiService 에 기본 컨텍스트로 저장. 이 파일이 없으면 조용히 건너뛰고,
 * 사용자가 파일을 직접 첨부하지 않은 질문에는 컨텍스트 없이 답변.
 */
@Slf4j
@Configuration
public class RagInitializer {

	@Bean
	public ApplicationRunner initializePdfData(ResourceLoader resourceLoader, AiService aiService) {
		return args -> {
			var pdfResource = resourceLoader.getResource("classpath:docs/company.pdf");

			if (pdfResource.exists()) {
				try (InputStream is = pdfResource.getInputStream()) {
					String context = aiService.extractTextFromPdf(is);
					aiService.setCompanyContext(context);
					log.info("[RAG] 기본 PDF 로드 완료! (글자 수: {}자)", context.length());
				} catch (Exception e) {
					log.warn("[RAG] 기본 PDF 파싱 중 오류 발생: {}", e.getMessage());
				}
			} else {
				log.info("[RAG] 'src/main/resources/docs/company.pdf' 파일이 없습니다. "
						+ "파일을 넣어두면 서버 기동 시 자동으로 로드됩니다(선택사항).");
			}
		};
	}
}
