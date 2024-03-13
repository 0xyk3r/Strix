package cn.projectan.strix.model.system;

import com.aliyun.oss.OSS;
import lombok.Data;

/**
 * @author ProjectAn
 * @date 2021/5/2 17:28
 */
@Data
public class AliyunOssInstance implements java.io.Serializable {

    private OSS publicInstance;

    private OSS privateInstance;

}
