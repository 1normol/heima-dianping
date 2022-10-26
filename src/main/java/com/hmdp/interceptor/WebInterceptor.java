package com.hmdp.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import io.jsonwebtoken.Claims;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @author ：limaolin
 * @date ：Created in 2022/10/24 9:51
 * @description：拦截器
 * @modified By：
 */

public class WebInterceptor implements HandlerInterceptor {

    @Resource
    private RedisTemplate redisTemplate;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String token = request.getHeader("authorization");
        if (token == null) {
            response.setStatus(401);
            return false;
        }

        Map userMap = redisTemplate.opsForHash().entries(SystemConstants.REDIS_CACHE_USER+token+":");

        if (CollectionUtil.isEmpty(userMap)){
            response.setStatus(401);
            return false;
        }

        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        UserHolder.saveUser(userDTO);

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }


}
