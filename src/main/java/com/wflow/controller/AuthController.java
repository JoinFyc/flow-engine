package com.wflow.controller;

import com.wflow.bean.vo.UserLoginVo;
import com.wflow.service.AuthService;
import com.wflow.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author : willian fu
 * @date : 2022/8/15
 */
@RestController
@RequestMapping("sys/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 获取登录验证码，暂未使用
     * @param request 请求
     * @param response 响应
     * @throws IOException
     */
    @GetMapping("login/code")
    public void getLoginCode(HttpServletRequest request, HttpServletResponse response) throws IOException {
        authService.getLoginCode(request, response);
    }

    /**
     * 用户登录，暂未使用
     * @param request 请求
     * @param username 用户名
     * @param password 密码
     * @param code 验证码
     * @return 登录结果
     */
    @GetMapping("login")
    public Object userLogin(HttpServletRequest request,
                            @RequestParam String username,
                            @RequestParam String password,
                            @RequestParam String code) {
        UserLoginVo loginVo = authService.userLogin(request, username, password, code);
        return R.ok(loginVo);
    }

    /**
     * 忽略账号密码快速登录进行用户切换
     * @param userId 用户ID
     * @return 登录用户的信息
     */
    @GetMapping("login/ignore/{userId}")
    public Object userLoginIgnore(@PathVariable String userId) {
        UserLoginVo loginVo = authService.userLoginIgnore(userId);
        return R.ok(loginVo);
    }
}
