package com.iba.chatbot.ui.session;

import com.github.messenger4j.userprofile.UserProfile;
import com.iba.chatbot.ui.flow.MessageBotEntity;
import com.iba.chatbot.ui.fsm.StateMachine;

import java.util.List;

public class UserSession {

/*    private Iterable<MessageBotEntity> steps;
    private MessageBotEntity currentState;*/
    private boolean isMenuUpdateNeeded;
    private UserProfile userProfile;
    private StateMachine stateMachine;

    public UserSession(boolean isMenuUpdateNeeded, UserProfile userProfile, StateMachine stateMachine) {
        this.stateMachine = stateMachine;
        this.isMenuUpdateNeeded = isMenuUpdateNeeded;
        this.userProfile = userProfile;
    }

    public boolean isMenuUpdateNeeded() {
        return isMenuUpdateNeeded;
    }

    public void setMenuUpdateNeeded(boolean menuUpdateNeeded) {
        isMenuUpdateNeeded = menuUpdateNeeded;
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    public void setUserProfile(UserProfile userProfile) {
        this.userProfile = userProfile;
    }

    public StateMachine getStateMachine() {
        return stateMachine;
    }

    public void setStateMachine(StateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

  /*  public List<MessageBotEntity> getSteps() {
        return steps;
    }

    public void setSteps(List<MessageBotEntity> steps) {
        this.steps = steps;
    }*/
}
