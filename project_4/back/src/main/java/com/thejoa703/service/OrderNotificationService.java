package com.thejoa703.service;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.thejoa703.api.CoolSmsService;
import com.thejoa703.entity.OrderItem;
import com.thejoa703.entity.Orders;
import com.thejoa703.event.OrderPaidEvent;
import com.thejoa703.repository.OrderItemRepository;
import com.thejoa703.repository.OrdersRepository;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 결제완료 알림 - 이메일(주문 영수증) + 문자(SMS)
 *
 * - 결제 트랜잭션이 "커밋된 뒤에만" 실행 (AFTER_COMMIT) → 결제가 롤백되면 알림도 안 나감
 * - @Async 로 별도 스레드 실행 → 메일/문자 서버가 느리거나 실패해도 결제 응답에는 영향 없음
 * - 메일 설정(GMAIL_USERNAME) 또는 CoolSMS 키가 없으면 해당 알림만 건너뜀
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderNotificationService {

	private final OrdersRepository ordersRepository;
	private final OrderItemRepository orderItemRepository;
	private final JavaMailSender mailSender;
	private final CoolSmsService coolSmsService;

	@Value("${spring.mail.username:}")
	private String mailFrom;

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public void onOrderPaid(OrderPaidEvent event) {
		Orders order = ordersRepository.findById(event.orderId()).orElse(null);
		if (order == null) {
			return;
		}
		List<OrderItem> items = orderItemRepository.findByOrder_Id(order.getId());
		sendReceiptMail(order, items);
		sendPaidSms(order, items);
	}

	private void sendReceiptMail(Orders order, List<OrderItem> items) {
		String to = order.getUser().getEmail();
		if (mailFrom == null || mailFrom.isBlank() || to == null || to.isBlank()) {
			log.info("[MAIL] 메일 설정이 없거나 받는 주소가 없어 주문 영수증 메일을 건너뜁니다. orderId={}", order.getId());
			return;
		}
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
			helper.setFrom(mailFrom);
			helper.setTo(to);
			helper.setSubject("[BookStore] 주문번호 " + order.getId() + " 결제가 완료되었습니다");
			helper.setText(buildReceiptHtml(order, items), true);
			mailSender.send(message);
			log.info("[MAIL] 주문 영수증 발송 완료 orderId={}", order.getId());
		} catch (Exception e) {
			log.warn("[MAIL] 주문 영수증 발송 실패 orderId={} : {}", order.getId(), e.getMessage());
		}
	}

	private void sendPaidSms(Orders order, List<OrderItem> items) {
		if (order.getReceiverPhone() == null) {
			return;
		}
		String first = items.isEmpty() ? "도서" : items.get(0).getBookTitleSnapshot();
		if (first.length() > 14) {
			first = first.substring(0, 14) + "…";
		}
		String name = items.size() > 1 ? first + " 외 " + (items.size() - 1) + "건" : first;
		String text = "[BookStore] 주문번호 " + order.getId() + " 결제완료\n" + name + " / " + won(order.getTotalAmount());
		coolSmsService.send(order.getReceiverPhone(), text);
	}

	/** boot1 ApiEmail 의 HTML 메일 양식을 주문 영수증에 맞게 정리 */
	private String buildReceiptHtml(Orders order, List<OrderItem> items) {
		StringBuilder rows = new StringBuilder();
		for (OrderItem item : items) {
			rows.append("<tr><td style='padding:8px;border-bottom:1px solid #eee'>")
					.append(escape(item.getBookTitleSnapshot()))
					.append("</td><td style='padding:8px;border-bottom:1px solid #eee;text-align:center'>")
					.append(item.getQuantity())
					.append("</td><td style='padding:8px;border-bottom:1px solid #eee;text-align:right'>")
					.append(won(item.getPrice() * item.getQuantity()))
					.append("</td></tr>");
		}
		String address = order.getAddress() == null ? "-"
				: "(" + escape(order.getZipcode()) + ") " + escape(order.getAddress()) + " "
						+ escape(order.getAddressDetail() == null ? "" : order.getAddressDetail());
		return "<div style='max-width:600px;margin:auto;border:1px solid #e0e0e0;border-radius:8px;padding:28px;"
				+ "font-family:Segoe UI,Apple SD Gothic Neo,sans-serif;color:#333'>"
				+ "<h2 style='color:#1565c0;border-bottom:1px solid #ddd;padding-bottom:10px'>결제가 완료되었습니다</h2>"
				+ "<p>" + escape(order.getUser().getNickname()) + "님, BookStore 를 이용해 주셔서 감사합니다.</p>"
				+ "<p style='color:#666'>주문번호 <b>" + order.getId() + "</b></p>"
				+ "<table style='width:100%;border-collapse:collapse;font-size:14px'>"
				+ "<tr style='background:#f5f7fa'><th style='padding:8px;text-align:left'>도서</th>"
				+ "<th style='padding:8px'>수량</th><th style='padding:8px;text-align:right'>금액</th></tr>"
				+ rows
				+ "</table>"
				+ "<p style='text-align:right;font-size:16px'>총 결제금액 <b style='color:#1565c0'>" + won(order.getTotalAmount()) + "</b></p>"
				+ "<p style='font-size:14px'><b>받는 분</b> " + escape(order.getReceiverName() == null ? "-" : order.getReceiverName())
				+ "<br><b>배송지</b> " + address + "</p>"
				+ "<hr style='border:none;border-top:1px solid #eee;margin:28px 0'>"
				+ "<p style='font-size:12px;color:#888;text-align:center'>이 메일은 결제 완료 시 자동 발송됩니다. "
				+ "문의는 앱의 상담 챗봇을 이용해 주세요.</p></div>";
	}

	private String won(Integer amount) {
		return NumberFormat.getNumberInstance(Locale.KOREA).format(amount == null ? 0 : amount) + "원";
	}

	private String escape(String text) {
		if (text == null) {
			return "";
		}
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
}
