
# 数据库初始化


-- 创建库
create database if not exists hu_ai_code_mother;

-- 切换库
use hu_ai_code_mother;

-- 用户表
-- 以下是建表语句


-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    vipExpireTime datetime     null comment '会员过期时间',
    vipCode       varchar(128) null comment '会员兑换码',
    vipNumber     bigint       null comment '会员编号',
    shareCode     varchar(20)  DEFAULT NULL COMMENT '分享码',
    inviteUser    bigint       DEFAULT NULL COMMENT '邀请用户 id',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
    ) comment '用户' collate = utf8mb4_unicode_ci;

-- 应用表
create table if not exists app
(
    id           bigint auto_increment comment 'id' primary key,
    appName      varchar(256)  default null comment '应用名称',
    cover        varchar(512)  default null comment '应用封面',
    initPrompt   text          null comment '应用初始化的 prompt',
    codeGenType  varchar(64)   default null comment '代码生成类型（枚举）',
    templateVersion varchar(32) default null comment 'Vue 模板版本，旧工程为 NULL',
    buildStatus varchar(16) default null comment 'PENDING/BUILDING/SUCCESS/FAILED',
    buildError varchar(512) default null comment '构建失败摘要',
    buildToken varchar(36) default null comment '当前生成和构建批次',
    deployKey    varchar(64)   default null comment '部署标识',
    deployedTime datetime      default null comment '部署时间',
    priority     int           default 0 not null comment '优先级',
    userId       bigint        not null comment '创建用户id',
    editTime     datetime      default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime      default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime      default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint       default 0 not null comment '是否删除',
    UNIQUE KEY uk_deployKey (deployKey),
    INDEX idx_appName (appName),
    INDEX idx_userId (userId)
    ) comment '应用' collate = utf8mb4_unicode_ci;

-- 对话历史表
create table if not exists chat_history
(
    id          bigint auto_increment comment 'id' primary key,
    message     text                               not null comment '消息',
    messageType varchar(32)                        not null comment 'user/ai',
    appId       bigint                             not null comment '应用id',
    userId      bigint                             not null comment '创建用户id',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除',
    INDEX idx_appId (appId),                       -- 提升基于应用的查询性能
    INDEX idx_createTime (createTime),             -- 提升基于时间的查询性能
    INDEX idx_appId_createTime (appId, createTime) -- 游标查询核心索引
) comment '对话历史' collate = utf8mb4_unicode_ci;

create table if not exists app_asset
(
    id bigint not null primary key,
    appId bigint not null,
    userId bigint not null,
    purpose varchar(16) not null,
    mimeType varchar(32) not null,
    sizeBytes bigint not null,
    storageKey varchar(255) not null,
    sitePath varchar(255) null,
    createTime datetime default CURRENT_TIMESTAMP not null,
    unique key uk_storage_key (storageKey),
    key idx_app_created (appId, createTime)
) comment '应用图片附件' collate = utf8mb4_unicode_ci;

create table if not exists chat_history_asset
(
    chatHistoryId bigint not null,
    assetId bigint not null,
    primary key (chatHistoryId, assetId),
    key idx_assetId (assetId),
    constraint fk_chat_history_asset_asset foreign key (assetId) references app_asset (id)
) comment '聊天消息与图片关联' collate = utf8mb4_unicode_ci;
