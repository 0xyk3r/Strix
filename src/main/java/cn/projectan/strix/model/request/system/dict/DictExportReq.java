package cn.projectan.strix.model.request.system.dict;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * @author ProjectAn
 * @since 2026-04-19
 */
@Schema(description = "字典导出请求")
@Data
public class DictExportReq {

    @Schema(description = "要导出的字典 Key 列表")
    @NotEmpty(message = "导出列表不能为空")
    private List<String> dictKeys;

}
