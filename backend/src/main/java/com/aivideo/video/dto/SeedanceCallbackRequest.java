package com.aivideo.video.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SeedanceCallbackRequest {

    private String id;
    private String status;
}
