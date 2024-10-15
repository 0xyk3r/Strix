package cn.projectan.strix.model.other.module.workflow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 节点配置
 *
 * @author ProjectAn
 * @date 2024/3/21 12:25
 */
public class WorkflowProps {

    /**
     * 审批节点配置
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApprovalWorkflowProps extends WorkflowProps {

        /**
         * 审批人
         */
        private Assign assign;
        /**
         * 审批时限
         */
        private TimeLimit timeLimit;
        /**
         * 拒绝后操作
         */
        private Reject reject;

    }

    /**
     * 办理节点配置
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskWorkflowProps extends WorkflowProps {

        /**
         * 办理人
         */
        private Assign assign;
        /**
         * 办理时限
         */
        private TimeLimit timeLimit;

    }

    /**
     * 抄送节点配置
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CcWorkflowProps extends WorkflowProps {

        /**
         * 抄送人
         */
        private Assign assign;
        /**
         * 允许发起人添加
         */
        private Boolean allowAdd;

    }

    /**
     * 条件节点配置
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConditionWorkflowProps extends WorkflowProps {

        /**
         * 条件组之间的关系 <br>
         * AND 或 OR
         */
        private String type;

        /**
         * 条件组
         */
        private List<ConditionGroup> groups;

    }

    /**
     * 指派人员配置
     *
     * @author ProjectAn
     * @date 2024/3/21 12:43
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Assign {
        /**
         * 人员类型 <br> <br>
         * 指定人员: USER <br>
         * 指定角色: ROLE <br>
         * 发起人自选: SELECT <br>
         * 发起人自己: SELF <br>
         * 系统自动拒绝: AUTO_REJECT <br>
         */
        private String type;
        /**
         * 人员/角色 id 列表
         */
        private List<String> id;
        /**
         * 审批顺序类型 <br> <br>
         * 或签 (任意一人同意即可): ANY <br>
         * 会签 (允许同时审批, 所有人都需要同意): ALL <br>
         * 会签 (按选择顺序审批, 所有人都需要同意): SEQ <br>
         */
        private String mode;
    }

    /**
     * 条件
     *
     * @author ProjectAn
     * @date 2024/9/24 06:05
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Condition {
        /**
         * 字段
         */
        private String field;
        /**
         * 操作符 <br> <br>
         * 等于 EQ <br>
         * 不等于 NEQ <br>
         * 大于 GT <br>
         * 大于等于 GTE <br>
         * 小于 LT <br>
         * 小于等于 LTE <br>
         * 包含 IN <br>
         * 不包含 NIN <br>
         */
        private String operator;
        /**
         * 值
         */
        private String value;
    }

    /**
     * 条件组
     *
     * @author ProjectAn
     * @date 2024/9/24 06:05
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConditionGroup {
        /**
         * 条件之间的关系 <br>
         * AND 或 OR
         */
        private String type;
        /**
         * 条件组
         */
        private List<Condition> conditions;
    }

    /**
     * 审批驳回后操作
     *
     * @author ProjectAn
     * @date 2024/9/24 06:00
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Reject {
        /**
         * 审批驳回自动操作 <br> <br>
         * 结束流程：END <br>
         * 返回指定节点：NODE <br>
         */
        private String type;
        /**
         * 返回指定节点ID
         */
        private String nodeId;
    }

    /**
     * 时限配置
     *
     * @author ProjectAn
     * @date 2024/9/24 05:48
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeLimit {
        /**
         * 时限值
         */
        private Long value;
        /**
         * 时限单位
         *
         * @see cn.projectan.strix.model.dict.WorkflowPropsTimeLimitUnit
         */
        private String unit;
        /**
         * 超时处理
         *
         * @see cn.projectan.strix.model.dict.WorkflowPropsTimeLimitHandler
         */
        private String handler;
    }


}
