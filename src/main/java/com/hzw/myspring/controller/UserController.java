package com.hzw.myspring.controller;

import com.hzw.myspring.myframwork.annotation.MyAutowired;
import com.hzw.myspring.myframwork.annotation.MyController;
import com.hzw.myspring.myframwork.annotation.MyRequestMapping;
import com.hzw.myspring.myframwork.annotation.MyRequestParam;
import com.hzw.myspring.service.UserService;

/**
 * @Auther: huangzuwang
 * @Date: 2019/3/25 20:39
 * @Description:
 */

@MyController
@MyRequestMapping("/user")
public class UserController {

    @MyAutowired
    private UserService userService;

    @MyRequestMapping("/test")
    public String test(@MyRequestParam("userId") String userId, @MyRequestParam("age") Integer age){
        String str = ">>>>>>>> " + userId + " >>>>>>>> " + age;
        System.out.println(str);
        userService.userTest();
        return str;
    }
}
