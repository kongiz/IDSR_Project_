package com.idsr_project.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import com.idsr_project.Model.ChangePasswordRequest
import com.idsr_project.Model.OtpResponse
import com.idsr_project.Model.ResponseApi
import com.idsr_project.Model.UpdateProfileRequest
import com.idsr_project.api.ApiClient
import com.idsr_project.databinding.ActivityEditProfileBinding
import com.idsr_project.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EditProfile_Activity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefillProfileFields()
        setupTabs()
        setupClickListeners()
    }

    private fun prefillProfileFields() {
        binding.etFirstName.setText(SessionManager.getUserName(this))
        binding.etLastName.setText(SessionManager.getUserLastName(this))
        binding.etPhone.setText(SessionManager.getUserPhone(this))
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Profile"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Password"))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        binding.layoutProfile.visibility  = View.VISIBLE
                        binding.layoutPassword.visibility = View.GONE
                    }
                    1 -> {
                        binding.layoutProfile.visibility  = View.GONE
                        binding.layoutPassword.visibility = View.VISIBLE
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupClickListeners() {
        binding.toolbar.setOnClickListener { finish() }

        binding.btnSaveProfile.setOnClickListener {
            if (validateProfileFields()) saveProfile()
        }

        binding.btnChangePassword.setOnClickListener {
            if (validatePasswordFields()) changePassword()
        }
    }

    private fun validateProfileFields(): Boolean {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName  = binding.etLastName.text.toString().trim()

        binding.tilFirstName.error = null
        binding.tilLastName.error  = null

        if (firstName.isEmpty()) {
            binding.tilFirstName.error = "First name is required"
            return false
        }
        if (lastName.isEmpty()) {
            binding.tilLastName.error = "Last name is required"
            return false
        }
        return true
    }

    private fun validatePasswordFields(): Boolean {
        val current = binding.etCurrentPassword.text.toString().trim()
        val newPass  = binding.etNewPassword.text.toString().trim()
        val confirm  = binding.etConfirmNewPassword.text.toString().trim()

        binding.tilCurrentPassword.error    = null
        binding.tilNewPassword.error        = null
        binding.tilConfirmNewPassword.error = null

        if (current.isEmpty()) {
            binding.tilCurrentPassword.error = "Current password is required"
            return false
        }
        if (newPass.isEmpty()) {
            binding.tilNewPassword.error = "New password is required"
            return false
        }
        if (newPass.length < 8) {
            binding.tilNewPassword.error = "Password must be at least 8 characters"
            return false
        }
        if (!newPass.any { it.isDigit() }) {
            binding.tilNewPassword.error = "Password must contain at least one number"
            return false
        }
        if (newPass != confirm) {
            binding.tilConfirmNewPassword.error = "Passwords do not match"
            return false
        }
        return true
    }

    private fun saveProfile() {
        showProfileLoading(true)

        val request = UpdateProfileRequest(
            firstname = binding.etFirstName.text.toString().trim(),
            lastname  = binding.etLastName.text.toString().trim(),
            phone     = binding.etPhone.text.toString().trim().ifEmpty { null }
        )

        ApiClient.getClient(this)
            .updateProfile(request).enqueue(object : Callback<ResponseApi> {
                override fun onResponse(
                    call: Call<ResponseApi>,
                    response: Response<ResponseApi>
                ) {
                    showProfileLoading(false)
                    if (response.isSuccessful && response.body()?.success == true) {
                        // update session with new name
                        SessionManager.updateNameAndPhone(
                            this@EditProfile_Activity,
                            request.firstname,
                            request.lastname,
                            request.phone
                        )
                        Toast.makeText(
                            this@EditProfile_Activity,
                            "Profile updated successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(
                            this@EditProfile_Activity,
                            response.body()?.msg ?: "Update failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                override fun onFailure(call: Call<ResponseApi>, t: Throwable) {
                    showProfileLoading(false)
                    Toast.makeText(this@EditProfile_Activity, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun changePassword() {
        showPasswordLoading(true)

        val request = ChangePasswordRequest(
            currentPassword = binding.etCurrentPassword.text.toString().trim(),
            newPassword     = binding.etNewPassword.text.toString().trim()
        )

        ApiClient.getClient(this)
            .changePassword(request).enqueue(object : Callback<ResponseApi> {
                override fun onResponse(
                    call: Call<ResponseApi>,
                    response: Response<ResponseApi>
                ) {
                    showPasswordLoading(false)
                    val body = response.body()
                    if (response.isSuccessful && body?.success == true) {
                        Toast.makeText(
                            this@EditProfile_Activity,
                            "Password changed successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.etCurrentPassword.text?.clear()
                        binding.etNewPassword.text?.clear()
                        binding.etConfirmNewPassword.text?.clear()
                    } else {
                        binding.tilCurrentPassword.error = body?.msg ?: "Password change failed"
                    }
                }
                override fun onFailure(call: Call<ResponseApi>, t: Throwable) {
                    showPasswordLoading(false)
                    Toast.makeText(this@EditProfile_Activity, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showProfileLoading(isLoading: Boolean) {
        binding.btnSaveProfile.isEnabled = !isLoading
        binding.btnSaveProfile.text = if (isLoading) "Saving..." else "Save Changes"
        binding.progressProfile.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showPasswordLoading(isLoading: Boolean) {
        binding.btnChangePassword.isEnabled = !isLoading
        binding.btnChangePassword.text = if (isLoading) "Changing..." else "Change Password"
        binding.progressPassword.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}