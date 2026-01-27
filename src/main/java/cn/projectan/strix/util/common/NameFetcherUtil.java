package cn.projectan.strix.util.common;

import cn.projectan.strix.model.constant.system.StrixRedisKeyConst;
import cn.projectan.strix.service.base.NameFetcherService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ProjectAn
 * @since 2024-11-18 17:18:44
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NameFetcherUtil {

    private final RedisUtil redisUtil;

    private final static String SERVICE_SUFFIX = "ServiceImpl";
    private final List<String> serviceList = new ArrayList<>();

    @PostConstruct
    public void init() {
        String[] beanNamesForType = SpringUtil.getBeanNamesForType(NameFetcherService.class);
        for (String beanName : beanNamesForType) {
            if (beanName.endsWith(SERVICE_SUFFIX)) {
                serviceList.add(beanName);
            }
        }
        log.info("Strix NameFetcher: 初始化完成, 加载了 {} 个映射器.", serviceList.size());
    }

    public String get(String dataType, String dataId) {
        Object o = redisUtil.hGet(StrixRedisKeyConst.HASH_NAME_FETCHER_PREFIX + dataType, dataId);
        if (o == null) {
            String formDB = getFormDB(dataType, dataId);
            if (formDB != null) {
                redisUtil.hSet(StrixRedisKeyConst.HASH_NAME_FETCHER_PREFIX + dataType, dataId, formDB);
            }
            return formDB;
        } else {
            return o.toString();
        }
    }

    private String getFormDB(String dataType, String dataId) {
        for (String serviceName : serviceList) {
            if (serviceName.equalsIgnoreCase(dataType + SERVICE_SUFFIX)) {
                NameFetcherService<?> service = SpringUtil.getBean(serviceName);
                return service.getDataNameById(dataId);
            }
        }
        return null;
    }

}
