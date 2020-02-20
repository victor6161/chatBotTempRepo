package com.iba.chatbot.ui.fsm;

import java.util.Objects;

public class Condition {
    public Condition(String condition) {
        this.condition = condition;
    }

    public Condition() {
    }

    private String condition;

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Condition condition1 = (Condition) o;
        return Objects.equals(condition, condition1.condition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(condition);
    }
}
