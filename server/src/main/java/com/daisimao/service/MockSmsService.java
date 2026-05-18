package com.daisimao.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("dev")
public class MockSmsService implements SmsService {

    @Override
    public void sendCode(String phone, String code) {
        log.info("[MOCK SMS] To: {}, Code: {}", phone, code);
    }
}
