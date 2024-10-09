package cn.projectan.strix.util.ua;

import cn.projectan.strix.model.other.ua.UserAgent;

/**
 * User-Agent 工具类
 *
 * @author ProjectAn
 * @since 2024/3/31 02:56
 */
public class UserAgentUtil {

    /**
     * 解析User-Agent
     *
     * @param userAgentString User-Agent字符串
     * @return {@link UserAgent}
     */
    public static UserAgent parse(String userAgentString) {
        return UserAgentParser.parse(userAgentString);
    }

}
