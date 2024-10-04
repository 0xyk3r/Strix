package cn.projectan.strix.model.db;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * <p>
 * Strix OSS 文件
 * </p>
 *
 * @author ProjectAn
 * @since 2022-03-09
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_oss_file")
public class OssFile extends BaseModel<OssFile> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * OSS 配置 Key
     */
    private String configKey;

    /**
     * 文件组 Key
     */
    private String groupKey;

    /**
     * OSS 文件路径
     */
    private String path;

    /**
     * 文件大小 单位:字节
     */
    private Long size;

    /**
     * 图片扩展名
     */
    private String ext;

    /**
     * 上传者 id
     */
    private String uploaderId;

}
