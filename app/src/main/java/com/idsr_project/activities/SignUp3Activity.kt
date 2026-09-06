package com.idsr_project.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.idsr_project.R
import com.idsr_project.databinding.ActivitySignUp3Binding
import com.idsr_project.utils.ThemeManager
import com.idsr_project.utils.applyWindowInsets

class SignUp3Activity : BaseActivity() {
    override val excludeFromTimeout: Boolean = true

    private lateinit var binding: ActivitySignUp3Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySignUp3Binding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets(topView = binding.appBarLayout)

        val firstname = intent.getStringExtra("FIRSTNAME")
        val lastname = intent.getStringExtra("LASTNAME")
        val phone = intent.getStringExtra("PHONE")
        val email = intent.getStringExtra("EMAIL")
        val registerMode = intent.getStringExtra("REGISTER_MODE") ?: "SELF"
        val currentUserRole = intent.getStringExtra("CURRENT_USER_ROLE") ?: ""

        binding.btnBackSignup3.setOnClickListener { finish() }

        setupGenderDropdown()
        setupRoleDropdown(registerMode, currentUserRole)

        binding.btnNext3.setOnClickListener {
            validateAndProceed(
                firstname,
                lastname,
                phone,
                email,
                registerMode,
                currentUserRole
            )
        }
    }

    private fun setupGenderDropdown() {
        val genders = listOf("Male", "Female", "Other")

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            genders
        )

        binding.actGender.setAdapter(adapter)


        binding.actGender.setOnItemClickListener { _, _, _, _ ->
            binding.tilGender.error = null
        }
    }

    private fun setupRoleDropdown(registerMode: String, currentUserRole: String) {
        val allRoles = resources.getStringArray(R.array.user_roles).toMutableList()

        val filteredRoles: List<String> = when (registerMode) {
            "SELF" -> {
                allRoles.filterNot {
                    it == "Admin" || it == "Regional Officer" || it == "District Officer"
                }
            }

            "PRIVILEGED" -> {
                when (currentUserRole) {
                    "Admin" -> listOf("Admin", "Regional Officer", "District Officer")
                    "Regional Officer" -> listOf("District Officer")
                    else -> emptyList()
                }
            }

            else -> allRoles
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            filteredRoles
        )

        binding.actRole.setAdapter(adapter)

        // Clear error when user selects
        binding.actRole.setOnItemClickListener { _, _, _, _ ->
            binding.tilRole.error = null
        }
    }

    private fun validateAndProceed(
        firstname: String?,
        lastname: String?,
        phone: String?,
        email: String?,
        registerMode: String,
        currentUserRole: String
    ) {
        val selectedGender = binding.actGender.text.toString().trim()
        val selectedRole = binding.actRole.text.toString().trim()

        // Clear previous errors
        binding.tilGender.error = null
        binding.tilRole.error = null

        when {
            selectedGender.isEmpty() -> {
                binding.tilGender.error = "Please select gender"
                binding.actGender.requestFocus()
                return
            }
            selectedRole.isEmpty() -> {
                binding.tilRole.error = "Please select designation"
                binding.actRole.requestFocus()
                return
            }
        }


        val intent = Intent(this, Signup3_1Activity::class.java)
        intent.putExtra("FIRSTNAME", firstname)
        intent.putExtra("LASTNAME", lastname)
        intent.putExtra("PHONE", phone)
        intent.putExtra("EMAIL", email)
        intent.putExtra("GENDER", selectedGender)
        intent.putExtra("ROLE", selectedRole)
        intent.putExtra("REGISTER_MODE", registerMode)
        intent.putExtra("CURRENT_USER_ROLE", currentUserRole)
        startActivity(intent)
    }
}
