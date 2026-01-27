package cn.projectan.strix.util.dataset;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 国家和地区工具类
 *
 * @author ProjectAn
 * @since 2026/1/28 1:48
 */
@Slf4j
public class CountryUtil {

    public static final List<CountryInfo> COUNTRY_INFOS;

    static {
        List<CountryInfo> list = new ArrayList<>();

        for (String code2 : Locale.getISOCountries()) {
            Locale locale = Locale.of("", code2);

            String code3;
            try {
                code3 = locale.getISO3Country();
            } catch (MissingResourceException e) {
                code3 = null;
            }
            String zhName = locale.getDisplayCountry(Locale.CHINA);

            list.add(new CountryInfo(code2, code3, zhName));
        }

        list.sort(Comparator.comparing(CountryInfo::zhName));

        COUNTRY_INFOS = List.copyOf(list);
    }

    private CountryUtil() {
    }

    public record CountryInfo(String code2, String code3, String zhName) {
    }

}
