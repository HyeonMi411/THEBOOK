package com.thejoa703.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.thejoa703.entity.Orders;
import com.thejoa703.repository.OrdersRepository;
import com.thejoa703.service.PaymentService;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;

/**
 * 카카오페이 결제 후 돌아오는 "콜백 페이지" (모바일 앱용)
 *
 * project_3(웹)은 결제 후 Next.js 페이지(/payment/complete)가 pg_token 을 받아 승인 API 를 호출했지만,
 * 모바일 앱은 결제창을 외부 브라우저로 열기 때문에 그 페이지를 받아줄 화면이 없다.
 * 그대로 두면 결제창에서 결제를 마쳐도 서버 승인이 호출되지 않아 주문이 PENDING 으로 남고 재고도 차감되지 않는다.
 * 그래서 project_4 백엔드가 이 콜백 페이지를 직접 제공한다.
 *
 * 설정: application-oauth.yml 의 app.frontend-base-url (환경변수 FRONTEND_BASE_URL) 을
 *       project_4 백엔드의 외부 주소(예: https://bookproject4.duckdns.org)로 지정하면
 *       카카오페이가 approval/cancel/fail URL 로 이 컨트롤러(/payment/**)를 호출한다.
 *
 * 보안 메모
 * - 승인(/complete)은 카카오가 검증하는 일회용 pg_token 이 있어야 성공하므로 로그인 토큰 없이 호출돼도 안전하다.
 * - 취소/실패(/cancel, /fail)는 누구나 주문번호만 바꿔 호출할 수 있어서, 상태를 바꾸지 않고 안내 문구만 보여준다.
 *   (결제 전 주문은 PENDING 으로 남을 뿐 재고 차감은 승인 시점에만 일어나므로 데이터 정합성에는 영향이 없다.)
 */
@Hidden   // swagger 문서에서 제외 (사람이 브라우저로 여는 페이지)
@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentCallbackController {

	private final PaymentService paymentService;
	private final OrdersRepository ordersRepository;

	@org.springframework.beans.factory.annotation.Value("${app.oauth2.app-scheme:bookstore4}")
	private String appScheme;   // 결제 후 "앱으로 돌아가기" 딥링크

	@GetMapping(value = "/complete", produces = "text/html;charset=UTF-8")
	@ResponseBody
	public String complete(
			@RequestParam("orderId") Long orderId,
			@RequestParam("pg_token") String pgToken
	) {
		try {
			Orders order = ordersRepository.findById(orderId)
					.orElseThrow(() -> new IllegalStateException("존재하지 않는 주문입니다."));
			paymentService.approve(order.getUser().getId(), orderId, pgToken);
			return page("결제가 완료되었습니다",
					"주문번호 " + orderId + " 결제가 정상적으로 승인되었습니다. "
							+ "앱의 '마이 → 주문내역'에서 확인할 수 있고, 결제완료 메일·문자가 발송됩니다.",
					"complete", orderId);
		} catch (Exception e) {
			return page("결제 승인에 실패했습니다",
					"사유: " + escape(e.getMessage()) + "<br>앱으로 돌아가 다시 시도해 주세요.", "fail", orderId);
		}
	}

	@GetMapping(value = "/cancel", produces = "text/html;charset=UTF-8")
	@ResponseBody
	public String cancel(@RequestParam("orderId") Long orderId) {
		return page("결제가 취소되었습니다",
				"주문번호 " + orderId + " 결제를 진행하지 않았습니다. 앱으로 돌아가 주세요.", "cancel", orderId);
	}

	@GetMapping(value = "/fail", produces = "text/html;charset=UTF-8")
	@ResponseBody
	public String fail(@RequestParam("orderId") Long orderId) {
		return page("결제에 실패했습니다",
				"주문번호 " + orderId + " 결제가 정상적으로 진행되지 않았습니다. 앱으로 돌아가 다시 시도해 주세요.", "fail", orderId);
	}

	/** 앱으로 돌아가라는 안내만 있는 아주 작은 HTML 페이지 */
	private String page(String title, String body, String result, Long orderId) {
		String deepLink = appScheme + "://payment/" + result + "?orderId=" + orderId;
		return "<!DOCTYPE html><html lang=\"ko\"><head><meta charset=\"UTF-8\">"
				+ "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
				+ "<title>" + escape(title) + "</title>"
				+ "<style>body{font-family:sans-serif;padding:32px 20px;text-align:center;color:#222}"
				+ "h1{font-size:22px}p{line-height:1.7;color:#555}"
				+ "a{display:inline-block;margin-top:16px;padding:12px 22px;background:#1565c0;color:#fff;border-radius:8px;text-decoration:none}"
				+ "</style></head><body>"
				+ "<h1>" + escape(title) + "</h1><p>" + body + "</p>"
				+ "<a href=\"" + escape(deepLink) + "\">앱으로 돌아가기</a></body></html>";
	}

	/** 사용자/예외 메시지가 그대로 HTML 에 들어가지 않도록 최소한의 이스케이프 */
	private String escape(String text) {
		if (text == null) {
			return "";
		}
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
}
