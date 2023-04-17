package cn.projectan.strix.core.ss.error;

import cn.projectan.strix.core.ret.RetCode;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.utils.I18nUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author 安炯奕
 * @date 2023/2/25 0:53
 */
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter out = response.getWriter();
        RetResult<Object> res = RetMarker.makeErrRsp(RetCode.NOT_LOGIN, I18nUtil.getMessage("error.not_login"));
        out.write(objectMapper.writeValueAsString(res));
        out.flush();
        out.close();
    }

}
