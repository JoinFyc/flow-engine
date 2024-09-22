package com.wflow.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.CircleCaptcha;
import cn.hutool.core.util.ObjectUtil;
import com.wflow.bean.do_.UserDo;
import com.wflow.bean.entity.WflowUsers;
import com.wflow.bean.vo.UserLoginVo;
import com.wflow.exception.BusinessException;
import com.wflow.service.AuthService;
import com.wflow.service.OrgRepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author : JoinFyc
 * @date : 2024/8/16
 */
@Service
@Primary
public class AuthServiceImpl implements AuthService {

    private static final String SESSION_CODE_KEY = "login-code";

    @Autowired
    private OrgRepositoryService orgRepositoryService;


    @Override
    public void getLoginCode(HttpServletRequest request, HttpServletResponse response) throws IOException {
        CircleCaptcha captcha = CaptchaUtil.createCircleCaptcha(200, 45, 4, 4);
        //ShearCaptcha captcha = CaptchaUtil.createShearCaptcha(200, 45, 4, 4);
        // 自定义验证码内容为四则运算方式
        //captcha.setGenerator(new MathGenerator());
        // 重新生成code
        captcha.createCode();
        request.getSession().setAttribute(SESSION_CODE_KEY, captcha.getCode());
        captcha.write(response.getOutputStream());
    }

    @Override
    public UserLoginVo userLogin(HttpServletRequest request, String username, String password, String code) {
        String catchCode = String.valueOf(request.getSession().getAttribute(SESSION_CODE_KEY));
        if (code.equals(catchCode)) {
            WflowUsers users = null;//usersMapper.selectOne(new QueryWrapper<WflowUsers>().eq("user_name", username));
            if (ObjectUtil.isNull(users)) {
                throw new BusinessException("账号或密码错误");
            }
            return UserLoginVo.builder().userId(users.getUserId()).userName(username)
                    .alisa(users.getAlisa()).avatar(users.getAvatar()).build();
        }
        throw new BusinessException("验证码错误");
    }

    @Override
    public UserLoginVo userLoginIgnore(String userId) {
        UserDo user = orgRepositoryService.getUserById(userId);
        StpUtil.login(userId);
        return UserLoginVo.builder().userId(user.getUserId())
                .userName(user.getUserName())
                .token(StpUtil.getTokenInfo().tokenValue)
                .avatar(user.getAvatar()).build();
    }
}
