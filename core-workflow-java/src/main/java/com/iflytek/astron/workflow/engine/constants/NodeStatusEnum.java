package com.iflytek.astron.workflow.engine.constants;

/**
 * 节点运行状态
 *
 * @author YiHui
 * @date 2025/11/28
 */
public enum NodeStatusEnum {
    /**
     * 初始化
     */
    INIT,
    /**
     * 运行中
     */
    RUNNING,

    /**
     * 标记，适用于条件分支、异常分支时，对于走不到这个链路的node
     */
    MARK,

    /**
     * 成功
     */
    SUCCESS {
        @Override
        public boolean executed() {
            return true;
        }
    },
    /**
     * 失败
     */
    ERROR {
        @Override
        public boolean executed() {
            return true;
        }
    },
    SKIP {
        @Override
        public boolean executed() {
            return true;
        }
    }
    ;


    /**
     * 是否已经执行完毕
     *
     * @return
     */
    public boolean executed() {
        return false;
    }
}
