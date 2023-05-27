package cn.projectan.strix.model.db;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;

/**
 * <p>
 *
 * </p>
 *
 * @author 安炯奕
 * @since 2022-03-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_oss_file")
public class OssFile extends BaseModel {

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
