package io.beldex.bchat.textformatter


import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.LeadingMarginSpan

class CustomQuoteSpan(
    private val stripeColor: Int = 0xFFCCCCCC.toInt(), // light gray
    private val stripeWidth: Int = 6,                 // thin vertical stripe
    private val gapWidth: Int = 16,                   // space between stripe and text
    private val backgroundColor: Int = 0xFFECECEC.toInt()
) : LeadingMarginSpan {

    override fun getLeadingMargin(first: Boolean) = stripeWidth + gapWidth

    override fun drawLeadingMargin(
        c: Canvas,
        p: Paint,
        x: Int,
        dir: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        first: Boolean,
        layout: android.text.Layout
    ) {
        val style = p.style
        val oldColor = p.color

        // Draw the stripe
        p.style = Paint.Style.FILL
        p.color = stripeColor
        val left = x.toFloat()
        val right = (x + dir * stripeWidth).toFloat()
        c.drawRect(left, top.toFloat(), right, bottom.toFloat(), p)

        p.style = style
        p.color = oldColor
    }
}