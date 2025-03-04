package com.wflow.workflow.bean.process.enums;

import com.wflow.workflow.bean.process.props.*;

/**
 * @author : JoinFyc
 * @date : 2024/7/6
 */
public enum NodeTypeEnum {

    ROOT {
        @Override
        public Class<?> getTypeClass() {
            return RootProps.class;
        }
    },

    APPROVAL {
        @Override
        public Class<?> getTypeClass() {
            return ApprovalProps.class;
        }
    },

    TASK {
        @Override
        public Class<?> getTypeClass() {
            return ApprovalProps.class;
        }
    },

    CC {
        @Override
        public Class<?> getTypeClass() {
            return CcProps.class;
        }
    },

    CONDITIONS {
        @Override
        public Class<?> getTypeClass() {
            return Object.class;
        }
    },

    CONCURRENTS {
        @Override
        public Class<?> getTypeClass() {
            return Object.class;
        }
    },

    CONDITION {
        @Override
        public Class<?> getTypeClass() {
            return ConditionProps.class;
        }
    },

    INCLUSIVES {
        @Override
        public Class<?> getTypeClass() {
            return Object.class;
        }
    },

    ACT_RESULTS {
        @Override
        public Class<?> getTypeClass() {
            return Object.class;
        }
    },

    INCLUSIVE {
        @Override
        public Class<?> getTypeClass() {
            return ConditionProps.class;
        }
    },

    CONCURRENT {
        @Override
        public Class<?> getTypeClass() {
            return Object.class;
        }
    },

    DELAY {
        @Override
        public Class<?> getTypeClass() {
            return DelayProps.class;
        }
    },

    TRIGGER {
        @Override
        public Class<?> getTypeClass() {
            return TriggerProps.class;
        }
    },

    EMPTY {
        @Override
        public Class<?> getTypeClass() {
            return Object.class;
        }
    },

    SUBPROC {
        @Override
        public Class<?> getTypeClass() {
            return SubProcessProps.class;
        }
    };

    public abstract Class<?> getTypeClass();
}
