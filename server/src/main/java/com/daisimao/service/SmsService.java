package com.daisimao.service;

public interface SmsService {
    void sendCode(String phone, String code);
}
