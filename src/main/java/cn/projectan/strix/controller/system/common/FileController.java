package cn.projectan.strix.controller.system.common;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.model.constant.StrixOssFileGroupSecretType;
import cn.projectan.strix.service.OssFileService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 安炯奕
 * @date 2023/5/26 21:57
 */
@Slf4j
@RestController("SystemCommonFileController")
@RequestMapping("system/common/file")
public class FileController extends BaseSystemController {

    @Autowired
    private OssFileService ossFileService;

    @GetMapping("{fileId}")
    public void getImage(@PathVariable String fileId, HttpServletResponse response) throws Exception {
        response.setContentType("image/jpeg");
        response.sendRedirect(ossFileService.getUrl(fileId, StrixOssFileGroupSecretType.MANAGER, getLoginManagerId(), "https://oss.huiboche.cn/System/404.png"));
    }

}
