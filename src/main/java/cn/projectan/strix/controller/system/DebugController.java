package cn.projectan.strix.controller.system;

import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.job.PopularityJob;
import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.annotation.IgnoreDataEncryption;
import cn.projectan.strix.utils.PopularityUtil;
import cn.projectan.strix.utils.async.CompletableUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    private final PopularityUtil popularityUtil;
    private final PopularityJob popularityJob;

    @IgnoreDataEncryption
    @GetMapping("test/{key}")
    public RetResult<Object> test(@PathVariable String key, HttpServletRequest request) throws JsonProcessingException {

        CompletableUtil.allOf(
                () -> {
                    log.info("test1");
                    try {
                        Thread.sleep(5000);
                        log.info("5000");
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                },
                () -> {
                    log.info("test2");
                    try {
                        Thread.sleep(7000);
                        log.info("7000");
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                },
                () -> {
                    log.info("test3");
                    try {
                        Thread.sleep(3000);
                        log.info("3000");
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                },
                () -> {
                    log.info("test4");
                    try {
                        Thread.sleep(9000);
                        log.info("9000");
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
        );

        return RetMarker.makeSuccessRsp();
    }

}
