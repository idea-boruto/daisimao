package com.daisimao.service;

import com.daisimao.exception.BusinessException;
import com.daisimao.model.entity.CreditLog;
import com.daisimao.model.entity.User;
import com.daisimao.repository.CreditLogRepository;
import com.daisimao.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditServiceTest {

    @Mock UserRepository userRepository;
    @Mock CreditLogRepository creditLogRepository;

    @InjectMocks CreditService creditService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setCreditScore(100);
        user.setStatus(1);
    }

    @Nested
    class RatingMapping {

        @Test
        void shouldMapRating1ToMinus5() {
            when(userRepository.selectById(1L)).thenReturn(user);
            when(userRepository.updateById(any(User.class))).thenReturn(1);

            int newScore = creditService.applyReviewScore(1L, 1, 10L);

            assertThat(newScore).isEqualTo(95);
            verify(creditLogRepository).insert(argThat(log -> log.getChangeAmount() == -5));
        }

        @Test
        void shouldMapRating2ToMinus2() {
            when(userRepository.selectById(1L)).thenReturn(user);
            when(userRepository.updateById(any(User.class))).thenReturn(1);

            int newScore = creditService.applyReviewScore(1L, 2, 10L);

            assertThat(newScore).isEqualTo(98);
            verify(creditLogRepository).insert(argThat(log -> log.getChangeAmount() == -2));
        }

        @Test
        void shouldMapRating3ToPlus1() {
            when(userRepository.selectById(1L)).thenReturn(user);
            when(userRepository.updateById(any(User.class))).thenReturn(1);

            int newScore = creditService.applyReviewScore(1L, 3, 10L);

            assertThat(newScore).isEqualTo(101);
            verify(creditLogRepository).insert(argThat(log -> log.getChangeAmount() == 1));
        }

        @Test
        void shouldMapRating4ToPlus3() {
            when(userRepository.selectById(1L)).thenReturn(user);
            when(userRepository.updateById(any(User.class))).thenReturn(1);

            int newScore = creditService.applyReviewScore(1L, 4, 10L);

            assertThat(newScore).isEqualTo(103);
            verify(creditLogRepository).insert(argThat(log -> log.getChangeAmount() == 3));
        }

        @Test
        void shouldMapRating5ToPlus5() {
            when(userRepository.selectById(1L)).thenReturn(user);
            when(userRepository.updateById(any(User.class))).thenReturn(1);

            int newScore = creditService.applyReviewScore(1L, 5, 10L);

            assertThat(newScore).isEqualTo(105);
            verify(creditLogRepository).insert(argThat(log -> log.getChangeAmount() == 5));
        }
    }

    @Nested
    class Validation {

        @Test
        void shouldRejectRatingBelow1() {
            assertThatThrownBy(() -> creditService.applyReviewScore(1L, 0, 10L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("1-5");
        }

        @Test
        void shouldRejectRatingAbove5() {
            assertThatThrownBy(() -> creditService.applyReviewScore(1L, 6, 10L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("1-5");
        }

        @Test
        void shouldRejectNonExistentUser() {
            when(userRepository.selectById(99L)).thenReturn(null);

            assertThatThrownBy(() -> creditService.applyReviewScore(99L, 3, 10L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(404);
        }
    }

    @Nested
    class FreezeMechanism {

        @Test
        void shouldFreezeAccountWhenBelowThreshold() {
            user.setCreditScore(33);
            when(userRepository.selectById(1L)).thenReturn(user);
            when(userRepository.updateById(any(User.class))).thenReturn(1);

            creditService.applyReviewScore(1L, 1, 10L); // 33 + (-5) = 28 < 30

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).updateById(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(0);
        }

        @Test
        void shouldNotFreezeAccountWhenAtThreshold() {
            user.setCreditScore(35);
            when(userRepository.selectById(1L)).thenReturn(user);
            when(userRepository.updateById(any(User.class))).thenReturn(1);

            creditService.applyReviewScore(1L, 1, 10L); // 35 + (-5) = 30 >= 30

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).updateById(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(1);
        }
    }

    @Nested
    class OptimisticLock {

        @Test
        void shouldThrowWhenUpdateReturnsZero() {
            when(userRepository.selectById(1L)).thenReturn(user);
            when(userRepository.updateById(any(User.class))).thenReturn(0);

            assertThatThrownBy(() -> creditService.applyReviewScore(1L, 3, 10L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("重试");
        }
    }
}
