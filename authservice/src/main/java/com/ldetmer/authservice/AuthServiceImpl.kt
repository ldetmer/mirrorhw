package com.ldetmer.authservice

import android.util.Log
import com.ldetmer.authservice.api.Endpoints
import com.ldetmer.authservice.models.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException
import service.Datastore
import java.util.*

//class handles all api calls as well as caching logic, for testing you can mock objects for apiService & datastore
class AuthServiceImpl(val apiService: Endpoints, val datastore: Datastore) : IAuthService.Stub() {

    var callBacks = ArrayList<AuthServiceCallBack>()

    fun setupAuthHeader(): MutableMap<String, String> {
        val headers: MutableMap<String, String> = HashMap()
        val apiToken = datastore.getApiKey()
        if (apiToken != null) {
            headers.put("Authorization", "Bearer " + apiToken)
        }
        return headers
    }

    override fun isLoggedIn(): Boolean {
        return datastore.getUserInfo() != null
    }

    //TODO: this function seems specific to an app and probably should take in a separate callback instance to only update that app
    override fun getProfileInfo() {
        val currentTime = Calendar.getInstance().timeInMillis

        if (currentTime < datastore.softTTL) {
            //if time is within soft ttl return cache version from datastore
            callBacks.forEach {
                it.updateProfile(datastore.getUserInfo())
            }
        } else if (currentTime < datastore.hardTTL) {
            //if time is between hard and soft ttl return cache version to app, and refresh in background
            getProfile(true)
            callBacks.forEach {
                it.updateProfile(datastore.getUserInfo())
            }
        } else {
            //if time is more than hard ttl refresh from back end
            getProfile(false)
        }
    }

    //TODO: right now only using one callback type, would definitely create different callback types for auth vs profile updates
    //i would assume that multiple apps would want to know a user was signed into system, but not all would necessarily care about profile updates
    override fun registerCallBack(callback: AuthServiceCallBack) {
        callBacks.add(callback)
    }

    override fun unregisterCallBack(callback: AuthServiceCallBack) {
        callBacks.remove(callback)
    }

    override fun signUp(name: String?, password: String?, confirmPassword: String?, email: String?) {

        apiService.signUp(UserSignUpRequest(name, password, confirmPassword, email))
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(
                { response ->
                    datastore.storeAuthInfo(response)
                    datastore.updateUserInfo(name, null, null, email)
                    callBacks.forEach {
                        it.authSuccessful(true, datastore.getUserInfo())
                    }
                },
                { error ->
                    getError(error)
                }
            )
    }


    override fun login(email: String?, password: String?) {
        apiService.login(UserLoginRequest(email, password))
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(
                { response ->
                    datastore.storeAuthInfo(response)
                    //after login get profile info
                    getProfile(false)
                },
                { error ->
                    getError(error)
                }
            )
    }

    override fun updateProfile(name: String?, location: String?, birthdate: String?) {
        apiService.updateUserProfile(setupAuthHeader(), UserUpdateRequest(name, location, birthdate))
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(
                { response ->
                    datastore.updateUserInfo(name, location, birthdate, null)
                    callBacks.forEach {
                        it.updateProfile(datastore.getUserInfo())
                    }
                },
                { error ->
                    getError(error)
                }
            )
    }

    fun getProfile(refreshInBackground: Boolean) {
        apiService.getUserInfo(setupAuthHeader())
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(
                { userInfo ->
                    var user = userInfo.user
                    var userInfo = UserInfo(user.name, user.email, user.profile.birthdate, user.profile.location)
                    if (!refreshInBackground) {
                        //if we're not refreshing in background update profile when we get response
                        callBacks.forEach {
                            it.updateProfile(userInfo)
                        }
                    }
                    datastore.storeUserInfo(user)
                    //everytime we store the user information from back end only we should reset cache times
                    datastore.setCacheTimes()
                    datastore.currentUserInfo = userInfo
                },
                { error ->
                    getError(error)
                }
            )
    }

    //this function would be used to parse out error response
    fun checkForErrors(baseResponse: BaseResponse): Boolean {
        if (!baseResponse.message.equals(BaseResponse.SUCCESS_MESSAGE)) {
            sendErrorToClient(baseResponse.message)
            return true
        } else if (baseResponse.errorShortCode?.equals(BaseResponse.ERROR_INVALID_TOKEN) ?: false) {
            sendLogoutToClient()
            return true
        }
        return false
    }

    private fun sendLogoutToClient() {
        callBacks.forEach {
            it.logout()
        }
        datastore.clearAllData()
    }

    fun getError(throwable: Throwable?) {
        var messageToSend: String? = null
        throwable?.let {
            if (it is HttpException) {
                if (it.code() == 401) {
                    sendLogoutToClient()
                    messageToSend ="Session has expired please log back in again"
                    //TODO: handle error checking to parse out response
                } else if (it.code() == 400)
                    messageToSend = "TBD needs to be formatted: " + it.response()?.errorBody()?.string()
            }
        }
        sendErrorToClient(messageToSend)
    }

    fun sendErrorToClient(message: String?) {
        var messageToSend = message
        if (messageToSend == null)
            messageToSend = "There was an error but we haven't coded for it yet"
        callBacks.forEach {
            it.showError(messageToSend)
        }
    }


}