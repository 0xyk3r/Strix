package cn.projectan.strix.controller.wechat;

import cn.projectan.strix.controller.wechat.base.BaseWechatController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.annotation.IgnoreDataEncryption;
import cn.projectan.strix.model.db.OssFile;
import cn.projectan.strix.service.OssFileService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 文件
 *
 * @author ProjectAn
 * @since 2022/3/9 11:05
 */
@Slf4j
@RestController
@RequestMapping(value = {"wechat/file", "wechat/{wechatConfigId}/file"})
@RequiredArgsConstructor
public class FileController extends BaseWechatController {

    private final OssFileService ossFileService;

    /**
     * 上传图片
     */
    @IgnoreDataEncryption
    @PostMapping("upload")
    public RetResult<Object> uploadImage(String imageBase64) {
        Assert.hasText(imageBase64, "参数错误");

        OssFile ossFile = ossFileService.upload("Wechat", imageBase64);
        return RetBuilder.success(Map.of("fileId", ossFile.getId()));
    }

    /**
     * 获取图片
     */
    @Anonymous
    @IgnoreDataEncryption
    @GetMapping("{fileId}")
    public void getImage(@PathVariable String fileId, HttpServletResponse response) throws Exception {
        response.setContentType("image/jpeg");
        // TODO 增加权限验证?
        response.sendRedirect(ossFileService.getUrl(fileId, "https://oss.huiboche.cn/System/404.png"));
    }

}
