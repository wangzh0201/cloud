package com.cetc.cloud.controller;

import com.cetc.cloud.entity.User;
import com.cetc.cloud.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Created by wangzh on 2020/4/1.
 */
@RestController
public class TestController {

    @Autowired
    UserService userService;

    @GetMapping("/user/list")
    public List<User> listUsers() {
        return userService.getUsersByPage(1, -1);
    }

    @PostMapping("/user/login")
    public String login(@RequestBody User user) {
        User result = userService.getUserByAccount(user);

        if (result != null && StringUtils.equals(result.getPassword(), user.getPassword())) {
            return result.toString();
        }
        return "fail";
    }

    @PostMapping("/user/add")
    public int addUser(@RequestBody User user) {
        User result = userService.insertUser(user);
        if (result == null) {
            return 0;
        }
        return 1;
    }

    @PostMapping("/user/update")
    public int updateUser(@RequestBody User user) {
        User result = userService.updateUser(user);
        if (result == null) {
            return 0;
        }
        return 1;
    }

    @DeleteMapping("/user/delete")
    public int deleteUser(@RequestBody User user) {
        if (user != null) {
            return userService.deleteUser(user);
        }
        return 0;
    }
}
