package com.iba.chatbot.ui.session;

import com.github.messenger4j.userprofile.UserProfile;

public class UserSession {
    private String step;
    private String type;
    private boolean isMenuUpdateNeeded;

    public UserSession() {
    }

    public UserSession(String step, UserProfile userProfile) {
        this.step = step;
        this.userProfile = userProfile;
    }

    public UserSession(UserProfile userProfile) {
        this.userProfile = userProfile;
    }

    public UserSession(String type, boolean isMenuUpdateNeeded, UserProfile userProfile) {
        this.type = type;
        this.isMenuUpdateNeeded = isMenuUpdateNeeded;
        this.userProfile = userProfile;
    }

    private UserProfile userProfile;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }
}
