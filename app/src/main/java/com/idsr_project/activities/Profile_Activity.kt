package com.idsr_project.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.idsr_project.Model.ResponseApi
import com.idsr_project.api.ApiClient
import com.idsr_project.databinding.ActivityProfileBinding
import com.idsr_project.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Profile_Activity : BaseActivity() {

    private lateinit var binding: ActivityProfileBinding

    private val editProfileLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadUserProfile()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            val versionName = packageManager.getPackageInfo(packageName, 0).versionName
            binding.tvAppVersion.text = "IDSR v$versionName"
        } catch (e: Exception) {
            binding.tvAppVersion.text = "IDSR v1.0.0"
        }

        loadUserProfile()
        setupClickListeners()
        setupRoleBasedAccess()
    }

    private fun loadUserProfile() {
        val firstName = SessionManager.getUserName(this)     ?: "Unknown"
        val lastName  = SessionManager.getUserLastName(this) ?: "User"
        val email     = SessionManager.getUserEmail(this)    ?: "No Email"
        val phone     = SessionManager.getUserPhone(this)    ?: "No Phone"
        val userRole  = SessionManager.getUserRole(this)     ?: "User"

        val fullName = buildString {
            append(firstName.trim())
            if (lastName.trim().isNotEmpty()) {
                append(" ")
                append(lastName.trim())
            }
        }

        binding.tvUserName.text  = fullName
        binding.tvUserEmail.text = email
        binding.tvPhone.text     = phone
        binding.tvRole.text      = userRole

        setProfileInitials(firstName, lastName)
    }

    private fun setProfileInitials(firstName: String, lastName: String) {
        val initials = buildString {
            if (firstName.isNotEmpty()) append(firstName.first().uppercaseChar())
            if (lastName.isNotEmpty())  append(lastName.first().uppercaseChar())
        }
        binding.tvInitials.text = if (initials.isNotEmpty()) initials else "?"
    }

    private fun setupRoleBasedAccess() {
        val userRole = SessionManager.getUserRole(this) ?: ""
        val canAddUser = userRole.trim() in listOf("Admin", "Regional Officer")

        binding.btnAddUser.visibility = if (canAddUser) View.VISIBLE else View.GONE
        binding.btnAddUser.isEnabled  = canAddUser
    }

    private fun setupClickListeners() {
        binding.btnBackProfile.setOnClickListener { finish() }

        binding.btnAddUser.setOnClickListener { navigateToAddUser() }

        binding.btnLogout.setOnClickListener { showLogoutDialog() }

        binding.btnEditProfile.setOnClickListener {
            editProfileLauncher.launch(Intent(this, EditProfile_Activity::class.java))
        }
        binding.btnThemeToggle.setOnClickListener { toggleTheme() }
    }

    private fun toggleTheme() {
        val currentMode = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
        when (currentMode) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }
    private fun navigateToAddUser() {
        startActivity(
            Intent(this, SignUp1Activity::class.java).apply {
                putExtra("REGISTER_MODE",       "PRIVILEGED")
                putExtra("CURRENT_USER_ROLE",   SessionManager.getUserRole(this@Profile_Activity)     ?: "")
                putExtra("CREATOR_REGION_ID",   SessionManager.getUserRegion(this@Profile_Activity)   ?: "")
                putExtra("CREATOR_DISTRICT_ID", SessionManager.getUserDistrict(this@Profile_Activity) ?: "")
            }
        )
    }

    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ -> performLogout() }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun performLogout() {
        val fcmToken = SessionManager.getFcmToken(this) ?: ""
        ApiClient.getClient(this)
            .logout(mapOf("fcm_token" to fcmToken))
            .enqueue(object : Callback<ResponseApi> {
                override fun onResponse(call: Call<ResponseApi>, response: Response<ResponseApi>) {
                    finishLogout()
                }
                override fun onFailure(call: Call<ResponseApi>, t: Throwable) {
                    finishLogout()
                }
            })
    }

    private fun finishLogout() {
        SessionManager.clearSession(this)
        startActivity(
            Intent(this, Login_Activity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}