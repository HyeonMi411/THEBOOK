package com.thejoa703.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thejoa703.api.ChatbotAiService;
import com.thejoa703.dto.ChatbotDto.Answer;
import com.thejoa703.dto.ChatbotDto.AskRequest;
import com.thejoa703.dto.ChatbotDto.Category;
import com.thejoa703.dto.ChatbotDto.Suggestion;
import com.thejoa703.entity.ChatbotLog;
import com.thejoa703.repository.ChatbotLogRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * 상담 챗봇 (PDF RAG 챗봇 대신, 실제 쇼핑몰·은행 앱에서 가장 많이 쓰는 "하이브리드 상담 챗봇")
 *
 *  1) 버튼형   : GET /api/chatbot/menu 로 카테고리/질문 버튼을 내려주고, 버튼을 누르면 faqId 로 즉시 답변
 *  2) 검색형   : 직접 입력한 문장에서 키워드를 찾아 가장 알맞은 FAQ 를 답변 (외부 API 불필요)
 *  3) AI 보조  : FAQ 로 못 찾은 질문만 ChatGPT 에게 FAQ 를 근거자료로 주고 짧게 답하게 함 (키가 있을 때만)
 *  4) 상담원   : 그래도 답을 못 하면 handoff=true 로 내려, 앱이 "이메일로 문의하기" 버튼을 보여준다.
 *  + 모든 질문을 CHATBOT_LOGS 에 남겨 Django 대시보드에서 "많이 묻는 질문 / 답 못한 질문" 을 분석
 *
 * 답변 내용은 이 앱(project_4)에 실제로 있는 기능만 근거로 작성했다.
 */
@Slf4j
@Service
public class ChatbotService {

	@Value("${chatbot.contact-email:jewelkhm@naver.com}")
	private String contactEmail;

	private final ChatbotAiService chatbotAiService;
	private final ChatbotLogRepository chatbotLogRepository;

	public ChatbotService(ChatbotAiService chatbotAiService, ChatbotLogRepository chatbotLogRepository) {
		this.chatbotAiService = chatbotAiService;
		this.chatbotLogRepository = chatbotLogRepository;
	}

	/** FAQ 한 항목 */
	private static final class Faq {
		private final String id;
		private final String category;
		private final String question;
		private final String answer;
		private final List<String> keywords;   // 공백/기호 제거 + 소문자 기준
		private final boolean handoff;         // 답변과 함께 상담원 문의 버튼도 보여줄지
		private final boolean hidden;          // 메뉴 버튼에는 노출하지 않는 항목(인사말 등)

		private Faq(String id, String category, String question, String answer,
				List<String> keywords, boolean handoff, boolean hidden) {
			this.id = id;
			this.category = category;
			this.question = question;
			this.answer = answer;
			this.keywords = keywords;
			this.handoff = handoff;
			this.hidden = hidden;
		}
	}

	private static final Map<String, String> CATEGORIES = new LinkedHashMap<>();
	static {
		CATEGORIES.put("account", "회원·로그인");
		CATEGORIES.put("shop", "도서·주문·결제");
		CATEGORIES.put("community", "커뮤니티·공지");
		CATEGORIES.put("service", "날씨·매장·소식");
		CATEGORIES.put("etc", "기타");
	}

	private final List<Faq> faqs = buildFaqs();
	private final String knowledge = buildKnowledge(faqs);

	private static List<Faq> buildFaqs() {
		List<Faq> list = new ArrayList<>();

		// ── 회원·로그인 ──
		list.add(new Faq("acc-signup", "account", "회원가입은 어떻게 하나요?",
				"로그인 화면의 '회원가입'에서 이메일로 받은 인증번호(5분 유효)를 확인한 뒤 가입합니다. "
						+ "이메일과 닉네임은 중복 확인을 거치고, 인증을 마친 뒤 30분 안에 가입을 완료해 주세요.",
				List.of("회원가입", "가입", "계정만들", "signup", "인증번호", "인증메일", "인증"), false, false));
		list.add(new Faq("acc-login", "account", "로그인이 안 되거나 자꾸 풀려요",
				"먼저 이메일과 비밀번호를 다시 확인해 주세요. 로그인 후 발급되는 접속 토큰은 15분 동안 유효하고, "
						+ "만료되면 앱이 자동으로 재발급합니다(재발급용 토큰은 14일 유효). 앱을 다시 켜도 로그인이 유지됩니다. "
						+ "그래도 풀린다면 로그아웃 후 다시 로그인해 주세요.",
				List.of("로그인", "로그아웃", "풀려", "토큰", "세션", "재로그인", "비밀번호"), false, false));
		list.add(new Faq("acc-social", "account", "카카오·네이버·구글로 로그인할 수 있나요?",
				"네. 로그인 화면 아래의 카카오·네이버·구글 버튼을 누르면 브라우저에서 로그인한 뒤 앱으로 자동으로 돌아옵니다. "
						+ "처음 소셜 로그인하는 경우 닉네임 확인과 이메일 인증을 한 번 거칩니다. "
						+ "같은 이메일로 이미 다른 방법으로 가입했다면 원래 가입한 방법으로 로그인해 주세요.",
				List.of("소셜", "구글로", "네이버로", "카카오로", "구글로그인", "네이버로그인", "카카오로그인", "간편로그인", "oauth", "google", "naver"), false, false));
		list.add(new Faq("acc-mypage", "account", "닉네임·프로필 사진 변경이나 회원 탈퇴는 어떻게 하나요?",
				"하단 '마이' 탭에서 프로필 사진과 닉네임을 바꿀 수 있고(닉네임은 중복 불가), 맨 아래 '회원 탈퇴'로 탈퇴할 수 있습니다. "
						+ "탈퇴하면 같은 계정으로 다시 로그인할 수 없고, 주문·결제 이력은 기록 보존을 위해 남습니다.",
				List.of("닉네임", "별명", "프로필", "프사", "탈퇴", "회원탈퇴", "계정삭제", "내정보", "마이페이지"), false, false));

		// ── 도서·주문·결제 ──
		list.add(new Faq("shop-browse", "shop", "도서는 어디서 보고 검색하나요?",
				"하단 '도서' 탭에서 카테고리별 목록과 제목 검색을 이용할 수 있고, 도서를 누르면 상세 정보와 재고를 볼 수 있습니다. "
						+ "'홈' 탭에서는 판매량 기준 베스트셀러 TOP 10도 볼 수 있어요.",
				List.of("도서", "책", "목록", "상세", "제목검색", "베스트셀러", "인기"), false, false));
		list.add(new Faq("shop-external", "shop", "찾는 책이 쇼핑몰에 없어요",
				"도서 탭 상단의 '외부 도서 검색'에서 카카오·네이버·국립중앙도서관 검색 결과를 볼 수 있어요. "
						+ "쇼핑몰 판매 등록은 관리자가 하므로, 원하는 책이 없으면 아래 상담원 문의로 제목을 남겨 주세요.",
				List.of("외부검색", "외부도서", "카카오도서", "네이버도서", "국립중앙도서관", "국립중앙", "도서관", "없는책", "책이없",
						"안보여", "입고"), true, false));
		list.add(new Faq("shop-ocr", "shop", "책 표지 사진으로 검색할 수 있나요?",
				"네. 도서 탭 상단의 카메라 버튼을 누르고 표지를 찍거나 사진을 고르면, 글자를 인식해서(CLOVA OCR) "
						+ "제목으로 바로 검색합니다. 로그인이 필요하고 JPG·PNG 5MB 이하 사진만 가능합니다.",
				List.of("표지사진", "사진으로", "사진검색", "카메라", "ocr", "찍어서", "촬영"), false, false));
		list.add(new Faq("shop-cart", "shop", "장바구니는 어떻게 사용하나요?",
				"도서 상세에서 장바구니에 담고, 하단 '장바구니' 탭에서 수량 변경과 삭제를 할 수 있습니다. "
						+ "'주문하기'를 누르면 배송지 입력 화면으로 넘어갑니다.",
				List.of("장바구니", "담기", "담았", "수량"), false, false));
		list.add(new Faq("shop-address", "shop", "배송지 주소는 어떻게 입력하나요?",
				"주문서 화면에서 '우편번호 검색'을 누르면 주소 검색창이 열립니다. 도로명·지번·건물명으로 검색해 고른 뒤 "
						+ "상세주소를 입력하세요. 받는 분 이름과 휴대폰 번호도 함께 입력합니다.",
				List.of("주소", "우편번호", "배송지", "주소입력", "받는분", "받는사람"), false, false));
		list.add(new Faq("shop-pay", "shop", "결제는 어떻게 하나요?",
				"카카오페이로 결제합니다. '주문서 작성 → 카카오페이 결제창(브라우저) → 결제 승인' 순서로 진행되며, "
						+ "결제가 승인되는 시점에 재고가 차감됩니다. 결제창을 닫거나 취소하면 재고는 차감되지 않습니다.",
				List.of("결제", "결재", "카카오페이", "승인", "페이"), false, false));
		list.add(new Faq("shop-notify", "shop", "결제 완료 알림(문자·메일)이 오나요?",
				"결제가 승인되면 가입한 이메일로 주문 영수증 메일이, 주문서에 적은 휴대폰 번호로 결제완료 문자가 발송됩니다. "
						+ "알림이 오지 않아도 주문은 정상 처리되며 '마이 → 주문내역'에서 확인할 수 있어요.",
				List.of("문자", "알림", "메일", "영수증", "sms", "메세지", "메시지"), false, false));
		list.add(new Faq("shop-cancel", "shop", "결제를 취소하거나 환불받고 싶어요",
				"결제창에서 취소하면 주문이 취소 처리되고 재고도 차감되지 않습니다. "
						+ "이미 승인 완료된 결제의 취소·환불은 챗봇에서 처리할 수 없어요. 아래 상담원 문의로 안내해 드릴게요.",
				List.of("결제취소", "취소", "환불", "반품", "환급"), true, false));
		list.add(new Faq("shop-orders", "shop", "주문 내역은 어디서 확인하나요?",
				"하단 '마이' 탭의 '주문내역'에서 결제 상태와 배송지까지 확인할 수 있습니다. 본인 주문만 조회됩니다.",
				List.of("주문내역", "주문", "내역", "구매내역", "샀던"), false, false));
		list.add(new Faq("shop-delivery", "shop", "배송은 언제 되나요?",
				"현재 앱에는 배송 조회 기능이 없습니다. 배송 관련 문의는 상담원에게 남겨 주세요. "
						+ "가까운 오프라인 매장 위치는 홈 탭의 '매장 지도'에서 볼 수 있어요.",
				List.of("배송", "택배", "송장", "도착"), true, false));
		list.add(new Faq("shop-stock", "shop", "재고가 부족하다고 나와요",
				"여러 사용자가 같은 도서를 동시에 결제할 수 있어서, 결제 승인 시점에 재고를 한 번 더 확인합니다. "
						+ "그 사이 재고가 소진되면 결제가 승인되지 않을 수 있어요. 수량을 줄이거나 잠시 후 다시 시도해 주세요.",
				List.of("재고", "품절", "수량부족", "부족"), false, false));

		// ── 커뮤니티·공지 ──
		list.add(new Faq("board-write", "community", "게시글은 어떻게 작성하나요?",
				"하단 '커뮤니티' 탭 오른쪽 아래의 글쓰기 버튼으로 작성합니다. 로그인이 필요하고, "
						+ "이미지는 최대 5장(장당 5MB 이하, jpg·png·gif·webp)까지 올릴 수 있습니다.",
				List.of("게시글", "게시판", "커뮤니티", "글쓰기", "글작성", "작성", "이미지", "업로드"), false, false));
		list.add(new Faq("board-tag-like", "community", "해시태그·좋아요·댓글은 어떻게 쓰나요?",
				"글을 쓸 때 #태그를 쉼표나 공백으로 구분해 입력하면 됩니다(최대 10개). 태그를 누르면 같은 태그가 붙은 글만 모아 볼 수 있어요. "
						+ "좋아요(♡)와 댓글은 로그인 후 목록/상세에서 사용할 수 있습니다.",
				List.of("해시태그", "태그", "좋아요", "댓글", "하트"), false, false));
		list.add(new Faq("board-social", "community", "리트윗과 팔로우는 어떻게 하나요?",
				"글 아래의 리트윗 버튼을 누르면 내 프로필과 나를 팔로우한 사람의 피드에 그 글이 공유됩니다(본인 글은 불가). "
						+ "작성자 닉네임을 누르면 프로필로 이동해 팔로우할 수 있고, 커뮤니티 상단의 '팔로잉' 탭에서 팔로우한 사람들의 글만 볼 수 있어요.",
				List.of("리트윗", "공유", "팔로우", "팔로잉", "팔로워", "구독", "언팔", "프로필보기"), false, false));
		list.add(new Faq("board-edit", "community", "내 글이나 댓글을 수정·삭제하고 싶어요",
				"본인이 쓴 글과 댓글만 수정·삭제할 수 있습니다. 게시글 상세 화면 오른쪽 위 메뉴를 이용해 주세요.",
				List.of("수정", "삭제", "지우", "지워", "고치"), false, false));
		list.add(new Faq("board-notice", "community", "공지사항은 어디서 보나요?",
				"'홈' 탭의 공지사항 영역에서 최신 공지를 보고, '전체보기'로 전체 목록을 볼 수 있습니다. 공지 작성은 관리자만 할 수 있습니다.",
				List.of("공지", "공지사항", "안내문"), false, false));

		// ── 날씨·매장·소식 ──
		list.add(new Faq("svc-weather", "service", "홈 화면의 날씨는 어디 기준인가요?",
				"기상청 초단기실황 자료로, 위치 권한을 허용하면 현재 위치, 허용하지 않으면 서울 기준 날씨를 보여줍니다. "
						+ "매시 정각 관측값이 약 40분 뒤 반영되고, 날씨에 어울리는 독서 한마디도 함께 보여드려요.",
				List.of("날씨", "기온", "온도", "기상청", "비와", "눈와", "습도"), false, false));
		list.add(new Faq("svc-store", "service", "오프라인 매장은 어디에 있나요?",
				"홈 탭의 '매장 지도'에서 매장 위치와 영업시간을 지도로 볼 수 있어요. 매장을 누르면 길찾기(카카오맵)로 연결되고, "
						+ "내 위치 권한을 허용하면 가까운 매장부터 보여줍니다.",
				List.of("매장", "지점", "오프라인", "지도", "위치", "길찾기", "영업시간"), false, false));
		list.add(new Faq("svc-news", "service", "오늘의 책 소식은 무엇인가요?",
				"홈 탭의 '오늘의 책 소식'은 신간·도서 관련 뉴스를 30분마다 자동으로 모아 보여줍니다. "
						+ "기사를 누르면 원문 페이지로 이동합니다.",
				List.of("뉴스", "소식", "신간", "기사"), false, false));

		// ── 기타 ──
		list.add(new Faq("etc-about", "etc", "이 앱은 어떤 서비스인가요?",
				"온라인 도서 쇼핑몰 프로젝트(BookStore v4)의 Flutter 모바일 앱입니다. 도서 조회·외부 도서검색·표지 사진 검색, "
						+ "장바구니와 카카오페이 결제(문자·메일 알림), SNS 커뮤니티(해시태그·좋아요·댓글·리트윗·팔로우), "
						+ "날씨·매장 지도·도서 뉴스를 제공합니다.",
				List.of("서비스", "소개", "앱소개", "뭐하는", "어떤앱", "북스토어", "bookstore"), false, false));
		list.add(new Faq("etc-agent", "etc", "상담원과 연결하고 싶어요",
				"챗봇으로 해결되지 않는 내용은 이메일로 문의를 남겨 주세요. 확인 후 답변드리겠습니다.",
				List.of("상담원", "상담", "문의", "이메일", "연락", "담당자", "사람"), true, false));
		list.add(new Faq("etc-hello", "etc", "인사",
				"안녕하세요! BookStore 상담 챗봇입니다. 궁금한 내용을 입력하시거나 아래 자주 묻는 질문을 골라 주세요.",
				List.of("안녕", "hello", "헬로", "반가"), false, true));

		return list;
	}

	/** ChatGPT 보조 답변에 넣을 근거자료 (FAQ 질문/답변 전체) */
	private static String buildKnowledge(List<Faq> faqs) {
		StringBuilder sb = new StringBuilder();
		for (Faq f : faqs) {
			if (!f.hidden) {
				sb.append("Q. ").append(f.question).append("\nA. ").append(f.answer).append("\n");
			}
		}
		return sb.toString();
	}

	// ───────────────────────── 공개 API ─────────────────────────

	/** 버튼형 메뉴 - 카테고리별 질문 버튼 목록 */
	public List<Category> getMenu() {
		List<Category> menu = new ArrayList<>();
		for (Map.Entry<String, String> c : CATEGORIES.entrySet()) {
			List<Suggestion> items = faqs.stream()
					.filter(f -> f.category.equals(c.getKey()) && !f.hidden)
					.map(f -> new Suggestion(f.id, f.question))
					.collect(Collectors.toList());
			if (!items.isEmpty()) {
				menu.add(new Category(c.getKey(), c.getValue(), items));
			}
		}
		return menu;
	}

	/** 기존 호출부 호환 (AI 호출 제한 키 없음) */
	public Answer ask(AskRequest request) {
		return ask(request, null);
	}

	/**
	 * 질문 처리 - faqId(버튼)가 있으면 그 답변, 없으면 message(직접 입력)를 FAQ 에서 검색하고,
	 * 그래도 없으면 AI 보조 답변 → 상담원 안내 순서로 처리한다.
	 * @param clientKey AI 호출 횟수 제한용 식별값 (컨트롤러가 요청 IP 를 넘김, 저장하지 않음)
	 */
	@Transactional
	public Answer ask(AskRequest request, String clientKey) {
		String faqId = request.getFaqId();
		if (faqId != null && !faqId.isBlank()) {
			for (Faq f : faqs) {
				if (f.id.equals(faqId)) {
					saveLog(f.question, f.id, "menu");
					return toAnswer(f);
				}
			}
			return notFound();
		}

		String message = request.getMessage() == null ? "" : request.getMessage().trim();
		if (message.isEmpty()) {
			return welcome();
		}

		Faq best = findBest(message);
		if (best != null) {
			saveLog(message, best.id, "faq");
			return toAnswer(best);
		}

		String ai = chatbotAiService.answer(message, knowledge, clientKey);
		if (ai != null) {
			saveLog(message, null, "ai");
			return new Answer(ai, popularSuggestions(), true, contactEmail, "ai");
		}
		saveLog(message, null, "none");
		return notFound();
	}

	// ───────────────────────── 내부 로직 ─────────────────────────

	private Faq findBest(String message) {
		String normalized = normalize(message);
		Faq best = null;
		int bestScore = 0;
		for (Faq f : faqs) {
			int s = score(normalized, f);
			if (s > bestScore) {
				bestScore = s;
				best = f;
			}
		}
		return best;
	}

	/** 로그 저장 실패가 답변을 막지 않도록 예외는 삼킨다 */
	private void saveLog(String question, String faqId, String source) {
		try {
			chatbotLogRepository.save(new ChatbotLog(question, faqId, source));
		} catch (Exception e) {
			log.warn("[챗봇] 로그 저장 실패: {}", e.getMessage());
		}
	}

	private Answer toAnswer(Faq faq) {
		List<Suggestion> related = faqs.stream()
				.filter(f -> f.category.equals(faq.category) && !f.hidden && !f.id.equals(faq.id))
				.limit(3)
				.map(f -> new Suggestion(f.id, f.question))
				.collect(Collectors.toList());
		return new Answer(faq.answer, related, faq.handoff, contactEmail, "faq");
	}

	private Answer welcome() {
		return new Answer("궁금한 내용을 입력하시거나, 아래 자주 묻는 질문을 골라 주세요.",
				popularSuggestions(), false, contactEmail, "faq");
	}

	private Answer notFound() {
		return new Answer("죄송해요, 질문을 정확히 이해하지 못했어요. 아래 자주 묻는 질문을 골라 보시거나, "
				+ "상담원(이메일)에게 문의해 주세요.", popularSuggestions(), true, contactEmail, "none");
	}

	private List<Suggestion> popularSuggestions() {
		List<String> ids = List.of("shop-pay", "acc-signup", "board-write", "etc-agent");
		List<Suggestion> result = new ArrayList<>();
		for (String id : ids) {
			for (Faq f : faqs) {
				if (f.id.equals(id)) {
					result.add(new Suggestion(f.id, f.question));
				}
			}
		}
		return result;
	}

	/**
	 * 점수 = (키워드 적중 점수) + (질문 문장과 겹치는 2글자 조각 수).
	 * 키워드가 하나도 적중하지 않으면 0점 - "먹고 싶어요" 처럼 어미만 비슷한 엉뚱한 문장이
	 * 아무 FAQ 에나 끌려가는 오답을 막기 위해 키워드 적중을 필수 조건으로 둔다.
	 */
	private int score(String normalizedMessage, Faq faq) {
		int keywordScore = 0;
		for (String kw : faq.keywords) {
			if (normalizedMessage.contains(kw)) {
				int len = kw.length();   // 긴(구체적인) 키워드일수록 가중치를 더 준다
				keywordScore += len >= 5 ? 6 : (len >= 4 ? 5 : (len >= 3 ? 4 : 3));
			}
		}
		if (keywordScore == 0) {
			return 0;
		}
		Set<String> a = bigrams(normalizedMessage);
		Set<String> b = bigrams(normalize(faq.question));
		a.retainAll(b);
		return keywordScore + a.size();
	}

	/** 소문자로 바꾸고 글자·숫자만 남긴다 (공백·기호 제거) */
	private static String normalize(String text) {
		StringBuilder sb = new StringBuilder();
		for (char ch : text.toLowerCase(Locale.ROOT).toCharArray()) {
			if (Character.isLetterOrDigit(ch)) {
				sb.append(ch);
			}
		}
		return sb.toString();
	}

	private static Set<String> bigrams(String s) {
		Set<String> set = new HashSet<>();
		for (int i = 0; i + 2 <= s.length(); i++) {
			set.add(s.substring(i, i + 2));
		}
		return set;
	}
}
