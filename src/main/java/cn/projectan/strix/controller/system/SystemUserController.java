package cn.projectan.strix.controller.system;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.exception.StrixUniqueCheckerException;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.system.SystemUser;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.model.dict.system.SystemUserStatus;
import cn.projectan.strix.model.enums.common.DuplicateStrategy;
import cn.projectan.strix.model.request.common.BatchImportReq;
import cn.projectan.strix.model.request.common.BatchModifyReq;
import cn.projectan.strix.model.request.common.BatchRemoveReq;
import cn.projectan.strix.model.request.common.SingleFieldModifyReq;
import cn.projectan.strix.model.request.system.user.SystemUserListReq;
import cn.projectan.strix.model.request.system.user.SystemUserUpdateReq;
import cn.projectan.strix.model.response.common.BatchImportResp;
import cn.projectan.strix.model.response.common.BatchImportResp.ImportError;
import cn.projectan.strix.model.response.system.user.SystemUserListResp;
import cn.projectan.strix.model.response.system.user.SystemUserResp;
import cn.projectan.strix.service.system.SystemUserService;
import cn.projectan.strix.util.common.I18nUtil;
import cn.projectan.strix.util.common.ObjectMapperUtil;
import cn.projectan.strix.util.common.UniqueChecker;
import cn.projectan.strix.util.common.UpdateBuilder;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 系统用户
 *
 * @author ProjectAn
 * @since 2021/8/27 14:20
 */
@Slf4j
@RestController
@RequestMapping("system/user")
@RequiredArgsConstructor
@Tag(name = "系统 - 用户管理")
public class SystemUserController extends BaseSystemController {

    private final SystemUserService systemUserService;
    private final Validator validator;

    /**
     * 查询用户列表
     */
    @Operation(summary = "用户列表")
    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:user')")
    @StrixLog(operationGroup = "系统用户", operationName = "查询用户列表")
    public RetResult<SystemUserListResp> getSystemUserList(SystemUserListReq req) {
        Page<SystemUser> page = systemUserService.listPage(req);

        SystemUserListResp resp = new SystemUserListResp(page.getRecords(), page.getTotal());

        return RetBuilder.success(resp);
    }

    /**
     * 查询用户信息
     */
    @Operation(summary = "用户详情")
    @GetMapping("{userId}")
    @PreAuthorize("@ss.hasPermission('system:user')")
    @StrixLog(operationGroup = "系统用户", operationName = "查询用户信息")
    public RetResult<SystemUserResp> getSystemUser(@Parameter(description = "用户 ID") @PathVariable String userId) {
        SystemUser systemUser = systemUserService.getById(userId);
        Assert.notNull(systemUser, I18nUtil.notFound("field.systemUser"));

        return RetBuilder.success(new SystemUserResp(systemUser));
    }

    /**
     * 修改用户信息
     */
    @Operation(summary = "修改用户字段")
    @PostMapping("modify/{userId}")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    @StrixLog(operationGroup = "系统用户", operationName = "更改用户信息", operationType = SystemLogOperType.UPDATE)
    public RetResult<Void> modifyField(@Parameter(description = "用户 ID") @PathVariable String userId, @RequestBody @Validated SingleFieldModifyReq req) {
        Assert.hasText(req.getField(), "参数错误");
        SystemUser systemUser = systemUserService.getById(userId);
        Assert.notNull(systemUser, I18nUtil.notFound("field.systemUser"));

        LambdaUpdateWrapper<SystemUser> queryWrapper = new LambdaUpdateWrapper<>();
        queryWrapper.eq(SystemUser::getId, userId);

        switch (req.getField()) {
            case "nickname" -> queryWrapper.set(SystemUser::getNickname, req.getValue());
            case "status" -> {
                Assert.isTrue(SystemUserStatus.valid(Short.parseShort(req.getValue())), "参数错误");
                queryWrapper.set(SystemUser::getStatus, req.getValue());
            }
            case "phoneNumber" -> queryWrapper.set(SystemUser::getPhoneNumber, req.getValue());
            default -> {
                return RetBuilder.error(I18nUtil.get("error.param.invalid"));
            }
        }

        Assert.isTrue(systemUserService.update(queryWrapper), "修改失败");

        return RetBuilder.success();
    }

    /**
     * 新增用户
     */
    @Operation(summary = "新增用户")
    @PostMapping("update")
    @PreAuthorize("@ss.hasPermission('system:user:add')")
    @StrixLog(operationGroup = "系统用户", operationName = "新增用户", operationType = SystemLogOperType.ADD)
    public RetResult<Void> update(@RequestBody @Validated(InsertGroup.class) SystemUserUpdateReq req) {
        Assert.notNull(req, I18nUtil.get("error.param.invalid"));

        SystemUser systemUser = new SystemUser(
                req.getNickname(),
                req.getStatus(),
                req.getPhoneNumber(),
                null,
                null
        );

        UniqueChecker.check(systemUser);

        Assert.isTrue(systemUserService.save(systemUser), "保存失败");

        return RetBuilder.success();
    }

    /**
     * 修改用户
     */
    @Operation(summary = "编辑用户")
    @PostMapping("update/{userId}")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    @StrixLog(operationGroup = "系统用户", operationName = "修改用户", operationType = SystemLogOperType.UPDATE)
    public RetResult<Void> update(@Parameter(description = "用户 ID") @PathVariable String userId, @RequestBody @Validated(UpdateGroup.class) SystemUserUpdateReq req) {
        Assert.notNull(req, I18nUtil.get("error.param.invalid"));
        SystemUser systemUser = systemUserService.getById(userId);
        Assert.notNull(systemUser, I18nUtil.notFound("field.systemUser"));

        LambdaUpdateWrapper<SystemUser> updateWrapper = UpdateBuilder.build(systemUser, req);
        UniqueChecker.check(systemUser);
        Assert.isTrue(systemUserService.update(updateWrapper), "保存失败");

        return RetBuilder.success();
    }

    /**
     * 删除用户
     */
    @Operation(summary = "删除用户")
    @PostMapping("remove/{userId}")
    @PreAuthorize("@ss.hasPermission('system:user:remove')")
    @StrixLog(operationGroup = "系统用户", operationName = "删除用户", operationType = SystemLogOperType.DELETE)
    public RetResult<Void> remove(@Parameter(description = "用户 ID") @PathVariable String userId) {
        SystemUser systemUser = systemUserService.getById(userId);
        Assert.notNull(systemUser, I18nUtil.notFound("field.systemUser"));

        systemUserService.deleteUserWithRelations(systemUser);

        return RetBuilder.success();
    }

    /**
     * 批量删除用户
     */
    @Operation(summary = "批量删除用户")
    @PostMapping("batch/remove")
    @PreAuthorize("@ss.hasPermission('system:user:remove')")
    @StrixLog(operationGroup = "系统用户", operationName = "批量删除用户", operationType = SystemLogOperType.DELETE)
    public RetResult<Void> batchRemove(@RequestBody @Validated BatchRemoveReq req) {
        List<SystemUser> users = systemUserService.listByIds(req.getIds());
        Assert.notEmpty(users, I18nUtil.notFound("field.systemUser"));

        for (SystemUser user : users) {
            systemUserService.deleteUserWithRelations(user);
        }

        return RetBuilder.success();
    }

    /**
     * 批量修改用户字段
     */
    @Operation(summary = "批量修改用户字段")
    @PostMapping("batch/modify")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    @StrixLog(operationGroup = "系统用户", operationName = "批量修改用户字段", operationType = SystemLogOperType.UPDATE)
    public RetResult<Void> batchModify(@RequestBody @Validated BatchModifyReq req) {
        Assert.hasText(req.getField(), "参数错误");

        switch (req.getField()) {
            case "status" -> {
                Assert.isTrue(SystemUserStatus.valid(Short.parseShort(req.getValue())), "参数错误");
                systemUserService.lambdaUpdate()
                        .in(SystemUser::getId, req.getIds())
                        .set(SystemUser::getStatus, req.getValue())
                        .update();
            }
            default -> {
                return RetBuilder.error(I18nUtil.get("error.param.invalid"));
            }
        }

        return RetBuilder.success();
    }

    /**
     * 批量导入用户
     */
    @Operation(summary = "批量导入用户")
    @PostMapping("batch/create")
    @PreAuthorize("@ss.hasPermission('system:user:add')")
    @StrixLog(operationGroup = "系统用户", operationName = "批量导入用户", operationType = SystemLogOperType.ADD)
    public RetResult<BatchImportResp> batchCreate(@RequestBody @Validated BatchImportReq req) {
        DuplicateStrategy strategy = DuplicateStrategy.fromString(req.getDuplicateStrategy());
        List<ImportError> errors = new ArrayList<>();
        int successCount = 0;
        int skippedCount = 0;

        // 1. 批量反序列化 + 校验
        List<SystemUserUpdateReq> validItems = new ArrayList<>();
        for (int i = 0; i < req.getItems().size(); i++) {
            Map<String, Object> itemMap = req.getItems().get(i);
            try {
                SystemUserUpdateReq itemReq = ObjectMapperUtil.get().convertValue(itemMap, SystemUserUpdateReq.class);
                Set<ConstraintViolation<SystemUserUpdateReq>> violations = validator.validate(itemReq, InsertGroup.class);
                if (!violations.isEmpty()) {
                    for (ConstraintViolation<SystemUserUpdateReq> v : violations) {
                        errors.add(new ImportError(i, v.getPropertyPath().toString(), v.getMessage()));
                    }
                    continue;
                }
                validItems.add(itemReq);
            } catch (Exception e) {
                errors.add(new ImportError(i, "parse", e.getMessage()));
            }
        }

        // 2. 批量预加载已存在的用户
        List<String> phoneNumbers = validItems.stream()
                .map(SystemUserUpdateReq::getPhoneNumber)
                .distinct()
                .collect(Collectors.toList());
        Map<String, SystemUser> existingMap = systemUserService.getByPhoneNumbers(phoneNumbers);

        // 3. 分流: 新增 vs 更新
        List<SystemUser> toSave = new ArrayList<>();
        List<SystemUser> toUpdate = new ArrayList<>();

        for (int i = 0; i < validItems.size(); i++) {
            SystemUserUpdateReq itemReq = validItems.get(i);
            SystemUser existing = existingMap.get(itemReq.getPhoneNumber());

            if (existing != null) {
                if (strategy == DuplicateStrategy.SKIP) {
                    skippedCount++;
                    errors.add(new ImportError(i, "phoneNumber", "手机号码已存在，已跳过"));
                    continue;
                }
                existing.setNickname(itemReq.getNickname());
                existing.setStatus(itemReq.getStatus());
                try {
                    UniqueChecker.check(existing);
                } catch (StrixUniqueCheckerException e) {
                    errors.add(new ImportError(i, "unique", e.getMessage()));
                    continue;
                }
                toUpdate.add(existing);
            } else {
                SystemUser systemUser = new SystemUser(
                        itemReq.getNickname(),
                        itemReq.getStatus(),
                        itemReq.getPhoneNumber(),
                        null,
                        null
                );
                try {
                    UniqueChecker.check(systemUser);
                } catch (StrixUniqueCheckerException e) {
                    errors.add(new ImportError(i, "unique", e.getMessage()));
                    continue;
                }
                toSave.add(systemUser);
            }
        }

        // 4. 批量写入
        if (!toSave.isEmpty()) {
            systemUserService.saveBatch(toSave);
            successCount += toSave.size();
        }
        if (!toUpdate.isEmpty()) {
            systemUserService.updateBatchById(toUpdate);
            successCount += toUpdate.size();
        }

        int failedCount = req.getItems().size() - successCount - skippedCount;
        return RetBuilder.success(new BatchImportResp(req.getItems().size(), successCount, failedCount, skippedCount, errors));
    }

}
