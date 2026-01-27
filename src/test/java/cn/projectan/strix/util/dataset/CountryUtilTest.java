package cn.projectan.strix.util.dataset;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

/**
 * @author ProjectAn
 * @since 2026/1/28 01:40
 */
@Slf4j
class CountryUtilTest {

    @Test
    void getCountries() {
        List<CountryInfo> countryInfoList = new ArrayList<>();

        for (String code2 : Locale.getISOCountries()) {
            Locale locale = Locale.of("", code2);

            String code3;
            try {
                code3 = locale.getISO3Country();
            } catch (MissingResourceException e) {
                code3 = "";
            }
            String zhName = locale.getDisplayCountry(Locale.CHINA);

            countryInfoList.add(new CountryInfo(code2, code3, zhName));
        }

        countryInfoList.forEach(info ->
                log.info("CountryInfo(code2='{}', code3='{}', zhName='{}')", info.code2, info.code3, info.zhName)
        );
        log.info("size={}", countryInfoList.size());
    }

    record CountryInfo(String code2, String code3, String zhName) {
    }

}