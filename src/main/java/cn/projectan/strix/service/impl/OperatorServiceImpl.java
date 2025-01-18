package cn.projectan.strix.service.impl;

import cn.projectan.strix.model.constant.OperatorType;
import cn.projectan.strix.model.constant.RedisKeyConstants;
import cn.projectan.strix.model.db.SystemManager;
import cn.projectan.strix.model.db.SystemUser;
import cn.projectan.strix.model.response.common.CommonOperatorInfoResp;
import cn.projectan.strix.service.OperatorService;
import cn.projectan.strix.service.SystemManagerService;
import cn.projectan.strix.service.SystemUserService;
import cn.projectan.strix.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author ProjectAn
 * @since 2025-01-18 10:21:33
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperatorServiceImpl implements OperatorService {

    private final RedisUtil redisUtil;
    private final SystemManagerService systemManagerService;
    private final SystemUserService systemUserService;

    @Override
    public CommonOperatorInfoResp queryOperatorInfo(short operatorType, String operatorId) {
        switch (operatorType) {
            case OperatorType.NONE -> {
                return new CommonOperatorInfoResp(null, OperatorType.NONE, "未知", null);
            }
            case OperatorType.SYSTEM -> {
                return new CommonOperatorInfoResp(null, OperatorType.SYSTEM, "系统", null);
            }
            case OperatorType.SYSTEM_MANAGER -> {
                Object o = redisUtil.hGet(RedisKeyConstants.HASH_OPERATOR_INFO_PREFIX + operatorType, operatorId);
                if (o instanceof CommonOperatorInfoResp resp) {
                    return resp;
                } else {
                    SystemManager systemManager = systemManagerService.getById(operatorId);
                    if (systemManager != null) {
                        CommonOperatorInfoResp resp = new CommonOperatorInfoResp(systemManager.getId(), OperatorType.SYSTEM_MANAGER, systemManager.getNickname(), systemManager);
                        redisUtil.hSet(RedisKeyConstants.HASH_OPERATOR_INFO_PREFIX + operatorType, operatorId, resp);
                        return resp;
                    }
                }
            }
            case OperatorType.SYSTEM_USER -> {
                Object o = redisUtil.hGet(RedisKeyConstants.HASH_OPERATOR_INFO_PREFIX + operatorType, operatorId);
                if (o instanceof CommonOperatorInfoResp resp) {
                    return resp;
                } else {
                    SystemUser systemUser = systemUserService.getById(operatorId);
                    if (systemUser != null) {
                        CommonOperatorInfoResp resp = new CommonOperatorInfoResp(systemUser.getId(), OperatorType.SYSTEM_USER, systemUser.getNickname(), systemUser);
                        redisUtil.hSet(RedisKeyConstants.HASH_OPERATOR_INFO_PREFIX + operatorType, operatorId, resp);
                        return resp;
                    }
                }
            }
            default -> {
                log.warn("未知的操作人员类型：{}", operatorType);
                return new CommonOperatorInfoResp(null, OperatorType.NONE, "未知", null);
            }
        }
        return null;
    }

}
