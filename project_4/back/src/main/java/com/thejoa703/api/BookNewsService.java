package com.thejoa703.api;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.thejoa703.dto.UtilDto.NewsDto;

import lombok.extern.slf4j.Slf4j;

/**
 * 도서 뉴스 크롤링 (jsoup) - 앱 홈 화면 "오늘의 책 소식"
 *
 * 웹페이지 HTML 을 긁는 대신, 기계가 읽도록 공개된 RSS 피드를 jsoup 의 XML 파서로 수집한다.
 * (사이트 구조가 바뀌어도 잘 안 깨지고, 약관상으로도 안전한 방식)
 * - 서버 시작 직후 1번 + 30분마다 갱신 (@Scheduled - boot1 ApiScheduledTask 와 같은 방식)
 * - 앱 요청 때마다 외부 사이트를 호출하지 않고, 메모리에 저장된 최신 목록만 내려준다.
 */
@Slf4j
@Service
public class BookNewsService {

	private static final int MAX_ITEMS = 15;

	@Value("${book-news.rss-url:}")
	private String rssUrl;

	private volatile List<NewsDto> latest = List.of();
	private volatile String lastUpdated = null;

	@Scheduled(initialDelay = 5_000, fixedDelay = 30 * 60 * 1000)
	public void refresh() {
		if (rssUrl == null || rssUrl.isBlank()) {
			return;
		}
		try {
			Document doc = Jsoup.connect(rssUrl)
					.userAgent("Mozilla/5.0 (BookStore portfolio; RSS reader)")
					.timeout(8_000)
					.ignoreContentType(true)
					.parser(Parser.xmlParser())
					.get();
			List<NewsDto> items = new ArrayList<>();
			for (Element item : doc.select("item")) {
				String title = item.selectFirst("title") != null ? item.selectFirst("title").text() : "";
				String link = item.selectFirst("link") != null ? item.selectFirst("link").text() : "";
				String source = item.selectFirst("source") != null ? item.selectFirst("source").text() : "";
				String pubDate = item.selectFirst("pubDate") != null ? item.selectFirst("pubDate").text() : "";
				if (title.isBlank() || !link.startsWith("http")) {
					continue;
				}
				// 구글 뉴스 제목은 "기사 제목 - 언론사" 형태 → 언론사 부분 정리
				if (!source.isBlank() && title.endsWith(" - " + source)) {
					title = title.substring(0, title.length() - source.length() - 3);
				}
				items.add(new NewsDto(title, link, source, formatDate(pubDate)));
				if (items.size() >= MAX_ITEMS) {
					break;
				}
			}
			if (!items.isEmpty()) {
				latest = List.copyOf(items);
				lastUpdated = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
				log.info("[도서뉴스] {}건 수집", items.size());
			}
		} catch (Exception e) {
			log.warn("[도서뉴스] 수집 실패 (기존 목록 유지): {}", e.getMessage());
		}
	}

	public List<NewsDto> getLatest() {
		return latest;
	}

	public String getLastUpdated() {
		return lastUpdated;
	}

	/** RSS 날짜(RFC 1123) → "2026-09-29 14:30" */
	private String formatDate(String rfc) {
		try {
			ZonedDateTime t = ZonedDateTime.parse(rfc, DateTimeFormatter.RFC_1123_DATE_TIME)
					.withZoneSameInstant(java.time.ZoneId.of("Asia/Seoul"));
			return t.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.KOREA));
		} catch (Exception e) {
			return rfc;
		}
	}
}
