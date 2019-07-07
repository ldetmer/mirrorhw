package com.ldetmer.authservice.models

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName


data class UserAuth(
    @SerializedName("user_uuid") val uuid: String, @SerializedName("user_token") val userToken: String,
    @SerializedName("api_token") val apiToken: String
)

data class UserAuthResponse(@SerializedName("data") val auth: UserAuth) : BaseResponse()

data class UserSignUpRequest(val name: String?, val password: String?, val password2: String?, val email: String?)

data class UserInfoResponse(@SerializedName("data") var user: UserAPIInfo) : BaseResponse()

data class UserAPIInfo(val name: String, val email: String, val profile: Profile)

data class Profile(val birthdate: String, val location: String)

data class UserLoginRequest(val email: String?, val password: String?)

data class UserUpdateRequest(val name: String?, val location: String?, val birthdate: String?) : BaseResponse()

//TODO add data error message response
open class BaseResponse {
    companion object {
        const val SUCCESS_MESSAGE = "ok"
        const val ERROR_INVALID_TOKEN = "error_invalid_token"
    }

    @SerializedName("message")
    var message: String? = null
    @SerializedName("error_short_code")
    var errorShortCode: String? = null
}


class UserInfo(var name: String, var email: String, var birthday: String?, var location: String?) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(email)
        parcel.writeString(birthday)
        parcel.writeString(location)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<UserInfo> {
        override fun createFromParcel(parcel: Parcel): UserInfo {
            return UserInfo(parcel)
        }

        override fun newArray(size: Int): Array<UserInfo?> {
            return arrayOfNulls(size)
        }
    }

}