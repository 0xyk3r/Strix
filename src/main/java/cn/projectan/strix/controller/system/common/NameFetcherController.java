package cn.projectan.strix.controller.system.common;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.response.common.CommonNameFetcherResp;
import cn.projectan.strix.util.common.I18nUtil;
import cn.projectan.strix.util.common.NameFetcherUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据 ID 映射器
 *
 * @author ProjectAn
 * @since 2024-11-13 08:22:33
 */
@Slf4j
@RestController("SystemNameFetcherController")
@RequestMapping("system/common/namefetcher")
@RequiredArgsConstructor
@Tag(name = "通用 - 数据 ID 映射器")
public class NameFetcherController extends BaseSystemController {

    private final NameFetcherUtil nameFetcherUtil;

    @GetMapping("")
    @Operation(summary = "获取名称映射")
    @Parameters({
            @Parameter(name = "dataType", description = "数据类型", required = true),
            @Parameter(name = "dataId", description = "数据 ID", required = true)
    })
    public RetResult<CommonNameFetcherResp> nameFetcher(String dataType, String dataId) {
        Assert.hasText(dataType, I18nUtil.get("error.param.invalid"));
        Assert.hasText(dataId, I18nUtil.get("error.param.invalid"));
        String name = nameFetcherUtil.get(dataType, dataId);
        return RetBuilder.success(
                new CommonNameFetcherResp(name)
        );
    }

}
