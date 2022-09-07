package cn.projectan.strix.model.response.system.systemmenu;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 安炯奕
 * @date 2021/6/20 20:04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemMenuQueryByIdResp {

    private String id;

    /**
     * 菜单名称
     */
    private String name;

    /**
     * 访问地址
     */
    private String url;

    /**
     * 菜单ICON
     */
    private String icon;

    /**
     * 父菜单ID
     */
    private String parentId;

    /**
     * 排序值 越小越靠前
     */
    private Integer sortValue;

}
