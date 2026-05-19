package com.daisimao.service;

import com.daisimao.exception.BusinessException;
import com.daisimao.model.entity.CreditLog;
import com.daisimao.model.entity.User;
import com.daisimao.repository.CreditLogRepository;
import com.daisimao.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditService {

    private final UserRepository userRepository;
    private final CreditLogRepository creditLogRepository;

    private static final int[] RATING_DELTA = {0, -5, -2, 1, 3, 5};
    private static final int FREEZE_THRESHOLD = 30;

    @Transactional
    public int applyReviewScore(Long targetUserId, int rating, Long taskId) {
        if (rating < 1 || rating > 5) {
            throw new BusinessException("评分必须在 1-5 之间");
        }

        int delta = RATING_DELTA[rating];
        User target = userRepository.selectById(targetUserId);
        if (target == null) {
            throw new BusinessException(404, "用户不存在");
        }

        int newScore = target.getCreditScore() + delta;
        target.setCreditScore(newScore);

        if (newScore < FREEZE_THRESHOLD) {
            target.setStatus(0);
            log.warn("Account frozen: userId={}, creditScore={}", targetUserId, newScore);
        }

        int updated = userRepository.updateById(target);
        if (updated == 0) {
            throw new BusinessException("信用分更新失败，请重试");
        }

        CreditLog logEntry = new CreditLog();
        logEntry.setUserId(targetUserId);
        logEntry.setChangeAmount(delta);
        logEntry.setReason("评价获得" + rating + "星");
        logEntry.setRelatedTaskId(taskId);
        creditLogRepository.insert(logEntry);

        log.info("Credit adjusted: userId={}, delta={}, rating={}, newScore={}",
                targetUserId, delta, rating, newScore);
        return newScore;
    }
}
