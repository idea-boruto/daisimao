package com.daisimao.controller;

import com.daisimao.exception.BusinessException;
import com.daisimao.model.dto.UpdateProfileRequest;
import com.daisimao.model.entity.User;
import com.daisimao.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User user = userRepository.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return ResponseEntity.ok(Map.of(
                "userId", user.getId(),
                "username", user.getUsername(),
                "nickname", user.getNickname(),
                "campus", user.getCampus() != null ? user.getCampus() : "",
                "creditScore", user.getCreditScore()
        ));
    }

    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @RequestBody UpdateProfileRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User user = userRepository.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            user.setNickname(request.getNickname().trim());
        }
        if (request.getCampus() != null) {
            user.setCampus(request.getCampus().trim());
        }
        int updated = userRepository.updateById(user);
        if (updated == 0) {
            throw new BusinessException("更新失败，请稍后重试");
        }
        return ResponseEntity.ok(Map.of(
                "userId", user.getId(),
                "username", user.getUsername(),
                "nickname", user.getNickname(),
                "campus", user.getCampus() != null ? user.getCampus() : "",
                "creditScore", user.getCreditScore()
        ));
    }
}
