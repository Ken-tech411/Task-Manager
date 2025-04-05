package com.example.taskmanager

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

class EditProfileFragment : Fragment() {
    private lateinit var profileManager: ProfileManager
    private var profileId: String? = null
    private var isNewProfile: Boolean = false

    interface OnProfileUpdatedListener {
        fun onProfileUpdated()
    }

    private var listener: OnProfileUpdatedListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnProfileUpdatedListener) {
            listener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profileManager = ProfileManager(requireContext())
        
        arguments?.let {
            profileId = it.getString("profileId")
            isNewProfile = it.getBoolean("isNewProfile", false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_profile, container, false)

        val titleTextView = view.findViewById<TextView>(R.id.profileEditTitle)
        val editUserName = view.findViewById<EditText>(R.id.editUserName)
        val editUserEmail = view.findViewById<EditText>(R.id.editUserEmail)
        val saveButton = view.findViewById<MaterialButton>(R.id.saveProfileButton)
        
        // Set title based on whether we're creating or editing
        titleTextView.text = if (isNewProfile) "Create New Profile" else "Edit Profile"

        // If editing existing profile, load the data
        if (!isNewProfile && profileId != null) {
            val profile = profileManager.getProfileById(profileId!!)
            profile?.let {
                editUserName.setText(it.name)
                editUserEmail.setText(it.email)
            }
        }

        saveButton.setOnClickListener {
            val name = editUserName.text.toString().trim()
            val email = editUserEmail.text.toString().trim()

            if (name.isEmpty() || email.isEmpty()) {
                Toast.makeText(requireContext(), "Fields cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create or update the profile
            val profile = if (isNewProfile) {
                UserProfile(name = name, email = email)
            } else {
                profileId?.let { id ->
                    profileManager.getProfileById(id)?.copy(name = name, email = email)
                        ?: UserProfile(name = name, email = email)
                } ?: UserProfile(name = name, email = email)
            }

            // Save the profile
            profileManager.saveProfile(profile)
            
            // If this is a new profile and it's the only one, set it as active
            if (isNewProfile && profileManager.getAllProfiles().size == 1) {
                profileManager.setActiveProfile(profile.id)
            }

            listener?.onProfileUpdated()
            Toast.makeText(requireContext(), "Profile saved successfully", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }

        return view
    }
}
