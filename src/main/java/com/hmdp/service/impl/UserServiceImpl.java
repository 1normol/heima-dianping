package com.hmdp.service.impl;

import cn.hutool.Hutool;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.Random;

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

    /**
     * 1.校验手机号，2.生成验证码，3.保存验证码,4.发送验证码到客户端
     * @param phone
     * @param session
     * @return
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        if (RegexUtils.isPhoneInvalid(phone)){
            return new Result(false,"手机号码无效",null,null);
        }
        String code = RandomUtil.randomNumbers(6);
        session.setAttribute("code",code);
        log.debug("验证码为:"+code);
        //此处应调用服务发送验证码至手机，暂不实现
        return new Result();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        if (RegexUtils.isPhoneInvalid(loginForm.getPhone())){
            return new Result(false,"手机号码无效",null,null);
        }
        User user = query().eq("phone", loginForm.getPhone()).one();
        if (user == null){
            creatUser(loginForm);
        }

        return null;
    }

    private void creatUser(LoginFormDTO loginForm) {

    }
}
