package com.wflow.workflow.facade;

import cn.hutool.core.util.StrUtil;
import com.wflow.bean.FlowProcessContext;
import com.wflow.service.OrgRepositoryService;
import com.wflow.service.OrgUserAndDeptService;
import com.wflow.workflow.bean.vo.UserDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author JoinFyc
 * @description 组织架构控制器
 * @date 2024-08-30
 */
@Tag(name = "组织架构", description = "组织架构相关接口")
@RestController
@RequestMapping("/flow-engine/rest/org")
public class OrgTreeFacadeController {

    @Autowired
    private OrgUserAndDeptService orgService;

    @Autowired
    private OrgRepositoryService orgRepositoryService;

    /**
     * 查询公司一级部门
     * @return 组织架构树数据
     */
    @GetMapping("company")
    @Operation(summary = "查询公司一级部门")
    public Object getOrgTreeData(){
        final FlowProcessContext flowProcessContext = FlowProcessContext.initFlowProcessContext();
        flowProcessContext.setFieldTag(Boolean.TRUE);
        flowProcessContext.setFieldDesc("查询公司一级部门");
        return orgRepositoryService.getDeptByCoNo(Long.valueOf(flowProcessContext.getTenantId()));
    }

    /**
     * 查询组织架构树
     * @param deptId 部门id
     * @param type 类型
     * @return 组织架构树数据
     */
    @GetMapping("tree")
    @Operation(summary = "查询组织架构树")
    public Object getOrgTreeData(@RequestParam(defaultValue = "0") String deptId,
                                 @RequestParam String type){
        final FlowProcessContext flowProcessContext = FlowProcessContext.initFlowProcessContext();
        flowProcessContext.setFieldTag(Boolean.TRUE);
        flowProcessContext.setFieldDesc("查询组织架构树-取HR组织架构");
        return orgService.getOrgTreeData(deptId, type);
    }

    /**
     * 模糊搜索用户
     * @param dto 用户名/拼音/首字母
     * @return 匹配到的用户
     */
    @PostMapping("tree/user/search")
    @Operation(summary = "模糊搜索用户")
    public Object getOrgTreeUser(@RequestBody UserDTO dto){
        if(StrUtil.isEmpty(dto.getUserName())) return "用户名不能为空";
        final FlowProcessContext flowProcessContext = FlowProcessContext.initFlowProcessContext();
        flowProcessContext.setFieldTag(Boolean.TRUE);
        flowProcessContext.setFieldDesc("查询组织架构树-模糊搜索用户");
        return orgService.getOrgTreeUser(dto.getUserName().trim());
    }
}
