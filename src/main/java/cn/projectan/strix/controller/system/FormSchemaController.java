package cn.projectan.strix.controller.system;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.schema.FormSchemaResp;
import cn.projectan.strix.core.schema.FormSchemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 表单 Schema 控制器
 * <p>
 * 所有已认证用户均可访问, 依赖 Spring Security 的默认 anyRequest().authenticated()
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@RestController
@RequestMapping("system/form-schema")
@RequiredArgsConstructor
@Tag(name = "系统 - 表单 Schema")
public class FormSchemaController extends BaseSystemController {

    private final FormSchemaService formSchemaService;

    @GetMapping("{dtoName}")
    @Operation(summary = "获取表单校验 Schema")
    public RetResult<FormSchemaResp> getSchema(@PathVariable String dtoName) {
        return RetBuilder.success(formSchemaService.getSchema(dtoName));
    }
}
