package com.wflow.workflow.execute;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wflow.workflow.bean.process.HttpDefinition;
import com.wflow.workflow.config.WflowGlobalVarDef;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;

/**
 * @author : willian fu
 * @date : 2023/10/28
 */
@Slf4j
public class HttpExecute {

    public <T> T execute(HttpDefinition definition, ExecutorService executorService, Class<T> clazz, Map<String, Object> ctx){
        JsExecute jsExecute = new JsExecute(executorService);
        jsExecute.executeVoid("preHandler", definition.getPreHandler(), definition);
        HttpRequest request = HttpUtil.createRequest(definition.getMethod(), getStrByRegex(definition.getUrl(), ctx));
        request.timeout(10000);
        definition.getHeaders().forEach(h -> {
            if (StrUtil.isNotBlank(h.getName())){
                request.header(getStrByRegex(h.getName(), ctx), getStrByRegex(h.getValue(), ctx));
            }
        });
        if ("JSON".equals(definition.getContentType())){
            request.body(getStrByRegex(String.valueOf(definition.getData()), ctx), "application/json");
        } else if ("XFORM".equals(definition.getContentType())) {
            definition.getParams().forEach(h -> {
                if (StrUtil.isNotBlank(h.getName())){
                    request.form(getStrByRegex(h.getName(), ctx), getStrByRegex(h.getValue(), ctx));
                }
            });
        }
        HttpResponse response = request.executeAsync();
        Map<String, Object> rsp = new HashMap<>();
        rsp.put("statusCode", response.getStatus());
        rsp.put("headers", response.headers());
        String body = response.body();
        rsp.put("data", JSON.isValid(body) ? JSON.parse(response.body()) : body);
        return jsExecute.execute("aftHandler", definition.getAftHandler(), clazz, rsp);
    }

    /**
     * 解析模板
     *
     * @param str    模板字符串
     * @param params 替换的变量
     * @return 解析后的内容
     */
    public String getStrByRegex(String str, Map<String, Object> params) {
        if (StrUtil.isNotBlank(str)){
            StringBuffer sb = new StringBuffer();
            Matcher m = WflowGlobalVarDef.TEMPLATE_REPLACE_REG.matcher(str);
            while (m.find()) {
                String key = m.group();
                String value = String.valueOf(params.get(key.substring(2, key.length() - 1).trim()));
                m.appendReplacement(sb, value == null ? "" : value);
            }
            m.appendTail(sb);
            return sb.toString();
        }
        return str;
    }
}
