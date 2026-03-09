package com.ch.swaplychatservice.dto.request;

import com.ch.swaplychatservice.entity.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SendMessageRequest {

    @NotNull(message = "roomId 는 필수입니다")
    private Long roomId;

    @NotBlank(message = "content 는 필수입니다")
    private String content;

    private MessageType type = MessageType.TEXT;
}