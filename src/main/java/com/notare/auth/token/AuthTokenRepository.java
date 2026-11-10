package com.notare.auth.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface AuthTokenRepository extends JpaRepository<AuthToken, UUID> {

    Optional<AuthToken> findByTokenHashAndPurpose(String tokenHash, AuthTokenPurpose purpose);

    /** Marks every still-unused token of this purpose for the user as used, so only the newest link works. */
    @Modifying
    @Query("UPDATE AuthToken t SET t.usedAt = :now "
            + "WHERE t.user.id = :userId AND t.purpose = :purpose AND t.usedAt IS NULL")
    int invalidateOpenTokens(@Param("userId") UUID userId,
                             @Param("purpose") AuthTokenPurpose purpose,
                             @Param("now") LocalDateTime now);
}
