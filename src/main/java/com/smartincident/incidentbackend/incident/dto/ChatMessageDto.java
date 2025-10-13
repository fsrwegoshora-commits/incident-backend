package com.smartincident.incidentbackend.incident.dto;

import com.smartincident.incidentbackend.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {

    private String uid;
    private String senderUid;
    private String incidentUid;
    private String message;
    private MessageType messageType;
    private LocalDateTime sentAt;
}