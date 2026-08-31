package com.thejoa703.api;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import lombok.extern.slf4j.Slf4j;

/**
 * 카카오페이 결제 API(정기가 아닌 "일반결제") 연동 서비스
 * ------------------------------------------------------------------
 * 결제준비(ready) → 사용자가 카카오페이 결제창에서 결제 진행 → 결제승인(approve) 의
 * 3단계 흐름 중, 우리 서버가 담당하는 1),3) 단계를 처리합니다.
 * (문서: https://developers.kakaopay.com/docs/payment/online/single-payment)
 *
 * 2024.01.03 API 개편 이후, 신)API(open-api.kakaopay.com)는 요청 본문을
 *   application/x-www-form-urlencoded(폼) 가 아니라 **application/json** 으로
 *   받습니다. 예전 방식(폼 인코딩)으로 보내면 카카오 서버가 파라미터를 제대로
 *   읽지 못해 "error_code:-1 internal server error!" 처럼 원인을 알 수 없는
 *   응답을 돌려줍니다. 반드시 JSON 으로 보내야 합니다.
 * ------------------------------------------------------------------
 */
@Slf4j
@Service
public class KakaoPayApiService {

    @Value("${kakao.pay.secret-key}")
    private String secretKey; // 카카오페이 전용 개발자센터(developers.kakaopay.com)에서 발급받는 Secret Key
                               //  (2024.01 개편 이전의 "Admin Key" 는 더 이상 사용 불가) - 절대 프론트엔드에 노출하면 안됨

    @Value("${kakao.pay.cid:TC0ONETIME}")
    private String cid; // 가맹점 코드 (테스트용 기본값 TC0ONETIME)

    private final RestClient restClient;

    public KakaoPayApiService(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    /** 1) 결제 준비 - tid 와 결제창 리다이렉트 URL 을 받아옵니다 */
    public KakaoPayReadyResponse ready(
            String partnerOrderId, String partnerUserId, String itemName,
            int quantity, int totalAmount, String approvalUrl, String cancelUrl, String failUrl
    ) {
        // JSON 본문 - 숫자 필드(quantity/total_amount 등)는 문자열이 아니라 숫자 타입 그대로 담습니다.
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cid", cid);
        body.put("partner_order_id", partnerOrderId);
        body.put("partner_user_id", partnerUserId);
        body.put("item_name", itemName);
        body.put("quantity", quantity);
        body.put("total_amount", totalAmount);
        body.put("tax_free_amount", 0);
        body.put("approval_url", approvalUrl);
        body.put("cancel_url", cancelUrl);
        body.put("fail_url", failUrl);

        // 진단용 로그 - 카카오페이가 "-1 내부 처리 오류"처럼 원인을 알 수 없는 응답을 줄 때,
        //  실제로 우리가 어떤 값을 보냈는지 서버 로그에서 바로 확인할 수 있게 남깁니다.
        //  (total_amount=0, quantity=0 등 값 자체가 이상한 경우가 가장 흔한 원인입니다)
        log.info("[KakaoPay ready 요청] cid={}, partner_order_id={}, item_name={}, quantity={}, total_amount={}",
                cid, partnerOrderId, itemName, quantity, totalAmount);

        try {
            return restClient.post()
                    .uri("https://open-api.kakaopay.com/online/v1/payment/ready")
                    .header("Authorization", "SECRET_KEY " + secretKey)
                    .contentType(MediaType.APPLICATION_JSON) // 폼(form) 이 아니라 JSON
                    .body(body)
                    .retrieve()
                    .body(KakaoPayReadyResponse.class);
        } catch (RestClientException ex) {
            // 서버 콘솔 로그를 따로 찾아볼 필요 없이, 우리가 실제로 보낸 요청 파라미터를
            //  에러 메시지 자체에 그대로 실어서 보냅니다. 브라우저 개발자도구의
            //  Network 탭(Response) 에서 바로 확인할 수 있습니다.
            throw new IllegalStateException(
                    "카카오페이 결제준비 실패 - 요청값[cid=" + cid + ", item_name=" + itemName
                            + ", quantity=" + quantity + ", total_amount=" + totalAmount
                            + "] / 카카오 응답: " + ex.getMessage(),
                    ex
            );
        }
    }

    /** 3) 결제 승인 - pg_token 과 tid 로 실제 결제를 확정합니다 */
    public KakaoPayApproveResponse approve(
            String tid, String partnerOrderId, String partnerUserId, String pgToken
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cid", cid);
        body.put("tid", tid);
        body.put("partner_order_id", partnerOrderId);
        body.put("partner_user_id", partnerUserId);
        body.put("pg_token", pgToken);

        try {
            return restClient.post()
                    .uri("https://open-api.kakaopay.com/online/v1/payment/approve")
                    .header("Authorization", "SECRET_KEY " + secretKey)
                    .contentType(MediaType.APPLICATION_JSON) // 폼(form) 이 아니라 JSON
                    .body(body)
                    .retrieve()
                    .body(KakaoPayApproveResponse.class);
        } catch (RestClientException ex) {
            throw new IllegalStateException(
                    "카카오페이 결제승인 실패 - 요청값[cid=" + cid + ", tid=" + tid
                            + ", partner_order_id=" + partnerOrderId + "] / 카카오 응답: " + ex.getMessage(),
                    ex
            );
        }
    }
}
