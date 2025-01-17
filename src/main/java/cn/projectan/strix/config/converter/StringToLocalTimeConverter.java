package cn.projectan.strix.config.converter;

import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * String -> LocalTime 转换器
 *
 * @author ProjectAn
 * @since 2025/1/13 23:20
 */
@Slf4j
@Configuration
public class StringToLocalTimeConverter implements Converter<String, LocalTime> {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public LocalTime convert(@Nonnull String source) {
        try {
            return LocalTime.parse(source, formatter);
        } catch (Exception e) {
            log.warn("Date Convert Error", e);
        }
        return null;
    }

}
