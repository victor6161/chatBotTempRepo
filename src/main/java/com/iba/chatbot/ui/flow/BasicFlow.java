package com.iba.chatbot.ui.flow;

import java.util.List;
import java.util.Queue;

public class BasicFlow {
    private Iterable<FlowTypeEnum> flowTypeEnums;
    public void traverseForward() {
        flowTypeEnums.iterator().next();
    }

    private void validation() {

    }

    public Iterable<FlowTypeEnum> getFlowTypeEnums() {
        return flowTypeEnums;
    }

    public void setFlowTypeEnums(List<FlowTypeEnum> flowTypeEnums) {
        this.flowTypeEnums = flowTypeEnums;
    }
}
