package com.syt.graduationproject.util;


import com.syt.graduationproject.model.dto.UserDto;

public class UserHolderUtil {

    private static final ThreadLocal<UserDto> tl = new ThreadLocal<>();

    public static void saveUser(UserDto user){
        tl.set(user);
    }

    public static UserDto getUser(){
        return tl.get();
    }

    public static void removeUser(){
        tl.remove();
    }
}
