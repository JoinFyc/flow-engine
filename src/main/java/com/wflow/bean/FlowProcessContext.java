package com.wflow.bean;

import com.wflow.bean.do_.LoginDo;
import com.wflow.utils.RpcUtil;
import lombok.Getter;
import lombok.Setter;

/**
 * Work-FLow上下文
 *
 * @author fei
 */
@Setter
@Getter
public class FlowProcessContext implements AutoCloseable {

    private static ThreadLocal<FlowProcessContext> CONTEXT_HOLDER = new InheritableThreadLocal<>();

    private Boolean fieldTag = Boolean.TRUE;
    private String userId;
    private String tenantId;
    private Long groupId;

    private String fieldDesc;

    public static FlowProcessContext getFlowProcessContext() {
        return CONTEXT_HOLDER.get();
    }

    public static FlowProcessContext initFlowProcessContext() {
         FlowProcessContext context = new FlowProcessContext();
        final LoginDo loginDo = RpcUtil.getLoginTenant();
        context.setUserId(loginDo.getUserId());
        context.setTenantId(loginDo.getTenantId());
        CONTEXT_HOLDER.set(context);
        return CONTEXT_HOLDER.get();
    }

    private FlowProcessContext() {
        super();
    }

    @Override
    public void close() throws Exception {
        CONTEXT_HOLDER.remove();
    }

}
