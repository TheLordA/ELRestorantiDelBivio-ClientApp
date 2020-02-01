package com.project.clientapp.Modal;

import java.util.List;

public class FavoriteModel {
    private boolean success ;
    private List<Favorite> result ;
    private String message ;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<Favorite> getResult() {
        return result;
    }

    public void setResult(List<Favorite> result) {
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
