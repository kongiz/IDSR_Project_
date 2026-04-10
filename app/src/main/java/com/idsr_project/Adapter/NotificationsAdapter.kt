package com.idsr_project.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.idsr_project.Model.NotificationData
import com.idsr_project.databinding.ItemNotificationBinding

class NotificationsAdapter(
    private val items: MutableList<NotificationData>,
    private val onItemClick: (NotificationData) -> Unit
) : RecyclerView.Adapter<NotificationsAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.apply {
            txtNotificationTitle.text = item.title
            txtNotificationBody.text  = item.body
            txtNotificationTime.text  = com.idsr_project.utils.DateUtils.formatIsoDateTime(item.created_at)


            root.alpha = if (item.is_read) 0.6f else 1.0f


            viewUnreadDot.visibility = if (item.is_read)
                android.view.View.INVISIBLE else android.view.View.VISIBLE

            root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun getItemCount() = items.size
}