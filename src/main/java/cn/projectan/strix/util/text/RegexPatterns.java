package cn.projectan.strix.util.text;

import java.util.regex.Pattern;

/**
 * 正则表达式常量
 *
 * @author ProjectAn
 * @since 2023/5/23 10:05
 */
public class RegexPatterns {

    public static final Pattern DOMAIN_PATTERN = Pattern.compile("(https?://[^/]+)");

    public static final Pattern BASE64_FILE_PATTERN = Pattern.compile("^data:(\\w+/\\w+);base64,(.+)$", Pattern.CASE_INSENSITIVE);

}
