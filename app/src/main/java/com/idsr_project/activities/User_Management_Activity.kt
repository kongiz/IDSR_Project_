package com.idsr_project.activities


import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.idsr_project.Adapter.UserAdapter
import com.idsr_project.Model.AdminUser
import com.idsr_project.Model.UpdateRoleRequest
import com.idsr_project.Model.UpdateStatusRequest
import com.idsr_project.Model.UpdateUserResponse
import com.idsr_project.api.ApiClient
import com.idsr_project.databinding.ActivityUserManagementBinding
import com.idsr_project.utils.applyWindowInsets
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserManagementActivity : BaseActivity() {

    private lateinit var binding: ActivityUserManagementBinding
    private lateinit var adapter: UserAdapter
    private val hintHandler = Handler(Looper.getMainLooper())
    private var hintRunnable: Runnable? = null
    private var currentHintIndex = 0
    private var allUsers: MutableList<AdminUser> = mutableListOf()
    private var searchJob: Job? = null


    private val searchHints = listOf(
        "Search by first name...",
        "Search by last name...",
        "Search by email...",
        "Search by role...",
        "Search by region...",
        "Search by district..."
    )

    private val roles = listOf(
        "Admin",
        "Regional Officer",
        "District Officer",
        "Health Officer",
        "Clinician",
        "Lab Technician",
        "Community Health Worker"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets(
            topView = binding.appBarLayout
        )

        setupRecyclerView()
        setupChipFilters()
        setupSearch()
        loadUsers()

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = UserAdapter(
            users          = mutableListOf(),
            onToggleStatus = { user, position -> confirmToggleStatus(user, position) },
            onChangeRole   = { user, position -> showRoleDialog(user, position) }
        )
        binding.rvUsers.layoutManager = LinearLayoutManager(this)
        binding.rvUsers.adapter = adapter
    }

    private fun setupChipFilters() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, _ ->
            applyFilters(binding.etSearch.text.toString().trim())
        }
    }

    private fun setupSearch() {
        binding.etSearch.doAfterTextChanged { editable ->
            val query = editable?.toString()?.trim() ?: ""

            if (query.isNotEmpty()) stopHintAnimation()
            else if (!binding.etSearch.hasFocus()) startHintAnimation()

            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                delay(400)
                applyFilters(query)
            }
        }

        binding.etSearch.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && binding.etSearch.text.isNullOrEmpty()) {
                startHintAnimation()
            } else {
                stopHintAnimation()
            }
        }

        startHintAnimation()
    }

    private fun loadUsers(isActive: Boolean? = null, role: String? = null) {
        showLoading(true)

        ApiClient.getClient(this).getAllUsers(
            role     = role,
            isActive = isActive
        ).enqueue(object : Callback<com.idsr_project.Model.AdminUsersResponse> {
            override fun onResponse(
                call: Call<com.idsr_project.Model.AdminUsersResponse>,
                response: Response<com.idsr_project.Model.AdminUsersResponse>
            ) {
                showLoading(false)
                if (response.isSuccessful && response.body()?.success == true) {
                    allUsers = response.body()!!.data!!.toMutableList()
                    applyFilters(binding.etSearch.text.toString().trim())
                } else {
                    showEmpty(true)
                    Toast.makeText(this@UserManagementActivity, "Failed to load users", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(
                call: Call<com.idsr_project.Model.AdminUsersResponse>,
                t: Throwable
            ) {
                showLoading(false)
                showEmpty(true)
                Toast.makeText(this@UserManagementActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun applyFilters(searchText: String) {
        val query = searchText.lowercase()
        val checkedId = binding.chipGroupFilter.checkedChipId

        android.util.Log.d("CHIP_DEBUG", "checkedId=$checkedId " +
                "chipAll=${binding.chipAll.id} " +
                "chipAdmin=${binding.chipAdmin.id} " +
                "chipActive=${binding.chipActive.id}")

        val roleFilter: String? = when (checkedId) {
            binding.chipAdmin.id         -> "Admin"
            binding.chipRegional.id      -> "Regional Officer"
            binding.chipDistrict.id      -> "District Officer"
            binding.chipHealthOfficer.id -> "Health Officer"
            binding.chipClinician.id     -> "Clinician"
            binding.chipLabTech.id       -> "Lab Technician"
            binding.chipCHW.id           -> "Community Health Worker"
            else                         -> null
        }

        val activeFilter: Boolean? = when (checkedId) {
            binding.chipActive.id   -> true
            binding.chipInactive.id -> false
            else                    -> null
        }

        var filtered = allUsers.toList()
        if (roleFilter != null)  filtered = filtered.filter { it.user_role == roleFilter }
        if (activeFilter != null) filtered = filtered.filter { it.is_active == activeFilter }
        if (query.isNotEmpty()) {
            filtered = filtered.filter {
                it.firstname.lowercase().contains(query) ||
                        it.lastname.lowercase().contains(query)  ||
                        it.email.lowercase().contains(query)
            }
        }

        adapter.updateList(filtered)
        binding.tvUserCount.text = "${filtered.size} users"
        showEmpty(filtered.isEmpty())
    }

    private fun confirmToggleStatus(user: AdminUser, position: Int) {
        val action = if (user.is_active) "deactivate" else "activate"
        val name   = "${user.firstname} ${user.lastname}"

        AlertDialog.Builder(this)
            .setTitle("Confirm")
            .setMessage("Are you sure you want to $action $name?")
            .setPositiveButton("Yes") { _, _ -> toggleStatus(user, position) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleStatus(user: AdminUser, position: Int) {
        val newStatus = !user.is_active

        android.util.Log.d("TOGGLE", "Toggling user ${user.id} to $newStatus")

        ApiClient.getClient(this).updateUserStatus(
            userId  = user.id,
            request = UpdateStatusRequest(is_active = newStatus)
        ).enqueue(object : Callback<UpdateUserResponse> {
            override fun onResponse(
                call: Call<UpdateUserResponse>,
                response: Response<UpdateUserResponse>
            ) {
                android.util.Log.d("TOGGLE", "Response code: ${response.code()}")
                android.util.Log.d("TOGGLE", "Body: ${response.body()}")
                android.util.Log.d("TOGGLE", "Error: ${response.errorBody()?.string()}")

                if (response.isSuccessful && response.body()?.success == true) {
                    val updated = user.copy(is_active = newStatus)
                    val idx = allUsers.indexOfFirst { it.id == user.id }
                    if (idx >= 0) allUsers[idx] = updated
                    applyFilters(binding.etSearch.text.toString().trim())
                    Toast.makeText(
                        this@UserManagementActivity,
                        "${user.firstname} ${if (newStatus) "activated" else "deactivated"}",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@UserManagementActivity,
                        "Failed: ${response.errorBody()?.string()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<UpdateUserResponse>, t: Throwable) {
                android.util.Log.e("TOGGLE", "Failure: ${t.message}", t)
                Toast.makeText(
                    this@UserManagementActivity,
                    "Error: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun showRoleDialog(user: AdminUser, position: Int) {
        val roleArray = roles.toTypedArray()
        val currentIndex = roles.indexOf(user.user_role).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Change Role — ${user.firstname} ${user.lastname}")
            .setSingleChoiceItems(roleArray, currentIndex) { dialog, which ->
                val selectedRole = roles[which]
                if (selectedRole == user.user_role) {
                    dialog.dismiss()
                    return@setSingleChoiceItems
                }
                dialog.dismiss()
                confirmRoleChange(user, position, selectedRole)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmRoleChange(user: AdminUser, position: Int, newRole: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Role Change")
            .setMessage("Change ${user.firstname}'s role to \"$newRole\"?")
            .setPositiveButton("Yes") { _, _ -> changeRole(user, position, newRole) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun changeRole(user: AdminUser, position: Int, newRole: String) {
        ApiClient.getClient(this).updateUserRole(
            userId  = user.id,
            request = UpdateRoleRequest(role = newRole)
        ).enqueue(object : Callback<UpdateUserResponse> {
            override fun onResponse(
                call: Call<UpdateUserResponse>,
                response: Response<UpdateUserResponse>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val updated = user.copy(user_role = newRole)
                    val idx = allUsers.indexOfFirst { it.id == user.id }
                    if (idx >= 0) allUsers[idx] = updated
                    applyFilters(binding.etSearch.text.toString().trim())
                    Toast.makeText(
                        this@UserManagementActivity,
                        "${user.firstname}'s role updated to $newRole",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(this@UserManagementActivity, "Failed to update role", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UpdateUserResponse>, t: Throwable) {
                Toast.makeText(this@UserManagementActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvUsers.visibility     = if (show) View.GONE else View.VISIBLE
        binding.layoutEmpty.visibility = View.GONE
    }

    private fun showEmpty(show: Boolean) {
        binding.layoutEmpty.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvUsers.visibility     = if (show) View.GONE else View.VISIBLE
    }


    private fun startHintAnimation() {
        hintRunnable = object : Runnable {
            override fun run() {
                if (binding.etSearch.text.isNullOrEmpty() && !binding.etSearch.hasFocus()) {
                    val nextHint = searchHints[currentHintIndex % searchHints.size]

                    binding.etSearch.animate().alpha(0.3f)
                        .setDuration(300)
                        .withEndAction {
                            binding.etSearch.hint = nextHint
                            binding.etSearch.animate()
                                .alpha(1f)
                                .setDuration(300)
                                .start()
                        }
                        .start()

                    currentHintIndex++
                }
                hintHandler.postDelayed(this, 2500)
            }
        }
        hintHandler.postDelayed(hintRunnable!!, 2500)
    }

    private fun stopHintAnimation() {
        hintRunnable?.let { hintHandler.removeCallbacks(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopHintAnimation()
        searchJob?.cancel()
    }
}