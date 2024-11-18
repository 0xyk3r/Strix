package cn.projectan.strix.controller.system.common;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.util.IDMapperUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据 ID 映射器
 *
 * @author ProjectAn
 * @since 2024-11-13 08:22:33
 */
@Slf4j
@RestController("SystemIDMapperController")
@RequestMapping("system/common/idmapper")
@RequiredArgsConstructor
public class IDMapperController extends BaseSystemController {

    private final IDMapperUtil idMapperUtil;

    @GetMapping("{dataType}/{dataId}")
    public RetResult<Object> idMapper(@PathVariable String dataType, @PathVariable String dataId) {
        String s = idMapperUtil.get(dataType, dataId);
        return RetBuilder.success(s);
    }

}
