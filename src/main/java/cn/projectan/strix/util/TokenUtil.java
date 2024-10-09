package cn.projectan.strix.util;

import cn.hutool.core.map.MapUtil;
import cn.projectan.strix.model.other.security.*;
import cn.projectan.strix.model.properties.StrixJwtProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecureDigestAlgorithm;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JWT 工具类
 *
 * @author ProjectAn
 * @since 2024/4/5 下午6:32
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(StrixJwtProperties.class)
public class TokenUtil {

    private final StrixJwtProperties jwtProperties;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    private final SecureDigestAlgorithm<SecretKey, SecretKey> alg = Jwts.SIG.HS256;
    private SecretKey key;
    private JwtParser parser;

    @PostConstruct
    public void init() {
        key = Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes());
        parser = Jwts.parser().verifyWith(key).build();
    }

    public TokenDTO createToken(SystemManagerTokenInfo info) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("nickname", info.getNickname());
        userInfo.put("status", info.getStatus());
        userInfo.put("type", info.getType());
        userInfo.put("regionId", info.getRegionId());
        userInfo.put("menuKeys", info.getMenuKeys());
        userInfo.put("permissionKeys", info.getPermissionKeys());

        Map<String, Object> deviceInfo = new HashMap<>();

        return createToken(
                UserType.SYSTEM_MANAGER,
                info.getUid(),
                userInfo,
                deviceInfo
        );
    }

    public TokenDTO createToken(SystemUserTokenInfo info) {
        return createToken(
                UserType.SYSTEM_USER,
                info.getUid(),
                Map.of(
                        "nickname", info.getNickname(),
                        "phoneNumber", info.getPhoneNumber(),
                        "status", info.getStatus()
                ),
                Map.of()
        );
    }

    /**
     * 创建 Token
     *
     * @param userType   用户类型
     * @param userId     用户 ID
     * @param userInfo   用户信息
     * @param deviceInfo 设备信息
     * @return TokenDTO
     */
    public TokenDTO createToken(Integer userType, String userId, Map<String, Object> userInfo, Map<String, Object> deviceInfo) {
        // 计算过期时间
        Date tokenExpireTime = new Date(System.currentTimeMillis() + jwtProperties.getExpireTime());

        userInfo.put("_type", userType);
        userInfo.put("_id", userId);

        deviceInfo.put("_type", userType);
        deviceInfo.put("_id", userId);

        String token = Jwts.builder()
                .signWith(key, alg)
                .expiration(tokenExpireTime)
                .claims(userInfo)
                .compact();
        String refreshToken = createRefreshToken(deviceInfo);

        return new TokenDTO(token, refreshToken);
    }

    /**
     * 生成 Refresh Token
     *
     * @param info 设备信息
     */
    private String createRefreshToken(Map<String, Object> info) {
        Date refreshTokenExpireTime = new Date(System.currentTimeMillis() + jwtProperties.getRefreshExpireTime());

        return Jwts.builder()
                .signWith(key, alg)
                .expiration(refreshTokenExpireTime)
                .claims(info)
                .compact();
    }

    /**
     * 解析 Token
     *
     * @param token Token
     * @return Token 信息
     */
    @SuppressWarnings("unchecked")
    public BaseTokenInfo parseToken(String token) {
        Jws<Claims> claimsJws = parseJWT(token);
        Claims claims = claimsJws.getPayload();

        Integer userType = claims.get("_type", Integer.class);

        switch (userType) {
            case UserType.SYSTEM_MANAGER -> {
                SystemManagerTokenInfo info = new SystemManagerTokenInfo();
                info.setUid(claims.get("_id", String.class));
                info.setUType(claims.get("_type", Integer.class));

                info.setNickname(claims.get("nickname", String.class));
                info.setStatus(claims.get("status", Integer.class));
                info.setType(claims.get("type", Integer.class));
                info.setRegionId(claims.get("regionId", String.class));
                info.setMenuKeys(claims.get("menuKeys", List.class));
                info.setPermissionKeys(claims.get("permissionKeys", List.class));

                return info;
            }
            case UserType.SYSTEM_USER -> {
                SystemUserTokenInfo info = new SystemUserTokenInfo();
                info.setUid(claims.get("_id", String.class));
                info.setUType(claims.get("_type", Integer.class));

                info.setNickname(claims.get("nickname", String.class));
                info.setPhoneNumber(claims.get("phoneNumber", String.class));
                info.setStatus(claims.get("status", Integer.class));

                return info;
            }
        }
        return null;
    }

    /**
     * 解析 Refresh Token
     *
     * @param refreshToken Refresh Token
     * @return Refresh Token 信息
     */
    public BaseRefreshTokenInfo parseRefreshToken(String refreshToken) {
        Jws<Claims> claimsJws = parseJWT(refreshToken);
        Claims claims = claimsJws.getPayload();

        BaseRefreshTokenInfo info = new BaseRefreshTokenInfo();
        info.setId(claims.get("_id", String.class));
        info.setType(claims.get("_type", Integer.class));

        return info;
    }

    /**
     * 验证并解析 JWT
     *
     * @param token JWT
     * @return JWT 包含的信息
     */
    private Jws<Claims> parseJWT(String token) {
        return parser.parseSignedClaims(token);
    }

    /**
     * 查看 JWT 过期时间
     *
     * @param token JWT
     */
    public Date getTokenExpiration(String token) {
        String payload = token.split("\\.")[1];
        byte[] decode = java.util.Base64.getDecoder().decode(payload);
        String json = new String(decode);
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {
            });
            Long exp = MapUtil.getLong(map, "exp");
            return new Date(exp * 1000L);
        } catch (Exception e) {
            log.error("解析jwt失败", e);
        }
        return null;
    }

}
