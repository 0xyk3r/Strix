package cn.projectan.strix.config.converter;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author 安炯奕
 * @date 2022/3/9 15:51
 */
@Slf4j
@Configuration
public class LocalDateTimeConvert implements Converter<String, LocalDateTime> {

    @Override
    public LocalDateTime convert(@NotNull String source) {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime = null;
        try {
            dateTime = LocalDateTime.parse(source, df);
        } catch (Exception e) {
            log.error("Strix - LocalDateTimeConvert: 捕获到时间转换异常：", e);
        }
        return dateTime;
    }

}
