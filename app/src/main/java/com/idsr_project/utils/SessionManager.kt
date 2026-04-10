package com.idsr_project.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object SessionManager {

    private const val PREF_NAME    = "UserSession"

    private const val KEY_USER_ID  = "user_Id"
    private const val KEY_USERNAME = "firstname"
    private const val KEY_LASTNAME = "lastname"
    private const val KEY_EMAIL    = "email"
    private const val KEY_PHONE    = "phone"
    private const val KEY_ROLE     = "role"
    private const val KEY_REGION   = "region"
    private const val KEY_DISTRICT = "district"
    private const val KEY_ACCESS   = "access_token"
    private const val KEY_REFRESH  = "refresh_token"
    private const val KEY_FCM_TOKEN = "fcm_token"

    @Volatile
    private var prefs: SharedPreferences? = null

    private fun getPrefs(context: Context): SharedPreferences {
        return prefs ?: synchronized(this) {
            prefs ?: context.applicationContext
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .also { prefs = it }
        }
    }

    fun saveUserSession(
        context: Context,
        userId: Int,
        firstname: String,
        lastname: String,
        email: String,
        phone: String,
        role: String,
        region: String?,
        district: String?,
        accessToken: String?,
        refreshToken: String?
    ) {
        getPrefs(context).edit(commit = false) {
            putInt(KEY_USER_ID, userId)
            putString(KEY_USERNAME, firstname)
            putString(KEY_LASTNAME, lastname)
            putString(KEY_EMAIL, email)
            putString(KEY_PHONE, phone)
            putString(KEY_ROLE, role)
            putString(KEY_REGION, region)
            putString(KEY_DISTRICT, district)
            putString(KEY_ACCESS, accessToken)
            putString(KEY_REFRESH, refreshToken)
        }
    }

    fun saveTokens(context: Context, accessToken: String?, refreshToken: String?) {
        getPrefs(context).edit(commit = true) {
            putString(KEY_ACCESS, accessToken)
            putString(KEY_REFRESH, refreshToken)
        }
    }

    fun clearSession(context: Context) {
        getPrefs(context).edit(commit = true) {
            clear()
        }
    }

    fun getUserId(context: Context): Int =
        getPrefs(context).getInt(KEY_USER_ID, 0)

    fun getUserName(context: Context): String? =
        getPrefs(context).getString(KEY_USERNAME, null)

    fun getUserLastName(context: Context): String? =
        getPrefs(context).getString(KEY_LASTNAME, null)

    fun getUserEmail(context: Context): String? =
        getPrefs(context).getString(KEY_EMAIL, null)

    fun getUserPhone(context: Context): String? =
        getPrefs(context).getString(KEY_PHONE, null)

    fun getUserRole(context: Context): String? =
        getPrefs(context).getString(KEY_ROLE, null)

    fun getUserRegion(context: Context): String? =
        getPrefs(context).getString(KEY_REGION, null)

    fun getUserDistrict(context: Context): String? =
        getPrefs(context).getString(KEY_DISTRICT, null)

    fun getAccessToken(context: Context): String? =
        getPrefs(context).getString(KEY_ACCESS, null)

    fun getRefreshToken(context: Context): String? =
        getPrefs(context).getString(KEY_REFRESH, null)

    fun getFcmToken(context: Context): String? =
        getPrefs(context).getString(KEY_FCM_TOKEN, null)


    fun saveFcmToken(context: Context, token: String) {
        getPrefs(context).edit(commit = false) {
            putString(KEY_FCM_TOKEN, token)
        }
    }


    fun updateNameAndPhone(context: Context, firstname: String, lastname: String, phone: String?) {
        getPrefs(context).edit(commit = false) {
            putString(KEY_USERNAME, firstname)
            putString(KEY_LASTNAME, lastname)
            if (phone != null) putString(KEY_PHONE, phone)
        }
    }


    fun isLoggedIn(context: Context): Boolean =
        !getAccessToken(context).isNullOrEmpty()

    fun isSessionValid(context: Context): Boolean =
        getUserId(context) != 0 &&
                !getAccessToken(context).isNullOrEmpty() &&
                !getUserName(context).isNullOrEmpty()

    fun getFullName(context: Context): String {
        val firstName = getUserName(context) ?: ""
        val lastName  = getUserLastName(context) ?: ""
        return listOfNotNull(
            firstName.takeIf { it.isNotBlank() },
            lastName.takeIf { it.isNotBlank() }
        ).joinToString(" ")
    }
}