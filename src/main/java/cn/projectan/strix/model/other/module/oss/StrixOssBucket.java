package cn.projectan.strix.model.other.module.oss;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author ProjectAn
 * @since 2023/5/23 10:52
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StrixOssBucket {

    private String name;

    private String publicEndpoint;

    private String privateEndpoint;

    private String region;

    private String storageClass;

    private LocalDateTime createdTime;

}
