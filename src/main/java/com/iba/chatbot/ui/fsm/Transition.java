package com.iba.chatbot.ui.fsm;

import java.util.Set;

public class Transition {

    private State from;
    private Set<Condition> conditions;
    private State to;

    public Transition(State from, Set<Condition> conditions, State to) {
        this.from = from;
        this.conditions = conditions;
        this.to = to;
    }

    public State getFrom() {
        return from;
    }

    public void setFrom(State from) {
        this.from = from;
    }

    public Set<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(Set<Condition> conditions) {
        this.conditions = conditions;
    }

    public State getTo() {
        return to;
    }

    public void setTo(State to) {
        this.to = to;
    }
}
