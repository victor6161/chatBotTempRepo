package com.iba.chatbot.ui.flow;

public class MessageBotEntity {
    private TypeStepEnum typeStepEnum;
    private String message;

    public MessageBotEntity() {
    }

    public MessageBotEntity(TypeStepEnum typeStepEnum, String message) {
        this.typeStepEnum = typeStepEnum;
        this.message = message;
    }

    public TypeStepEnum getTypeStepEnum() {
        return typeStepEnum;
    }

    public void setTypeStepEnum(TypeStepEnum typeStepEnum) {
        this.typeStepEnum = typeStepEnum;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
