package com.hzj.aicodemother.generator;

import cn.hutool.core.lang.Dict;
import cn.hutool.setting.yaml.YamlUtil;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.codegen.Generator;
import com.mybatisflex.codegen.config.ColumnConfig;
import com.mybatisflex.codegen.config.EntityConfig;
import com.mybatisflex.codegen.config.GlobalConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.util.Map;

public class MyBatisCodeGenerator {

    // 需要生成的表名
    private static final String[] TABLE_NAMES = {"user"};

    public static void main(String[] args) {
        // 获取数据源信息（datasource 配置在 application-druid.yml 中，主配置通过 spring.profiles.active=druid 激活它）
        Dict dict = YamlUtil.loadByPath("application-druid.yml");
        Map<String, Object> dataSourceConfig = dict.getByPath("spring.datasource");
        String url = String.valueOf(dataSourceConfig.get("url"));
        String username = String.valueOf(dataSourceConfig.get("username"));
        String password = String.valueOf(dataSourceConfig.get("password"));
        // 配置数据源
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        // 创建配置内容
        GlobalConfig globalConfig = createGlobalConfig();

        // 通过 datasource 和 globalConfig 创建代码生成器
        Generator generator = new Generator(dataSource, globalConfig);

        // 生成代码
        generator.generate();
    }

    // 详细配置见：https://mybatis-flex.com/zh/others/codegen.html
    public static GlobalConfig createGlobalConfig() {
        // 创建配置内容
        GlobalConfig globalConfig = new GlobalConfig();

        // 设置根包，建议先生成到一个临时目录下，生成代码后，再移动到项目目录下
        globalConfig.getPackageConfig()
                .setBasePackage("com.hzj.aicodemother.genresult");

        // 设置表前缀和只生成哪些表，setGenerateTable 未配置时，生成所有表
        globalConfig.getStrategyConfig()
                .setGenerateTable(TABLE_NAMES)
                // 设置逻辑删除的默认字段名称
                .setLogicDeleteColumn("isDelete")
                // 全局列配置：所有表中名为 id 的列使用数据库自增主键（生成 @Id(keyType = KeyType.Auto)）
                // 若某张表的主键不叫 id，可用 setColumnConfig("表名", ColumnConfig.create().setColumnName("主键列名").setKeyType(...)) 单独设置
                .setColumnConfig(ColumnConfig.create()
                        .setColumnName("id")
                        .setKeyType(KeyType.Auto));

        // 使用自定义 Controller 模板（classpath: codegen/controller.tpl）
        // 模板基于官方内置模板定制：统一返回 BaseResponse、新增批量增删/按 id 列表查询/总数统计接口、分页改为显式 pageNum/pageSize 参数
        globalConfig.getTemplateConfig()
                .setController("/codegen/controller.tpl");

        // 设置生成 entity 并启用 Lombok；启用 Swagger 注解（springdoc v3，配合 knife4j 在线调试）
        globalConfig.enableEntity()
                .setWithLombok(true)
                .setJdkVersion(21)
                .setWithSwagger(true)
                .setSwaggerVersion(EntityConfig.SwaggerVersion.DOC);

        // 设置生成 mapper
        globalConfig.enableMapper();
        globalConfig.enableMapperXml();

        // 设置生成 service
        globalConfig.enableService();
        globalConfig.enableServiceImpl();

        // 设置生成 controller
        globalConfig.enableController();

        // 设置生成时间和字符串为空，避免多余的代码改动
        globalConfig.getJavadocConfig()
                .setAuthor("<a href=\"https://github.com/\">程序员拉丽</a>")
                .setSince("");
        return globalConfig;
    }
}
