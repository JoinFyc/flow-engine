package com.wflow.controller;

import com.wflow.bean.vo.UserAgentVo;
import com.wflow.service.OrgUserAndDeptService;
import com.wflow.utils.R;
import com.wflow.utils.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author : willian fu
 * @date : 2022/6/27
 */
@RestController
@RequestMapping("oa/org")
public class OaUserController {

    @Autowired
    private OrgUserAndDeptService orgService;

    /**
     * 查询组织架构树
     * @param deptId 部门id
     * @param type 类型
     * @return 组织架构树数据
     */
    @GetMapping("tree")
    public Object getOrgTreeData(@RequestParam(defaultValue = "0") String deptId,
                                 @RequestParam String type){
        return orgService.getOrgTreeData(deptId, type);
    }

    /**
     * 模糊搜索用户
     * @param userName 用户名/拼音/首字母
     * @return 匹配到的用户
     */
    @GetMapping("tree/user/search")
    public Object getOrgTreeUser(@RequestParam String userName){
        return orgService.getOrgTreeUser(userName.trim());
    }


    /**
     * 查询用户的所有直属部门
     * @param userId 用户名/拼音/首字母
     * @return 所有直属部门
     */
    @GetMapping("user/{userId}/dept")
    public Object getOrgUserDept(@PathVariable String userId){
        return R.ok(orgService.getOrgUserDept(userId));
    }

    /**
     * 获取用户得审批代理人，过期的不返回
     * @return 代理人信息
     */
    @GetMapping("user/agent")
    public Object getUserAgent(){
        return R.ok(orgService.getUserAgent(UserUtil.getLoginUserId()));
    }

    /**
     * 设置用户代理人
     * @param agent 代理人
     * @return 操作结果
     */
    @PutMapping("user/agent")
    public Object setUserAgent(@RequestBody UserAgentVo agent){
        orgService.setUserAgent(agent);
        return R.ok("设置成功");
    }

    /**
     * 清除设置的审批代理人
     * @return 操作结果
     */
    @DeleteMapping("user/agent")
    public Object cleanUserAgent(){
        orgService.cleanUserAgent();
        return R.ok("清除审批代理人成功");
    }

    @GetMapping("user/sign")
    public Object getUserSign(){
        return R.ok(orgService.getUserSign());
    }

    @GetMapping("user/{userId}/detail")
    public Object getUserDetail(@PathVariable String userId){
        return R.ok(orgService.getUserDetail(userId));
    }

}
