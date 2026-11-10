CREATE TABLE IF NOT EXISTS app_asset (
    id BIGINT NOT NULL PRIMARY KEY,
    appId BIGINT NOT NULL,
    userId BIGINT NOT NULL,
    purpose VARCHAR(16) NOT NULL,
    mimeType VARCHAR(32) NOT NULL,
    sizeBytes BIGINT NOT NULL,
    storageKey VARCHAR(255) NOT NULL,
    sitePath VARCHAR(255) NULL,
    createTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_storage_key (storageKey),
    KEY idx_app_created (appId, createTime)
) COMMENT '应用图片附件' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_history_asset (
    chatHistoryId BIGINT NOT NULL,
    assetId BIGINT NOT NULL,
    PRIMARY KEY (chatHistoryId, assetId),
    KEY idx_assetId (assetId),
    CONSTRAINT fk_chat_history_asset_asset FOREIGN KEY (assetId) REFERENCES app_asset (id)
) COMMENT '聊天消息与图片关联' COLLATE = utf8mb4_unicode_ci;
