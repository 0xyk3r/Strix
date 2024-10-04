package cn.projectan.strix.core.ss.handler;

import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.utils.ServletUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author ProjectAn
 * @since 2024/4/6 下午4:32
 */
@Component
@RequiredArgsConstructor
public class SystemManagerLoginFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        String result = objectMapper.writeValueAsString(RetBuilder.error(exception.getMessage()));
        ServletUtils.write(response, result);
    }

}
