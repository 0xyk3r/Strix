package cn.projectan.strix.initialize;

import cn.projectan.strix.model.properties.StrixPackageScanProperties;
import cn.projectan.strix.utils.SpringUtil;
import com.baomidou.mybatisplus.extension.service.IService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Entity -> Service 映射
 *
 * @author ProjectAn
 * @date 2023/6/18 16:22
 */
@Slf4j
@Order(50)
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(StrixPackageScanProperties.class)
public class ServiceMapInit implements ApplicationRunner {

    private final Map<Class<?>, Class<? extends IService>> SERVICE_MAP = new HashMap<>();
    private final List<String> STRIX_SERVICE_PACKAGE = new ArrayList<>();

    private final StrixPackageScanProperties strixPackageScanProperties;

    @Override
    public void run(ApplicationArguments args) {
        STRIX_SERVICE_PACKAGE.add("cn.projectan.strix.service");
        STRIX_SERVICE_PACKAGE.addAll(List.of(strixPackageScanProperties.getService()));

        Set<Class<? extends IService>> classSet = new HashSet<>();
        STRIX_SERVICE_PACKAGE.forEach(pkg -> {
            Reflections reflections = new Reflections(pkg);
            classSet.addAll(reflections.getSubTypesOf(IService.class));
        });

        classSet.stream().filter(Class::isInterface).forEach(clazz -> {
            Type[] genericInterfaces = clazz.getGenericInterfaces();
            for (Type genericInterface : genericInterfaces) {
                if (genericInterface instanceof ParameterizedType parameterizedType) {
                    Type[] typeArgs = parameterizedType.getActualTypeArguments();
                    if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> genericClass) {
                        SERVICE_MAP.put(genericClass, clazz);
                    }
                }
            }
        });
    }

    public <T> IService<T> findServiceByEntity(Class<T> entityClass) {
        Class<? extends IService> aClass = SERVICE_MAP.get(entityClass);
        if (aClass != null) {
            return SpringUtil.getBean(aClass);
        }
        return null;
    }

}
