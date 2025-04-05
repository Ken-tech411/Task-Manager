package com.example.taskmaster

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

abstract class SwipeToDeleteCallback(context: Context) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    private val trashIcon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_delete)
    private val iconWidth = trashIcon?.intrinsicWidth ?: 0
    private val iconHeight = trashIcon?.intrinsicHeight ?: 0
    private val redBackground = ColorDrawable()
    private val deleteBackgroundColor = Color.parseColor("#f44336")
    private val eraserPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onChildDraw(
        canvas: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val itemHeight = itemView.bottom - itemView.top
        val isCanceled = (dX == 0f && !isCurrentlyActive)

        if (isCanceled) {
            clearArea(
                canvas,
                left = itemView.right + dX,
                top = itemView.top.toFloat(),
                right = itemView.right.toFloat(),
                bottom = itemView.bottom.toFloat()
            )
            super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        redBackground.color = deleteBackgroundColor
        redBackground.setBounds(
            (itemView.right + dX).toInt(),
            itemView.top,
            itemView.right,
            itemView.bottom
        )
        redBackground.draw(canvas)

        val iconTop = itemView.top + (itemHeight - iconHeight) / 2
        val iconMargin = (itemHeight - iconHeight) / 2
        val iconLeft = itemView.right - iconMargin - iconWidth
        val iconRight = itemView.right - iconMargin
        val iconBottom = iconTop + iconHeight

        trashIcon?.setBounds(iconLeft, iconTop, iconRight, iconBottom)
        trashIcon?.draw(canvas)

        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    private fun clearArea(
        canvas: Canvas?,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ) {
        canvas?.drawRect(left, top, right, bottom, eraserPaint)
    }
}
