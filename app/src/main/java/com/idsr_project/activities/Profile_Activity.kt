package com.idsr_project.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

        val username = SessionManager.getUserName(this) ?: "Unknown User"
        val lastname = SessionManager.getUserLastName(this) ?: "Unknown User"
        val email = SessionManager.getUserEmail(this) ?: "No Email"
        val phone = SessionManager.getUserPhone(this) ?: "No Phone"
        val userRole = SessionManager.getUserRole(this) ?: ""
        val userRegion = SessionManager.getUserRegion(this) ?: ""
        val userDistrict = SessionManager.getUserDistrict(this) ?: ""

        binding.tvUserName.text = "$username ${lastname.trim()}"
        binding.tvUserEmail.text = email
        binding.tvPhone.text = "Phone: $phone"
        binding.tvRole.text = "Role: $userRole"

        when (userRole) {
            "Admin", "Regional Officer" -> {
                binding.btnAddUser.visibility = View.VISIBLE
                binding.btnAddUser.isEnabled = true
            }
            else -> {
                binding.btnAddUser.visibility = View.GONE
                binding.btnAddUser.isEnabled = false
            }
        }

        binding.btnBackProfile.setOnClickListener {
            finish()
        }
        binding.btnAddUser.setOnClickListener {
            val intent = Intent(this, SignUp1Activity::class.java)
            intent.putExtra("REGISTER_MODE", "PRIVILEGED")
            intent.putExtra("CURRENT_USER_ROLE", userRole)
            intent.putExtra("CREATOR_REGION_ID", userRegion)
            intent.putExtra("CREATOR_DISTRICT_ID", userDistrict)
            startActivity(intent)
        }


        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes") { _, _ ->
                    SessionManager.clearSession(this)
                    val intent = Intent(this, Login_Activity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("No", null)
                .show()

        }

    }
}