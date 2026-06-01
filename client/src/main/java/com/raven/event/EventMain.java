package com.raven.event;

import com.raven.shared.dto.UserAccountDto;

public interface EventMain {

    public void showLoading(boolean show);

    public void initChat();

    public void selectUser(UserAccountDto user);

    public void updateUser(UserAccountDto user);
}
