package cn.projectan.strix.core.schema;

import cn.projectan.strix.core.validation.annotation.ConstantDictValue;
import cn.projectan.strix.core.validation.annotation.DynamicDictValue;
import cn.projectan.strix.core.validation.annotation.PasswordComplexity;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.FormSchema;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Validator;
import jakarta.validation.constraints.*;
import jakarta.validation.groups.Default;
import jakarta.validation.metadata.BeanDescriptor;
import jakarta.validation.metadata.ConstraintDescriptor;
import jakarta.validation.metadata.PropertyDescriptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 表单 Schema 服务
 * <p>
 * 扫描 @FormSchema 注解的 DTO, 使用 Hibernate Validator Metadata API 提取校验规则,
 * 转换为前端可消费的 JSON Schema.
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FormSchemaService {

    private static final String SCAN_PACKAGE = "cn.projectan.strix.model.request";

    private final Validator validator;
    private final ConcurrentHashMap<String, FormSchemaResp> cache = new ConcurrentHashMap<>();
    private final Map<String, Class<?>> registry = new HashMap<>();

    @PostConstruct
    void init() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(FormSchema.class));

        for (var bd : scanner.findCandidateComponents(SCAN_PACKAGE)) {
            try {
                Class<?> clazz = Class.forName(bd.getBeanClassName());
                FormSchema annotation = clazz.getAnnotation(FormSchema.class);
                String name = annotation.value().isEmpty() ? clazz.getSimpleName() : annotation.value();
                registry.put(name, clazz);
            } catch (ClassNotFoundException e) {
                log.error("FormSchema 加载失败: {}", bd.getBeanClassName(), e);
            }
        }
        log.info("FormSchema 注册表: 扫描到 {} 个 DTO: {}", registry.size(), registry.keySet());
    }

    /**
     * 获取 DTO 的表单 Schema
     *
     * @param dtoName DTO 名称 (类的 simpleName 或 @FormSchema.value)
     * @return 表单 Schema 响应
     */
    public FormSchemaResp getSchema(String dtoName) {
        Assert.isTrue(registry.containsKey(dtoName), "未找到表单 Schema: " + dtoName);
        return cache.computeIfAbsent(dtoName, this::generateSchema);
    }

    /**
     * 获取所有已注册的 DTO 名称
     */
    public Set<String> getRegisteredNames() {
        return Collections.unmodifiableSet(registry.keySet());
    }

    private FormSchemaResp generateSchema(String dtoName) {
        Class<?> clazz = registry.get(dtoName);
        BeanDescriptor beanDescriptor = validator.getConstraintsForClass(clazz);

        Map<String, FieldSchema> fields = new LinkedHashMap<>();

        for (PropertyDescriptor pd : beanDescriptor.getConstrainedProperties()) {
            String fieldName = pd.getPropertyName();
            FieldSchema fieldSchema = new FieldSchema();

            // 从 @Schema 注解获取 label
            extractLabel(clazz, fieldName, fieldSchema);

            // 从 Java 字段类型推断 schema type
            inferType(clazz, fieldName, fieldSchema);

            // 解析所有约束注解
            for (ConstraintDescriptor<?> cd : pd.getConstraintDescriptors()) {
                processConstraint(fieldSchema, cd);
            }

            fields.put(fieldName, fieldSchema);
        }

        return new FormSchemaResp(dtoName, fields);
    }

    private void extractLabel(Class<?> clazz, String fieldName, FieldSchema schema) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            io.swagger.v3.oas.annotations.media.Schema schemaAnn =
                    field.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
            if (schemaAnn != null && !schemaAnn.description().isEmpty()) {
                schema.setLabel(schemaAnn.description());
            }
        } catch (NoSuchFieldException ignored) {
            // 可能是 getter 上的约束, 忽略
        }
    }

    private void inferType(Class<?> clazz, String fieldName, FieldSchema schema) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            Class<?> fieldType = field.getType();
            if (fieldType == Short.class || fieldType == short.class
                    || fieldType == Integer.class || fieldType == int.class
                    || fieldType == Long.class || fieldType == long.class) {
                schema.setTypeIfAbsent("number");
            }
        } catch (NoSuchFieldException ignored) {
        }
    }

    private void processConstraint(FieldSchema schema, ConstraintDescriptor<?> cd) {
        Annotation ann = cd.getAnnotation();
        Set<Class<?>> groups = cd.getGroups();

        // 解析 groups
        resolveGroups(schema, groups);

        // 解析各类注解
        switch (ann) {
            case NotEmpty ne -> {
                schema.setRequired(true);
                resolveRequiredGroups(schema, groups);
            }
            case NotBlank nb -> {
                schema.setRequired(true);
                schema.setTypeIfAbsent("text");
                resolveRequiredGroups(schema, groups);
            }
            case NotNull nn -> {
                schema.setRequired(true);
                resolveRequiredGroups(schema, groups);
            }
            case Size s -> {
                if (s.min() > 0) schema.setMin(s.min());
                if (s.max() < Integer.MAX_VALUE) schema.setMax(s.max());
            }
            case Min m -> schema.setMin((int) m.value());
            case Max m -> schema.setMax((int) m.value());
            case Pattern p -> schema.setPattern(p.regexp());
            case Email e -> schema.setType("email");
            case DynamicDictValue ddv -> {
                schema.setType("select");
                schema.setDictName(ddv.dictName());
            }
            case ConstantDictValue cdv -> {
                // 常量字典标记为 select, 前端已有对应选项数据
                schema.setType("select");
            }
            case PasswordComplexity pc -> {
                schema.setType("password");
                schema.setComplexity(true);
            }
            default -> log.debug("未映射的约束注解: {}", ann.annotationType().getSimpleName());
        }
    }

    private void resolveGroups(FieldSchema schema, Set<Class<?>> groups) {
        if (groups.isEmpty() || groups.contains(Default.class)) {
            schema.addGroup("insert");
            schema.addGroup("update");
            return;
        }
        if (groups.contains(InsertGroup.class)) schema.addGroup("insert");
        if (groups.contains(UpdateGroup.class)) schema.addGroup("update");
    }

    private void resolveRequiredGroups(FieldSchema schema, Set<Class<?>> groups) {
        if (groups.isEmpty() || groups.contains(Default.class)) {
            schema.addRequiredGroup("insert");
            schema.addRequiredGroup("update");
            return;
        }
        if (groups.contains(InsertGroup.class)) schema.addRequiredGroup("insert");
        if (groups.contains(UpdateGroup.class)) schema.addRequiredGroup("update");
    }
}
