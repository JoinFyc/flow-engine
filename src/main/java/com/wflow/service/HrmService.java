package com.wflow.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.wflow.bean.do_.UserDo;
import com.wflow.bean.entity.HrmStaffInfo;
import com.wflow.bean.vo.OrgTreeVo;
import com.wflow.exception.BusinessException;
import com.wflow.mapper.HrmStaffInfoMapper;
import com.wflow.workflow.bean.dto.OrgDto;
import com.wflow.workflow.bean.vo.UserDTO;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HrmService {

    @Resource
    private HrmStaffInfoMapper hrmStaffInfoMapper;

    @Resource
    private RestTemplate restTemplate;

    @Value("${hr.request.production.url:http://localhost:32170/hr/org/v1/}")
    private String requestUrl;

    public List<OrgTreeVo> selectUsersByDept(Long deptId){

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> exchange = restTemplate.exchange(
                requestUrl + "selectUsersByDept",
                HttpMethod.POST,
                new HttpEntity<>(DeptDTO.builder().deptId(deptId).build(),headers),
                new ParameterizedTypeReference<>() {
                });
        if (!exchange.getStatusCode().is2xxSuccessful()) {
            throw new BusinessException("请稍后重试，查询人事系统组织架构异常");
        }
        return JSONArray.parseArray(exchange.getBody(), OrgTreeVo.class);
    }

    public List<OrgTreeVo> selectUsersLikeName(String userName){
        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> exchange = restTemplate.exchange(
                requestUrl + "selectUsersLikeName",
                HttpMethod.POST,
                new HttpEntity<>(UserDTO.builder().userName(userName).build(),headers),
                new ParameterizedTypeReference<>() {
                });
        if (!exchange.getStatusCode().is2xxSuccessful()) {
            throw new BusinessException("请稍后重试，查询人事系统组织架构异常");
        }
        return JSONArray.parseArray(exchange.getBody(), OrgTreeVo.class);
    }

    public UserDo getUserById(Long userId){
        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> exchange = restTemplate.exchange(
                requestUrl + "selectByUserId",
                HttpMethod.POST,
                new HttpEntity<>(UserDTO.builder().userId(userId).build(),headers),
                new ParameterizedTypeReference<>() {
                });
        if (!exchange.getStatusCode().is2xxSuccessful()) {
            throw new BusinessException("请稍后重试，查询人事系统组织架构异常");
        }
        return JSON.parseObject(exchange.getBody(), UserDo.class);
    }

    public List<HrmStaffInfo> getStaffInfoByIds(Collection ids) {
        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> exchange = restTemplate.exchange(
                requestUrl + "getStaffInfoByIds",
                HttpMethod.POST,
                new HttpEntity<>(OrgDto.builder().deptIds(new HashSet<>(ids)),headers),
                new ParameterizedTypeReference<>() {
                });
        if (!exchange.getStatusCode().is2xxSuccessful()) {
            throw new BusinessException("请稍后重试，查询人事系统组织架构异常");
        }
        return JSONArray.parseArray(exchange.getBody(), HrmStaffInfo.class);
    }

    public Set<String> selectByDeptIds(Collection<String> deptIds) {
        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> exchange = restTemplate.exchange(
                requestUrl + "selectByDeptIds",
                HttpMethod.POST,
                new HttpEntity<>(OrgDto.builder().deptIds(new HashSet<>(deptIds)),headers),
                new ParameterizedTypeReference<>() {
                });
        if (!exchange.getStatusCode().is2xxSuccessful()) {
            throw new BusinessException("请稍后重试，查询人事系统组织架构异常");
        }
        List<HrmStaffInfo> hrmStaffInfo = JSONArray.parseArray(exchange.getBody(), HrmStaffInfo.class);
        return hrmStaffInfo.stream().map(s -> s.getUserId().toString()).collect(Collectors.toSet());
    }

    //TODO 查询部门全部人员
    public List<HrmStaffInfo> selectByDeptIds() {
        return hrmStaffInfoMapper.selectList(null);
    }
}
