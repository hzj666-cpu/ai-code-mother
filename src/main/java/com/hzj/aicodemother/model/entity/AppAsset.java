package com.hzj.aicodemother.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Table("app_asset")
public class AppAsset {
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;
    @Column("appId")
    private Long appId;
    @Column("userId")
    private Long userId;
    @Column("purpose")
    private String purpose;
    @Column("mimeType")
    private String mimeType;
    @Column("sizeBytes")
    private Long sizeBytes;
    @Column("storageKey")
    private String storageKey;
    @Column("sitePath")
    private String sitePath;
    @Column("createTime")
    private LocalDateTime createTime;
}
