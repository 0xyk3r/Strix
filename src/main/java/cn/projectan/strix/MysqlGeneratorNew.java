package cn.projectan.strix;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.TemplateType;
import com.baomidou.mybatisplus.generator.config.converts.MySqlTypeConvert;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.fill.Column;
import com.baomidou.mybatisplus.generator.fill.Property;
import com.baomidou.mybatisplus.generator.keywords.MySqlKeyWordsHandler;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * MyBatisPlus 代码生成器 新版本
 *
 * @author 安炯奕
 * @date 2022-07-15
 */
public class MysqlGeneratorNew {

    private static final String DB_URL = "jdbc:mysql://huibochepolardb001.rwlb.rds.aliyuncs.com:3306/strix?useSSL=true&serverTimezone=Asia/Shanghai&autoReconnect=true";

    private static final String DB_USER = "strix";

    private static final String DB_PWD = "Str1XP8Pr0j3cTAAAm";

    public static void main(String[] args) {
        String projectPath = System.getProperty("user.dir");

        // 数据库配置
        DataSourceConfig.Builder dataSourceConfigBuilder = new DataSourceConfig.Builder(DB_URL, DB_USER, DB_PWD)
                .typeConvert(new MySqlTypeConvert())
                .keyWordsHandler(new MySqlKeyWordsHandler());

        FastAutoGenerator.create(dataSourceConfigBuilder)
                .globalConfig(builder -> {
                    builder.outputDir(projectPath + "/src/main/java")
                            .author("安炯奕")
                            .dateType(DateType.TIME_PACK)
                            .commentDate("yyyy-MM-dd")
                            .build();
                })
                .packageConfig(builder -> {
                    builder.parent("cn.projectan.strix")
                            .entity("model.db")
                            .service("service")
                            .serviceImpl("service.impl")
                            .mapper("mapper")
                            .xml("mapper.xml")
                            .controller("controller")
                            .other("other")
                            .pathInfo(Collections.singletonMap(OutputFile.xml, projectPath + "/src/main/resources/mapper"))
                            .build();
                })
                .templateConfig(builder -> {
                    builder.disable(TemplateType.CONTROLLER)
                            .build();
                })
                .strategyConfig((scanner, builder) -> {
                    builder.addInclude(getTables(scanner.apply("请输入表名，多个英文逗号分隔。所有输入 all")))
                            .addTablePrefix("tab_") // 表前缀过滤
                            // Entity 策略配置
                            .entityBuilder()
                            .enableLombok()
                            .enableChainModel()
                            .superClass(BaseModel.class)
                            .addSuperEntityColumns("id", "deleted_status", "create_time", "create_by", "update_time", "update_by")
                            .idType(IdType.ASSIGN_ID)
                            .logicDeleteColumnName("deleted_status")
                            .logicDeletePropertyName("deletedStatus")
                            .addTableFills(new Column("create_time", FieldFill.INSERT))
                            .addTableFills(new Property("createTime", FieldFill.INSERT))
                            .addTableFills(new Column("update_time", FieldFill.INSERT_UPDATE))
                            .addTableFills(new Property("update_time", FieldFill.INSERT_UPDATE))
                            .addTableFills(new Column("deleted_status", FieldFill.INSERT))
                            .addTableFills(new Property("deleted_status", FieldFill.INSERT))
                            // Service 策略配置
                            .serviceBuilder()
                            .formatServiceFileName("%sService")
                            .formatServiceImplFileName("%sServiceImp")
                            .build();
                })
                .execute();

    }

    // 处理 all 情况
    protected static List<String> getTables(String tables) {
        return "all".equals(tables) ? Collections.emptyList() : Arrays.asList(tables.split(","));
    }

}
