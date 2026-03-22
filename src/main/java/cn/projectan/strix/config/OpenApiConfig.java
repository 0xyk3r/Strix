package cn.projectan.strix.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Strix API 文档",
                version = "3.0.0",
                description = "Strix 业务中台框架 API 文档",
                contact = @Contact(
                        name = "ProjectAn",
                        url = "https://github.com/0xyk3r/Strix"
                ),
                license = @License(
                        name = "Proprietary"
                )
        )
)
@SecurityScheme(
        name = "Authorization",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        description = "Bearer Token — 登录后获取，格式: Bearer {token}"
)
public class OpenApiConfig {

    /**
     * 全局 operationId 生成：自动使用 "Controller前缀_方法名" 格式，
     * 避免不同 Controller 中同名方法（如 update、remove）引起 operationId 冲突。
     * 同一 Controller 内的重载方法自动追加序号后缀（如 _1）。
     * <p>
     * 使用此全局定制器后，无需在 @Operation 注解中手动指定 operationId。
     */
    @Bean
    public GlobalOperationCustomizer operationIdCustomizer() {
        Map<String, AtomicInteger> counter = new ConcurrentHashMap<>();
        return (operation, handlerMethod) -> {
            String controllerName = handlerMethod.getBeanType().getSimpleName();
            if (controllerName.endsWith("Controller")) {
                controllerName = controllerName.substring(0, controllerName.length() - "Controller".length());
            }
            String methodName = handlerMethod.getMethod().getName();
            String baseId = controllerName + "_" + methodName;
            int index = counter.computeIfAbsent(baseId, k -> new AtomicInteger(0)).getAndIncrement();
            operation.setOperationId(index == 0 ? baseId : baseId + "_" + index);
            return operation;
        };
    }

    @Bean
    public GroupedOpenApi strixSystemApi() {
        return GroupedOpenApi.builder()
                .group("Strix 管理中心接口")
                .pathsToMatch(
                        "/system/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi strixSrvApi() {
        return GroupedOpenApi.builder()
                .group("Strix 通用服务接口")
                .pathsToMatch(
                        "/srv/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi strixPayApi() {
        return GroupedOpenApi.builder()
                .group("Strix 通用支付接口")
                .pathsToMatch(
                        "/pay/**"
                )
                .build();
    }

    /**
     * 业务接口（默认主要展示）
     */
    @Bean
    public GroupedOpenApi bizApi() {
        return GroupedOpenApi.builder()
                .group("业务接口")
                .pathsToMatch("/api/**")
                .pathsToExclude(
                        "/system/**",
                        "/srv/**",
                        "/pay/**"
                )
                .build();
    }

    /**
     * 全局 OpenAPI 定制：通用响应码 + 鉴权方案注入
     * <p>
     * Knife4j 要求每个 Operation 都显式声明 security 才会在调试界面附带 Authorization Header，
     * 因此通过 GlobalOpenApiCustomizer 为非匿名接口统一注入 BearerAuth 鉴权方案。
     *
     * @see <a href="https://doc.xiaominfo.com/docs/blog/add-authorization-header">Knife4j 鉴权文档</a>
     */
    @Bean
    @SuppressWarnings("unchecked")
    public GlobalOpenApiCustomizer globalOpenApiCustomizer() {
        // 匿名路径前缀（与 @Anonymous 注解标注的 Controller/方法对应）
        final Set<String> anonymousPrefixes = Set.of(
                "/system/login",
                "/system/captcha",
                "/pay/",
                "/srv/wechat/",
                "/srv/common/file/"
        );

        return openApi -> {
            // 注册 RetResult 错误响应 schema
            Schema<?> errorSchema = new Schema<>()
                    .description("错误响应")
                    .addProperty("code", new IntegerSchema().description("错误码").example(400))
                    .addProperty("msg", new StringSchema().description("错误信息").example("参数错误"))
                    .addProperty("data", new Schema<>().description("null"));

            Components components = openApi.getComponents();
            if (components == null) {
                components = new Components();
                openApi.setComponents(components);
            }
            components.addSchemas("ErrorResponse", errorSchema);

            // 为所有路径添加通用响应码 + 鉴权方案
            if (openApi.getPaths() != null) {
                SecurityRequirement bearerAuth = new SecurityRequirement().addList("Authorization");

                openApi.getPaths().forEach((path, pathItem) -> {
                    boolean isAnonymous = anonymousPrefixes.stream().anyMatch(path::startsWith);

                    pathItem.readOperations().forEach(operation -> {
                        // 通用响应码
                        ApiResponses responses = operation.getResponses();
                        if (responses == null) {
                            responses = new ApiResponses();
                            operation.setResponses(responses);
                        }
                        responses.putIfAbsent("400", new ApiResponse().description("请求参数错误"));
                        responses.putIfAbsent("401", new ApiResponse().description("未登录或 Token 已过期"));
                        responses.putIfAbsent("403", new ApiResponse().description("无操作权限"));
                        responses.putIfAbsent("429", new ApiResponse().description("请求过于频繁"));
                        responses.putIfAbsent("500", new ApiResponse().description("服务器内部错误"));

                        // 鉴权方案：非匿名接口注入 BearerAuth
                        if (!isAnonymous) {
                            operation.addSecurityItem(bearerAuth);
                        }
                    });
                });
            }
        };
    }

    /**
     * Schema 名称简化定制器：配合 springdoc.use-fqn=true 使用。
     * <p>
     * FQN 模式下 Schema 名称为全限定类名（如 cn.projectan.strix.model.response.system.SystemMenuResp），
     * 此定制器自动将其简化为可读格式：
     * <ul>
     *   <li>顶层类 → ClassName（如 SystemMenuResp）</li>
     *   <li>内部类 → OuterClass.InnerClass（如 SystemMenuListResp.SystemMenuManageItem）</li>
     * </ul>
     * 同时自动更新所有 $ref 引用，确保文档一致性。
     * 若简化后出现重名则保留 FQN，确保安全。
     */
    @Bean
    public GlobalOpenApiCustomizer schemaNameSimplifier() {
        return openApi -> {
            Components components = openApi.getComponents();
            if (components == null || components.getSchemas() == null) return;

            Map<String, Schema> schemas = components.getSchemas();

            // 1. 计算简化名称并检测冲突
            Map<String, List<String>> simplifiedToFqns = new LinkedHashMap<>();
            for (String fqn : schemas.keySet()) {
                String simplified = simplifySchemaName(fqn);
                simplifiedToFqns.computeIfAbsent(simplified, k -> new ArrayList<>()).add(fqn);
            }

            // 2. 构建重命名映射（冲突时保留 FQN，确保安全）
            Map<String, String> renameMap = new LinkedHashMap<>();
            for (Map.Entry<String, List<String>> entry : simplifiedToFqns.entrySet()) {
                List<String> fqns = entry.getValue();
                if (fqns.size() == 1) {
                    String fqn = fqns.getFirst();
                    if (!fqn.equals(entry.getKey())) {
                        renameMap.put(fqn, entry.getKey());
                    }
                }
            }

            if (renameMap.isEmpty()) return;

            // 3. 应用重命名
            Map<String, Schema> newSchemas = new LinkedHashMap<>();
            for (Map.Entry<String, Schema> entry : schemas.entrySet()) {
                String key = renameMap.getOrDefault(entry.getKey(), entry.getKey());
                newSchemas.put(key, entry.getValue());
            }
            components.setSchemas(newSchemas);

            // 4. 更新所有 $ref 引用
            Map<String, String> refMap = new LinkedHashMap<>();
            renameMap.forEach((oldName, newName) ->
                    refMap.put("#/components/schemas/" + oldName, "#/components/schemas/" + newName));

            updateOpenApiRefs(openApi, refMap);
        };
    }

    /**
     * 将 FQN Schema 名称简化为可读格式。
     * <p>
     * 通过类名首字母大写判断是否为内部类的外部类名。
     */
    private String simplifySchemaName(String fqn) {
        if (!fqn.contains(".")) return fqn;

        String[] parts = fqn.split("\\.");
        String className = parts[parts.length - 1];

        // 检查倒数第二段是否为类名（首字母大写 → 内部类场景）
        if (parts.length >= 2) {
            String secondLast = parts[parts.length - 2];
            if (!secondLast.isEmpty() && Character.isUpperCase(secondLast.charAt(0))) {
                return secondLast + "." + className;
            }
        }
        return className;
    }

    /**
     * 递归更新 OpenAPI 文档中所有 $ref 引用。
     */
    @SuppressWarnings("unchecked")
    private void updateOpenApiRefs(OpenAPI openApi, Map<String, String> refMap) {
        // 更新路径中的引用
        if (openApi.getPaths() != null) {
            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(op -> updateOperationRefs(op, refMap)));
        }
        // 更新 Schema 自身的属性引用
        if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
            openApi.getComponents().getSchemas().values().forEach(schema ->
                    updateSchemaRefs(schema, refMap));
        }
    }

    private void updateOperationRefs(Operation op, Map<String, String> refMap) {
        if (op.getRequestBody() != null && op.getRequestBody().getContent() != null) {
            op.getRequestBody().getContent().values().forEach(mt ->
                    updateSchemaRefs(mt.getSchema(), refMap));
        }
        if (op.getResponses() != null) {
            op.getResponses().values().forEach(resp -> {
                if (resp.getContent() != null) {
                    resp.getContent().values().forEach(mt ->
                            updateSchemaRefs(mt.getSchema(), refMap));
                }
            });
        }
        if (op.getParameters() != null) {
            op.getParameters().forEach(p -> updateSchemaRefs(p.getSchema(), refMap));
        }
    }

    @SuppressWarnings("unchecked")
    private void updateSchemaRefs(Schema<?> schema, Map<String, String> refMap) {
        if (schema == null) return;

        // $ref
        if (schema.get$ref() != null) {
            String newRef = refMap.get(schema.get$ref());
            if (newRef != null) schema.set$ref(newRef);
        }
        // properties
        if (schema.getProperties() != null) {
            schema.getProperties().values()
                    .forEach(s -> updateSchemaRefs((Schema<?>) s, refMap));
        }
        // items (array)
        if (schema.getItems() != null) {
            updateSchemaRefs(schema.getItems(), refMap);
        }
        // allOf / oneOf / anyOf
        if (schema.getAllOf() != null) schema.getAllOf().forEach(s -> updateSchemaRefs(s, refMap));
        if (schema.getOneOf() != null) schema.getOneOf().forEach(s -> updateSchemaRefs(s, refMap));
        if (schema.getAnyOf() != null) schema.getAnyOf().forEach(s -> updateSchemaRefs(s, refMap));
        // additionalProperties
        if (schema.getAdditionalProperties() instanceof Schema<?> ap) {
            updateSchemaRefs(ap, refMap);
        }
        // discriminator mapping
        Discriminator discriminator = schema.getDiscriminator();
        if (discriminator != null && discriminator.getMapping() != null) {
            Map<String, String> mapping = discriminator.getMapping();
            Map<String, String> updated = new LinkedHashMap<>();
            mapping.forEach((key, ref) -> updated.put(key, refMap.getOrDefault(ref, ref)));
            discriminator.setMapping(updated);
        }
    }

}
