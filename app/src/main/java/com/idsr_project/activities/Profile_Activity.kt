package com.idsr_project.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.idsr_project.R
import com.idsr_project.databinding.ActivityProfileBinding
import com.idsr_project.utils.SessionManager

class Profile_Activity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadUserProfile()
        setupClickListeners()
        setupRoleBasedAccess()
    }

    private fun loadUserProfile() {
        val firstName = SessionManager.getUserName(this) ?: "Unknown"
        val lastName = SessionManager.getUserLastName(this) ?: "User"
        val email = SessionManager.getUserEmail(this) ?: "No Email"
        val phone = SessionManager.getUserPhone(this) ?: "No Phone"
        val userRole = SessionManager.getUserRole(this) ?: "User"


        val fullName = buildString {
            append(firstName.trim())
            if (lastName.trim().isNotEmpty()) {
                append(" ")
                append(lastName.trim())
            }
        }

        binding.tvUserName.text = fullName
        binding.tvUserEmail.text = email
        binding.tvPhone.text = phone
        binding.tvRole.text = userRole


        setProfileInitials(firstName, lastName)
    }

    private fun setProfileInitials(firstName: String, lastName: String) {
        val initials = buildString {
            if (firstName.isNotEmpty()) append(firstName.first().uppercaseChar())
            if (lastName.isNotEmpty()) append(lastName.first().uppercaseChar())
        }

        binding.tvInitials.text = if (initials.isNotEmpty()) initials else "?"
    }

    private fun setupRoleBasedAccess() {
        val userRole = SessionManager.getUserRole(this) ?: ""

        when (userRole.trim()) {
            "Admin", "Regional Officer" -> {
                binding.btnAddUser.visibility = View.VISIBLE
                binding.btnAddUser.isEnabled = true
            }
            else -> {
                binding.btnAddUser.visibility = View.GONE
                binding.btnAddUser.isEnabled = false
            }
        }
    }

    private fun setupClickListeners() {

        binding.btnBackProfile.setOnClickListener {
            finish()
        }


        binding.btnAddUser.setOnClickListener {
            navigateToAddUser()
        }


        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }

    }

    private fun navigateToAddUser() {
        val userRole = SessionManager.getUserRole(this) ?: ""
        val userRegion = SessionManager.getUserRegion(this) ?: ""
        val userDistrict = SessionManager.getUserDistrict(this) ?: ""

        val intent = Intent(this, SignUp1Activity::class.java).apply {
            putExtra("REGISTER_MODE", "PRIVILEGED")
            putExtra("CURRENT_USER_ROLE", userRole)
            putExtra("CREATOR_REGION_ID", userRegion)
            putExtra("CREATOR_DISTRICT_ID", userDistrict)
        }
        startActivity(intent)
    }

    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {

        SessionManager.clearSession(this)


        val intent = Intent(this, Login_Activity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        finish()
    }
}