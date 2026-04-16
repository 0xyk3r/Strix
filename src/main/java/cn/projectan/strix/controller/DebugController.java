package cn.projectan.strix.controller;

import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.annotation.IgnoreEncryption;
import cn.projectan.strix.model.response.common.CommonOperatorInfoResp;
import cn.projectan.strix.service.common.OperatorService;
import cn.projectan.strix.util.common.I18nUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 调试
 *
 * @author ProjectAn
 * @since 2022/7/29 14:19
 */
@Slf4j
@Anonymous
@IgnoreEncryption
@RestController
@RequestMapping("debug")
@ConditionalOnProperty(prefix = "spring.profiles", name = "active", havingValue = "dev")
@RequiredArgsConstructor
@Tag(name = "调试")
public class DebugController extends BaseController {

    private final OperatorService operatorService;

    @Operation(summary = "查询操作人信息")
    @GetMapping("operator/{operatorType}/{operatorId}")
    public RetResult<CommonOperatorInfoResp> queryOperatorInfo(@Parameter(description = "操作人类型") @PathVariable Short operatorType, @Parameter(description = "操作人 ID") @PathVariable String operatorId) {
        CommonOperatorInfoResp operatorInfoResp = operatorService.queryOperatorInfo(operatorType, operatorId);
        Assert.notNull(operatorInfoResp, I18nUtil.notFound("field.systemManager"));
        return RetBuilder.success(operatorInfoResp);
    }

}
