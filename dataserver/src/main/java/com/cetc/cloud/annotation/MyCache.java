package com.cetc.cloud.annotation;

import java.lang.annotation.*;

/**
 * Created by wangzh on 2020/4/10.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyCache {

    String name() default "";
    String action() default "";
    String key() default "";
    boolean filter() default false;
}
