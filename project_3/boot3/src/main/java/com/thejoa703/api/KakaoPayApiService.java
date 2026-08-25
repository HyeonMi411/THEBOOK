package com.thejoa703.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;

/**
 * 카카오페이 결제 API(정기가 아닌 "일반결제") 연동 서비스
 * ------------------------------------------------------------------
 * 결제준비(ready) → 사용자가 카카오페이 결제창에서 결제 진행 → 결제승인(approve) 의
 * 3단계 흐름 중, 우리 서버가 담당하는 1),3) 단계를 처리합니다.
 * (문서: https://developers.kakaopay.com/docs/payment/online/single-payment)
 * ------------------------------------------------------------------
 */
@Slf4j
@Service
public class KakaoPayApiService {

    @Value("${kakao.pay.secret-key}")
    private String secretKey; // ★카카오페이 전용 개발자센터(developers.kakaopay.com)에서 발급받는 Secret Key
                               //   (2024.01 개편 이전의 "Admin Key" 는 더 이상 사용 불가) - 절대 프론트엔드에 노출하면 안됨

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
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("cid", cid);
        params.add("partner_order_id", partnerOrderId);
        params.add("partner_user_id", partnerUserId);
        params.add("item_name", itemName);
        params.add("quantity", String.valueOf(quantity));
        params.add("total_amount", String.valueOf(totalAmount));
        params.add("tax_free_amount", "0");
        params.add("approval_url", approvalUrl);
        params.add("cancel_url", cancelUrl);
        params.add("fail_url", failUrl);

        // ★진단용 로그 - 카카오페이가 "-1 내부 처리 오류"처럼 원인을 알 수 없는 응답을 줄 때,
        //   실제로 우리가 어떤 값을 보냈는지 서버 로그에서 바로 확인할 수 있게 남깁니다.
        //   (total_amount=0, quantity=0 등 값 자체가 이상한 경우가 가장 흔한 원인입니다)
        log.info("[KakaoPay ready 요청] cid={}, partner_order_id={}, item_name={}, quantity={}, total_amount={}",
                cid, partnerOrderId, itemName, quantity, totalAmount);

        return restClient.post()
                .uri("https://open-api.kakaopay.com/online/v1/payment/ready")
                .header("Authorization", "SECRET_KEY " + secretKey)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .body(KakaoPayReadyResponse.class);
    }

    /** 3) 결제 승인 - pg_token 과 tid 로 실제 결제를 확정합니다 */
    public KakaoPayApproveResponse approve(
            String tid, String partnerOrderId, String partnerUserId, String pgToken
    ) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("cid", cid);
        params.add("tid", tid);
        params.add("partner_order_id", partnerOrderId);
        params.add("partner_user_id", partnerUserId);
        params.add("pg_token", pgToken);

        return restClient.post()
                .uri("https://open-api.kakaopay.com/online/v1/payment/approve")
                .header("Authorization", "SECRET_KEY " + secretKey)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .body(KakaoPayApproveResponse.class);
    }
}
