package com.example.taskmaster

import android.content.Context
import com.example.taskmaster.UserProfile
import com.google.gson.Gson

class ProfileManager(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences("UserProfiles", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val ACTIVE_PROFILE_KEY = "active_profile_id"
        private const val PROFILE_PREFIX = "profile_"
    }

    // Get all profiles
    fun getAllProfiles(): List<UserProfile> {
        return sharedPreferences.all
            .filter { it.key.startsWith(PROFILE_PREFIX) }
            .map { gson.fromJson(it.value as String, UserProfile::class.java) }
            .sortedBy { it.name }
    }

    // Get active profile
    fun getActiveProfile(): UserProfile? {
        val activeProfileId = sharedPreferences.getString(ACTIVE_PROFILE_KEY, null) ?: return null
        return getProfileById(activeProfileId)
    }

    // Get profile by ID
    fun getProfileById(profileId: String): UserProfile? {
        val profileJson = sharedPreferences.getString(PROFILE_PREFIX + profileId, null) ?: return null
        return gson.fromJson(profileJson, UserProfile::class.java)
    }

    // Save profile
    fun saveProfile(profile: UserProfile) {
        val profileJson = gson.toJson(profile)
        sharedPreferences.edit().putString(PROFILE_PREFIX + profile.id, profileJson).apply()

        // If this is the first profile, set it as active
        if (getAllProfiles().size == 1) {
            setActiveProfile(profile.id)
        }
    }

    // Set active profile
    fun setActiveProfile(profileId: String) {
        sharedPreferences.edit().putString(ACTIVE_PROFILE_KEY, profileId).apply()
    }

    // Delete profile
    fun deleteProfile(profileId: String) {
        sharedPreferences.edit().remove(PROFILE_PREFIX + profileId).apply()

        // If the active profile was deleted, select another one if available
        if (sharedPreferences.getString(ACTIVE_PROFILE_KEY, "") == profileId) {
            val remainingProfiles = getAllProfiles()
            if (remainingProfiles.isNotEmpty()) {
                setActiveProfile(remainingProfiles.first().id)
            } else {
                sharedPreferences.edit().remove(ACTIVE_PROFILE_KEY).apply()
            }
        }
    }
}