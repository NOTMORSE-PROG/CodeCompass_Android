package com.example.codecompass.model;

import com.google.gson.annotations.SerializedName;

public class ChangePasswordRequest {

    @SerializedName("old_password")
    private final String oldPassword;

    @SerializedName("new_password")
    private final String newPassword;

    @SerializedName("new_password_confirm")
    private final String newPasswordConfirm;

    @SerializedName("refresh")
    private final String refresh;

    public ChangePasswordRequest(String oldPassword, String newPassword,
                                  String newPasswordConfirm, String refresh) {
        this.oldPassword        = oldPassword;
        this.newPassword        = newPassword;
        this.newPasswordConfirm = newPasswordConfirm;
        this.refresh            = refresh;
    }
}
