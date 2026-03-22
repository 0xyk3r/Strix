package cn.projectan.strix.model.other.system.workflow;

import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(
        description = "工作流节点配置（多态基类，根据节点 type 字段区分子类型）",
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "approval", schema = WorkflowProps.ApprovalWorkflowProps.class),
                @DiscriminatorMapping(value = "task", schema = WorkflowProps.TaskWorkflowProps.class),
                @DiscriminatorMapping(value = "cc", schema = WorkflowProps.CcWorkflowProps.class),
                @DiscriminatorMapping(value = "condition", schema = WorkflowProps.ConditionWorkflowProps.class)
        }
)
public class WorkflowProps {

    /**
     * 获取指派配置（子类可覆盖）
     */
    public Assign getAssign() {
        return null;
    }

    /**
     * 获取时限配置（子类可覆盖）
     */
    public TimeLimit getTimeLimit() {
        return null;
    }

    /**
     * 获取拒绝操作配置（子类可覆盖）
     */
    public Reject getReject() {
        return null;
    }

    /**
     * 审批节点配置
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "审批节点配置 (type=approval)")
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
    @Schema(description = "办理节点配置 (type=task)")
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
    @Schema(description = "抄送节点配置 (type=cc)")
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
    @Schema(description = "条件节点配置 (type=condition)")
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
    @Schema(description = "指派人员配置")
    public static class Assign {
        @Schema(description = "人员类型: USER=指定人员, ROLE=指定角色, SELECT=发起人自选, SELF=发起人自己, AUTO_REJECT=系统自动拒绝")
        private String type;
        @Schema(description = "人员/角色 ID 列表")
        private List<String> id;
        @Schema(description = "审批顺序类型: ANY=或签, ALL=会签(允许同时), SEQ=会签(按顺序)")
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
    @Schema(description = "条件")
    public static class Condition {
        @Schema(description = "字段名")
        private String field;
        @Schema(description = "操作符: EQ=等于, NEQ=不等于, GT=大于, GTE=大于等于, LT=小于, LTE=小于等于, IN=包含, NIN=不包含")
        private String operator;
        @Schema(description = "比较值")
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
    @Schema(description = "条件组")
    public static class ConditionGroup {
        @Schema(description = "条件之间的关系: AND 或 OR")
        private String type;
        @Schema(description = "条件列表")
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
    @Schema(description = "审批驳回后操作")
    public static class Reject {
        @Schema(description = "驳回操作类型: END=结束流程, NODE=返回指定节点")
        private String type;
        @Schema(description = "返回指定节点 ID (type=NODE 时必填)")
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
    @Schema(description = "时限配置")
    public static class TimeLimit {
        @Schema(description = "时限值")
        private Long value;
        @Schema(description = "时限单位: MINUTE=分钟, HOUR=小时, DAY=天")
        private String unit;
        @Schema(description = "超时处理: NOTIFY=通知提醒, AUTO_PASS=自动通过, AUTO_REJECT=自动拒绝")
        private String handler;
    }


}
