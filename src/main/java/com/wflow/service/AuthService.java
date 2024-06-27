package com.wflow.service;

import com.wflow.bean.vo.UserLoginVo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author : willian fu
 * @date : 2022/8/15
 */
public interface AuthService {

    void getLoginCode(HttpServletRequest request, HttpServletResponse response) throws IOException;

    UserLoginVo userLogin(HttpServletRequest request, String username, String password, String code);

    UserLoginVo userLoginIgnore(String userId);
}
