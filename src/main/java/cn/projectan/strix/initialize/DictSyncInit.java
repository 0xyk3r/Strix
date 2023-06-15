package cn.projectan.strix.initialize;

import cn.projectan.strix.config.StrixOssConfig;
import cn.projectan.strix.model.constant.DictDataStatus;
import cn.projectan.strix.model.constant.DictProvided;
import cn.projectan.strix.model.constant.DictStatus;
import cn.projectan.strix.model.db.Dict;
import cn.projectan.strix.model.db.DictData;
import cn.projectan.strix.model.request.system.dict.DictDataUpdateReq;
import cn.projectan.strix.model.request.system.dict.DictUpdateReq;
import cn.projectan.strix.model.response.common.CommonDictResp;
import cn.projectan.strix.model.response.system.dict.DictDataListResp;
import cn.projectan.strix.service.DictService;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 同步字典常量至数据库
 *
 * @author 安炯奕
 * @date 2023/6/9 15:18
 */
@Slf4j
@Order(20)
@Component
@ConditionalOnBean(StrixOssConfig.class)
public class DictSyncInit implements ApplicationRunner {

    @Autowired
    private DictService dictService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Reflections reflections = new Reflections("cn.projectan.strix.model.constant");
        Set<Class<?>> dictClassSet = reflections.getTypesAnnotatedWith(cn.projectan.strix.model.annotation.Dict.class);

        dictClassSet.forEach(clazz -> {
            cn.projectan.strix.model.annotation.Dict annotationDict = clazz.getAnnotation(cn.projectan.strix.model.annotation.Dict.class);
            String key = StringUtils.hasText(annotationDict.key()) ? annotationDict.key() : clazz.getSimpleName();
            String name = StringUtils.hasText(annotationDict.value()) ? annotationDict.value() : clazz.getSimpleName();

            Dict dict = new Dict(key, name, 0, DictStatus.ENABLE, null, 0, DictProvided.YES);
            dict.setCreateBy("SYSTEM");
            dict.setUpdateBy("SYSTEM");
            List<DictData> dictDataList = new ArrayList<>();

            try {
                Field[] fields = clazz.getDeclaredFields();
                for (int i = 0; i < fields.length; i++) {
                    Field field = fields[i];
                    cn.projectan.strix.model.annotation.DictData annotationDictData = field.getAnnotation(cn.projectan.strix.model.annotation.DictData.class);

                    if (i == 0) {
                        // 获取 field 的类型
                        String typeName = field.getType().getName();
                        int dataType = convertTypeName(typeName);
                        dict.setDataType(dataType);
                    }

                    String value = field.get(null).toString();
                    String label = StringUtils.hasText(annotationDictData.label()) ? annotationDictData.label() : field.getName();
                    int sort = annotationDictData.sort() >= 0 ? annotationDictData.sort() : i;
                    String style = annotationDictData.style();
                    DictData dictData = new DictData(key, value, label, sort, style, DictDataStatus.ENABLE, null);
                    dictData.setCreateBy("SYSTEM");
                    dictData.setUpdateBy("SYSTEM");
                    dictDataList.add(dictData);
                }
                syncToDb(dict, dictDataList);
            } catch (Exception e) {
                log.error("同步字典常量至数据库失败", e);
            }

        });

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
            default -> 0;
        };
    }

    private void syncToDb(Dict dict, List<DictData> dictDataList) {
        CommonDictResp dictResp = dictService.getDictResp(dict.getKey());

        if (dictResp == null) {
            dictService.saveDict(dict);
            dictDataList.forEach(dictData -> dictService.saveDictData(dictData));
        } else {
            Dict dbDict = dictService.getById(dictResp.getId());
            try {
                dictService.updateDict(dbDict, new DictUpdateReq(dict.getKey(), dict.getName(), dict.getDataType(), null, null));
            } catch (Exception ignore) {
                // 这里可能会由于没有发生任何变化而抛出异常，忽略即可
            }

            // dictDataList 为新的字典常量，dictResp.getDictDataList() 为数据库中已存在的字典常量 存在于数据库则更新，不存在则新增
            dictDataList.forEach(dictData -> {
                DictDataListResp.DictDataItem cacheDictData = dictResp.getDictDataList().stream().filter(d -> d.getValue().equals(dictData.getValue())).findFirst().orElse(null);
                if (cacheDictData == null) {
                    dictService.saveDictData(dictData);
                } else {
                    try {
                        // 根据缓存 组装一个 DB DictData 对象
                        DictData dbDictData = new DictData(cacheDictData.getKey(), cacheDictData.getValue(), cacheDictData.getLabel(), cacheDictData.getSort(), cacheDictData.getStyle(), cacheDictData.getStatus(), cacheDictData.getRemark());
                        dbDictData.setId(cacheDictData.getId());
                        dictService.updateDictData(dbDictData, new DictDataUpdateReq(dictData.getKey(), dictData.getValue(), dictData.getLabel(), dictData.getSort(), dictData.getStyle(), null, null));
                    } catch (Exception ignore) {
                        // 这里可能会由于没有发生任何变化而抛出异常，忽略即可
                    }
                }
            });
            // dictResp.getDictDataList() 为数据库中已存在的字典常量，dictDataList 为新的字典常量，数据库中存在但是新的字典常量中不存在的字典常量需要删除
            dictResp.getDictDataList().forEach(dictData -> {
                DictData cacheDictData = dictDataList.stream().filter(d -> d.getValue().equals(dictData.getValue())).findFirst().orElse(null);
                if (cacheDictData == null) {
                    dictService.removeDictData(new DictData(dictData.getKey(), dictData.getValue(), null, null, null, null, null));
                }
            });
        }

    }

}
