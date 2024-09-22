package com.wflow.workflow.bean.process.props;

import com.wflow.workflow.bean.process.enums.ConditionModeEnum;
import com.wflow.workflow.bean.process.form.ValueType;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author : JoinFyc
 * @date : 2024/7/7
 */
@Data
public class ConditionProps implements Serializable {
    private static final long serialVersionUID = -45475579271153023L;
    //条件模式
    private ConditionModeEnum mode;
    //条件组类型
    private String groupsType;
    //条件组
    private List<Group> groups;
    //条件表达式
    private String expression;
    //js表达式
    private String js;
    //http请求配置
    private Map<String, Object> http;

    @Data
    public static class Group implements Serializable {
        private static final long serialVersionUID = -45475579271153023L;
        private String groupType;
        private List<Condition> conditions;
        private List<String> cids;

        @Data
        public static class Condition implements Serializable {
            private static final long serialVersionUID = -45475579271153023L;
            private String compare;
            private String id;
            private ValueType valueType;
            private List<Object> value;
        }
    }
}
