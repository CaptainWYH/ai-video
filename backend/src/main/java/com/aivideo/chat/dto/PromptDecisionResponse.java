package com.aivideo.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptDecisionResponse {

    private ChatMessageResponse proposalMessage;
    private ChatMessageResponse videoMessage;
}
