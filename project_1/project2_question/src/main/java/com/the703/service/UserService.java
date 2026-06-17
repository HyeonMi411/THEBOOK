package com.the703.service;

import com.the703.dto.AuthUserDto;
import com.the703.dto.UserDto;

public interface UserService {


	public     AuthUserDto    readAuth( String email );
	
	public int insert(UserDto dto);
	public String   findByEmail( String email );
	public UserDto  findByEmailUserInfo( String email );
	public UserDto  findByNickname( String nickname );
}