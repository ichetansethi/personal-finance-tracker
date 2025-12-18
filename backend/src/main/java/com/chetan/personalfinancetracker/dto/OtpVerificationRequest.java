package com.chetan.personalfinancetracker.dto;

public class OtpVerificationRequest {

    private String username;
    private String otp;

    public OtpVerificationRequest() {
    }

    public OtpVerificationRequest(String username, String otp) {
        this.username = username;
        this.otp = otp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}
