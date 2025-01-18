package cn.projectan.strix.service;

import cn.projectan.strix.model.response.common.CommonOperatorInfoResp;

/**
 * @author ProjectAn
 * @since 2025-01-18 10:21:10
 */
public interface OperatorService {

    CommonOperatorInfoResp queryOperatorInfo(short operatorType, String operatorId);

}
