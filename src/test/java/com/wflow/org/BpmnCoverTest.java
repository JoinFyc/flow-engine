package com.wflow.org;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson2.JSONObject;
import com.wflow.exception.BusinessException;
import com.wflow.workflow.WFlowToBpmnCreator;
import com.wflow.workflow.bean.process.ProcessNode;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.validation.ProcessValidator;
import org.flowable.validation.ProcessValidatorFactory;
import org.flowable.validation.ValidationError;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author : JoinFyc
 * @date : 2024/9/29
 */
@SpringBootTest
public class BpmnCoverTest {

    public static void main(String[] args) {
        String process = "";
        BpmnModel bpmnModel = new WFlowToBpmnCreator().loadBpmnFlowXmlByProcess("id", "name", JSONObject.parseObject(process, ProcessNode.class), false);
        String xml = new String(new BpmnXMLConverter().convertToXML(bpmnModel));
        ProcessValidatorFactory processValidatorFactory = new ProcessValidatorFactory();
        ProcessValidator defaultProcessValidator = processValidatorFactory.createDefaultProcessValidator();
        // 验证失败信息的封装ValidationError
        List<ValidationError> validate = defaultProcessValidator.validate(bpmnModel);
        if (CollectionUtil.isNotEmpty(validate)) {
            System.err.println("流程[验证失败]：" + JSONObject.toJSONString(validate));
        }
        FileUtil.writeString(xml, "C:\\Users\\willianfu\\Desktop\\bpmn.xml", StandardCharsets.UTF_8);
        //System.out.println(xml);
    }

}
