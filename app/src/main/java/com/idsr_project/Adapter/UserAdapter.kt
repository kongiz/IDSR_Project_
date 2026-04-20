package com.idsr_project.Adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.idsr_project.Model.AdminUser
import com.idsr_project.databinding.ItemUserBinding
import androidx.core.graphics.toColorInt

class UserAdapter(
    private var users: MutableList<AdminUser>,
    private val onToggleStatus: (AdminUser, Int) -> Unit,
    private val onChangeRole: (AdminUser, Int) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(val binding: ItemUserBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return UserViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        val b = holder.binding

        val initials = buildString {
            if (user.firstname.isNotEmpty()) append(user.firstname[0].uppercaseChar())
            if (user.lastname.isNotEmpty()) append(user.lastname[0].uppercaseChar())
        }
        b.tvAvatar.text = initials

        b.tvFullName.text = "${user.firstname} ${user.lastname}"
        b.tvEmail.text = user.email


        b.tvRole.text = user.user_role


        val location = when {
            user.district_name != null -> " ${user.district_name}"
            user.region_name != null   -> " ${user.region_name}"
            else                       -> " National"
        }
        b.tvLocation.text = location


        if (user.is_active) {
            b.tvStatusBadge.text = "Active"
            b.tvStatusBadge.setBackgroundResource(com.idsr_project.R.drawable.bg_role_badge)
        } else {
            b.tvStatusBadge.text = "Inactive"
            b.tvStatusBadge.setBackgroundResource(com.idsr_project.R.drawable.bg_badge_inactive)
        }


        b.btnToggleStatus.text = if (user.is_active) "Deactivate" else "Activate"
        b.btnToggleStatus.setStrokeColorResource(
            if (user.is_active) com.idsr_project.R.color.idsr_error
            else com.idsr_project.R.color.idsr_primary
        )
        b.btnToggleStatus.setTextColor(
            if (user.is_active) "#C62828".toColorInt()
            else "#005BAC".toColorInt()
        )


        b.btnToggleStatus.setOnClickListener { onToggleStatus(user, position) }
        b.btnChangeRole.setOnClickListener   { onChangeRole(user, position) }
    }

    override fun getItemCount() = users.size

    fun updateList(newUsers: List<AdminUser>) {
        users.clear()
        users.addAll(newUsers)
        notifyDataSetChanged()
    }

    fun updateUserAt(position: Int, updated: AdminUser) {
        users[position] = updated
        notifyItemChanged(position)
    }
}