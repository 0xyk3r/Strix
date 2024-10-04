package cn.projectan.strix.core.ss.handler;

import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.utils.ServletUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

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
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        String result = objectMapper.writeValueAsString(RetBuilder.success("登录成功"));
        ServletUtils.write(response, result);
    }

}
