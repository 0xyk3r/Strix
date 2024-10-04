package cn.projectan.strix.model.response.module.oss;

import cn.projectan.strix.model.db.OssFile;
import cn.projectan.strix.model.response.base.BasePageResp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ProjectAn
 * @since 2023/5/26 20:45
 */
@Getter
public class OssFileListResp extends BasePageResp {

    private final List<OssFileItem> files;

    public OssFileListResp(List<OssFile> data, Long total) {
        files = data.stream().map(d ->
                new OssFileItem(d.getId(), d.getConfigKey(), d.getGroupKey(), d.getPath(), d.getSize(), d.getExt(), d.getUploaderId(), d.getCreateTime())
        ).collect(Collectors.toList());
        this.setTotal(total);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OssFileItem {

        private String id;

        private String configKey;

        private String groupKey;

        private String path;

        private Long size;

        private String ext;

        private String uploaderId;

        private LocalDateTime createTime;

    }

}
