package cn.projectan.strix.utils;

import java.util.regex.Pattern;

/**
 * @author ProjectAn
 * @date 2023/5/23 10:05
 */
public class RegexUtils {

    public static final Pattern DOMAIN_PATTERN = Pattern.compile("(https?://[^/]+)");

    public static final Pattern BASE64_FILE_PATTERN = Pattern.compile("^data:(\\w+/\\w+);base64,(.+)$", Pattern.CASE_INSENSITIVE);

}
