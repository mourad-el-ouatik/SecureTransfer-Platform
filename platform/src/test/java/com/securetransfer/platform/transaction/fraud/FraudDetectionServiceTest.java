package com.securetransfer.platform.transaction.fraud;

import com.securetransfer.platform.transaction.repository.TransactionRepository;
import com.securetransfer.platform.user.dto.UserResponse;
import com.securetransfer.platform.user.entity.KycStatus;
import com.securetransfer.platform.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private FraudDetectionService fraudDetectionService;

    private UserResponse verifiedUser;

    @BeforeEach
    void setUp() {
        verifiedUser = new UserResponse(
                1L, "test@test.com", KycStatus.VERIFIED,
                new BigDecimal("10000"), new BigDecimal("2000"), LocalDateTime.now());
    }

    @Test
    void amountExceedsAbsoluteThreshold_shouldReturnBlocked() {
        when(userService.getUser(1L)).thenReturn(verifiedUser);

        FraudResult result = fraudDetectionService.analyze(1L, new BigDecimal("60000"));

        assertInstanceOf(FraudResult.Blocked.class, result);
    }

    @Test
    void userKycIsPending_shouldReturnSuspicious() {
        UserResponse pendingUser = new UserResponse(
                1L, "test@test.com", KycStatus.PENDING,
                new BigDecimal("10000"), new BigDecimal("2000"), LocalDateTime.now());
        when(userService.getUser(1L)).thenReturn(pendingUser);

        FraudResult result = fraudDetectionService.analyze(1L, new BigDecimal("1000"));

        assertInstanceOf(FraudResult.Suspicious.class, result);
    }

    @Test
    void amountCloseToLimit_shouldReturnSuspicious() {
        when(userService.getUser(1L)).thenReturn(verifiedUser); // limit is 2000

        FraudResult result = fraudDetectionService.analyze(1L, new BigDecimal("1995")); // 99.75% of limit

        assertInstanceOf(FraudResult.Suspicious.class, result);
    }

    @Test
    void highVelocity_shouldReturnSuspicious() {
        when(userService.getUser(1L)).thenReturn(verifiedUser);
        when(transactionRepository.countBySenderIdAndCreatedAtAfter(eq(1L), any(LocalDateTime.class)))
                .thenReturn(6L);

        FraudResult result = fraudDetectionService.analyze(1L, new BigDecimal("100"));

        assertInstanceOf(FraudResult.Suspicious.class, result);
    }

    @Test
    void normalTransaction_shouldReturnClean() {
        when(userService.getUser(1L)).thenReturn(verifiedUser);
        when(transactionRepository.countBySenderIdAndCreatedAtAfter(eq(1L), any(LocalDateTime.class)))
                .thenReturn(1L);

        FraudResult result = fraudDetectionService.analyze(1L, new BigDecimal("500"));

        assertInstanceOf(FraudResult.Clean.class, result);
    }
}
