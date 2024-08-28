package cn.projectan.strix.controller.system.common;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.annotation.IgnoreDataEncryption;
import cn.projectan.strix.service.OssFileService;
import cn.projectan.strix.utils.tempurl.TempUrlUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * @author ProjectAn
 * @date 2024/8/15 18:42
 */
@Slf4j
@RestController("SystemCommonTempUrlController")
@RequestMapping("system/common/url")
@RequiredArgsConstructor
public class TempUrlController extends BaseSystemController {

    private final TempUrlUtil tempUrlUtil;
    private final OssFileService ossFileService;

    @Anonymous
    @IgnoreDataEncryption
    @GetMapping("file/{key}")
    public void file(@PathVariable String key, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String tempFilePath = tempUrlUtil.get(key);
        File file = new File(tempFilePath);
        Assert.isTrue(file.exists(), "文件不存在");
        String ext = FileUtil.extName(file);
        String mimeType = FileUtil.getMimeType(file.getPath());

        String filenameEncoded = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8).replace("+", "%20");

        // 设置响应头，明确指定文件名和扩展名
        response.setHeader("Content-Type", mimeType);
        response.setHeader("Content-Disposition", "attachment; filename=" + filenameEncoded);

        try (FileInputStream fis = new FileInputStream(tempFilePath);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        } catch (Exception e) {
            log.error("临时文件下载失败, 文件路径: {}, 错误信息: {}", tempFilePath, e.getMessage(), e);
        } finally {
            // 删除临时文件
            if (!FileUtil.del(file)) {
                log.warn("临时文件删除失败: {}", tempFilePath);
            }
        }
    }

}
