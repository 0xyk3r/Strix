package cn.projectan.strix.core.ss.error;

import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetCode;
import cn.projectan.strix.utils.I18nUtil;
import cn.projectan.strix.utils.ServletUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author ProjectAn
 * @date 2023/2/25 0:52
 */
@Component
@RequiredArgsConstructor
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        ServletUtils.write(response, objectMapper.writeValueAsString(RetBuilder.error(RetCode.NOT_PERMISSION, I18nUtil.getMessage("error.not_permission"))));
    }

}
