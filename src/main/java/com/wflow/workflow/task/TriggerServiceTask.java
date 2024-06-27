package com.wflow.workflow.task;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSONObject;
import com.wflow.utils.BeanUtil;
import com.wflow.utils.EmailUtil;
import com.wflow.workflow.UELTools;
import com.wflow.workflow.bean.process.props.TriggerProps;
import com.wflow.workflow.config.WflowGlobalVarDef;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

import javax.mail.MessagingException;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * 触发器任务
 *
 * @author : willian fu
 * @date : 2022/9/12
 */
@Slf4j
public class TriggerServiceTask implements JavaDelegate {

    public static RuntimeService runtimeService;

    public static EmailUtil emailUtil;

    public static UELTools uelTools;

    public TriggerServiceTask() {
        runtimeService = BeanUtil.getBean(RuntimeService.class);
        emailUtil = BeanUtil.getBean(EmailUtil.class);
        uelTools = BeanUtil.getBean(UELTools.class);
    }

    @Override
    public void execute(DelegateExecution execution) {
        FlowElement element = execution.getCurrentFlowElement();
        Map nodeProps = execution.getVariable(WflowGlobalVarDef.WFLOW_NODE_PROPS, Map.class);
        Optional.ofNullable(nodeProps.get(element.getId())).ifPresent(props -> {
            TriggerProps triggerProps = (TriggerProps) props;
            if (TriggerProps.TriggerTypeEnum.WEBHOOK.equals(triggerProps.getType())) {
                TriggerProps.Http http = triggerProps.getHttp();
                try {
                    doRequest(execution, http);
                } catch (Exception e) {
                    log.error("触发器[{}]请求[{}]异常,{}", element.getId(), http.getUrl(), e.getMessage());
                }
                log.info("触发器[{}]发送请求[{}]", element.getId(), http.getUrl());
            } else if (TriggerProps.TriggerTypeEnum.EMAIL.equals(triggerProps.getType())) {
                TriggerProps.Email email = triggerProps.getEmail();
                doSendEmail(execution, email);
                log.info("触发器[{}]发送邮件[{}]", element.getId(), email.getTo());
            }
        });
    }

    /**
     * 发送http请求
     *
     * @param execution 上下文
     * @param http      http配置
     */
    private void doRequest(DelegateExecution execution, TriggerProps.Http http) {
        Map<String, Object> variables = uelTools.getContextVar(execution);
        String url = getStrByRegex(http.getUrl(), variables);
        HttpRequest request;
        switch (http.getMethod()) {
            case "POST":
                request = HttpRequest.post(url);
                break;
            case "PUT":
                request = HttpRequest.put(url);
                break;
            case "DELETE":
                request = HttpRequest.delete(url);
                break;
            default:
                request = HttpRequest.get(url);
                break;
        }
        //处理请求头
        http.getHeaders().forEach(hd -> {
            request.header(hd.getName(), hd.getIsField() ?
                    String.valueOf(variables.getOrDefault(String.valueOf(hd.getValue()), ""))
                    : getStrByRegex(String.valueOf(hd.getValue()), variables));
        });
        //处理请求体
        if ("FORM".equals(http.getContentType())) {
            http.getParams().forEach(hd -> {
                request.form(hd.getName(), hd.getIsField() ?
                        String.valueOf(variables.getOrDefault(String.valueOf(hd.getValue()), ""))
                        : getStrByRegex(String.valueOf(hd.getValue()), variables));
            });
        } else {
            request.body(JSONObject.toJSONString(http.getParams().stream()
                            .collect(Collectors.toMap(TriggerProps.Http.Variable::getName,
                                    v -> v.getIsField() ? variables.getOrDefault(String.valueOf(v.getValue()), "")
                                            : getStrByRegex(String.valueOf(v.getValue()), variables))))
                    , "application/json");
        }
        HttpResponse response = request.timeout(10000).executeAsync();
        if (response.isOk()) {
            log.info("审批实例[{}]触发器发送请求成功[{} {}] 返回[{}]", execution.getProcessInstanceId(), http.getMethod(), http.getUrl(), response.body());
        } else {
            log.warn("审批实例[{}]触发器发送请求失败[{} {}]", execution.getProcessInstanceId(), http.getMethod(), http.getUrl());
        }
    }

    /**
     * 发送邮件
     *
     * @param execution 上下文
     * @param email     email配置
     */
    private void doSendEmail(DelegateExecution execution, TriggerProps.Email email) {
        Map<String, Object> variables = uelTools.getContextVar(execution);
        //模板解析标题
        String subject = getStrByRegex(email.getSubject(), variables);
        //模板解析内容
        String content = getStrByRegex(email.getContent(), variables);
        try {
            emailUtil.sendHtmlMail(subject, content, email.getTo().toArray(new String[0]));
        } catch (MessagingException e) {
            log.error("发送邮件给{}失败[标题：{}] [内容 {}]", email.getTo(), subject, content);
        }
        //TODO 执行邮件发送
        log.info("发送邮件给{}[标题：{}] [内容 {}]", email.getTo(), subject, content);
    }

    /**
     * 解析模板
     *
     * @param str    模板字符串
     * @param params 替换的变量
     * @return 解析后的内容
     */
    public static synchronized String getStrByRegex(String str, Map<String, Object> params) {
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

}
