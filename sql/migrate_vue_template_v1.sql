-- 对已存在的 app 表执行一次；旧应用保持 NULL，禁止自动覆盖其工程文件。
ALTER TABLE app
    ADD COLUMN templateVersion varchar(32) DEFAULT NULL COMMENT 'Vue 模板版本，旧工程为 NULL',
    ADD COLUMN buildStatus varchar(16) DEFAULT NULL COMMENT 'PENDING/BUILDING/SUCCESS/FAILED',
    ADD COLUMN buildError varchar(512) DEFAULT NULL COMMENT '构建失败摘要',
    ADD COLUMN buildToken varchar(36) DEFAULT NULL COMMENT '当前生成和构建批次';
