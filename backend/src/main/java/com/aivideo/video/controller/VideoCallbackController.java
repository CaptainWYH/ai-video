package com.aivideo.video.controller;

import com.aivideo.common.Result;
import com.aivideo.video.dto.SeedanceCallbackRequest;
import com.aivideo.video.service.VideoCallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/video/callbacks/seedance")
@RequiredArgsConstructor
public class VideoCallbackController {

    private final VideoCallbackService videoCallbackService;

    @PostMapping("/{taskId}/{signature}")
    public Result<Void> callback(@PathVariable("taskId") Long taskId,
                                 @PathVariable("signature") String signature,
                                 @RequestBody SeedanceCallbackRequest request) {
        videoCallbackService.handleSeedanceCallback(taskId, signature, request);
        return Result.ok();
    }
}
