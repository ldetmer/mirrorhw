package com.ldetmer.mirror


import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.ldetmer.authservice.AuthServiceCallBack
import com.ldetmer.authservice.IAuthService
import com.ldetmer.authservice.models.UserInfo
import kotlinx.android.synthetic.main.activity_main.*
import service.MirrorAuthService

const val EDIT_PROFILE_KEY = "EDIT_PROFILE"
const val PROFILE_INFO_KEY = "PROFILE_INFO"

class MainActivity : AppCompatActivity() {

    private val authCallBack = object : AuthServiceCallBack.Stub() {
        override fun authSuccessful(missingInfo: Boolean, userInfo: UserInfo?) {
            runOnUiThread {
                hideLoading()
                goToProfilePage(missingInfo, userInfo)
            }
        }

        override fun updateProfile(userInfo: UserInfo?) {
            runOnUiThread {
                updateProfileInfo(userInfo)
                hideLoading()
            }
        }

        override fun logout() {
            runOnUiThread {
                goToAuthPage()
            }
        }

        override fun showError(message: String?) {
            runOnUiThread {
                hideLoading()
                errorMessage.text = message
            }
        }

    }

    fun updateProfileInfo(userInfo: UserInfo?) {
        val profileFragment = supportFragmentManager.findFragmentByTag(ProfileFragment::class.java.simpleName)
        profileFragment?.let {
            (it as ProfileFragment).refreshProfile(userInfo)
        } ?: goToProfilePage(userInfo?.birthday == null, userInfo)
    }

    fun clearBackStack() {
        for (i in 0 until supportFragmentManager.getBackStackEntryCount()) {
            supportFragmentManager.popBackStack()
        }
    }

    fun goToProfilePage(showEdit: Boolean, userInfo: UserInfo?) {
        clearBackStack()
        val ft = supportFragmentManager.beginTransaction()
        var profilePage = ProfileFragment()
        var bundle = Bundle()
        bundle.putBoolean(EDIT_PROFILE_KEY, showEdit)
        bundle.putParcelable(PROFILE_INFO_KEY, userInfo)
        profilePage.arguments = bundle
        goToPage(profilePage)
    }

    fun goToSignUpPage() {
        goToPage(SignUpFragment())
    }

    fun goToLoginPage() {
        goToPage(LoginFragment())
    }

    fun goToAuthPage() {
        clearBackStack()
        goToPage(AuthFragment())
    }

    fun goToPage(fragment: Fragment) {
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.container, fragment, fragment::class.java.simpleName)
        ft.addToBackStack(fragment::class.java.simpleName)
        ft.commitAllowingStateLoss()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val intent = Intent(this, MirrorAuthService::class.java)
        intent.action = MirrorAuthService::class.java.name
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            authService?.unregisterCallBack(authCallBack)
        } catch (e: RemoteException) {
        }

        unbindService(mConnection)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val topFrag = supportFragmentManager.findFragmentById(R.id.container)
        if (topFrag == null)
            finish()

    }

    var authService: IAuthService? = null

    val mConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {

            authService = IAuthService.Stub.asInterface(service)

            try {
                authService?.registerCallBack(authCallBack)
            } catch (e: RemoteException) {
                Log.e("Main", "error registering auth service " + authService)
            }
            if (authService?.isLoggedIn() ?: false)
                authService?.getProfileInfo()
            else
                goToAuthPage()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            authService = null
        }
    }

    fun signUp(name: String?, password: String?, confirmPassword: String?, email: String?) {
        showLoading()
        authService?.signUp(name, password, confirmPassword, email)
    }

    fun login(email: String?, password: String?) {
        showLoading()
        authService?.login(email, password)
    }

    fun updateProfileInfo(name: String?, location: String?, birthday: String?) {
        showLoading()
        authService?.updateProfile(name, location, birthday)
    }

    fun refreshProfile() {
        showLoading()
        authService?.getProfileInfo()
    }

    fun showLoading() {
        progressbar.visibility = View.VISIBLE
        overlay.visibility = View.VISIBLE
        errorMessage.text = ""
    }

    fun hideLoading() {
        progressbar.visibility = View.GONE
        overlay.visibility = View.GONE
    }

}
