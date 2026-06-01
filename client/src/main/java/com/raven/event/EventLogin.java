package com.raven.event;

import com.raven.shared.dto.LoginRequest;
import com.raven.shared.dto.RegisterRequest;

public interface EventLogin {

    public void login(LoginRequest data);

    public void register(RegisterRequest data, EventMessage message);

    public void goRegister();

    public void goLogin();
}
