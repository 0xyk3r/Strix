package cn.projectan.strix.service.common;

import cn.projectan.strix.model.constant.system.OperatorType;
import cn.projectan.strix.model.constant.system.StrixRedisKeyConst;
import cn.projectan.strix.model.db.system.SystemManager;
import cn.projectan.strix.model.db.system.SystemUser;
import cn.projectan.strix.model.response.common.CommonOperatorInfoResp;
import cn.projectan.strix.service.system.SystemManagerService;
import cn.projectan.strix.service.system.SystemUserService;
import cn.projectan.strix.util.common.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 操作人员服务
 *
 * @author ProjectAn
 * @since 2025-01-18 10:21:33
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperatorService {

    private final RedisUtil redisUtil;
    private final SystemManagerService systemManagerService;
    private final SystemUserService systemUserService;

    /**
     * 查询操作人员信息
     *
     * @param operatorType 操作人员类型
     * @param operatorId   操作人员 ID
     * @return 操作人员信息
     */
    public CommonOperatorInfoResp queryOperatorInfo(short operatorType, String operatorId) {
        switch (operatorType) {
            case OperatorType.NONE -> {
                return new CommonOperatorInfoResp(null, OperatorType.NONE, "未知", null);
            }
            case OperatorType.SYSTEM -> {
                return new CommonOperatorInfoResp(null, OperatorType.SYSTEM, "系统", null);
            }
            case OperatorType.SYSTEM_MANAGER -> {
                Object o = redisUtil.hGet(StrixRedisKeyConst.HASH_OPERATOR_INFO_PREFIX + operatorType, operatorId);
                if (o instanceof CommonOperatorInfoResp resp) {
                    return resp;
                }
                SystemManager systemManager = systemManagerService.getById(operatorId);
                if (systemManager != null) {
                    CommonOperatorInfoResp resp = new CommonOperatorInfoResp(systemManager.getId(), OperatorType.SYSTEM_MANAGER, systemManager.getNickname(), systemManager);
                    redisUtil.hSet(StrixRedisKeyConst.HASH_OPERATOR_INFO_PREFIX + operatorType, operatorId, resp);
                    return resp;
                }
                return new CommonOperatorInfoResp(operatorId, OperatorType.SYSTEM_MANAGER, "未知管理员", null);
            }
            case OperatorType.SYSTEM_USER -> {
                Object o = redisUtil.hGet(StrixRedisKeyConst.HASH_OPERATOR_INFO_PREFIX + operatorType, operatorId);
                if (o instanceof CommonOperatorInfoResp resp) {
                    return resp;
                }
                SystemUser systemUser = systemUserService.getById(operatorId);
                if (systemUser != null) {
                    CommonOperatorInfoResp resp = new CommonOperatorInfoResp(systemUser.getId(), OperatorType.SYSTEM_USER, systemUser.getNickname(), systemUser);
                    redisUtil.hSet(StrixRedisKeyConst.HASH_OPERATOR_INFO_PREFIX + operatorType, operatorId, resp);
                    return resp;
                }
                return new CommonOperatorInfoResp(operatorId, OperatorType.SYSTEM_USER, "未知用户", null);
            }
            default -> {
                log.warn("未知的操作人员类型：{}", operatorType);
                return new CommonOperatorInfoResp(null, OperatorType.NONE, "未知", null);
            }
        }
    }

}
