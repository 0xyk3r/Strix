package cn.projectan.strix.controller.api.common;

import cn.projectan.strix.controller.api.base.BaseApiController;
import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.annotation.IgnoreDataEncryption;
import cn.projectan.strix.service.OssFileService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件
 *
 * @author ProjectAn
 * @since 2023/5/26 21:57
 */
@Slf4j
@RestController("ApiCommonFileController")
@RequestMapping("api/common/file")
@RequiredArgsConstructor
public class FileController extends BaseApiController {

    private final OssFileService ossFileService;

    /**
     * 获取图片
     */
    @Anonymous
    @GetMapping("{fileId}")
    @IgnoreDataEncryption
    public void getImage(@PathVariable String fileId, HttpServletResponse response) throws Exception {
        // TODO 权限验证
        response.setContentType("image/jpeg");
        response.sendRedirect(ossFileService.getUrl(fileId, "https://oss.huiboche.cn/System/404.png"));
    }

}
