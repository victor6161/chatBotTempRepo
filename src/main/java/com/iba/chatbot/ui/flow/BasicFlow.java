package com.iba.chatbot.ui.flow;

public abstract class BasicFlow {
    private FlowTypeEnum flowTypeEnum;

    public void startFlow() {

    }

    public void endFlow() {

    }

    public FlowTypeEnum getFlowTypeEnum() {
        return flowTypeEnum;
    }

    public void setFlowTypeEnum(FlowTypeEnum flowTypeEnum) {
        this.flowTypeEnum = flowTypeEnum;
    }
}
