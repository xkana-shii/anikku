package eu.kanade.tachiyomi.ui.download

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ReplacementSpan
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.viewholders.ExpandableViewHolder
import eu.kanade.tachiyomi.databinding.DownloadHeaderBinding

class DownloadHeaderHolder(view: View, adapter: FlexibleAdapter<*>) : ExpandableViewHolder(view, adapter) {

    private val binding = DownloadHeaderBinding.bind(view)

    @SuppressLint("SetTextI18n")
    fun bind(item: DownloadHeaderItem) {
        setDragHandleView(binding.reorder)
        val label = "${item.name} (${item.size})"
        val countStart = item.name.length + 1
        binding.title.text = SpannableString(label).apply {
            setSpan(
                RoundedBackgroundSpan(),
                countStart,
                label.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
    }

    override fun onActionStateChanged(position: Int, actionState: Int) {
        super.onActionStateChanged(position, actionState)
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            binding.container.isDragged = true
            mAdapter.collapseAll()
        }
    }

    override fun onItemReleased(position: Int) {
        super.onItemReleased(position)
        binding.container.isDragged = false
        mAdapter.expandAll()
        (mAdapter as DownloadAdapter).downloadItemListener.onItemReleased(position)
    }
}

private class RoundedBackgroundSpan : ReplacementSpan() {
    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        return paint.measureText(text, start, end).toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint,
    ) {
        val width = paint.measureText(text, start, end)
        val oldColor = paint.color
        paint.color = 0x1F6750A4
        canvas.drawRoundRect(RectF(x, top.toFloat(), x + width, bottom.toFloat()), 12f, 12f, paint)
        paint.color = oldColor
        canvas.drawText(text, start, end, x, y.toFloat(), paint)
    }
}
