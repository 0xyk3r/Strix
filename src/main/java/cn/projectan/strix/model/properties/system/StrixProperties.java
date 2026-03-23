package cn.projectan.strix.model.properties.system;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Strix 核心配置属性
 * <p>
 * 整合散落在各处的 strix.* 配置项，提供类型安全的统一访问。
 *
 * @author ProjectAn
 */
@Data
@Validated
@ConfigurationProperties(prefix = "strix")
public class StrixProperties {

    /**
     * 默认语言区域
     */
    @NotBlank
    private String defaultLocale = "zh_CN";

    /**
     * 是否在日志中展示响应体（调试用）
     */
    private boolean showResponse = false;

    /**
     * 是否在日志中展示请求体（调试用）
     */
    private boolean showRequest = false;

    /**
     * 字段加密配置
     */
    private Encrypt encrypt = new Encrypt();

    /**
     * 安全配置
     */
    private Security security = new Security();

    /**
     * OSS 配置
     */
    private Oss oss = new Oss();

    @Data
    public static class Encrypt {
        private Field field = new Field();

        @Data
        public static class Field {
            /**
             * SM4 字段加密密钥（必须 16 字节）
             */
            private String key = "Strix@FieldCrypt";
        }
    }

    @Data
    public static class Security {
        /**
         * API 签名时间戳有效窗口（毫秒）
         */
        @Positive
        private long timestampWindow = 60_000L;
    }

    @Data
    public static class Oss {
        /**
         * 默认图片 URL（图片不存在时的占位图）
         */
        private String defaultImageUrl = "";
    }

}
