package com.aivideo.video.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("video_tasks")
public class VideoTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long projectId;
    private String prompt;
    /** JSON 字符串：分辨率、时长、风格等生成参数 */
    private String params;
    /** PENDING / RUNNING / SUCCESS / FAILED */
    private String status;
    private Integer progress;
    private String resultUrl;
    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
