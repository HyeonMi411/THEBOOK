package com.thejoa703;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import com.thejoa703.dto.LoginRequest;
import com.thejoa703.dto.UserDto.UserRequestDto;
import com.thejoa703.dto.UserDto.UserResponseDto;
import com.thejoa703.security.EmailVerificationStore;
import com.thejoa703.service.UserService;
 

@SpringBootTest
@Transactional    
class Boot2ApplicationTests_2_Service {  
	@Autowired  UserService   userService;
	@Autowired  EmailVerificationStore emailVerificationStore;
	
	// 공통으로 사용할 유저를 생성해주는 헬퍼메서드
    private Long createTestUser(String email, String nickname) {
        UserRequestDto signupDto = new UserRequestDto();
        signupDto.setEmail(email);
        signupDto.setPassword("password123");
        signupDto.setNickname(nickname);
        signupDto.setProvider("local");
        MockMultipartFile profileImage = new MockMultipartFile(
                "profileImage", "test.png", "image/png",
                // 1x1 투명 PNG 실제 바이너리 - FileStorageService.uploadImage() 가
                // ImageIO 로 실제 이미지인지 검증하므로, 텍스트가 아닌 유효한 PNG 여야 통과
                java.util.Base64.getDecoder().decode(
                        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=")
        );

        // UserService.createUser() 는 local 가입일 때 이메일 인증완료 상태를 확인.
        // 실제 메일 발송/인증번호 확인 절차 대신, Redis 에 인증완료 상태를 직접
        // 세팅해서 테스트가 이메일 인증 자체가 아니라 회원가입 로직을 검증하도록 함.
        emailVerificationStore.markVerified(email, 1800);

        UserResponseDto res = userService.createUser(signupDto, profileImage);
        return res.getId();
    }
	//-------------------------------------------------------------------
    // UserService - CRUD
	//-------------------------------------------------------------------
	@Test
	@Order(1)
	@DisplayName("■ UserService - CRUD :  회원가입, 로그인, 마이페이지, 수정, 삭제")
	void testAppUserService() {
		Long  userId = createTestUser("test1@email.com", "test1");
		
		LoginRequest  loginDto = new LoginRequest();
		loginDto.setEmail("test1@email.com");
		loginDto.setPassword("password123");
		loginDto.setProvider("local");
		
		UserResponseDto loginRes = userService.login(loginDto);
		assertThat(loginRes).isNotNull();
		assertThat(loginRes.getId()).isEqualTo( userId );
		 
		// 이메일중복검사 - 존재확인
		assertThat( userService.existsByEmail("test1@email.com") ).isTrue();	
		
		// 닉네임중복검사 - 존재확인
		assertThat( userService.existsByNickname("test1") ).isTrue();

		// 마이페이지
		UserResponseDto  foundUser = userService.getUser(userId);
		assertThat(foundUser.getNickname()).isEqualTo("test1");
		//유저닉네임수정
		UserResponseDto  updatedUser = userService.updateNickname(userId, "111");
		assertThat(updatedUser.getNickname()).isEqualTo("111");
		// 유저삭제
		//userService.deleteById(userId);
		//UserResponseDto  deleteUser = userService.getUser(userId); // 없는유저여서 오류남
		//assertThat(deleteUser).isNull();
	}
}
	












