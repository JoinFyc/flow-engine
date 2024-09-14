package com.wflow.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wflow.bean.do_.DeptDo;
import com.wflow.bean.do_.UserDo;
import com.wflow.bean.entity.WflowUserAgents;
import com.wflow.bean.vo.DeptVo;
import com.wflow.bean.vo.OrgTreeVo;
import com.wflow.bean.vo.UserAgentVo;
import com.wflow.bean.vo.UserVo;
import com.wflow.mapper.*;
import com.wflow.service.OrgRepositoryService;
import com.wflow.service.OrgUserAndDeptService;
import com.wflow.utils.R;
import com.wflow.utils.UserUtil;
import com.wflow.workflow.bean.process.OrgUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author : willian fu
 * @version : 1.0
 */
@Service
public class OrgUserAndDeptServiceImpl implements OrgUserAndDeptService {

    @Autowired
    private OrgRepositoryService orgRepositoryService;

    @Autowired
    private WflowUserAgentsMapper userAgentsMapper;

    private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm";

    /**
     * 查询组织架构树
     *
     * @param deptId 部门id
     * @param type   类型
     * @return 组织架构树数据
     */
    @Override
    public Object getOrgTreeData(String deptId, String type) {
        if ("role".equals(type)) {
            return orgRepositoryService.getSysAllRoles().stream()
                    .map(r -> OrgTreeVo.builder().id(r.getRoleId()).type("role")
                            .name(r.getRoleName()).build());
        } else {
            //查询当前部门信息
            DeptDo department = orgRepositoryService.getDeptById(deptId);
            if(department == null){return R.ok(null);}
            //查询子部门信息
            List<OrgTreeVo> subDeptById = orgRepositoryService.getSubDeptById(deptId);
            if(CollectionUtils.isEmpty(subDeptById)){return R.ok(null);}
            List<OrgTreeVo> orgs = new LinkedList<>(subDeptById);
            if ("user".equals(type) || "org".equals(type)) {
                List<OrgTreeVo> orgTreeVos = orgRepositoryService.selectUsersByDept(deptId);
                if(CollectionUtils.isEmpty(orgTreeVos)){return R.ok(null);}
                orgs.addAll(orgTreeVos.stream().peek(u -> {
                    u.setIsLeader(StrUtil.isNotBlank(department.getLeader()) && department.getLeader().equals(u.getId()));
                }).sorted(Comparator.comparing(OrgTreeVo::getIsLeader).reversed()).collect(Collectors.toList()));
            }
            return R.ok(orgs);
        }
    }

    /**
     * 模糊搜索用户
     *
     * @param userName 用户名/拼音/首字母
     * @return 匹配到的用户
     */
    @Override
    public Object getOrgTreeUser(String userName) {
        return R.ok(orgRepositoryService.selectUsersByPy(userName));
    }

    @Override
    public List<DeptVo> getOrgUserDept(String userId) {
        return orgRepositoryService.getDeptsByUser(userId)
                .stream().map(d -> new DeptVo(d.getId(), d.getDeptName()))
                .collect(Collectors.toList());
    }

    @Override
    public UserAgentVo getUserAgent(String userId) {
        WflowUserAgents userAgent = userAgentsMapper.selectById(userId);
        if (Objects.nonNull(userAgent)) {
            Date time = GregorianCalendar.getInstance().getTime();
            if (userAgent.getEndTime().after(time)) {
                //在代理期限内或者
                UserDo user = orgRepositoryService.getUserById(userAgent.getAgentUserId());
                return UserAgentVo.builder()
                        .effective(time.after(userAgent.getStartTime()) && time.before(userAgent.getEndTime()))
                        .user(OrgUser.builder().id(user.getUserId())
                                .name(user.getUserName()).type("user")
                                .avatar(user.getAvatar()).build())
                        .timeRange(CollectionUtil.newArrayList(
                                DateUtil.format(userAgent.getStartTime(), TIME_FORMAT),
                                DateUtil.format(userAgent.getEndTime(), TIME_FORMAT)
                        )).build();
            }
        }
        return null;
    }

    @Override
    public void setUserAgent(UserAgentVo agent) {
        String userId = UserUtil.getLoginUserId();
        WflowUserAgents userAgent = userAgentsMapper.selectById(userId);
        WflowUserAgents userAgents = WflowUserAgents.builder().userId(userId)
                .agentUserId(agent.getUser().getId())
                .startTime(DateUtil.parse(agent.getTimeRange().get(0), TIME_FORMAT))
                .endTime(DateUtil.parse(agent.getTimeRange().get(1), TIME_FORMAT))
                .createTime(GregorianCalendar.getInstance().getTime())
                .build();
        //如果A设置B为代理人，C又被A代理，那么需要更新C被B代理
        userAgentsMapper.update(WflowUserAgents.builder().agentUserId(agent.getUser().getId()).build(),
                new LambdaQueryWrapper<WflowUserAgents>()
                .eq(WflowUserAgents::getAgentUserId, userId));
        if (Objects.nonNull(userAgent)) {
            userAgentsMapper.updateById(userAgents);
        }else {
            userAgentsMapper.insert(userAgents);
        }
    }

    @Override
    public void cleanUserAgent() {
        userAgentsMapper.deleteById(UserUtil.getLoginUserId());
    }

    @Override
    public String getUserSign() {
        return orgRepositoryService.getUserSign(UserUtil.getLoginUserId());
    }

    @Override
    public UserVo getUserDetail(String userId) {
        UserVo userVo = orgRepositoryService.getUserDetail(userId);
        userVo.setUserAgent(getUserAgent(userId));
        return userVo;
    }
}
