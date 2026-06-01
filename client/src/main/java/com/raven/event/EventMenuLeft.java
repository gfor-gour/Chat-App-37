package com.raven.event;

import com.raven.shared.dto.UserAccountDto;
import java.util.List;

public interface EventMenuLeft {

    public void newUser(List<UserAccountDto> users);

    public void userConnect(int userID);

    public void userDisconnect(int userID);
}
