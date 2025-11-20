package io.beldex.bchat.textformatter
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import androidx.core.graphics.toColorInt
import android.text.style.LeadingMarginSpan

class AppTextFormatter(private val text: String) {
    private val pattern = Regex(
        "(?s)" +
                "(```.+?```)|" +             // 1: code block
                "\\*([^*]+)\\*|" +           // 2: bold
                "_([^_]+)_|" +               // 3: italic
                "~([^~]+)~|" +               // 4: strike
                "(`[^`]+`)|" +               // 5: inline code
                "(?:^|\\n)(> .+?)|" +         // 6: Block quote
                "(?:^|\\n)\\s*(\\d+)\\.\\s+(.*)$"   // 7: Numbered list (1. text)

    )

    fun appendFormatted(out: SpannableStringBuilder, showMarkup: Boolean) {
        var last = 0
        pattern.findAll(text).forEach { match ->
            if (match.range.first > last) {
                out.append(text.substring(last, match.range.first))
            }

            val content = match.value

            val start = out.length
            when {
                // 1: code block
                content.startsWith("```") -> {
                    // Triple backtick code block
                    val innerText = content.substring(3, content.length - 3)
                    val mono = TextFormatter.toUnicodeMonospace(innerText)
                    if (showMarkup) out.append("```")
                    out.append(mono)
                    if (showMarkup) out.append("```")
                    out.setSpan(
                        TypefaceSpan("monospace"),
                        start + if (showMarkup) 3 else 0,
                        out.length - if (showMarkup) 3 else 0,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                // 2: bold
                content.startsWith('*') -> {
                    val innerText = content.substring(1, content.length - 1)
                    val boldText = TextFormatter.toUnicodeBold(innerText)
                    if (showMarkup) out.append("*")
                    out.append(boldText)
                    if (showMarkup) out.append("*")
                    // Optional: span for live preview
                    out.setSpan(
                        StyleSpan(Typeface.BOLD),
                        start + if (showMarkup) 1 else 0,
                        out.length - if (showMarkup) 1 else 0,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                // 3: italic
                content.startsWith('_') -> {
                    val innerText = content.substring(1, content.length - 1)
                    val italicText = TextFormatter.toUnicodeItalic(innerText)
                    if (showMarkup) out.append("_")
                    out.append(italicText)
                    if (showMarkup) out.append("_")
                    out.setSpan(
                        StyleSpan(Typeface.ITALIC),
                        start + if (showMarkup) 1 else 0,
                        out.length - if (showMarkup) 1 else 0,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                // 4: strike
                content.startsWith('~') -> {
                    val innerText = content.substring(1, content.length - 1)
                    val strikeText = TextFormatter.toUnicodeStrikethrough(innerText)
                    if (showMarkup) out.append("~")
                    out.append(strikeText)
                    if (showMarkup) out.append("~")
                    out.setSpan(
                        StrikethroughSpan(),
                        start + if (showMarkup) 1 else 0,
                        out.length - if (showMarkup) 1 else 0,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                // 5: inline code
                content.startsWith('`') -> {

                    val innerText = content.substring(1, content.length - 1)
                    val mono = TextFormatter.toUnicodeInlineCode(innerText)

                    val startingLength = out.length   // <--- REAL start before adding anything

                    if (showMarkup) out.append("`")
                    out.append(mono)
                    if (showMarkup) out.append("`")

                    val endingLength = out.length     // <--- REAL end after adding everything

                    // Correct monospace span
                    val spanStart = startingLength + if (showMarkup) 1 else 0
                    val spanEnd = endingLength - if (showMarkup) 1 else 0

                    out.setSpan(
                        TypefaceSpan("monospace"),
                        spanStart,
                        spanEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    out.setSpan(
                        BackgroundColorSpan("#797984".toColorInt()),
                        spanStart,
                        spanEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                // 6: quote
                match.groups[6] != null -> {
                    // WhatsApp-style block quote (> text)
                    val lines = content.split("\n")
                    for (line in lines) {
                        val trimmed = line.trimStart()
                        // Only apply quote span if there is actual content after ">"
                        if (trimmed.startsWith("> ") && trimmed.length > 2) {
                            val cleanText = trimmed.substring(2)

                            val startingLength = out.length
                            out.append(cleanText)
                            val endingLength = out.length

                            out.setSpan(
                                CustomQuoteSpan(
                                    stripeColor = 0xFFCCCCCC.toInt(),
                                    stripeWidth = 10,
                                    gapWidth = 25
                                ),
                                startingLength,
                                endingLength,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        } else {
                            // If empty quote line, just append a newline (or space)
                            out.append("\n")
                        }

                        if (line != lines.last()) out.append("\n")
                    }
                }

                // 7: Numbered list
                match.groups[7] != null -> {
                    val number = match.groups[7]!!.value   // "1"
                    val textPart = match.groups[8]!!.value // "Hello world"

                    val startN = out.length
                    out.append("$number. $textPart")
                    val endN = out.length

                    // WhatsApp-style indent
                    out.setSpan(
                        LeadingMarginSpan.Standard(40),    // indent amount
                        startN,
                        endN,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    // Optional: bold number if you want
                    out.setSpan(
                        StyleSpan(Typeface.BOLD),
                        startN,
                        startN + number.length + 1, // covers "1."
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            last = match.range.last + 1
        }
        if (last < text.length) {
            out.append(text.substring(last))
        }
    }
}


