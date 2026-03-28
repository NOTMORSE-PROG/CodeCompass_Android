package com.example.codecompass.model;

import com.google.gson.annotations.SerializedName;

public class TokenRefreshResponse {

    @SerializedName("access")
    private String access;

    public String getAccess() { return access; }
}
