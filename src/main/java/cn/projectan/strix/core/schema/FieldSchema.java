package cn.projectan.strix.core.schema;

import lombok.Data;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 单字段 schema 定义
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Data
public class FieldSchema {

    /** 字段类型: text / select / password / email / number */
    private String type;

    /** 字段中文标签 (来自 @Schema(description)) */
    private String label;

    /** 是否必填 (来自 @NotEmpty / @NotBlank / @NotNull) */
    private Boolean required;

    /** 必填适用的验证组 (如仅 insert 时必填, 则为 ["insert"]) */
    private Set<String> requiredGroups;

    /** 适用的验证组: insert / update */
    private Set<String> groups;

    /** 最小值/最小长度 */
    private Integer min;

    /** 最大值/最大长度 */
    private Integer max;

    /** 正则表达式 (来自 @Pattern) */
    private String pattern;

    /** 动态字典名称 (来自 @DynamicDictValue) */
    private String dictName;

    /** 是否需要密码复杂度校验 (来自 @PasswordComplexity) */
    private Boolean complexity;

    /**
     * 添加验证组
     */
    public void addGroup(String group) {
        if (this.groups == null) {
            this.groups = new LinkedHashSet<>();
        }
        this.groups.add(group);
    }

    /**
     * 添加必填验证组
     */
    public void addRequiredGroup(String group) {
        if (this.requiredGroups == null) {
            this.requiredGroups = new LinkedHashSet<>();
        }
        this.requiredGroups.add(group);
    }

    /**
     * 设置类型 (仅当尚未设置时)
     */
    public void setTypeIfAbsent(String type) {
        if (this.type == null) {
            this.type = type;
        }
    }
}
