package com.wflow.service;

import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.wflow.bean.entity.OrgDept;
import com.wflow.exception.BusinessException;
import com.wflow.mapper.OrgDeptMapper;
import com.wflow.workflow.bean.dto.OrgDto;
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
public class OrgService {

    @Resource
    private OrgDeptMapper orgDeptMapper;

    @Resource
    private RestTemplate restTemplate;

    @Value("${hr.request.production.url:http://localhost:32170/hr/org/v1/}")
    private String requestUrl;

    public List<OrgDept> getDepartmentsByIds(Collection<String> deptIds) {
        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> exchange = restTemplate.exchange(
                requestUrl + "getDepartmentsByCoNoIds",
                HttpMethod.POST,
                new HttpEntity<>(OrgDto.builder().deptIds((deptIds == null ? null : new HashSet<>(deptIds))).build(),headers),
                new ParameterizedTypeReference<>() {
                });
        if (!exchange.getStatusCode().is2xxSuccessful()) {
            throw new BusinessException("请稍后重试，查询人事系统组织架构异常");
        }
        return JSONArray.parseArray(exchange.getBody(), OrgDept.class);
    }

    public List<OrgDept> selectList(Wrapper<OrgDept> wrapper) {
//        ResponseEntity<List<OrgDept>> exchange = restTemplate.getForEntity(
//                requestUrl + "selectByDeptIds",new TypeReference<>() {
//                }
//                );
//        if (!exchange.getStatusCode().is2xxSuccessful() || exchange.getBody() == null) {
//            throw new BusinessException("请稍后重试，查询人事系统组织架构异常");
//        }
        return orgDeptMapper.selectList(wrapper);
    }

    public List<OrgDept> getChildDeptId(String parentDeptId) {
        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> exchange = restTemplate.exchange(
                requestUrl + "getDepartmentsByCoNoIds",
                HttpMethod.POST,
                new HttpEntity<>(OrgDto.builder().parentDeptId(parentDeptId).build(),headers),
                new ParameterizedTypeReference<>() {
                });
        if (!exchange.getStatusCode().is2xxSuccessful()) {
            throw new BusinessException("请稍后重试，查询人事系统组织架构异常");
        }
        return JSONArray.parseArray(exchange.getBody(), OrgDept.class);
    }
}
