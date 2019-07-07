// IAuthService.aidl
package com.ldetmer.authservice;

import com.ldetmer.authservice.AuthServiceCallBack;
import com.ldetmer.authservice.models.UserInfo;

interface IAuthService {

    void signUp(String name, String password, String confirmPassword, String email);

    void login(String email, String password);

    void updateProfile(String name, String location, String birthdate);

    void registerCallBack(AuthServiceCallBack callback);

    void unregisterCallBack(AuthServiceCallBack callback);

    boolean isLoggedIn();

    void getProfileInfo();
}

