package cn.projectan.strix.config.converter;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * String -> LocalDate 转换器
 *
 * @author ProjectAn
 * @since 2022/3/9 15:51
 */
@Slf4j
@Configuration
public class StringToLocalDateConverter implements Converter<String, LocalDate> {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public LocalDate convert(@NotNull String source) {
        try {
            return LocalDate.parse(source, formatter);
        } catch (Exception e) {
            log.warn("Date Convert Error", e);
        }
        return null;
    }

}
