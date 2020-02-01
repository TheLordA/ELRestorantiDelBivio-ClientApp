package com.project.clientapp.Modal.EventBus;

public class SendTotalCostEvent {

    private String cash;

    public SendTotalCostEvent(String cash) {
        this.cash = cash;
    }

    public String getCash() {
        return cash;
    }

    public void setCash(String cash) {
        this.cash = cash;
    }
}
