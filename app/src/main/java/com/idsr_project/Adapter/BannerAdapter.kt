package com.idsr_project.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.idsr_project.R

class BannerAdapter(
    private val images: List<Int>
) : RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {


    private val ITEM_COUNT = 1000

    inner class BannerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.bannerImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_banner, parent, false)
        return BannerViewHolder(view)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        val imageRes = images[position % images.size]
        holder.image.setImageResource(imageRes)
    }

    override fun onViewRecycled(holder: BannerViewHolder) {
        super.onViewRecycled(holder)
        holder.image.setImageDrawable(null)
    }

    override fun getItemCount() = ITEM_COUNT
}