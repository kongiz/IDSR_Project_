//package com.idsr_project.activities
//
//import android.content.Intent
//import android.os.Bundle
//import android.util.Log
//import android.widget.Toast
//import androidx.activity.enableEdgeToEdge
//import androidx.appcompat.app.AppCompatActivity
//import com.idsr_project.Model.loginResponse
//import com.idsr_project.Model.LoginRequest
//import com.idsr_project.api.ApiClient
//import com.idsr_project.databinding.ActivityLoginPassBinding
//import com.idsr_project.utils.SessionManager
//import retrofit2.Call
//import retrofit2.Response
//
//class LoginPassActivity : AppCompatActivity() {
//    private lateinit var binding: ActivityLoginPassBinding
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        binding = ActivityLoginPassBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        binding.btnBackPass.setOnClickListener {
//            finish()
//        }
//
//        val email = intent.getStringExtra("USER_EMAIL") ?: ""
//
//        binding.btnLogin2.setOnClickListener {
//            val password = binding.etPassword.text.toString().trim()
//
//            if (password.isEmpty()) {
//                binding.etPassword.error = "Password is required"
//                binding.etPassword.requestFocus()
//                return@setOnClickListener
//            }
//
//            if (password.length < 6) {
//                binding.etPassword.error = "Password must be at least 6 characters"
//                binding.etPassword.requestFocus()
//                return@setOnClickListener
//            }
//
//            if (!password.any { it.isDigit() }) {
//                binding.etPassword.error = "Password must contain at least one digit"
//                binding.etPassword.requestFocus()
//                return@setOnClickListener
//            }
//            binding.progressBar.visibility = android.view.View.VISIBLE
//
//            val request = LoginRequest(email = email, password = password)
//            ApiClient.getClient(context = this).loginUser(request.email, request.password).enqueue(object : retrofit2.Callback<loginResponse> {
//                override fun onResponse(
//                    call: Call<loginResponse?>,
//                    response: Response<loginResponse?>
//                ) {
//                    binding.progressBar.visibility = android.view.View.GONE
//                    if (response.isSuccessful && response.body() != null) {
//                        val res = response.body()!!
//                        Log.d("LOGIN", "Response: $res")
//
//                        if (res.success) {
//                            res.user?.let { user ->
//                                SessionManager.saveUserSession(
//                                    context = this@LoginPassActivity,
//                                    userId = user.id,
//                                    firstname = user.firstname,
//                                    lastname = user.lastname,
//                                    email = user.email,
//                                    phone = user.phone ?: "",
//                                    role = user.role ?: "Health Officer",
//                                    region = user.regionId?.toString(),
//                                    district = user.districtId?.toString(),
//                                    accessToken = res.access_token,
//                                    refreshToken = res.refresh_token
//                                )
//                            }
//                            Toast.makeText(
//                                this@LoginPassActivity,
//                                "Welcome ${res.user?.firstname}",
//                                Toast.LENGTH_SHORT
//                            ).show()
//
//                            val intent = Intent(this@LoginPassActivity, MainActivity::class.java)
//                            startActivity(intent)
//                            finish()
//                        } else {
//                            Toast.makeText(this@LoginPassActivity, res.msg, Toast.LENGTH_SHORT).show()
//                        }
//                    } else {
//                        val errorMsg = response.errorBody()?.string()
//                        Log.e("LOGIN", "Error: $errorMsg")
//                        Toast.makeText(this@LoginPassActivity, "Invalid email or password", Toast.LENGTH_SHORT).show()
//                    }
//
//                }
//
//                override fun onFailure(call: Call<loginResponse?>, t: Throwable) {
//                    binding.progressBar.visibility = android.view.View.GONE
//                    Log.e("LOGIN", "Error: ${t.message}")
//                    Toast.makeText(this@LoginPassActivity, t.message, Toast.LENGTH_SHORT).show()
//                }
//            })
//        }
//    }
//}
