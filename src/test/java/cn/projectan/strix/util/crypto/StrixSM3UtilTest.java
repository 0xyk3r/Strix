package cn.projectan.strix.util.crypto;

import org.junit.jupiter.api.Test;

/**
 * @author ProjectAn
 * @since 2026/3/21 18:44
 */
class StrixSM3UtilTest {

    @Test
    void hashPassword() {
        String password = "An@12121";
        String hashPassword = StrixSM3Util.hashPassword(password);
        System.out.println("Hash Password: " + hashPassword);
    }
}