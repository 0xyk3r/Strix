package cn.projectan.strix.initializer.system;

import cn.projectan.strix.core.module.oss.StrixOssStore;
import cn.projectan.strix.model.constant.system.OperatorType;
import cn.projectan.strix.model.db.system.Dict;
import cn.projectan.strix.model.db.system.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import cn.projectan.strix.model.dict.system.DictDataStatus;
import cn.projectan.strix.model.dict.system.DictProvided;
import cn.projectan.strix.model.dict.system.DictStatus;
import cn.projectan.strix.service.system.DictDataService;
import cn.projectan.strix.service.system.DictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 字典常量同步初始化器
 *
 * @author ProjectAn
 * @since 2023/6/9 15:18
 */
@Slf4j
@Order(20)
@Component
@ConditionalOnBean(StrixOssStore.class)
@RequiredArgsConstructor
public class StrixDictSyncInitializer implements ApplicationRunner {

    private final DictService dictService;
    private final DictDataService dictDataService;
    private final CacheManager cacheManager;

    private static final String DICT_BASE_PACKAGE = "cn.projectan.strix.model.dict";

    @Override
    public void run(ApplicationArguments args) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 使用 ClassPath 扫描字典类
            Set<Class<?>> dictClassSet = scanDictClasses();
            if (dictClassSet.isEmpty()) {
                log.info("Strix Dict: 未找到字典类，跳过同步");
                return;
            }

            // 2. 一次性查询所有数据库数据
            Map<String, Dict> dbDictMap = loadDBDict();
            Map<String, List<DictData>> dbDictDataMap = loadDbDictData();

            // 3. 解析所有字典类并收集批量操作
            BatchOperations batchOps = new BatchOperations();
            for (Class<?> clazz : dictClassSet) {
                try {
                    processDictClass(clazz, dbDictMap, dbDictDataMap, batchOps);
                } catch (Exception e) {
                    log.error("Strix Dict: 处理字典类 {} 失败", clazz.getName(), e);
                }
            }

            // 4. 批量执行数据库操作
            executeBatchOperations(batchOps);

            long endTime = System.currentTimeMillis();
            log.info("Strix Dict: 同步完成, 处理了 {} 个字典, 耗时 {} ms", dictClassSet.size(), (endTime - startTime));
        } catch (Exception e) {
            log.error("Strix Dict: 同步过程发生错误", e);
        }
    }

    /**
     * 扫描所有字典类（使用 ClassPath 扫描）
     */
    private Set<Class<?>> scanDictClasses() {
        Set<Class<?>> dictClasses = new HashSet<>();
        try {
            ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AssignableTypeFilter(BaseDict.class));

            scanner.findCandidateComponents(DICT_BASE_PACKAGE).forEach(beanDef -> {
                try {
                    if (beanDef.getBeanClassName() == null) {
                        return;
                    }
                    Class<?> clazz = ClassUtils.forName(beanDef.getBeanClassName(), this.getClass().getClassLoader());
                    if (clazz.isAnnotationPresent(cn.projectan.strix.model.annotation.Dict.class)) {
                        dictClasses.add(clazz);
                    }
                } catch (ClassNotFoundException e) {
                    log.error("Strix Dict: 加载字典类失败 - {}", beanDef.getBeanClassName(), e);
                }
            });
        } catch (Exception e) {
            log.error("Strix Dict: 扫描字典类失败", e);
        }
        return dictClasses;
    }

    /**
     * 一次性加载数据库中的所有字典
     */
    private Map<String, Dict> loadDBDict() {
        return dictService.lambdaQuery()
                .eq(Dict::getProvided, DictProvided.YES)
                .list()
                .stream()
                .collect(Collectors.toMap(Dict::getKey, dict -> dict));
    }

    /**
     * 一次性加载数据库中的所有字典数据（按 key 分组）
     */
    private Map<String, List<DictData>> loadDbDictData() {
        List<DictData> allDictData = dictDataService.lambdaQuery()
                .in(DictData::getKey, dictService.lambdaQuery()
                        .eq(Dict::getProvided, DictProvided.YES)
                        .list()
                        .stream()
                        .map(Dict::getKey)
                        .collect(Collectors.toList()))
                .list();

        return allDictData.stream()
                .collect(Collectors.groupingBy(DictData::getKey));
    }

    /**
     * 处理单个字典类
     */
    private void processDictClass(Class<?> clazz,
                                  Map<String, Dict> dbDictMap,
                                  Map<String, List<DictData>> dbDictDataMap,
                                  BatchOperations batchOps) throws Exception {
        cn.projectan.strix.model.annotation.Dict annotationDict = clazz.getAnnotation(cn.projectan.strix.model.annotation.Dict.class);
        String key = StringUtils.hasText(annotationDict.key()) ? annotationDict.key() : clazz.getSimpleName();
        String name = StringUtils.hasText(annotationDict.value()) ? annotationDict.value() : clazz.getSimpleName();

        // 构建字典对象
        Dict dict = new Dict(key, name, 0, DictStatus.ENABLE, null, 0, DictProvided.YES)
                .setCreatedByType(OperatorType.SYSTEM)
                .setUpdatedByType(OperatorType.SYSTEM);

        // 解析字典数据字段
        List<DictData> dictDataList = parseDictDataFields(clazz, key, dict);

        // 收集批量操作
        collectBatchOperations(dict, dictDataList, dbDictMap, dbDictDataMap, batchOps);
    }

    /**
     * 解析字典类的字段，生成 DictData 列表
     */
    private List<DictData> parseDictDataFields(Class<?> clazz, String key, Dict dict) throws Exception {
        List<DictData> dictDataList = new ArrayList<>();
        Field[] fields = clazz.getDeclaredFields();
        boolean dataTypeSet = false;

        for (Field field : fields) {
            cn.projectan.strix.model.annotation.DictData annotationDictData = field.getAnnotation(cn.projectan.strix.model.annotation.DictData.class);

            if (annotationDictData == null) {
                continue; // 跳过没有 @DictData 注解的字段
            }

            // 第一个带有 @DictData 注解的字段决定字典的数据类型
            if (!dataTypeSet) {
                String typeName = field.getType().getName();
                int dataType = convertTypeName(typeName);
                dict.setDataType(dataType);
                dataTypeSet = true;
            }

            String value = field.get(null).toString();
            String label = StringUtils.hasText(annotationDictData.label()) ? annotationDictData.label() : field.getName();
            int sort = annotationDictData.sort() >= 0 ? annotationDictData.sort() : dictDataList.size();
            String style = annotationDictData.style();

            DictData dictData = new DictData(key, value, label, sort, style, DictDataStatus.ENABLE, null)
                    .setCreatedByType(OperatorType.SYSTEM)
                    .setUpdatedByType(OperatorType.SYSTEM);
            dictDataList.add(dictData);
        }

        return dictDataList;
    }

    /**
     * 批量操作容器
     */
    private static class BatchOperations {
        List<Dict> dictToInsert = new ArrayList<>();
        List<Dict> dictToUpdate = new ArrayList<>();
        List<DictData> dictDataToInsert = new ArrayList<>();
        List<DictData> dictDataToUpdate = new ArrayList<>();
        List<DictData> dictDataToDelete = new ArrayList<>();
        // 记录需要更新版本号的字典 key 集合
        Set<String> dictKeysNeedVersionUpdate = new HashSet<>();
    }

    /**
     * 收集需要批量操作的数据
     */
    private void collectBatchOperations(Dict dict, List<DictData> dictDataList,
                                        Map<String, Dict> dbDictMap,
                                        Map<String, List<DictData>> dbDictDataMap,
                                        BatchOperations batchOps) {
        String key = dict.getKey();
        Dict dbDict = dbDictMap.get(key);
        List<DictData> dbDictDataList = dbDictDataMap.get(key);

        // 处理字典本身
        if (dbDict == null) {
            // 新字典，需要插入
            batchOps.dictToInsert.add(dict);
            batchOps.dictDataToInsert.addAll(dictDataList);
            // 新字典插入后需要更新版本号（虽然是新的，但后续可能有 DictData 操作）
            batchOps.dictKeysNeedVersionUpdate.add(key);
        } else {
            // 已存在的字典，检查是否需要更新
            if (!dbDict.getName().equals(dict.getName()) || !dbDict.getDataType().equals(dict.getDataType())) {
                // 将更新后的值设置到原始对象上
                dbDict.setName(dict.getName());
                dbDict.setDataType(dict.getDataType());
                batchOps.dictToUpdate.add(dbDict);
                batchOps.dictKeysNeedVersionUpdate.add(key);
            }

            if (dbDictDataList == null || dbDictDataList.isEmpty()) {
                // 数据库中没有字典数据，全部插入
                batchOps.dictDataToInsert.addAll(dictDataList);
                batchOps.dictKeysNeedVersionUpdate.add(key);
            } else {
                // 构建数据库字典数据的映射，提升查找效率 O(1)
                Map<String, DictData> dbDataMap = dbDictDataList.stream()
                        .collect(Collectors.toMap(DictData::getValue, item -> item));

                // 检查每个新字典数据
                for (DictData dictData : dictDataList) {
                    DictData dbItem = dbDataMap.get(dictData.getValue());
                    if (dbItem == null) {
                        // 新数据，需要插入
                        batchOps.dictDataToInsert.add(dictData);
                        batchOps.dictKeysNeedVersionUpdate.add(key);
                    } else {
                        // 已存在，检查是否需要更新
                        if (!dbItem.getLabel().equals(dictData.getLabel()) ||
                                !dbItem.getSort().equals(dictData.getSort()) ||
                                !Objects.equals(dbItem.getStyle(), dictData.getStyle())) {
                            // 更新原始对象的值
                            dbItem.setLabel(dictData.getLabel());
                            dbItem.setSort(dictData.getSort());
                            dbItem.setStyle(dictData.getStyle());
                            batchOps.dictDataToUpdate.add(dbItem);
                            batchOps.dictKeysNeedVersionUpdate.add(key);
                        }
                    }
                }

                // 构建新字典数据的值集合
                Set<String> newValueSet = dictDataList.stream()
                        .map(DictData::getValue)
                        .collect(Collectors.toSet());

                // 找出需要删除的数据（数据库中有但新数据中没有）
                for (DictData dbItem : dbDictDataList) {
                    if (!newValueSet.contains(dbItem.getValue())) {
                        batchOps.dictDataToDelete.add(dbItem);
                        batchOps.dictKeysNeedVersionUpdate.add(key);
                    }
                }
            }
        }
    }

    /**
     * 批量执行数据库操作
     */
    private void executeBatchOperations(BatchOperations batchOps) {
        // 1. 批量插入字典
        if (!batchOps.dictToInsert.isEmpty()) {
            dictService.saveBatch(batchOps.dictToInsert);
            log.info("Strix Dict: 插入字典 {} 条", batchOps.dictToInsert.size());
        }

        // 2. 批量更新字典（直接使用 MyBatis-Plus updateById，不走 DictService）
        if (!batchOps.dictToUpdate.isEmpty()) {
            dictService.updateBatchById(batchOps.dictToUpdate);
            log.info("Strix Dict: 更新字典 {} 条", batchOps.dictToUpdate.size());
        }

        // 3. 批量插入字典数据
        if (!batchOps.dictDataToInsert.isEmpty()) {
            dictDataService.saveBatch(batchOps.dictDataToInsert);
            log.info("Strix Dict: 插入字典数据 {} 条", batchOps.dictDataToInsert.size());
        }

        // 4. 批量更新字典数据
        if (!batchOps.dictDataToUpdate.isEmpty()) {
            dictDataService.updateBatchById(batchOps.dictDataToUpdate);
            log.info("Strix Dict: 更新字典数据 {} 条", batchOps.dictDataToUpdate.size());
        }

        // 5. 批量删除字典数据
        if (!batchOps.dictDataToDelete.isEmpty()) {
            List<String> idsToDelete = batchOps.dictDataToDelete.stream()
                    .map(DictData::getId)
                    .collect(Collectors.toList());
            dictDataService.removeBatchByIds(idsToDelete);
            log.info("Strix Dict: 删除字典数据 {} 条", batchOps.dictDataToDelete.size());
        }

        // 6. 统一更新版本号
        if (!batchOps.dictKeysNeedVersionUpdate.isEmpty()) {
            updateDictVersions(batchOps.dictKeysNeedVersionUpdate);
            log.info("Strix Dict: 更新字典版本 {} 个", batchOps.dictKeysNeedVersionUpdate.size());
        }

        // 7. 清理缓存
        clearDictCache(batchOps.dictKeysNeedVersionUpdate);
    }

    /**
     * 统一更新字典版本号（每个字典的所有操作完成后，版本号 +1）
     */
    private void updateDictVersions(Set<String> dictKeys) {
        List<Dict> dictToUpdate = dictService.lambdaQuery()
                .in(Dict::getKey, dictKeys)
                .list();

        dictToUpdate.forEach(dict -> dict.setVersion(dict.getVersion() + 1));
        dictService.updateBatchById(dictToUpdate);
    }

    /**
     * 清理字典相关缓存
     */
    private void clearDictCache(Set<String> dictKeys) {
        // 清理字典版本缓存
        var versionCache = cacheManager.getCache("strix:dict:versionMap");
        if (versionCache != null) {
            versionCache.clear();
        }

        // 清理每个字典的详情缓存
        var dictRespCache = cacheManager.getCache("strix:dict:dictResp");
        if (dictRespCache != null) {
            for (String key : dictKeys) {
                dictRespCache.evict(key);
            }
        }
    }

    private int convertTypeName(String typeName) {
        return switch (typeName) {
            case "java.lang.String" -> 1;
            case "java.lang.Integer", "int" -> 2;
            case "java.lang.Long", "long" -> 3;
            case "java.lang.Float", "float" -> 4;
            case "java.lang.Double", "double" -> 5;
            case "java.lang.Boolean", "boolean" -> 6;
            case "java.lang.Byte", "byte" -> 7;
            case "java.lang.Short", "short" -> 8;
            default -> 0;
        };
    }

}
