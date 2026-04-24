package com.example.chatbackend.repository;

import com.example.chatbackend.domain.UserAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByExternalUserId(String externalUserId);
}
