package com.techmania.tumago.Model;

import com.google.gson.annotations.SerializedName;

public class ResetPasswordModel {
    @SerializedName("oldPassword")
    String old_password;
    @SerializedName("newPassword")
    String new_password;
    @SerializedName("confirmPassword")
    String confirm_password;

    public ResetPasswordModel(String confirm_password, String new_password, String old_password) {
        this.confirm_password = confirm_password;
        this.new_password = new_password;
        this.old_password = old_password;
    }
}
