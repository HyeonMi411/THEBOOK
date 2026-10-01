package com.thejoa703.api;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thejoa703.dto.UtilDto.WeatherDto;

import lombok.extern.slf4j.Slf4j;

/**
 * 기상청 단기예보 조회서비스 - 초단기실황(getUltraSrtNcst)
 *
 * boot1 ApiKmaWeather 는 base_time 을 0600, 격자를 (55,125) 로 고정해서 "오늘 아침 6시 서울" 값만 나왔다.
 * 여기서는
 *  1) 앱이 보낸 위도/경도를 기상청 격자(nx, ny)로 변환 (기상청 공식 LCC DFS 변환식)
 *  2) 가장 최근 발표 시각 계산 (매시 정각 발표, 약 40분 뒤 제공 → 45분 전이면 한 시간 전 자료)
 *  3) 같은 격자·발표시각은 10분간 메모리 캐시 (공공데이터 호출 한도 보호)
 *
 * [수정] KMA_API_KEY 가 없거나 기상청 호출/응답이 실패하면
 *        API 키가 필요 없는 Open-Meteo(현재 날씨)로 자동 대체한다.
 */
@Slf4j
@Service
public class KmaWeatherService {

        private static final String URL = "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst";
        private static final String OPEN_METEO_URL = "https://api.open-meteo.com/v1/forecast";
        private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
        private static final long CACHE_MILLIS = 10 * 60 * 1000L;

        @Value("${kma.api-key:}")
        private String apiKey;

        private final RestClient restClient;
        private final ObjectMapper objectMapper = new ObjectMapper();
        private final Map<String, CachedWeather> cache = new ConcurrentHashMap<>();

        private record CachedWeather(WeatherDto dto, long savedAt) {}

        public KmaWeatherService(RestClient.Builder builder) {
                this.restClient = builder.build();
        }

        public WeatherDto getWeather(double lat, double lon) {
                if (lat < 32 || lat > 39.5 || lon < 124 || lon > 132.5) {
                        return unavailable("대한민국 범위 밖의 위치입니다.");
                }
                int[] grid = toGrid(lat, lon);

                // 기상청 키가 없으면 바로 Open-Meteo 로 대체
                if (apiKey == null || apiKey.isBlank()) {
                        return openMeteo(lat, lon, grid);
                }

                LocalDateTime base = baseDateTime(LocalDateTime.now(SEOUL));
                String baseDate = base.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                String baseTime = base.format(DateTimeFormatter.ofPattern("HH00"));
                String key = grid[0] + ":" + grid[1] + ":" + baseDate + baseTime;

                CachedWeather cached = cache.get(key);
                if (cached != null && System.currentTimeMillis() - cached.savedAt() < CACHE_MILLIS) {
                        return cached.dto();
                }
                try {
                        URI uri = UriComponentsBuilder.fromUriString(URL)
                                        .queryParam("serviceKey", apiKey)
                                        .queryParam("numOfRows", 10)
                                        .queryParam("pageNo", 1)
                                        .queryParam("dataType", "JSON")
                                        .queryParam("base_date", baseDate)
                                        .queryParam("base_time", baseTime)
                                        .queryParam("nx", grid[0])
                                        .queryParam("ny", grid[1])
                                        .encode()   // 디코딩 키의 +, / 등이 깨지지 않도록 인코딩
                                        .build()
                                        .toUri();
                        String body = restClient.get().uri(uri).retrieve().body(String.class);
                        WeatherDto dto = parse(body, baseDate, baseTime, grid);
                        if (dto.isAvailable()) {
                                putCache(key, dto);
                                return dto;
                        }
                        log.info("[날씨] 기상청 응답 실패({}) - Open-Meteo 로 대체", dto.getMessage());
                        return openMeteo(lat, lon, grid);
                } catch (Exception e) {
                        log.warn("[날씨] 기상청 API 호출 실패: {} - Open-Meteo 로 대체", e.getMessage());
                        return openMeteo(lat, lon, grid);
                }
        }

        /** Open-Meteo 현재 날씨 (API 키 불필요) */
        private WeatherDto openMeteo(double lat, double lon, int[] grid) {
                String key = "om:" + grid[0] + ":" + grid[1];
                CachedWeather cached = cache.get(key);
                if (cached != null && System.currentTimeMillis() - cached.savedAt() < CACHE_MILLIS) {
                        return cached.dto();
                }
                try {
                        URI uri = UriComponentsBuilder.fromUriString(OPEN_METEO_URL)
                                        .queryParam("latitude", lat)
                                        .queryParam("longitude", lon)
                                        .queryParam("current", "temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m,weather_code")
                                        .queryParam("wind_speed_unit", "ms")
                                        .queryParam("timezone", "Asia/Seoul")
                                        .encode()
                                        .build()
                                        .toUri();
                        String body = restClient.get().uri(uri).retrieve().body(String.class);
                        JsonNode cur = objectMapper.readTree(body).path("current");
                        if (cur.isMissingNode() || !cur.has("temperature_2m")) {
                                return unavailable("날씨 정보를 가져오지 못했습니다.");
                        }

                        WeatherDto dto = new WeatherDto();
                        dto.setTemperature(cur.path("temperature_2m").asDouble());
                        dto.setHumidity((int) Math.round(cur.path("relative_humidity_2m").asDouble()));
                        dto.setRainfall1h(cur.path("precipitation").asDouble());
                        dto.setWindSpeed(cur.path("wind_speed_10m").asDouble());
                        dto.setPrecipitationType(ptyFromWmo(cur.path("weather_code").asInt(0)));

                        LocalDateTime observed;
                        try {
                                observed = LocalDateTime.parse(cur.path("time").asText());   // 예: 2026-10-01T16:15
                        } catch (Exception ignore) {
                                observed = LocalDateTime.now(SEOUL);
                        }
                        finish(dto,
                                        observed.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                                        observed.format(DateTimeFormatter.ofPattern("HHmm")),
                                        grid);
                        putCache(key, dto);
                        log.info("[날씨] Open-Meteo 사용 - {}℃ ({})", dto.getTemperature(), dto.getPrecipitationText());
                        return dto;
                } catch (Exception e) {
                        log.warn("[날씨] Open-Meteo 호출 실패: {}", e.getMessage());
                        return unavailable("날씨 서버에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.");
                }
        }

        /** WMO 날씨 코드 → 기상청 강수형태(PTY) 코드로 변환 */
        private int ptyFromWmo(int code) {
                if (code >= 51 && code <= 57) return 5;                                   // 이슬비 → 빗방울
                if ((code >= 61 && code <= 67) || (code >= 80 && code <= 82) || code >= 95) return 1;   // 비, 소나기, 뇌우
                if ((code >= 71 && code <= 77) || code == 85 || code == 86) return 3;     // 눈
                return 0;
        }

        private void putCache(String key, WeatherDto dto) {
                if (cache.size() > 500) {
                        cache.clear();
                }
                cache.put(key, new CachedWeather(dto, System.currentTimeMillis()));
        }

        private WeatherDto parse(String body, String baseDate, String baseTime, int[] grid) throws Exception {
                JsonNode root = objectMapper.readTree(body);
                String resultCode = root.path("response").path("header").path("resultCode").asText("");
                if (!"00".equals(resultCode)) {
                        String msg = root.path("response").path("header").path("resultMsg").asText("알 수 없는 오류");
                        return unavailable("기상청 응답 오류: " + msg);
                }
                WeatherDto dto = new WeatherDto();
                for (JsonNode item : root.path("response").path("body").path("items").path("item")) {
                        String category = item.path("category").asText();
                        String value = item.path("obsrValue").asText();
                        switch (category) {
                                case "T1H" -> dto.setTemperature(toDouble(value));
                                case "REH" -> dto.setHumidity((int) Math.round(toDouble(value)));
                                case "RN1" -> dto.setRainfall1h(toDouble(value));
                                case "WSD" -> dto.setWindSpeed(toDouble(value));
                                case "PTY" -> dto.setPrecipitationType((int) Math.round(toDouble(value)));
                                default -> { }
                        }
                }
                if (dto.getTemperature() == null) {
                        return unavailable("아직 발표된 관측 자료가 없습니다.");
                }
                finish(dto, baseDate, baseTime, grid);
                return dto;
        }

        /** 공통 마무리: 발표 시각/격자, 강수 문구, 독서 팁 */
        private void finish(WeatherDto dto, String baseDate, String baseTime, int[] grid) {
                dto.setAvailable(true);
                dto.setBaseDate(baseDate);
                dto.setBaseTime(baseTime);
                dto.setNx(grid[0]);
                dto.setNy(grid[1]);
                int pty = dto.getPrecipitationType() == null ? 0 : dto.getPrecipitationType();
                dto.setPrecipitationText(switch (pty) {
                        case 1 -> "비";
                        case 2 -> "비/눈";
                        case 3 -> "눈";
                        case 5 -> "빗방울";
                        case 6 -> "빗방울/눈날림";
                        case 7 -> "눈날림";
                        default -> "강수 없음";
                });
                dto.setReadingTip(tipFor(pty, dto.getTemperature()));
        }

        private String tipFor(int pty, double temp) {
                if (pty != 0) {
                        return "빗소리와 함께 읽기 좋은 날이에요. 따뜻한 소설 한 권 어때요?";
                }
                if (temp >= 28) {
                        return "더운 날엔 시원한 곳에서 가볍게 읽히는 에세이를 추천해요.";
                }
                if (temp <= 5) {
                        return "추운 날, 이불 속에서 긴 장편소설에 빠져 보세요.";
                }
                return "산책하기 좋은 날씨예요. 가방에 얇은 책 한 권 챙겨 보세요.";
        }

        private double toDouble(String value) {
                try {
                        return Double.parseDouble(value);
                } catch (NumberFormatException e) {
                        return 0.0;   // "강수없음" 같은 문자열
                }
        }

        private WeatherDto unavailable(String message) {
                WeatherDto dto = new WeatherDto();
                dto.setAvailable(false);
                dto.setMessage(message);
                return dto;
        }

        /** 초단기실황: 매시 정각 자료가 40분쯤 뒤에 제공되므로, 45분 전이면 한 시간 전 자료를 요청 */
        static LocalDateTime baseDateTime(LocalDateTime now) {
                LocalDateTime base = now.getMinute() < 45 ? now.minusHours(1) : now;
                return base.withMinute(0).withSecond(0).withNano(0);
        }

        /** 기상청 공식 위경도 → 격자(DFS) 변환 (Lambert Conformal Conic) */
        static int[] toGrid(double lat, double lon) {
                double RE = 6371.00877, GRID = 5.0, SLAT1 = 30.0, SLAT2 = 60.0, OLON = 126.0, OLAT = 38.0, XO = 43, YO = 136;
                double DEGRAD = Math.PI / 180.0;
                double re = RE / GRID;
                double slat1 = SLAT1 * DEGRAD, slat2 = SLAT2 * DEGRAD, olon = OLON * DEGRAD, olat = OLAT * DEGRAD;
                double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
                sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
                double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
                sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;
                double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
                ro = re * sf / Math.pow(ro, sn);
                double ra = Math.tan(Math.PI * 0.25 + lat * DEGRAD * 0.5);
                ra = re * sf / Math.pow(ra, sn);
                double theta = lon * DEGRAD - olon;
                if (theta > Math.PI) theta -= 2.0 * Math.PI;
                if (theta < -Math.PI) theta += 2.0 * Math.PI;
                theta *= sn;
                int nx = (int) Math.floor(ra * Math.sin(theta) + XO + 0.5);
                int ny = (int) Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);
                return new int[] { nx, ny };
        }
}
