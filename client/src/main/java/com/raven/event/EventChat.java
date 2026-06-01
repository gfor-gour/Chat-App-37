package com.raven.event;

import com.raven.shared.dto.ReceiveMessageResponse;
import com.raven.shared.dto.SendMessageRequest;

public interface EventChat {

    public void sendMessage(SendMessageRequest data);

    public void receiveMessage(ReceiveMessageResponse data);
}
