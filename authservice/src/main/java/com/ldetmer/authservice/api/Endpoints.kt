package com.ldetmer.authservice.api

import com.ldetmer.authservice.models.*
import io.reactivex.Observable
import retrofit2.http.*

interface Endpoints {

    @POST("auth/signup")
    fun signUp(@Body userSignUpRequest: UserSignUpRequest): Observable<UserAuthResponse>

    @POST("auth/login")
    fun login(@Body userLoginRequest: UserLoginRequest): Observable<UserAuthResponse>

    @GET("user/me")
    fun getUserInfo(@HeaderMap headerMap: MutableMap<String, String>): Observable<UserInfoResponse>

    @PATCH("user/me")
    fun updateUserProfile(@HeaderMap headerMap: MutableMap<String, String>, @Body userUpdateRequest: UserUpdateRequest): Observable<BaseResponse>
}

