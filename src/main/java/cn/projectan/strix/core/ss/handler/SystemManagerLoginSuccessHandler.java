package cn.projectan.strix.core.ss.handler;

import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.util.http.ServletUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * @author ProjectAn
 * @since 2024/4/6 下午4:31
 */
@Component
@RequiredArgsConstructor
public class SystemManagerLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Authentication authentication) throws IOException {

        String result = objectMapper.writeValueAsString(RetBuilder.success("登录成功"));
        ServletUtil.write(response, result);
    }

}
