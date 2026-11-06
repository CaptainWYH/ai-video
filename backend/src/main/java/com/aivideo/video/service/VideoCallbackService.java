package com.aivideo.video.service;

import com.aivideo.common.BizException;
import com.aivideo.video.dto.SeedanceCallbackRequest;
import com.aivideo.video.entity.VideoTask;
import com.aivideo.video.mapper.VideoTaskMapper;
import com.aivideo.video.provider.SeedanceCallbackSigner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class VideoCallbackService {

    private final VideoTaskMapper videoTaskMapper;
    private final SeedanceCallbackSigner callbackSigner;
    private final VideoTaskWorker videoTaskWorker;

    @Transactional
    public void handleSeedanceCallback(Long taskId, String signature,
                                       SeedanceCallbackRequest request) {
        if (!callbackSigner.verify(taskId, signature)) {
            throw BizException.forbidden("回调签名无效");
        }
        VideoTask task = videoTaskMapper.selectById(taskId);
        if (task == null || !"SEEDANCE".equalsIgnoreCase(task.getProvider())) {
            throw BizException.notFound("视频任务不存在");
        }
        if (!StringUtils.hasText(request.getId())) {
            throw BizException.badRequest("回调缺少任务 ID");
        }
        if (StringUtils.hasText(task.getProviderTaskId())
                && !task.getProviderTaskId().equals(request.getId())) {
            throw BizException.badRequest("回调任务 ID 不匹配");
        }
        if (!StringUtils.hasText(task.getProviderTaskId())) {
            task.setProviderTaskId(request.getId());
            videoTaskMapper.updateById(task);
        }
        // 回调仅作为唤醒信号，实际状态由后端主动查询火山接口确认。
        videoTaskWorker.refreshNow(taskId);
    }
}
