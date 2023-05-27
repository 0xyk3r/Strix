package cn.projectan.strix.controller.wechat;

import cn.projectan.strix.controller.wechat.base.BaseWechatController;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.annotation.IgnoreDataEncryption;
import cn.projectan.strix.model.db.OssFile;
import cn.projectan.strix.service.OssFileService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author 安炯奕
 * @date 2022/3/9 11:05
 */
@Slf4j
@RestController
@RequestMapping(value = {"wechat/file", "wechat/{wechatConfigId}/file"})
public class FileController extends BaseWechatController {

    @Autowired
    private OssFileService ossFileService;

    @IgnoreDataEncryption
    @PostMapping("upload")
    public RetResult<Object> uploadImage(String imageBase64) {
        Assert.hasText(imageBase64, "参数错误");

        OssFile ossFile = ossFileService.upload("Wechat", imageBase64, getLoginWechatUserId());
        return RetMarker.makeSuccessRsp(Map.of("fileId", ossFile.getId()));
    }

    @Anonymous
    @IgnoreDataEncryption
    @GetMapping("{fileId}")
    public void getImage(@PathVariable String fileId, HttpServletResponse response) throws Exception {
        response.setContentType("image/jpeg");
        // TODO 增加权限验证?
        response.sendRedirect(ossFileService.getUrl(fileId, "https://oss.huiboche.cn/System/404.png"));
    }

}
