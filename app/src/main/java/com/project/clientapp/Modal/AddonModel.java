package com.project.clientapp.Modal;

import java.util.List;

public class AddonModel {
    private String success;
    private List<Addon> result;
    private String message ;

    public String getSuccess() {
        return success;
    }

    public void setSuccess(String success) {
        this.success = success;
    }

    public List<Addon> getResult() {
        return result;
    }

    public void setResult(List<Addon> result) {
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
