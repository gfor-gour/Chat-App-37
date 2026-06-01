package com.raven.event;

import com.raven.shared.dto.ServiceResponse;

public interface EventMessage {

    public void callMessage(ServiceResponse message);
}
