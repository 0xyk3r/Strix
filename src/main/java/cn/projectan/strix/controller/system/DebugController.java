package cn.projectan.strix.controller.system;

import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.job.PopularityJob;
import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.annotation.IgnoreDataEncryption;
import cn.projectan.strix.utils.PopularityUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    @GetMapping("test")
    public RetResult<Object> test(HttpServletRequest request) throws JsonProcessingException {
        popularityUtil.addPopularity("test", "1");
        popularityUtil.addPopularity("test", "1");
        popularityUtil.addPopularity("test", "2");
        popularityUtil.addPopularity("test", "3");
        popularityUtil.addPopularity("test", "1");
        popularityUtil.addPopularity("test", "1");
        popularityUtil.addPopularity("test", "2");
        popularityUtil.addPopularity("test", "3");

        List<String> list =
                List.of(
                        popularityUtil.getPopularity("test", "1"),
                        popularityUtil.getPopularity("test", "2"),
                        popularityUtil.getPopularity("test", "3")
                );

        return RetMarker.makeSuccessRsp(list);
    }

}
