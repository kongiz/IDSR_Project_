package com.idsr_project.Model

data class userSignup(
    val firstname: String,
    val lastname: String,
    val phone: String,
    val email: String,
    val gender: String,
    val role: String,
    val region_id: Int,
    val district_id: Int,
    val password: String
)
