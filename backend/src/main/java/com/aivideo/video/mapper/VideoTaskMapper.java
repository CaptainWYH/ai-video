package com.aivideo.video.mapper;

import com.aivideo.video.entity.VideoTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface VideoTaskMapper extends BaseMapper<VideoTask> {

    @Select("""
            SELECT * FROM video_tasks
            WHERE deleted = 0
              AND status IN ('PENDING', 'SUBMITTING', 'QUEUED', 'RUNNING', 'DOWNLOADING')
              AND (chat_session_id IS NULL OR chat_message_id IS NOT NULL)
              AND (next_poll_at IS NULL OR next_poll_at <= #{now})
              AND (lease_until IS NULL OR lease_until < #{now})
            ORDER BY id
            LIMIT #{limit}
            """)
    List<VideoTask> findDueTasks(@Param("now") LocalDateTime now, @Param("limit") int limit);

    @Update("""
            UPDATE video_tasks
            SET lease_until = #{leaseUntil},
                updated_at = #{now},
                version = version + 1
            WHERE id = #{id}
              AND deleted = 0
              AND status = #{status}
              AND (lease_until IS NULL OR lease_until < #{now})
            """)
    int claimTask(@Param("id") Long id,
                  @Param("status") String status,
                  @Param("now") LocalDateTime now,
                  @Param("leaseUntil") LocalDateTime leaseUntil);

    @Select("""
            <script>
            SELECT * FROM video_tasks
            WHERE deleted = 0
              AND user_id = #{userId}
              AND chat_message_id IN
              <foreach collection="messageIds" item="id" open="(" separator="," close=")">
                #{id}
              </foreach>
            ORDER BY id DESC
            </script>
            """)
    List<VideoTask> findByChatMessageIds(@Param("userId") Long userId,
                                         @Param("messageIds") List<Long> messageIds);
}
