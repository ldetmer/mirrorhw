// AuthServiceCallBack.aidl
package com.ldetmer.authservice;

import com.ldetmer.authservice.models.UserInfo;

interface AuthServiceCallBack {
    void authSuccessful(boolean missingInfo, in UserInfo userInfo);
    void updateProfile(in UserInfo userInfo);
    void logout();
    void showError(String message);
}
