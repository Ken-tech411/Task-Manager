package com.example.taskmanager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ProfilesFragment : Fragment() {

    private lateinit var profileManager: ProfileManager
    private lateinit var adapter: ProfilesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profiles, container, false)
        profileManager = ProfileManager(requireContext())

        val recyclerView = view.findViewById<RecyclerView>(R.id.profilesRecyclerView)
        adapter = ProfilesAdapter(
            onProfileSelected = { profile ->
                // Update the active profile
                profileManager.setActiveProfile(profile.id)
                // Refresh the UI in MainActivity
                (activity as? MainActivity)?.refreshProfileUI()
                // Update TaskViewModel so that tasks refresh with the new profile
                (activity as? MainActivity)?.taskViewModel?.refreshActiveProfile()
                Toast.makeText(context, "Switched to ${profile.name}", Toast.LENGTH_SHORT).show()
                // Immediately pop this fragment off the back stack to show main activity UI
                parentFragmentManager.popBackStack()
            },
            onProfileEdit = { profile ->
                // Navigate to EditProfileFragment for editing
                val bundle = Bundle().apply { putString("profileId", profile.id) }
                val editFragment = EditProfileFragment().apply { arguments = bundle }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, editFragment)
                    .addToBackStack(null)
                    .commit()
            },
            onProfileDelete = { profile ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Profile")
                    .setMessage("Are you sure you want to delete ${profile.name}?")
                    .setPositiveButton("Delete") { _, _ ->
                        profileManager.deleteProfile(profile.id)
                        loadProfiles()
                        (activity as? MainActivity)?.refreshProfileUI()
                        (activity as? MainActivity)?.taskViewModel?.refreshActiveProfile()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        view.findViewById<FloatingActionButton>(R.id.addProfileFab).setOnClickListener {
            // Navigate to EditProfileFragment for creating a new profile
            val bundle = Bundle().apply { putBoolean("isNewProfile", true) }
            val editFragment = EditProfileFragment().apply { arguments = bundle }
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, editFragment)
                .addToBackStack(null)
                .commit()
        }

        loadProfiles()

        return view
    }

    private fun loadProfiles() {
        val profiles = profileManager.getAllProfiles()
        val activeProfile = profileManager.getActiveProfile()
        adapter.submitList(profiles, activeProfile?.id)
    }

    override fun onResume() {
        super.onResume()
        loadProfiles()
    }

    // Adapter for displaying profiles
    private class ProfilesAdapter(
        private val onProfileSelected: (UserProfile) -> Unit,
        private val onProfileEdit: (UserProfile) -> Unit,
        private val onProfileDelete: (UserProfile) -> Unit
    ) : RecyclerView.Adapter<ProfilesAdapter.ViewHolder>() {

        private var profiles: List<UserProfile> = emptyList()
        private var activeProfileId: String? = null

        fun submitList(newProfiles: List<UserProfile>, activeId: String?) {
            profiles = newProfiles
            activeProfileId = activeId
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_profile, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val profile = profiles[position]
            holder.bind(profile, profile.id == activeProfileId)
        }

        override fun getItemCount() = profiles.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val nameTextView: TextView = itemView.findViewById(R.id.profileNameTextView)
            private val emailTextView: TextView = itemView.findViewById(R.id.profileEmailTextView)
            private val activeIndicator: View = itemView.findViewById(R.id.activeProfileIndicator)
            private val editButton: ImageView = itemView.findViewById(R.id.editProfileButton)
            private val deleteButton: ImageView = itemView.findViewById(R.id.deleteProfileButton)

            fun bind(profile: UserProfile, isActive: Boolean) {
                nameTextView.text = profile.name
                emailTextView.text = profile.email
                activeIndicator.visibility = if (isActive) View.VISIBLE else View.INVISIBLE

                itemView.setOnClickListener {
                    if (!isActive) {
                        onProfileSelected(profile)
                    }
                }
                editButton.setOnClickListener { onProfileEdit(profile) }
                deleteButton.setOnClickListener { onProfileDelete(profile) }
            }
        }
    }
}
