package com.mobiliya.fleet.models;

public class ResponseModel {

    public short getStatus() {
        return status;
    }

    public void setStatus(short status) {
        this.status = status;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public short status;
    public Object data;
    public int total;
    public String message;

    public int getHttpResponseCode() {
        return HttpResponseCode;
    }

    public void setHttpResponseCode(int httpResponseCode) {
        HttpResponseCode = httpResponseCode;
    }

    public int HttpResponseCode;

}
