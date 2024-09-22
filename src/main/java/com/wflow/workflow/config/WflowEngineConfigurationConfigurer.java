package com.wflow.workflow.config;

import com.wflow.workflow.config.callActivity.WflowActivityBehaviorFactory;
import com.wflow.workflow.config.custom.CustomIdBpmnDeployer;
import com.wflow.workflow.config.custom.JsonVariableType;
import lombok.extern.slf4j.Slf4j;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.flowable.variable.api.types.VariableTypes;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 添加流程引擎默认配置
 * @author : JoinFyc
 * @date : 2023/12/19
 */
@Slf4j
@Component
public class WflowEngineConfigurationConfigurer implements EngineConfigurationConfigurer<SpringProcessEngineConfiguration> {

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Override
    public void configure(SpringProcessEngineConfiguration engineConfiguration) {
        engineConfiguration.setActivityBehaviorFactory(new WflowActivityBehaviorFactory());
        engineConfiguration.setBpmnDeployer(new CustomIdBpmnDeployer());
        //自定义流程变量存储类型，把对象全部存成json格式字符串，这样就不会序列化成字节了
        loadVariableTypes(engineConfiguration);
        //重写flowable的id生成器，生成流程实例ID规则为：wf+ 日期时间数字 + 4位随机数 TODO [租户&业务&系统] ===== 生成ID规则
        engineConfiguration.setIdGenerator(() -> "wf" + LocalDateTime.now().format(formatter) + String.format("%04d", new Random().nextInt(10000)));
    }

    private void loadVariableTypes(SpringProcessEngineConfiguration engineConfiguration) {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                //加载自定义变量类型
                VariableTypes variableTypes = engineConfiguration.getVariableTypes();
                if (variableTypes != null) {
                    log.info("加载自定义变量类型");
                    JsonVariableType jsonVariableType = new JsonVariableType();
                    //设置默认序列化器为SerializableType
                    jsonVariableType.setDefaultVariableType(variableTypes.getVariableType("serializable"));
                    //提升优先级
                    variableTypes.addTypeBefore(jsonVariableType, "emptyCollection");
                    timer.cancel();
                }else {
                    log.info("等待flowable初始化完成...");
                }
            }
        }, 1000, 1000);
    }
}
