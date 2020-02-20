package com.iba.chatbot.ui.fsm;

import java.util.List;
import java.util.Set;

public class StateMachine {
    private List<Transition> transitions;
    private State current;

    public StateMachine(State current, List<Transition> transitions) {
        this.transitions = transitions;
        this.current = current;
    }

    public StateMachine(State current) {
        this.current = current;
    }

    public void apply(Condition conditionParam) {
        current = getNextState(conditionParam);
    }

    private State getNextState(Condition conditionParam) {
        for(Transition transition : transitions) {
            boolean currentStateMatches = transition.getFrom().equals(current);
            boolean conditionsMatch = false;
            for (Condition condition : transition.getConditions()) {
                if(condition.equals(conditionParam)) {
                    conditionsMatch = true;
                    break;
                }
            }
            if(currentStateMatches && conditionsMatch) {
                return transition.getTo();
            }
        }// TODO Optional ?
        return current;
    }

    public List<Transition> getTransitions() {
        return transitions;
    }

    public void setTransitions(List<Transition> transitions) {
        this.transitions = transitions;
    }

    public State getCurrent() {
        return current;
    }

    public void setCurrent(State current) {
        this.current = current;
    }
}
