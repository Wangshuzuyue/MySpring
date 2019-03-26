package com.hzw.myspring.service;

import com.hzw.myspring.myframwork.annotation.MyService;

/**
 * @Auther: huangzuwang
 * @Date: 2019/3/25 20:43
 * @Description:
 */

@MyService
public class UserServiceImpl implements UserService{
    @Override
    public String userTest() {
        System.out.println(">>>>>>>>UserServiceImpl:");
        return "UserServiceImpl";
    }
}
