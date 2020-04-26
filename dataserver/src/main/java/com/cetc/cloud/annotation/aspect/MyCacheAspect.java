package com.cetc.cloud.annotation.aspect;

import com.cetc.cloud.annotation.MyCache;
import com.cetc.cloud.util.RedisBloomFilter;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Created by wangzh on 2020/4/10.
 */
@Aspect
@Component
public class MyCacheAspect {

    private static final Logger logger = LoggerFactory.getLogger(MyCacheAspect.class);

    @Autowired
    RedisBloomFilter redisBloomFilter;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${spring.cache.redis.time-to-live}")
    private Long timeToLive;

    /**
     * 定义切点
     */
    @Pointcut("@annotation(com.cetc.cloud.annotation.MyCache)")
    public void annotationPointcut() {
    }

    @Around("annotationPointcut()")
    public Object aroundPointcut(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        // 得到被代理的方法
        Method method = methodSignature.getMethod();
        MyCache annotation = method.getAnnotation(MyCache.class);
        String action = annotation.action();
        switch (action) {
            case "init":
                break;
            case "query": {
                // 是否使用布隆过滤器
                if (annotation.filter()) {
                    String key = annotation.key();
                    if (StringUtils.isNotBlank(key)) {
                        // 得到被切面修饰的方法的参数列表
                        Object[] args = joinPoint.getArgs();
                        String realKey = parseKey(key, method, args);
                        if (StringUtils.isNotBlank(realKey)) {
                            // 使用布隆过滤器，避免缓存穿透
                            if (redisBloomFilter.isExist(realKey)) {
                                return getValue(annotation, joinPoint, realKey);
                            } else {
                                logger.debug("BloomFilter查询realKey不存在：" + realKey);
                            }
                        } else {
                            logger.debug("BloomFilter查询realKey为空");
                        }
                    } else {
                        logger.debug("BloomFilter查询key为空");
                    }
                } else {
                    String key = annotation.key();
                    if (StringUtils.isNotBlank(key)) {
                        // 得到被切面修饰的方法的参数列表
                        Object[] args = joinPoint.getArgs();
                        String realKey = parseKey(key, method, args);
                        if (StringUtils.isNotBlank(realKey)) {
                            return getValue(annotation, joinPoint, realKey);
                        }
                    }
                }
                break;
            }
            case "insert":
                Object obj = joinPoint.proceed();
                return obj;
        }
        return null;
    }

    @AfterReturning(value = "annotationPointcut()", returning = "returnValue")
    public void doAfterReturning(JoinPoint joinPoint, Object returnValue) {
        ValueOperations<String, Object> operations = redisTemplate.opsForValue();
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        // 得到被代理的方法
        Method method = methodSignature.getMethod();
        MyCache annotation = method.getAnnotation(MyCache.class);
        String action = annotation.action();
        switch (action) {
            case "insert": {
                String key = annotation.key();
                if (StringUtils.isNotBlank(key)) {
                    // 得到被切面修饰的方法的参数列表
                    Object[] args = joinPoint.getArgs();
                    // 布隆过滤器key
                    String realKey = parseKey(key, method, args);
                    if (StringUtils.isNotBlank(realKey)) {
                        String prefix = annotation.name();
                        // Redis中的key
                        String redisKey = realKey;
                        if (StringUtils.isNotBlank(prefix)) {
                            redisKey = prefix + "::" + realKey;
                        }
                        operations.set(redisKey, returnValue, timeToLive, TimeUnit.SECONDS);
                        logger.debug("更新Redis中的值：" + redisKey);
                        if (annotation.filter() && !redisBloomFilter.isExist(realKey)) {
                            redisBloomFilter.put(realKey);
                            logger.debug("更新BloomFilter中的值：" + realKey);
                        }
                    }
                }
                break;
            }
        }
    }

    /**
     * 获取缓存的key
     * key 定义在注解上，支持SPEL表达式
     *
     * @param key
     * @param method
     * @param args
     * @return
     */
    private String parseKey(String key, Method method, Object[] args) {
        if (StringUtils.isBlank(key)) {
            return null;
        }
        // 获取被拦截方法参数名列表(使用Spring支持类库)
        LocalVariableTableParameterNameDiscoverer u = new LocalVariableTableParameterNameDiscoverer();
        String[] paraNameArr = u.getParameterNames(method);
        // 使用SPEL进行key的解析
        ExpressionParser parser = new SpelExpressionParser();
        // SPEL上下文
        StandardEvaluationContext context = new StandardEvaluationContext();
        // 把方法参数放入SPEL上下文中
        for (int i = 0; i < paraNameArr.length; i++) {
            context.setVariable(paraNameArr[i], args[i]);
        }
        return parser.parseExpression(key).getValue(context, String.class);
    }

    /**
     * 获取Redis缓存中的值
     *
     * @param annotation
     * @param joinPoint
     * @param realKey
     * @return
     * @throws Throwable
     */
    private Object getValue(MyCache annotation, ProceedingJoinPoint joinPoint, String realKey) throws Throwable {
        ValueOperations<String, Object> operations = redisTemplate.opsForValue();
        String prefix = annotation.name();
        String redisKey = realKey;
        // Redis键值增加自定义前缀
        if (StringUtils.isNotBlank(prefix)) {
            redisKey = prefix + "::" + realKey;
        }
        if (redisTemplate.hasKey(redisKey)) {
            logger.debug("查询时从Redis中获取：" + redisKey);
            return operations.get(redisKey);
        } else {
            synchronized (this) {
                // 双重检测锁，避免高并发时出现缓存击穿
                if (redisTemplate.hasKey(redisKey)) {
                    logger.debug("查询时从Redis中获取：" + redisKey);
                    return operations.get(redisKey);
                } else {
                    logger.debug("查询时从数据库中获取：" + redisKey);
                    Object obj = joinPoint.proceed();
                    operations.set(redisKey, obj, timeToLive, TimeUnit.SECONDS);
                    logger.debug("更新Redis中的值：" + redisKey);
                    if (annotation.filter() && !redisBloomFilter.isExist(realKey)) {
                        redisBloomFilter.put(realKey);
                        logger.debug("更新BloomFilter中的值：" + realKey);
                    }
                    return obj;
                }
            }
        }
    }
}
