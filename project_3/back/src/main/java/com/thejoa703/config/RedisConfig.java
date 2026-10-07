package com.thejoa703.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")		//application.yml
    private String host;

    @Value("${spring.data.redis.port}")		// 6379
    private int port;
    // Redis 연결 생성 관리 ( JWT 저장소) 
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {  //Lettuce  비동기/반응형 지원
        return new LettuceConnectionFactory(host, port);
    }
    // StringRedisTemplate - Redis 문자열기반 데이ㅓ를 저장/조회할수 있도록 해주는 템플릿
    @Bean
    public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    } // 관리
    
    // 객체/JSON 직렬화 템플릿 - 베스트셀러(판매량 TOP N) 캐싱에 사용 (BookService)
     @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        // key 직렬화 : KEY깨지지 않게 문자열로 
        template.setKeySerializer(new StringRedisSerializer());
        
        // Value 직렬화 - 도서 정보의 날짜(LocalDate/LocalDateTime)도 저장할 수 있도록 JavaTimeModule 을 등록한
        // ObjectMapper 사용. (기본 GenericJackson2JsonRedisSerializer 는 Java 날짜 타입을 못 다뤄서,
        //  판매 기록이 생긴 뒤 베스트셀러를 캐시에 저장할 때 500 오류가 났음)
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper());
        template.setValueSerializer(valueSerializer);
        // hash 구조 직렬화 설정
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(valueSerializer);

        return template;
    }

    /** Redis 캐시용 ObjectMapper - 기본 직렬화기와 같은 타입 정보(@class) + Java 날짜 지원 */
    private com.fasterxml.jackson.databind.ObjectMapper redisObjectMapper() {
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        om.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        om.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        om.disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        om.activateDefaultTyping(
                om.getPolymorphicTypeValidator(),
                com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.EVERYTHING,
                com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY);
        GenericJackson2JsonRedisSerializer.registerNullValueSerializer(om, null);
        return om;
    }    
}