package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private RedisTemplate redisTemplate;
    /**
     * 1.校验手机号，2.生成验证码，3.保存验证码,4.发送验证码到客户端
     * @param phone
     * @param
     * @return
     */
    @Override
    public Result sendCode(String phone) {
        if (RegexUtils.isPhoneInvalid(phone)){
            return new Result(false,"手机号码无效",null,null);
        }
        String code = RandomUtil.randomNumbers(6);
        redisTemplate.opsForValue().set(SystemConstants.REDIS_CACHE_CODE+phone+":",code,SystemConstants.CODE_TIMEOUT, TimeUnit.SECONDS);

        log.debug("验证码为:"+code);
        //此处应调用服务发送验证码至手机，暂不实现
        return new Result();
    }

    @Override
    public Result login(LoginFormDTO loginForm) {
        if (RegexUtils.isPhoneInvalid(loginForm.getPhone())){
            return new Result(false,"手机号码无效",null,null);
        }
        User user = query().eq("phone", loginForm.getPhone()).one();
        if (user == null){
            creatUser(loginForm);
            user = query().eq("phone", loginForm.getPhone()).one();
        }
        String code = (String) redisTemplate.opsForValue().get(SystemConstants.REDIS_CACHE_CODE+loginForm.getPhone()+":");
        if (StringUtils.isAnyBlank(loginForm.getPhone(),loginForm.getCode())){
            return Result.fail("密码或验证码为空");
        }

        if (!code.equals(loginForm.getCode())){
            return Result.fail("账号密码错误");
        }
        UserDTO userDTO = new UserDTO();
        userDTO.setNickName(user.getNickName());
        userDTO.setId(user.getId());
        userDTO.setIcon(user.getIcon());
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO);
        String token = RandomUtil.randomString(12);

        redisTemplate.opsForHash().putAll(SystemConstants.REDIS_CACHE_USER+token,userMap);
        redisTemplate.expire(SystemConstants.REDIS_CACHE_USER+token,SystemConstants.USER_TIMEOUT,TimeUnit.MINUTES);

        return Result.ok(token);
    }



    private void creatUser(LoginFormDTO loginForm) {
        User user = new User();
        user.setPhone(loginForm.getPhone());
        user.setPassword(loginForm.getPassword());
        user.setNickName("user:"+RandomUtil.randomString(5));
        baseMapper.insert(user);
    }
}
