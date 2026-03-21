package cn.projectan.strix.util.crypto;

import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.digest.DigestUtil;

/**
 * 密码重置工具
 * <p>
 * 用于将所有用户密码从旧版 MD5 格式批量迁移为 SM3 格式。
 * <br>
 * 使用方法:
 * <ol>
 *   <li>确认 MySQL 连接信息正确</li>
 *   <li>运行此类的 main 方法</li>
 *   <li>所有用户密码将被重置为指定的默认密码（SM3 哈希）</li>
 * </ol>
 * <p>
 * 注意: 此工具会将所有用户密码重置为相同的默认密码, 请在执行后通知用户修改密码。
 * </p>
 * <p>
 * 也可以直接通过 SQL 执行迁移（推荐方式）:
 * <pre>
 * -- 查看当前密码哈希长度（MD5=32, SM3=64）
 * SELECT id, login_name, LENGTH(login_password) AS hash_len FROM sys_system_manager WHERE deleted_status = 0;
 *
 * -- 将所有用户密码重置为 SM3 哈希（默认密码 An@12121）
 * -- SM3("An@12121ProjectAn Strix") 的值请通过 main 方法获取后替换下方占位符
 * UPDATE sys_system_manager SET login_password = '此处替换为SM3哈希值' WHERE deleted_status = 0;
 * </pre>
 * </p>
 *
 * @author ProjectAn
 * @since 2026/3/20
 */
public class PasswordResetUtil {

    private static final String PASSWORD_SALT = "ProjectAn Strix";
    private static final String DEFAULT_PASSWORD = "An@12121";

    public static void main(String[] args) {
        System.out.println("=== 密码迁移工具 ===\n");

        // 显示默认密码的 SM3 和 MD5 哈希
        String sm3Hash = SmUtil.sm3(DEFAULT_PASSWORD + PASSWORD_SALT);
        String md5Hash = DigestUtil.md5Hex(DEFAULT_PASSWORD + PASSWORD_SALT);

        System.out.println("默认密码: " + DEFAULT_PASSWORD);
        System.out.println("SM3 哈希: " + sm3Hash + " (长度: " + sm3Hash.length() + ")");
        System.out.println("MD5 哈希: " + md5Hash + " (长度: " + md5Hash.length() + ")");

        System.out.println("\n=== SQL 重置语句 ===\n");
        System.out.println("-- 将所有管理员密码重置为默认密码 '" + DEFAULT_PASSWORD + "' 的 SM3 哈希:");
        System.out.println("UPDATE sys_system_manager SET login_password = '" + sm3Hash + "' WHERE deleted_status = 0;");

        System.out.println("\n-- 查看密码格式分布:");
        System.out.println("SELECT");
        System.out.println("  CASE WHEN LENGTH(login_password) = 32 THEN 'MD5'");
        System.out.println("       WHEN LENGTH(login_password) = 64 THEN 'SM3'");
        System.out.println("       ELSE 'UNKNOWN' END AS hash_type,");
        System.out.println("  COUNT(*) AS count");
        System.out.println("FROM sys_system_manager WHERE deleted_status = 0");
        System.out.println("GROUP BY hash_type;");

        System.out.println("\n=== 自定义密码哈希 ===\n");
        if (args.length > 0) {
            for (String password : args) {
                String hash = SmUtil.sm3(password + PASSWORD_SALT);
                System.out.println("密码: " + password + " => SM3: " + hash);
            }
        } else {
            System.out.println("提示: 传入参数可以计算自定义密码的 SM3 哈希");
            System.out.println("用法: java PasswordResetUtil <password1> [password2] ...");
        }

        System.out.println("\n=== 注意事项 ===");
        System.out.println("1. 系统已支持自动迁移: 用户使用旧密码登录时, 系统会自动将 MD5 哈希转为 SM3");
        System.out.println("2. 如需强制重置, 请使用上述 SQL 语句");
        System.out.println("3. 重置后请通知用户修改密码");
    }

}
