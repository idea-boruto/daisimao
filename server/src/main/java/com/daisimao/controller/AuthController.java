package com.daisimao.controller;

import com.daisimao.exception.BusinessException;
import com.daisimao.model.dto.LoginRequest;
import com.daisimao.model.dto.LoginResponse;
import com.daisimao.model.entity.User;
import com.daisimao.repository.UserRepository;
import com.daisimao.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.selectByUsername(request.getUsername());
        boolean isNewUser = (user == null);

        if (isNewUser) {
            user = new User();
            user.setUsername(request.getUsername());
            user.setNickname(request.getUsername());
            user.setCreditScore(100);
            user.setStatus(1);
            userRepository.insert(user);
        }

        if (user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被冻结，请联系管理员");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());

        return ResponseEntity.ok(new LoginResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                isNewUser
        ));
    }
}
