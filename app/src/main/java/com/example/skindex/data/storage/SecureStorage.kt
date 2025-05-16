package com.example.skindex.data.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.skindex.core.constants.AppConstants
import javax.inject.Inject
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext

class SecureStorage @Inject constructor(@ApplicationContext context: Context) {
    private val sharedPreferences = EncryptedSharedPreferences.create(
        AppConstants.PREFS_NAME,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(token: String) {
        sharedPreferences.edit { putString(AppConstants.TOKEN_KEY, token) }
    }

    fun getToken(): String? {
        return sharedPreferences.getString(AppConstants.TOKEN_KEY, null)
    }

    fun clearToken() {
        sharedPreferences.edit { remove(AppConstants.TOKEN_KEY) }
    }

    fun hasToken(): Boolean {
        return getToken() != null
    }
}