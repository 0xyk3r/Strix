package cn.projectan.strix.controller.wechat;

import cn.hutool.core.map.MapUtil;
import cn.projectan.strix.controller.wechat.base.BaseWechatController;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.IgnoreDataEncryption;
import cn.projectan.strix.service.SystemFileService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

/**
 * @author 安炯奕
 * @date 2022/3/9 11:05
 */
@Slf4j
@RestController
@RequestMapping(value = {"wechat/file", "wechat/{wechatConfigId}/file"})
public class FileController extends BaseWechatController {

    @Autowired
    private SystemFileService systemFileService;

    @IgnoreDataEncryption
    @PostMapping("upload/{imageGroup}")
    public RetResult<HashMap<String, String>> uploadImage(@PathVariable String imageGroup, String imageBase64) {
        Assert.hasText(imageGroup, "参数错误-1");
        Assert.hasText(imageBase64, "参数错误-2");

        String fileId = systemFileService.uploadImage("default", imageGroup, imageBase64, 3, getLoginWechatUserId());

        return RetMarker.makeSuccessRsp(MapUtil.of("fileId", fileId));
    }

    @IgnoreDataEncryption
    @GetMapping("get/{imageGroup}/{fileId}")
    public void getImage(@PathVariable String imageGroup, @PathVariable String fileId, HttpServletResponse response) throws Exception {
        response.setContentType("image/jpeg");
        // TODO 增加权限验证
        response.sendRedirect(systemFileService.getImageUrl("default", imageGroup, fileId, 3));
    }

}
