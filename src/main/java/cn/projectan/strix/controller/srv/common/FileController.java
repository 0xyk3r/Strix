package cn.projectan.strix.controller.srv.common;

import cn.projectan.strix.controller.srv.base.BaseSrvController;
import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.annotation.IgnoreEncryption;
import cn.projectan.strix.service.system.OssFileService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
@RestController("SrvCommonFileController")
@RequestMapping("srv/common/file")
@RequiredArgsConstructor
public class FileController extends BaseSrvController {

    private final OssFileService ossFileService;

    @Value("${strix.oss.default-image-url:}")
    private String defaultImageUrl;

    /**
     * 获取图片
     */
    @Anonymous
    @GetMapping("{fileId}")
    @IgnoreEncryption
    public void getImage(@PathVariable String fileId, HttpServletResponse response) throws Exception {
        // 注意权限验证
        response.setContentType("image/jpeg");
        response.sendRedirect(ossFileService.getUrl(fileId, defaultImageUrl));
    }

}
