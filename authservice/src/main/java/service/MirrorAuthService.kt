package service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ldetmer.authservice.AuthServiceImpl
import com.ldetmer.authservice.api.Endpoints
import com.ldetmer.authservice.models.UserAPIInfo
import com.ldetmer.authservice.models.UserAuthResponse
import com.ldetmer.authservice.models.UserInfo
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

const val MIRROR_SHARED_PREF = "MIRROR_SHARED_PREF"
const val API_TOKEN_KEY = "API_TOKEN"
const val USER_NAME_KEY = "USER_NAME_KEY"
const val USER_BIRTHDAY_KEY = "USER_BIRTHDAY_KEY"
const val USER_LOCATION_KEY = "USER_LOCATION_KEY"
const val USER_EMAIL_KEY = "USER_EMAIL_KEY"

//service class used to interface for intents to AuthServiceImpl, sets up the API + datastore
class MirrorAuthService : Service() {

    var retrofit: Retrofit
    var apiService: Endpoints
    var client: OkHttpClient? = null
    var baseUrl = "https://dev.refinemirror.com/api/v1/"

    var builder: OkHttpClient.Builder = OkHttpClient.Builder()
    private var httpLoggingInterceptor: HttpLoggingInterceptor = HttpLoggingInterceptor()

    //TODO: to save time I used shared preferences, but I would probably have put this in a sqllite database
    //also would have probably made a sharedpref util class to remove code chatter saving/retrieving from shared pref
    //TODO: some small logic management is included in datastore which is not unit testable, should be refactored to handle setting/clearing currentUserInfo memory object in testable manner
    val dataStore = object : Datastore(60000, 120000) {
        override fun storeAuthInfo(authResponse: UserAuthResponse) {
            applicationContext.getSharedPreferences(MIRROR_SHARED_PREF, Context.MODE_PRIVATE).edit()
                .putString(API_TOKEN_KEY, authResponse.auth.apiToken).apply()
        }

        override fun clearAllData() {
            applicationContext.getSharedPreferences(MIRROR_SHARED_PREF, Context.MODE_PRIVATE).edit().clear().apply()
            softTTL = 0L
            hardTTL = 0L
            currentUserInfo = null
        }

        override fun storeUserInfo(userInfo: UserAPIInfo) {
            applicationContext.getSharedPreferences(MIRROR_SHARED_PREF, Context.MODE_PRIVATE).edit()
                .putString(USER_NAME_KEY, userInfo.name).apply()
            applicationContext.getSharedPreferences(MIRROR_SHARED_PREF, Context.MODE_PRIVATE).edit()
                .putString(USER_BIRTHDAY_KEY, userInfo.profile.birthdate).apply()
            applicationContext.getSharedPreferences(MIRROR_SHARED_PREF, Context.MODE_PRIVATE).edit()
                .putString(USER_LOCATION_KEY, userInfo.profile.location).apply()
            applicationContext.getSharedPreferences(MIRROR_SHARED_PREF, Context.MODE_PRIVATE).edit()
                .putString(USER_EMAIL_KEY, userInfo.email).apply()
            //TODO: handle clearing object in memory better
            currentUserInfo = null
        }

        override fun getUserInfo(): UserInfo? {
            //first try to get object in memory, if id doesn't exists recreate from shared prefs
            if (currentUserInfo != null)
                return currentUserInfo
            var email = applicationContext.getSharedPreferences(MIRROR_SHARED_PREF, Context.MODE_PRIVATE)
                .getString(USER_EMAIL_KEY, null)
            //for simplicity sake assume if email isn't stored user isn't logged in
            if (email == null)
                return null
            var name = applicationContext.getSharedPreferences(MIRROR_SHARED_PREF, Context.MODE_PRIVATE)
                .getString(USER_NAME_KEY, "")
            var birthday = applicationContext.getSharedPreferences(MIRROR_SHARED_PREF, Context.MODE_PRIVATE)
                .getString(USER_BIRTHDAY_KEY, "")
            var location = applicationContext.getSharedPreferences(MIRROR_SHARED_PREF, Context.MODE_PRIVATE)
                .getString(USER_LOCATION_KEY, "")
            currentUserInfo = UserInfo(name, email, birthday, location)
            return currentUserInfo
        }

        override fun updateUserInfo(name: String?, location: String?, birthdate: String?, email: String?) {
            name?.let {
                applicationContext.getSharedPreferences(MIRROR_SHARED_PREF, Context.MODE_PRIVATE).edit()
                    .putString(USER_NAME_KEY, it).apply()
            }
            location?.let {
                applicationContext.getSharedPreferences(MIRROR_SHARED_PREF, Context.MODE_PRIVATE).edit()
                    .putString(USER_LOCATION_KEY, it).apply()
            }
            birthdate?.let {
                applicationContext.getSharedPreferences(MIRROR_SHARED_PREF, Context.MODE_PRIVATE).edit()
                    .putString(USER_BIRTHDAY_KEY, it).apply()
            }
            email?.let {
                applicationContext.getSharedPreferences(MIRROR_SHARED_PREF, Context.MODE_PRIVATE).edit()
                    .putString(USER_EMAIL_KEY, it).apply()
            }
            //TODO could handle this more optimally and replace object in memory values instead of just wiping out
            currentUserInfo = null
        }

        override fun getApiKey(): String? {
            return applicationContext.getSharedPreferences(MIRROR_SHARED_PREF, Context.MODE_PRIVATE)
                .getString(API_TOKEN_KEY, null)
        }

    }

    override fun onBind(intent: Intent): IBinder? {
        return AuthServiceImpl(apiService, dataStore)
    }


    init {
        setUpHttpLoggingInterceptor()
        val gson: Gson = GsonBuilder().create()
        retrofit = Retrofit.Builder().baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(client).build()
        apiService = retrofit.create(Endpoints::class.java)
    }

    private fun setUpHttpLoggingInterceptor() {
        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        builder.interceptors().add(httpLoggingInterceptor)
        client = builder.build()
    }
}

abstract class Datastore(val softTTLTime: Long, val hardTTLTime: Long) {
    var hardTTL = 0L
    var softTTL = 0L
    var currentUserInfo: UserInfo? = null
    abstract fun storeAuthInfo(authResponse: UserAuthResponse)
    abstract fun getApiKey(): String?
    abstract fun getUserInfo(): UserInfo?
    abstract fun storeUserInfo(userInfo: UserAPIInfo)
    abstract fun updateUserInfo(name: String?, location: String?, birthdate: String?, email: String?)
    abstract fun clearAllData()
    fun setCacheTimes() {
        softTTL = Calendar.getInstance().timeInMillis + softTTLTime
        hardTTL = Calendar.getInstance().timeInMillis + hardTTLTime
    }

}
