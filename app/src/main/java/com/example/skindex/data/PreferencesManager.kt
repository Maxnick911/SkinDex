package com.example.skindex.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class PreferencesManager @Inject constructor(@ApplicationContext context: Context) {

    private val preferences = context.getSharedPreferences("skindex_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAST_POST_ID = "last_post_id"
    }

    fun saveLastPostId(postId: Int) {
        preferences.edit { putInt(KEY_LAST_POST_ID, postId) }
    }
}