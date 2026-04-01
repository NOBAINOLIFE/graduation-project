package com.syt.graduationproject.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;
import java.util.Map;

public class JwtUtil {

    private static final String SECRET_KEY = "experiment-4";
    private static final Long EXPIRATION = 24 * 60 * 60 * 1000L;

    /**
     * 生成JWT令牌
     */
    public static String generateJwtToken(Map<String, Object> claimMap){
        return Jwts.builder()
                .addClaims(claimMap)
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .compact();
    }

    /**
     * 解析JWT令牌
     */
    public static Claims parseJwtToken(String jwt){
        return Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(jwt)
                .getBody();
    }
}