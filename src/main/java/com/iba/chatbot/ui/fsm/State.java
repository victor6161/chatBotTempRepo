package com.iba.chatbot.ui.fsm;

public class State {
    private String state;
    private String text;
    private TypeNextActionEnum typeNextActionEnum;

    public State(String state, String text, TypeNextActionEnum typeNextActionEnum) {
        this.state = state;
        this.text = text;
        this.typeNextActionEnum = typeNextActionEnum;
    }

    public State() {
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public TypeNextActionEnum getTypeNextActionEnum() {
        return typeNextActionEnum;
    }

    public void setTypeNextActionEnum(TypeNextActionEnum typeNextActionEnum) {
        this.typeNextActionEnum = typeNextActionEnum;
    }
}
