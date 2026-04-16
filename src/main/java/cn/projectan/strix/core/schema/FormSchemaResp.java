package cn.projectan.strix.core.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 表单 Schema 响应
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Schema(description = "表单 Schema 响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormSchemaResp {

    @Schema(description = "DTO 名称")
    private String dtoName;

    @Schema(description = "字段 Schema 映射")
    private Map<String, FieldSchema> fields;
}
