package com.example.escommunity.service;

import com.example.escommunity.dto.UserDto;
import com.example.escommunity.entity.User;
import com.example.escommunity.jwt.JwtUtil;
import com.example.escommunity.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final RedisService redisService;
    private final JwtUtil jwtUtil;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;
    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    public ResponseEntity<?> sign(UserDto userDto) {
        User user = userRepository.findByEmail(userDto.getEmail());

        if(user != null) {
            return ResponseEntity.badRequest().body("이미 가입한 유저");
        }

        user = User.builder()
                .email(userDto.getEmail())
                .password(bCryptPasswordEncoder.encode(userDto.getPassword()))
                .role("ROLE_USER")
                .build();
        userRepository.save(user);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<?> login(UserDto userDto, HttpServletResponse res) {
        User user = userRepository.findByEmail(userDto.getEmail());

        if(user == null) {
            return ResponseEntity.badRequest().body("존재하지 않는 유저");
        }

        if(!bCryptPasswordEncoder.matches(userDto.getPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body("비밀번호가 일치하지 않음");
        }
        redisService.deleteRefreshToken(userDto.getEmail());

        String refreshToken = jwtUtil.generateToken(user.getUserId(), user.getEmail(), user.getRole(), refreshTokenExpiration);
        redisService.setRefreshToken(user.getEmail(), refreshToken, refreshTokenExpiration);
        String accessToken = jwtUtil.generateToken(user.getUserId(), user.getEmail(), user.getRole(), accessTokenExpiration);

        res.addCookie(creatCookie("refresh", refreshToken));
        res.addHeader("access-token", accessToken);

        userDto.setRole(user.getRole());
        return ResponseEntity.ok().body(userDto);
    }

    public ResponseEntity<?> logout(HttpServletRequest req, HttpServletResponse res) {
        String token = req.getHeader("Authorization");
        String email = jwtUtil.getEmail(token);

        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("refresh")) {
                    cookie.setMaxAge(0);
                    res.addCookie(cookie);
                }
            }
        }

        redisService.deleteRefreshToken(email);

        return ResponseEntity.ok("로그아웃 성공");
    }

    private Cookie creatCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24 * 60 * 60);
        cookie.setHttpOnly(true);
        return cookie;
    }
}
