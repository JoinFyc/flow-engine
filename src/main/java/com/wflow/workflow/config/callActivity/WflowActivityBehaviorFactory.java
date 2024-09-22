package com.wflow.workflow.config.callActivity;

import org.flowable.bpmn.model.CallActivity;
import org.flowable.engine.impl.bpmn.behavior.CallActivityBehavior;
import org.flowable.engine.impl.bpmn.helper.ClassDelegateFactory;
import org.flowable.engine.impl.bpmn.parser.factory.DefaultActivityBehaviorFactory;

/**
 * 重写 BehaviorFactory，改变默认子流程逻辑（为了自定义发起人）
 * @author : JoinFyc
 * @date : 2023/12/19
 */
public class WflowActivityBehaviorFactory extends DefaultActivityBehaviorFactory {
    public WflowActivityBehaviorFactory(ClassDelegateFactory classDelegateFactory) {
        super(classDelegateFactory);
    }

    public WflowActivityBehaviorFactory() {}

    @Override
    public CallActivityBehavior createCallActivityBehavior(CallActivity callActivity) {
        return new WflowCallActivityBehavior(callActivity);
    }
}
