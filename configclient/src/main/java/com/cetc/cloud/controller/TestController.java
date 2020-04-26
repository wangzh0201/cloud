package com.cetc.cloud.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by wangzh on 2020/3/5.
 */
@RestController
public class TestController {

    @GetMapping(value = "/test")
    public String getTestResult(){
        return "success";
    }
}
