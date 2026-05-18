package com.daisimao.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

@Slf4j
@Service
@Profile("prod")
public class AliyunSmsService implements SmsService {

    private final String accessKeyId;
    private final String accessKeySecret;
    private final String signName;
    private final String templateCode;

    public AliyunSmsService(
            @Value("${sms.aliyun.access-key-id}") String accessKeyId,
            @Value("${sms.aliyun.access-key-secret}") String accessKeySecret,
            @Value("${sms.aliyun.sign-name}") String signName,
            @Value("${sms.aliyun.template-code}") String templateCode) {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.signName = signName;
        this.templateCode = templateCode;
    }

    @Override
    public void sendCode(String phone, String code) {
        try {
            String response = callApi(phone, code);
            log.info("SMS sent to {}: result={}", phone, response);
        } catch (Exception e) {
            log.error("SMS send failed to {}", phone, e);
            throw new RuntimeException("短信发送失败，请稍后重试");
        }
    }

    private String callApi(String phone, String code) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timestamp = sdf.format(new Date());

        String params = String.format(
            "AccessKeyId=%s&Action=SendSms&Format=JSON&PhoneNumbers=%s&SignName=%s" +
            "&SignatureMethod=HMAC-SHA1&SignatureVersion=1.0&TemplateCode=%s" +
            "&TemplateParam={\"code\":\"%s\"}&Timestamp=%s&Version=2017-05-25",
            accessKeyId, phone, signName, templateCode, code, timestamp);

        String signature = hmacSha1("GET&%2F&" + urlEncode(params), accessKeySecret + "&");

        String url = "https://dysmsapi.aliyuncs.com/?" + params + "&Signature=" + urlEncode(signature);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private String hmacSha1(String data, String key) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
        mac.init(new javax.crypto.spec.SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA1"));
        byte[] raw = mac.doFinal(data.getBytes("UTF-8"));
        return java.util.Base64.getEncoder().encodeToString(raw);
    }

    private String urlEncode(String value) throws Exception {
        return java.net.URLEncoder.encode(value, "UTF-8")
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
    }
}
