package com.wflow.service;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.wflow.bean.do_.UserDo;
import com.wflow.bean.entity.HrmStaffInfo;
import com.wflow.bean.vo.OrgTreeVo;
import com.wflow.exception.BusinessException;
import com.wflow.mapper.HrmStaffInfoMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@DS("hr")
public class HrmService {

    @Resource
    private HrmStaffInfoMapper hrmStaffInfoMapper;

    @Resource
    private RestTemplate restTemplate;

    @Value("${hr.request.production.url:http://localhost:32170/hr/org/v1/}")
    private String requestUrl;

    public List<OrgTreeVo> selectUsersByDept(Long deptId){
        ResponseEntity<List<OrgTreeVo>> exchange = restTemplate.exchange(
                requestUrl + "selectUsersByDept",
                HttpMethod.GET,
                new HttpEntity<>(deptId),
                new ParameterizedTypeReference<>() {
                });
        if (!exchange.getStatusCode().is2xxSuccessful() || exchange.getBody() == null) {
            throw new BusinessException("请稍后重试，查询人事系统组织架构异常");
        }
        return exchange.getBody();
    }

    public List<OrgTreeVo> selectUsersLikeName(String userName){
        ResponseEntity<List<OrgTreeVo>> exchange = restTemplate.exchange(
                requestUrl + "selectUsersLikeName",
                HttpMethod.GET,
                new HttpEntity<>(userName),
                new ParameterizedTypeReference<>() {
                });
        if (!exchange.getStatusCode().is2xxSuccessful() || exchange.getBody() == null) {
            throw new BusinessException("请稍后重试，查询人事系统组织架构异常");
        }
        return exchange.getBody();
    }

    public UserDo selectByUserId(Long userId){
        ResponseEntity<UserDo> exchange = restTemplate.exchange(
                requestUrl + "selectByUserId",
                HttpMethod.GET,
                new HttpEntity<>(userId),
                new ParameterizedTypeReference<>() {
                });
        if (!exchange.getStatusCode().is2xxSuccessful() || exchange.getBody() == null) {
            throw new BusinessException("请稍后重试，查询人事系统组织架构异常");
        }
        return exchange.getBody();
    }

    public List<HrmStaffInfo> selectBatchIds(Collection ids) {
        ResponseEntity<List<HrmStaffInfo>> exchange = restTemplate.exchange(
                requestUrl + "selectBatchIds",
                HttpMethod.GET,
                new HttpEntity<>(ids),
                new ParameterizedTypeReference<>() {
                });
        if (!exchange.getStatusCode().is2xxSuccessful() || exchange.getBody() == null) {
            throw new BusinessException("请稍后重试，查询人事系统组织架构异常");
        }
        return exchange.getBody();
    }

    public Set<String> selectByDeptIds(Collection<String> deptIds) {
        ResponseEntity<List<HrmStaffInfo>> exchange = restTemplate.exchange(
                requestUrl + "selectByDeptIds",
                HttpMethod.GET,
                new HttpEntity<>(deptIds),
                new ParameterizedTypeReference<>() {
                });
        if (!exchange.getStatusCode().is2xxSuccessful() || exchange.getBody() == null) {
            throw new BusinessException("请稍后重试，查询人事系统组织架构异常");
        }
        return  exchange.getBody().stream().map(s -> s.getUserId().toString()).collect(Collectors.toSet());
    }

    public List<HrmStaffInfo> selectByDeptIds() {
        return hrmStaffInfoMapper.selectList(null);
    }
}
