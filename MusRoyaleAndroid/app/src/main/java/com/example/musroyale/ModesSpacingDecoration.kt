package com.example.musroyale

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class ModesSpacingDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return

        val halfSpacing = spacing / 2
        outRect.left = if (position == 0) spacing else halfSpacing
        outRect.right = if (position == state.itemCount - 1) spacing else halfSpacing
        outRect.top = spacing
        outRect.bottom = spacing
    }
}

