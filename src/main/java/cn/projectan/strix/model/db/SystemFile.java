package cn.projectan.strix.model.db;

import com.baomidou.mybatisplus.annotation.TableName;
import cn.projectan.strix.model.db.base.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 
 * </p>
 *
 * @author 安炯奕
 * @since 2022-03-09
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tab_system_file")
public class SystemFile extends BaseModel {

    private static final long serialVersionUID = 1L;

    private String ossId;

    /**
     * 分组用
     */
    private String imageGroup;

    /**
     * OSS访问地址
     */
    private String imageUrl;

    /**
     * 图片大小
     */
    private Integer imageSize;

    /**
     * 图片格式
     */
    private String imageType;

    /**
     * 1管理端上传 2可信终端上传 3用户端上传
     */
    private Integer imageUploaderType;

    /**
     * 上传者id
     */
    private String imageUploaderId;


}
