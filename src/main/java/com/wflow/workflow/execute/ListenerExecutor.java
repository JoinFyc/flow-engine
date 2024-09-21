package com.wflow.workflow.execute;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wflow.bean.entity.WflowModelHistorys;
import com.wflow.mapper.WflowModelHistorysMapper;
import com.wflow.workflow.UELTools;
import com.wflow.workflow.bean.process.HttpDefinition;
import com.wflow.workflow.bean.process.props.ApprovalProps;
import com.wflow.workflow.bean.process.props.RootProps;
import com.wflow.workflow.config.WflowGlobalVarDef;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * @author : willian fu
 * @date : 2023/10/29
 */
@Slf4j
@Component("listenerExecutor")
public class ListenerExecutor {

    @Autowired
    private WflowModelHistorysMapper modelHistorysMapper;

    @Autowired
    private UELTools uelTools;

    @Autowired
    private ExecutorService executorService;

    public void doProcessChangeHandler(String event, String instanceId, String defId){
        try {
            WflowModelHistorys selected = modelHistorysMapper.selectOne(
                    new LambdaQueryWrapper<WflowModelHistorys>()
                            .select(WflowModelHistorys::getProcessConfig)
                            .eq(WflowModelHistorys::getProcessDefId, defId));
            if (Objects.nonNull(selected) && StrUtil.isNotBlank(selected.getProcessConfig())){
                Map<String, Object> contextVar = uelTools.getContextVar(instanceId, defId);
                JSONObject parsed = JSONObject.parseObject(selected.getProcessConfig());
                JSONObject listener = parsed.getJSONObject("listener");
                if (Objects.nonNull(listener)){
                    listener.getJSONArray(event).toJavaList(JSONObject.class).forEach(object -> {
                        String actionType = object.getString("actionType");
                        switch (actionType){
                            case "JS":
                                new JsExecute(executorService).executeVoid("action", "function action(ctx){"+ object.getString("js") +"}" , contextVar);
                                break;
                            case "JAVA":
                                new ElExecute().execute(object.getString("java"), contextVar, Object.class);
                                break;
                            case "HTTP":
                                new HttpExecute().execute(object.getObject("http", HttpDefinition.class), executorService, Object.class, contextVar);
                                break;
                        }
                    });
                }
            }
        } catch (Exception e) {
            log.warn("流程实例[{}]的[{}]事件触发失败：{}", instanceId, event, e.getMessage());
        }
    }

    public void doProcessNodeChangeHandler(String event, String instanceId, String defId, String nodeId, String actType){
        if (!"userTask".equals(actType)){
            return; //过滤其他非必要节点
        }
        try {
//            todo 获取流程实例变量
            Map<String, Object> contextVar = uelTools.getContextVar(instanceId, defId);
            Map map = (Map) contextVar.get(WflowGlobalVarDef.WFLOW_NODE_PROPS);
            Object nodeProps = map.get(nodeId);
            Map<String, JSONArray> nodeLis = null;
            if (nodeProps instanceof ApprovalProps){
                nodeLis = ((ApprovalProps) nodeProps).getListeners();
            } else if (nodeProps instanceof RootProps) {
                nodeLis = ((RootProps) nodeProps).getListeners();
            }
            if (Objects.nonNull(nodeLis) && CollectionUtil.isNotEmpty(nodeLis)){
                nodeLis.get(event).toJavaList(JSONObject.class).forEach(object -> {
                    String actionType = object.getString("actionType");
                    switch (actionType){
                        case "JS":
                            new JsExecute(executorService).executeVoid("action","function action(ctx){"+ object.get("js") +"}", contextVar);
                            break;
                        case "JAVA":
                            new ElExecute().execute(object.getString("java"), contextVar, Object.class);
                            break;
                        case "HTTP":
                            new HttpExecute().execute(BeanUtil.mapToBean(object.getJSONObject("http"), HttpDefinition.class, true), executorService, Object.class, contextVar);
                            break;
                    }
                });
            }
        } catch (Exception e) {
            log.warn("流程实例[{}]的节点{} [{}]事件触发失败：{}", instanceId, nodeId, event, e.getMessage());
        }
    }

}
