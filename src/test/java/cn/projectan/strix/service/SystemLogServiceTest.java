package cn.projectan.strix.service;

import cn.projectan.strix.model.db.SystemLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author ProjectAn
 * @date 2023/6/16 22:25
 */
@SpringBootTest
class SystemLogServiceTest {

    @Autowired
    private SystemLogService systemLogService;

    @Test
    void saveTest() {
        System.out.println("==== saveTest ====");

        systemLogService.save(new SystemLog(
                "strix",
                "1.0.0",
                "test",
                "test",
                "test",
                0L,
                "",
                "",
                "",
                LocalDateTime.now(),
                "",
                "",
                "",
                "",
                "",
                0,
                "",
                ""
        ));

        List<SystemLog> list = systemLogService.list();
        System.out.println(list.size());
        list.forEach(System.out::println);

        System.out.println("==== saveTest ====");
    }

}
