//package cn.projectan.strix;
//
//import com.baomidou.mybatisplus.annotation.FieldFill;
//import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;
//import com.baomidou.mybatisplus.core.toolkit.StringPool;
//import com.baomidou.mybatisplus.generator.AutoGenerator;
//import com.baomidou.mybatisplus.generator.InjectionConfig;
//import com.baomidou.mybatisplus.generator.config.*;
//import com.baomidou.mybatisplus.generator.config.po.TableFill;
//import com.baomidou.mybatisplus.generator.config.po.TableInfo;
//import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
//import org.springframework.util.StringUtils;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Scanner;
//
///**
// * MyBatisPlus 代码生成器
// *
// * @author 安炯奕
// * @date 2021-05-02
// */
//public class MysqlGenerator {
//
//    public static void main(String[] args) {
//        // 代码生成器
//        AutoGenerator mpg = new AutoGenerator();
//
//        // 全局配置
//        GlobalConfig gc = new GlobalConfig();
//        String projectPath = System.getProperty("user.dir");
//
//        gc.setOutputDir(projectPath + "/src/main/java");
//        gc.setAuthor("安炯奕");
//        gc.setOpen(false);
//        // 是否覆盖生成文件
//        gc.setFileOverride(false);
//        gc.setServiceName("%sService");
//        // gc.setSwagger2(true); 实体属性 Swagger2 注解
//        mpg.setGlobalConfig(gc);
//
//        // 数据源配置
//        DataSourceConfig dsc = new DataSourceConfig();
//        dsc.setUrl("jdbc:mysql://huibochepolardb001.rwlb.rds.aliyuncs.com:3306/strix?useSSL=true");
//        // dsc.setSchemaName("public");
//        dsc.setDriverName("com.mysql.cj.jdbc.Driver");
//        dsc.setUsername("strix");
//        dsc.setPassword("Str1XP8Pr0j3cTAAAm");
//        mpg.setDataSource(dsc);
//
//        // 包配置
//        PackageConfig pc = new PackageConfig();
//        pc.setParent("cn.projectan.strix");
//        pc.setEntity("model.db");
//        pc.setMapper("mapper");
//        pc.setService("service");
//        pc.setServiceImpl("service.impl");
//        mpg.setPackageInfo(pc);
//
//        // 自定义配置
//        InjectionConfig cfg = new InjectionConfig() {
//            @Override
//            public void initMap() {
//                // to do nothing
//            }
//        };
//
//        String mapperXmlTemplatePath = "/templates/mapper.xml.vm";
//        String mapperJavaTemplatePath = "/templates/mapper.java.vm";
//        String entityTemplatePath = "/templates/entity.java.vm";
//        String serviceTemplatePath = "/templates/service.java.vm";
//        String serviceImplTemplatePath = "/templates/serviceImpl.java.vm";
//
//        // 自定义输出配置
//        List<FileOutConfig> focList = new ArrayList<>();
//        // 自定义配置会被优先输出
//        focList.add(new FileOutConfig(mapperXmlTemplatePath) {
//            @Override
//            public String outputFile(TableInfo tableInfo) {
//                return projectPath + "/src/main/resources/mapper/"
//                        + tableInfo.getEntityName() + "Mapper" + StringPool.DOT_XML;
//            }
//        });
//        focList.add(new FileOutConfig(mapperJavaTemplatePath) {
//            @Override
//            public String outputFile(TableInfo tableInfo) {
//                return projectPath + "/src/main/java/cn/projectan/strix/mapper/"
//                        + tableInfo.getEntityName() + "Mapper" + StringPool.DOT_JAVA;
//            }
//        });
//        focList.add(new FileOutConfig(entityTemplatePath) {
//            @Override
//            public String outputFile(TableInfo tableInfo) {
//                return projectPath + "/src/main/java/cn/projectan/strix/model/db/"
//                        + tableInfo.getEntityName() + StringPool.DOT_JAVA;
//            }
//        });
//        focList.add(new FileOutConfig(serviceTemplatePath) {
//            @Override
//            public String outputFile(TableInfo tableInfo) {
//                return projectPath + "/src/main/java/cn/projectan/strix/service/"
//                        + tableInfo.getEntityName() + "Service" + StringPool.DOT_JAVA;
//            }
//        });
//        focList.add(new FileOutConfig(serviceImplTemplatePath) {
//            @Override
//            public String outputFile(TableInfo tableInfo) {
//                return projectPath + "/src/main/java/cn/projectan/strix/service/impl/"
//                        + tableInfo.getEntityName() + "ServiceImpl" + StringPool.DOT_JAVA;
//            }
//        });
//        cfg.setFileOutConfigList(focList);
//        mpg.setCfg(cfg);
//
//        // 配置模板
//        TemplateConfig templateConfig = new TemplateConfig();
//        templateConfig.setController(null);
//        templateConfig.setEntity(null);
//        templateConfig.setMapper(null);
//        templateConfig.setEntityKt(null);
//        templateConfig.setService(null);
//        templateConfig.setServiceImpl(null);
//        templateConfig.setXml(null);
//        mpg.setTemplate(templateConfig);
//
//        // 自定义需要填充的字段 数据库中的字段
//        List<TableFill> tableFillList = new ArrayList<>();
//        tableFillList.add(new TableFill("update_time", FieldFill.INSERT_UPDATE));
//        tableFillList.add(new TableFill("create_time", FieldFill.INSERT));
//        tableFillList.add(new TableFill("deleted_status", FieldFill.INSERT));
//
//        // 策略配置
//        StrategyConfig strategy = new StrategyConfig();
//        strategy.setNaming(NamingStrategy.underline_to_camel);
//        strategy.setColumnNaming(NamingStrategy.underline_to_camel);
//        strategy.setSuperEntityClass("cn.projectan.strix.model.db.base.BaseModel");
//        strategy.setEntityLombokModel(true);
//        strategy.setRestControllerStyle(true);
//        // 写于父类中的公共字段
//        strategy.setSuperEntityColumns("id", "deleted_status", "create_time", "create_by", "update_time", "update_by");
//        strategy.setControllerMappingHyphenStyle(true);
//        // 表名前缀
//        strategy.setTablePrefix("tab_");
//        strategy.setTableFillList(tableFillList);
//        // 逻辑删除字段名
//        strategy.setLogicDeleteFieldName("deleted_status");
//        strategy.setInclude(scanner("表名，多个英文逗号分割").split(","));
//        mpg.setStrategy(strategy);
//        mpg.execute();
//    }
//
//    /**
//     * 读取控制台内容
//     */
//    public static String scanner(String tip) {
//        Scanner scanner = new Scanner(System.in);
//        System.out.println("请输入" + tip + "：");
//        if (scanner.hasNext()) {
//            String ipt = scanner.next();
//            if (StringUtils.hasText(ipt)) {
//                return ipt;
//            }
//        }
//        throw new MybatisPlusException("请输入正确的" + tip + "！");
//    }
//
//}
