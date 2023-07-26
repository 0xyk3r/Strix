package cn.projectan.strix.controller.system;

import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.annotation.IgnoreDataEncryption;
import cn.projectan.strix.utils.ip.IpLocationUtil;
import cn.projectan.strix.utils.ip.IpUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 安炯奕
 * @date 2022/7/29 14:19
 */
@Slf4j
@Anonymous
@RestController
@RequestMapping("debug")
@ConditionalOnProperty(prefix = "spring.profiles", name = "active", havingValue = "dev")
@RequiredArgsConstructor
public class DebugController {

    private final ObjectMapper objectMapper;

    @IgnoreDataEncryption
    @GetMapping("ip")
    public RetResult<Object> getMyIpAddress(HttpServletRequest request) throws JsonProcessingException {
        long start = System.nanoTime();
        Map<String, String> result = new HashMap<>();
        String ip = IpUtils.getIpAddr(request);
        String address = IpLocationUtil.getLocation(ip);
        result.put("ip", ip);
        result.put("address", address);
        long end = System.nanoTime();
        result.put("time", String.valueOf((end - start) / 1000000));
        System.out.println(objectMapper.writeValueAsString(result));
        return RetMarker.makeSuccessRsp(result);
    }

}
