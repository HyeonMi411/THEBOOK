package com.thejoa703.llmrag;

// OpenAI Chat API 요청/응답에 공통으로 쓰이는 메시지 - role: "system" / "user" / "assistant"
// record 로 선언해야 Jackson 이 OpenAI 응답(JSON)을 역직렬화할 때 별도 설정 없이도
// 인스턴스를 생성할 수 있음. Lombok @Value(불변 클래스, 기본생성자 없음)로 두면
// Jackson 이 생성 방법을 못 찾아 "Type definition error" 가 발생.
public record Message(String role, String content) {
}
