package cn.projectan.strix.controller.system.common;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.util.NameFetcherUtil;
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
public class NameFetcherController extends BaseSystemController {

    private final NameFetcherUtil nameFetcherUtil;

    @GetMapping("")
    public RetResult<Object> nameFetcher(String dataType, String dataId) {
        Assert.hasText(dataType, "参数错误");
        Assert.hasText(dataId, "参数错误");
        String s = nameFetcherUtil.get(dataType, dataId);
        return RetBuilder.success(s);
    }

}
