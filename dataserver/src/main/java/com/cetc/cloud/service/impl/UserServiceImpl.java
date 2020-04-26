package com.cetc.cloud.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cetc.cloud.annotation.MyCache;
import com.cetc.cloud.entity.User;
import com.cetc.cloud.mapper.UserMapper;
import com.cetc.cloud.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * Created by Latice on 2020/4/2.
 */
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    UserMapper userMapper;

    /**
     * 分页查询
     *
     * @param currentPage
     * @param pageSize
     * @return
     */
    @Override
    public List<User> getUsersByPage(Integer currentPage, Integer pageSize) {
        Page<User> page = new Page<>(currentPage, pageSize);
        QueryWrapper<User> wrapper = new QueryWrapper();
        wrapper.eq("deleted", 0);
        userMapper.selectPage(page, wrapper);
        return page.getRecords();
    }

    /**
     * 根据account获取用户信息
     *
     * @param user
     * @return
     */
    @Override
    @MyCache(name = "userCache", action = "query", key = "#user.account", filter = false)
    //@Cacheable(value = "userCache", key = "#user.account", sync = true)
    public User getUserByAccount(User user) {
        if (user != null) {
            // 缓存不存在，从数据库获取
            return userMapper.getUserByAccount(user.getAccount());
        }
        return null;
    }

    /**
     * 添加用户
     *
     * @param user
     * @return
     */
    @Override
    @MyCache(name = "userCache", action = "insert", key = "#user.account", filter = true)
    //@CachePut(value = "userCache", key = "#user.account", unless = "#result == null")
    public User insertUser(User user) {
        if (user != null) {
            Date now = new Date();
            user.setCreateTime(now);
            user.setModifyTime(now);
            user.setDeleted(0);
            int result = userMapper.insert(user);
            if (result > 0) {
                return user;
            }
        }
        return null;
    }

    /**
     * 根据id更新用户信息
     *
     * @param user
     * @return
     */
    @Override
    @CachePut(value = "userCache", key = "#user.account", unless = "#result == null")
    public User updateUser(User user) {
        if (user != null) {
            int result = userMapper.updateById(user);
            if (result > 0) {
                return user;
            }
        }
        return null;
    }

    /**
     * 根据id删除用户
     *
     * @param user
     * @return
     */
    @Override
    @CacheEvict(value = "userCache", key = "#user.account")
    public int deleteUser(User user) {
        if (user != null) {
            user.setDeleted(1);
            return userMapper.updateById(user);
        }
        return 0;
    }
}
