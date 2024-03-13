package cn.projectan.strix;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.TemplateType;
import com.baomidou.mybatisplus.generator.config.querys.ClickHouseQuery;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.query.SQLQuery;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * ClickHouse 代码生成器 新版本
 *
 * @author ProjectAn
 * @date 2023-06-16
 */
public class ClickHouseGenerator {

    private static final String DB_URL = "jdbc:clickhouse://192.168.31.188:8123/strix";

    private static final String DB_USER = "root";

    private static final String DB_PWD = "S1V/holh";

    public static void main(String[] args) {
        String projectPath = System.getProperty("user.dir");

        // 数据库配置
        DataSourceConfig.Builder dataSourceConfigBuilder = new DataSourceConfig.Builder(DB_URL, DB_USER, DB_PWD)
                .dbQuery(new ClickHouseQuery())
                .databaseQueryClass(SQLQuery.class)
                .schema("strix");

        FastAutoGenerator.create(dataSourceConfigBuilder)
                .globalConfig(builder -> builder.outputDir(projectPath + "/src/main/java")
                        .author("ProjectAn")
                        .dateType(DateType.TIME_PACK)
                        .commentDate("yyyy-MM-dd")
                        .disableOpenDir()
                        .build())
                .packageConfig(builder -> builder.parent("cn.projectan.strix")
                        .entity("model.db")
                        .service("service")
                        .serviceImpl("service.impl")
                        .mapper("mapper")
                        .xml("mapper.xml")
                        .controller("controller")
                        .pathInfo(Collections.singletonMap(OutputFile.xml, projectPath + "/src/main/resources/mapper"))
                        .build())
                .templateConfig(builder -> builder.disable(TemplateType.CONTROLLER)
                        .entity("templates/mp/entity.java")
                        .service("templates/mp/service.java")
                        .serviceImpl("templates/mp/serviceImpl.java")
                        .mapper("templates/mp/mapper.java")
                        .xml("templates/mp/mapper.xml")
                        .build())
                .strategyConfig((scanner, builder) -> builder.addInclude(getTables(scanner.apply("请输入表名，多个英文逗号分隔。所有输入 all")))
                        .addTablePrefix("sys_", "tab_") // 表前缀过滤
                        // Entity 策略配置
                        .entityBuilder()
                        .enableLombok()
                        .enableChainModel()
                        // Service 策略配置
                        .serviceBuilder()
                        .formatServiceFileName("%sService")
                        .formatServiceImplFileName("%sServiceImpl")
                        .build())
                .execute();

    }

    // 处理 all 情况
    protected static List<String> getTables(String tables) {
        return "all".equals(tables) ? Collections.emptyList() : Arrays.asList(tables.split(","));
    }

}
