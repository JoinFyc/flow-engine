package com.wflow.workflow.bean.process;

import com.wflow.workflow.bean.process.enums.NodeTypeEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author : willian fu
 * @date : 2022/7/6
 */
@Data
public class ProcessNode<T> implements Serializable {
    private static final long serialVersionUID = -45475579271153023L;

    private String id;

    private String parentId;

    private NodeTypeEnum type;

    private String name;
    //当前节点所在网关的ID
    private String gatewayId;

    private T props;

    private NodeTypeEnum parentType;

    private ProcessNode<?> children;

    private List<ProcessNode<?>> branchs;
}
