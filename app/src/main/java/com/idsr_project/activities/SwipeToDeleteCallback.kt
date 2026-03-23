package com.idsr_project.activities

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.idsr_project.data.local.PendingReportEntity

class SwipeToDeleteCallback(
    private val context: Context,
    private val adapter: ReportStatusAdapter,
    private val onDelete: (PendingReportEntity, Int) -> Unit,
    private val onRetry: (PendingReportEntity, Int) -> Unit
) : ItemTouchHelper.SimpleCallback(
    0,
    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
) {
    private val deleteIcon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_delete)!!
    private val retryIcon  = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_rotate)!!
    private val deleteBackground = ColorDrawable(Color.parseColor("#F44336"))
    private val retryBackground  = ColorDrawable(Color.parseColor("#1976D2"))
    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val position = viewHolder.adapterPosition
        if (position == RecyclerView.NO_ID.toInt()) return 0
        return try {
            when (adapter.getItemAt(position).status) {
                "FAILED"  -> ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                "SYNCED"  -> ItemTouchHelper.LEFT
                else      -> 0
            }
        } catch (e: Exception) { 0 }
    }

    override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        val item     = adapter.getItemAt(position)
        when (direction) {
            ItemTouchHelper.LEFT  -> onDelete(item, position)
            ItemTouchHelper.RIGHT -> onRetry(item, position)
        }
    }

    override fun onChildDraw(
        c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
        dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
    ) {
        val itemView   = viewHolder.itemView
        val itemHeight = itemView.bottom - itemView.top
        val isCancelled = dX == 0f && !isCurrentlyActive

        if (isCancelled) {
            c.drawRect(itemView.left.toFloat(), itemView.top.toFloat(),
                itemView.right.toFloat(), itemView.bottom.toFloat(), clearPaint)
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        if (dX < 0) {
            deleteBackground.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
            deleteBackground.draw(c)
            val iconMargin = (itemHeight - deleteIcon.intrinsicHeight) / 2
            deleteIcon.setBounds(
                itemView.right - iconMargin - deleteIcon.intrinsicWidth,
                itemView.top + iconMargin,
                itemView.right - iconMargin,
                itemView.top + iconMargin + deleteIcon.intrinsicHeight
            )
            deleteIcon.setTint(Color.WHITE)
            deleteIcon.draw(c)
        } else if (dX > 0) {
            retryBackground.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
            retryBackground.draw(c)
            val iconMargin = (itemHeight - retryIcon.intrinsicHeight) / 2
            retryIcon.setBounds(
                itemView.left + iconMargin,
                itemView.top + iconMargin,
                itemView.left + iconMargin + retryIcon.intrinsicWidth,
                itemView.top + iconMargin + retryIcon.intrinsicHeight
            )
            retryIcon.setTint(Color.WHITE)
            retryIcon.draw(c)
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}