# 系统地区修改层级关系操作可能发生业务数据异常等问题

    在业务数据Bean中增加一个注解@SystemRegion(type='xxx')，用于标记该字段为系统地区字段，当修改层级关系时，系统会自动更新该字段的值
    主要用于标记ParentId、FullPath、FullName等字段，并对其进行更新

# 系统操作日志功能

    增加注解@SystemLog实现，存储载体暂未决定，MySQL存储日志可能会导致数据库压力过大，后续再考虑使用ElasticSearch或ClickHouse存储日志
    日志线程池 StrixLogThreadPoolConfig
