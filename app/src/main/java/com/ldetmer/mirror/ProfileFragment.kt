package com.ldetmer.mirror

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ldetmer.authservice.models.UserInfo
import kotlinx.android.synthetic.main.fragment_profile.*

class ProfileFragment : Fragment() {

    var editMode = false
    var userInfo: UserInfo? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater!!.inflate(R.layout.fragment_profile, container, false)
        editMode = arguments?.getBoolean(EDIT_PROFILE_KEY, false) ?: false
        userInfo = arguments?.getParcelable(PROFILE_INFO_KEY) as UserInfo?
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        refreshProfile(userInfo)
        if (editMode) {
            editProfile()
        } else {
            viewProfile()
        }
        refreshProfile.setOnClickListener { (activity as MainActivity).refreshProfile() }
        editProfileButton.setOnClickListener { editProfile() }
        update.setOnClickListener {
            (activity as MainActivity).updateProfileInfo(
                editName.text?.toString(),
                editLocation.text?.toString(),
                editBirthday.text?.toString()
            )
        }
        cancel.setOnClickListener { viewProfile() }
    }

    fun editProfile() {
        viewProfile.visibility = View.GONE
        editProfile.visibility = View.VISIBLE
    }

    fun viewProfile() {
        viewProfile.visibility = android.view.View.VISIBLE
        editProfile.visibility = View.GONE
    }

    fun refreshProfile(userInfo: UserInfo?) {
        userInfo?.let {
            location.setText("Location: " + it.location)
            editLocation.setText(it.location)
            birthday.setText("Birthday: " + it.birthday)
            editBirthday.setText(it.birthday)
            name.setText("Name: " + it.name)
            editName.setText(it.name)
        }
        viewProfile()
    }
}