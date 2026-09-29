package com.project.shopapp.repository;

import com.project.shopapp.model.Token;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByToken(String token);

    List<Token> findAllByUserIdAndRevokedFalse(Long userId);
}
