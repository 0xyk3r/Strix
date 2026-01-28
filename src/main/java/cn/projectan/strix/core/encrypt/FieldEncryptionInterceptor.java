package cn.projectan.strix.core.encrypt;

import cn.projectan.strix.model.annotation.EncryptField;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.update.Update;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MyBatis 字段加密解密拦截器
 * <p>
 * 拦截 Executor 的 update 和 query 方法，自动对带有 {@link EncryptField} 注解的字段进行加密解密。
 * <p>
 * 支持的操作：
 * <ul>
 *   <li>INSERT: 插入时自动加密实体对象中的加密字段</li>
 *   <li>UPDATE: 更新时自动加密（包括 updateById、update with Wrapper）</li>
 *   <li>SELECT: 查询结果自动解密</li>
 * </ul>
 * <p>
 * 注意事项：
 * <ul>
 *   <li>Wrapper 条件中的加密字段值会自动加密（支持 eq、ne、in、like 等条件）</li>
 *   <li>UpdateWrapper.set() 设置的加密字段值会自动加密</li>
 *   <li>已加密的数据（带 ENC: 前缀）不会重复加密</li>
 * </ul>
 *
 * @author ProjectAn
 * @since 2026/01/29 02:20
 */
@Slf4j
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class FieldEncryptionInterceptor implements Interceptor {

    /**
     * 缓存类的加密字段信息
     */
    private static final Map<Class<?>, List<Field>> ENCRYPT_FIELD_CACHE = new ConcurrentHashMap<>();

    /**
     * 匹配 SQL 中的列名和参数占位符的正则表达式
     * 格式：column_name = #{ew.paramNameValuePairs.MPGENVAL1}
     * 或：`column_name` = #{ew.paramNameValuePairs.MPGENVAL1}
     */
    private static final Pattern COLUMN_PARAM_PATTERN = Pattern.compile(
            "`?([a-zA-Z_][a-zA-Z0-9_]*)`?\\s*(?:=|!=|<>|LIKE|IN)\\s*[^#]*#\\{ew\\.paramNameValuePairs\\.(\\w+)}",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 匹配 SET 子句中的列名和参数占位符
     * 格式：column_name = #{ew.paramNameValuePairs.MPGENVAL1}
     * 或：`column_name` = #{ew.paramNameValuePairs.MPGENVAL1}
     */
    private static final Pattern SET_PARAM_PATTERN = Pattern.compile(
            "`?([a-zA-Z_][a-zA-Z0-9_]*)`?\\s*=\\s*#\\{ew\\.paramNameValuePairs\\.(\\w+)}",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameter = args[1];

        SqlCommandType sqlCommandType = ms.getSqlCommandType();

        // 处理 INSERT 和 UPDATE 操作的参数加密
        if (sqlCommandType == SqlCommandType.INSERT || sqlCommandType == SqlCommandType.UPDATE) {
            encryptParameter(ms, parameter);
        }

        // 执行原方法
        Object result = invocation.proceed();

        // 处理 SELECT 操作的结果解密
        if (sqlCommandType == SqlCommandType.SELECT && result != null) {
            decryptResult(result);
        }

        return result;
    }

    /**
     * 加密参数
     */
    private void encryptParameter(MappedStatement ms, Object parameter) {
        switch (parameter) {
            case null -> {
            }
            // 处理 MapperMethod.ParamMap（MyBatis-Plus 的参数封装）
            case MapperMethod.ParamMap<?> paramMap -> processParamMap(ms, paramMap);
            // 处理普通 Map 参数
            case Map<?, ?> map -> processMap(ms, map);
            // 直接是实体对象
            default -> encryptObject(parameter);
        }

    }

    /**
     * 处理 MyBatis-Plus 的 ParamMap
     */
    private void processParamMap(MappedStatement ms, MapperMethod.ParamMap<?> paramMap) {
        // 处理实体对象 (et)
        Object et = paramMap.getOrDefault(Constants.ENTITY, null);
        if (et != null) {
            encryptObject(et);
        }

        // 处理 Wrapper (ew)
        Object ew = paramMap.getOrDefault(Constants.WRAPPER, null);
        if (ew instanceof AbstractWrapper<?, ?, ?> wrapper) {
            encryptWrapperParams(ms, wrapper);
        }

        // 处理批量插入的集合
        Object list = paramMap.getOrDefault("list", null);
        if (list instanceof Collection<?> collection) {
            for (Object item : collection) {
                encryptObject(item);
            }
        }

        // 处理 collection 参数（某些批量操作使用）
        Object collectionParam = paramMap.getOrDefault("collection", null);
        if (collectionParam instanceof Collection<?> collection) {
            for (Object item : collection) {
                encryptObject(item);
            }
        }
    }

    /**
     * 处理普通 Map 参数
     */
    private void processMap(MappedStatement ms, Map<?, ?> map) {
        Object et = map.get(Constants.ENTITY);
        if (et != null) {
            encryptObject(et);
        }
        Object ew = map.get(Constants.WRAPPER);
        if (ew instanceof AbstractWrapper<?, ?, ?> wrapper) {
            encryptWrapperParams(ms, wrapper);
        }
    }

    /**
     * 加密对象中带有 @EncryptField 注解的字段
     */
    private void encryptObject(Object obj) {
        if (obj == null) {
            return;
        }

        List<Field> encryptFields = getEncryptFields(obj.getClass());
        for (Field field : encryptFields) {
            try {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value instanceof String strValue) {
                    String encrypted = FieldEncryptUtil.encrypt(strValue);
                    field.set(obj, encrypted);
                }
            } catch (Exception e) {
                log.error("加密字段 {} 失败: {}", field.getName(), e.getMessage(), e);
            }
        }
    }

    /**
     * 加密 Wrapper 中的参数值
     * <p>
     * 通过解析 SQL 段来确定哪些参数对应加密字段，然后进行加密
     */
    private void encryptWrapperParams(MappedStatement ms, AbstractWrapper<?, ?, ?> wrapper) {
        if (wrapper == null) {
            return;
        }

        try {
            // ProjectAn 注: 我真牛逼!!!
            // 获取实体类：优先从 wrapper 获取，否则从 MappedStatement 获取
            Class<?> entityClass = wrapper.getEntityClass();
            if (entityClass == null) {
                entityClass = resolveEntityClass(ms);
                log.debug("从 MappedStatement 解析实体类: {}", entityClass);
            }
            if (entityClass == null) {
                return;
            }

            // 获取加密字段的数据库列名
            Set<String> encryptColumnNames = getEncryptColumnNames(entityClass);
            if (encryptColumnNames.isEmpty()) {
                return;
            }

            // 获取参数映射
            Map<String, Object> paramNameValuePairs = wrapper.getParamNameValuePairs();
            if (paramNameValuePairs == null || paramNameValuePairs.isEmpty()) {
                return;
            }

            // 获取完整的 SQL 段（包括 WHERE 条件和 SET 子句）
            String sqlSegment = wrapper.getExpression().getSqlSegment();
            String customSqlSegment = wrapper.getCustomSqlSegment();
            String fullSql = (sqlSegment != null ? sqlSegment : "") + " " + (customSqlSegment != null ? customSqlSegment : "");

            // 获取 SET 子句（仅 Update 类型的 Wrapper 有）
            if (wrapper instanceof Update<?, ?> updateWrapper) {
                String sqlSet = updateWrapper.getSqlSet();
                if (sqlSet != null) {
                    fullSql += " " + sqlSet;
                }
            }

            // 解析 SQL 找出需要加密的参数
            Set<String> paramsToEncrypt = new HashSet<>();

            // 匹配 WHERE 条件中的参数
            Matcher conditionMatcher = COLUMN_PARAM_PATTERN.matcher(fullSql);
            while (conditionMatcher.find()) {
                String columnName = conditionMatcher.group(1).toLowerCase();
                String paramName = conditionMatcher.group(2);
                if (encryptColumnNames.contains(columnName)) {
                    paramsToEncrypt.add(paramName);
                }
            }

            // 匹配 SET 子句中的参数
            Matcher setMatcher = SET_PARAM_PATTERN.matcher(fullSql);
            while (setMatcher.find()) {
                String columnName = setMatcher.group(1).toLowerCase();
                String paramName = setMatcher.group(2);
                if (encryptColumnNames.contains(columnName)) {
                    paramsToEncrypt.add(paramName);
                }
            }

            // 加密需要加密的参数
            for (String paramName : paramsToEncrypt) {
                Object value = paramNameValuePairs.get(paramName);
                if (value instanceof String strValue && !FieldEncryptUtil.isEncrypted(strValue)) {
                    paramNameValuePairs.put(paramName, FieldEncryptUtil.encrypt(strValue));
                } else if (value instanceof Collection<?> collection) {
                    // 处理 IN 条件中的集合
                    List<Object> encryptedList = new ArrayList<>();
                    for (Object item : collection) {
                        if (item instanceof String strItem && !FieldEncryptUtil.isEncrypted(strItem)) {
                            encryptedList.add(FieldEncryptUtil.encrypt(strItem));
                        } else {
                            encryptedList.add(item);
                        }
                    }
                    paramNameValuePairs.put(paramName, encryptedList);
                }
            }
        } catch (Exception e) {
            log.error("加密 Wrapper 参数失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 从 MappedStatement 解析实体类
     * <p>
     * 通过 mapper 接口的全限定名推断实体类，例如：
     * cn.projectan.strix.mapper.system.SystemManagerMapper.update -> SystemManager
     */
    private Class<?> resolveEntityClass(MappedStatement ms) {
        try {
            // 方式1：从 ParameterMap 获取参数类型
            Class<?> paramType = ms.getParameterMap().getType();
            if (paramType != null && !paramType.equals(Object.class) && !Map.class.isAssignableFrom(paramType)) {
                // 检查该类是否有加密字段
                if (!getEncryptFields(paramType).isEmpty()) {
                    return paramType;
                }
            }

            // 方式2：从 mapper ID 推断实体类
            // 格式：cn.projectan.strix.mapper.system.SystemManagerMapper.update
            String mapperId = ms.getId();
            int lastDot = mapperId.lastIndexOf('.');
            if (lastDot > 0) {
                String mapperClassName = mapperId.substring(0, lastDot);
                // 尝试通过 mapper 接口的泛型参数获取实体类
                Class<?> mapperClass = Class.forName(mapperClassName);
                // 检查是否实现了 BaseMapper<T>
                for (java.lang.reflect.Type genericInterface : mapperClass.getGenericInterfaces()) {
                    if (genericInterface instanceof java.lang.reflect.ParameterizedType pt) {
                        java.lang.reflect.Type[] typeArgs = pt.getActualTypeArguments();
                        if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> entityClass) {
                            return entityClass;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("从 MappedStatement 解析实体类失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 解密查询结果
     */
    private void decryptResult(Object result) {
        if (result == null) {
            return;
        }

        if (result instanceof Collection<?> collection) {
            for (Object item : collection) {
                decryptObject(item);
            }
        } else {
            decryptObject(result);
        }
    }

    /**
     * 解密对象中带有 @EncryptField 注解的字段
     */
    private void decryptObject(Object obj) {
        if (obj == null) {
            return;
        }

        // 跳过基本类型和常见不可变类型
        if (obj instanceof String || obj instanceof Number || obj instanceof Boolean
                || obj instanceof Character || obj.getClass().isPrimitive()) {
            return;
        }

        // 处理 Map 类型
        if (obj instanceof Map<?, ?> map) {
            for (Object value : map.values()) {
                decryptObject(value);
            }
            return;
        }

        List<Field> encryptFields = getEncryptFields(obj.getClass());
        for (Field field : encryptFields) {
            try {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value instanceof String strValue) {
                    String decrypted = FieldEncryptUtil.decrypt(strValue);
                    field.set(obj, decrypted);
                }
            } catch (Exception e) {
                log.error("解密字段 {} 失败: {}", field.getName(), e.getMessage(), e);
            }
        }
    }

    /**
     * 获取类中带有 @EncryptField 注解的字段（包含父类）
     */
    private List<Field> getEncryptFields(Class<?> clazz) {
        return ENCRYPT_FIELD_CACHE.computeIfAbsent(clazz, c -> {
            List<Field> fields = new ArrayList<>();
            Class<?> current = c;
            while (current != null && current != Object.class) {
                for (Field field : current.getDeclaredFields()) {
                    if (field.isAnnotationPresent(EncryptField.class)) {
                        if (field.getType() == String.class) {
                            fields.add(field);
                        } else {
                            log.warn("@EncryptField 仅支持 String 类型字段，字段 {} 类型为 {}",
                                    field.getName(), field.getType().getName());
                        }
                    }
                }
                current = current.getSuperclass();
            }
            return fields;
        });
    }

    /**
     * 获取类中加密字段对应的数据库列名
     */
    private Set<String> getEncryptColumnNames(Class<?> clazz) {
        List<Field> encryptFields = getEncryptFields(clazz);
        Set<String> columnNames = new HashSet<>();
        for (Field field : encryptFields) {
            // 转换为下划线命名（小写）
            columnNames.add(camelToUnderscore(field.getName()).toLowerCase());
        }
        return columnNames;
    }

    /**
     * 驼峰命名转下划线命名
     */
    private String camelToUnderscore(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 可以通过配置文件设置属性
    }

}
