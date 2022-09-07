package cn.projectan.strix.controller.system;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.NeedSystemPermission;
import cn.projectan.strix.service.SystemDictService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 安炯奕
 * @date 2022/4/4 23:43
 */
@Slf4j
@RestController
@RequestMapping("system/dict")
public class SystemDictController extends BaseSystemController {

    @Autowired
    private SystemDictService systemDictService;

    @GetMapping("{dictKey}")
    @NeedSystemPermission
    public RetResult<String> getSystemManagerList(@PathVariable String dictKey) {
        Assert.hasText(dictKey, "参数错误");
        String value = systemDictService.getDict(dictKey);
        Assert.hasText(value, "字典未找到");

        return RetMarker.makeSuccessRsp(value);
    }

}
