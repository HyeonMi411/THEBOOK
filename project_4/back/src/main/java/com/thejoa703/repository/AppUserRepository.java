package com.thejoa703.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.thejoa703.entity.AppUser;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

	Optional<AppUser> findByEmail(String email);

	Optional<AppUser> findByEmailAndProvider(String email, String provider);

	boolean existsByNickname(String nickname);

	boolean existsByEmail(String email);
}
