package com.cetc.cloud.service;

import com.cetc.cloud.entity.User;

import java.util.List;

/**
 * Created by wangzh on 2020/4/2.
 */
public interface UserService {

    List<User> getUsersByPage(Integer currentPage, Integer pageSize);

    User getUserByAccount(User user);

    User insertUser(User user);

    User updateUser(User user);

    int deleteUser(User user);
}
