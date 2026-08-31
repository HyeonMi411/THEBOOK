package com.thejoa703;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.thejoa703.entity.AppUser;
import com.thejoa703.entity.Hashtag;
import com.thejoa703.entity.Image;
import com.thejoa703.entity.Post;
import com.thejoa703.mapper.AppUserMapper;
import com.thejoa703.mapper.HashtagMapper;
import com.thejoa703.mapper.ImageMapper;
import com.thejoa703.mapper.PostMapper;


@SpringBootTest
@Transactional   // org.springframework.transaction.annotation.Transactional
class Boot2ApplicationTests_1_Entity {
	@Autowired  private  AppUserMapper  appUserMapper;
	@Autowired  private  PostMapper     postMapper;
	@Autowired  private  ImageMapper    imageMapper;
	@Autowired  private  HashtagMapper  hashtagMapper;
	

	
	//테스트공통데이터 : 사용자2명 + 게시글 1글
	private AppUser user1;
	private AppUser user2;
	private Post    post;
	

    @BeforeEach
    void setup() {   //import java.util.UUID
      //사용자 생성
      String email1 = "user1_" + UUID.randomUUID() + "@test.com";
      String email2 = "user2_" + UUID.randomUUID() + "@test.com";
      
      user1 = new AppUser();
      user1.setEmail(email1);
      user1.setPassword("pass123");
      user1.setNickname("user1");
      user1.setProvider("local");
      user1.setDeleted(false);
      
      user2 = new AppUser();
      user2.setEmail(email2);
      user2.setPassword("pass123");
      user2.setNickname("user2");
      user2.setProvider("local");
      user2.setDeleted(false);
      
      appUserMapper.insert(user1);
      appUserMapper.insert(user2);
       
      //게시글 생성 
      post = new Post();
      post.setContent("테스트 게시글");
      post.setUser(user1);
      postMapper.insert(post);
    }
	//-------------------------------------------------------------------
    // AppUserMapper
	//-------------------------------------------------------------------
	@Test 
	@DisplayName("■ AppUserMapper-CRUD")
	void testAppUserMapper() {
		// 이메일 중복검사
		assertThat(   appUserMapper.findByEmail(  user1.getEmail()  ).get().getEmail()  )
		          .isEqualTo(  user1.getEmail()  );
	}
	
	
	
	//-------------------------------------------------------------------
    // ImageMapper
	//-------------------------------------------------------------------
	// insert / select:findById / delete:deleteById
	@Test 
	@DisplayName("■ ImageMapper-CRUD")
	void testImageMapper() {
		// 이미지생성가능
		Image image = new Image();
		image.setSrc("1.png");
		image.setPost(post);  
		imageMapper.insert(image);
		
		// 단건조회
		assertThat( imageMapper.findById(image.getId()).getSrc()  )
				 .isEqualTo("1.png");
		
		// 삭제후조회불가확인
		imageMapper.deleteById(image.getId());
		assertThat( imageMapper.findById(image.getId()) )
		 		 .isNull();
	}
	
	
	
	
	
	//-------------------------------------------------------------------
    // HashtagMapper
	//-------------------------------------------------------------------
	// insert / select:findByName / 연결 : linkPostHashtag
	@Test 
	@DisplayName("■ HashtagMapper-CRUD")
	void testHashtagMapper() {
		// 해쉬태그저장
		Hashtag tag = new Hashtag();
		tag.setName("haha"); 
		hashtagMapper.insert(tag);
		
		// 포스트와 연결 (POST_HASHTAG 조인테이블)
		hashtagMapper.linkPostHashtag(post.getId(), tag.getId());
		
		// 검색 - 해시태그 이름으로 연결된 게시글까지 조회
		Hashtag found = hashtagMapper.findByName("haha");
		assertThat(found).isNotNull();
		assertThat(found.getName()).isEqualTo("haha");

		List<Post> linkedPosts = hashtagMapper.findPostsByHashtagName("haha");
		assertThat(linkedPosts).isNotEmpty();
	}

}
