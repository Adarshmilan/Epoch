package com.example.epoch

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.dispose
import coil.load
import com.google.android.material.button.MaterialButton
import okhttp3.OkHttpClient
import java.util.Calendar
import kotlin.math.abs

class FactAdapter : RecyclerView.Adapter<FactAdapter.FactViewHolder>() {

    private var events: List<HistoricalEvent> = emptyList()
    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    /** Height of the navigation bar, so buttons never sit under it. */
    var bottomInset: Int = 0
        set(value) {
            if (field != value) {
                field = value
                notifyItemRangeChanged(0, itemCount)
            }
        }

    fun submit(newEvents: List<HistoricalEvent>) {
        events = newEvents
        notifyDataSetChanged()
    }

    companion object {
        private const val USER_AGENT =
            "Epoch/1.0 (Android; https://github.com/Adarshmilan/Epoch)"
        private const val COLLAPSED_LINES = 3
        private const val EXPANDED_LINES = 12

        // Background gradients for events without a photo (top-left → bottom-right).
        private val FALLBACK_GRADIENTS = listOf(
            intArrayOf(0xFF3A2F5B.toInt(), 0xFF0E0E13.toInt()),
            intArrayOf(0xFF1F4E5A.toInt(), 0xFF0E0E13.toInt()),
            intArrayOf(0xFF5A3A22.toInt(), 0xFF0E0E13.toInt()),
            intArrayOf(0xFF2B4A2F.toInt(), 0xFF0E0E13.toInt()),
            intArrayOf(0xFF5A2333.toInt(), 0xFF0E0E13.toInt()),
        )

        @Volatile
        private var imageLoader: ImageLoader? = null

        fun getImageLoader(context: Context): ImageLoader =
            imageLoader ?: synchronized(this) {
                imageLoader ?: ImageLoader.Builder(context.applicationContext)
                    .okHttpClient(
                        OkHttpClient.Builder()
                            .addInterceptor { chain ->
                                chain.proceed(
                                    chain.request().newBuilder()
                                        .header("User-Agent", USER_AGENT)
                                        .build()
                                )
                            }
                            .build()
                    )
                    .crossfade(300)
                    .build()
                    .also { imageLoader = it }
            }

        fun formatYear(year: Int): String = if (year < 0) "${abs(year)} BC" else year.toString()

        private fun fixUrl(url: String?): String? =
            url?.let { if (it.startsWith("//")) "https:$it" else it }
    }

    class FactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fallback: View = view.findViewById(R.id.fallback)
        val txtWatermark: TextView = view.findViewById(R.id.txtWatermark)
        val imgBackground: ImageView = view.findViewById(R.id.imgBackground)
        val content: View = view.findViewById(R.id.content)
        val txtYear: TextView = view.findViewById(R.id.txtYear)
        val txtYearsAgo: TextView = view.findViewById(R.id.txtYearsAgo)
        val txtFact: TextView = view.findViewById(R.id.txtFact)
        val txtExtract: TextView = view.findViewById(R.id.txtExtract)
        val txtShowMore: TextView = view.findViewById(R.id.txtShowMore)
        val btnWiki: MaterialButton = view.findViewById(R.id.btnWiki)
        val btnShare: MaterialButton = view.findViewById(R.id.btnShare)
        val baseBottomPadding: Int = content.paddingBottom
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FactViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_fact, parent, false)
        return FactViewHolder(view)
    }

    override fun onBindViewHolder(holder: FactViewHolder, position: Int) {
        val event = events[position]
        val context = holder.itemView.context
        val yearLabel = formatYear(event.year)

        // Pick the page that has an image, otherwise the first one.
        val page = event.pages.find { it.thumbnail?.source != null } ?: event.pages.firstOrNull()

        // Bottom padding = design spacing + navigation bar
        holder.content.setPadding(
            holder.content.paddingLeft,
            holder.content.paddingTop,
            holder.content.paddingRight,
            holder.baseBottomPadding + bottomInset
        )

        // Year + "x years ago"
        holder.txtYear.text = yearLabel
        holder.txtWatermark.text = yearLabel
        val yearsAgo = currentYear - event.year
        holder.txtYearsAgo.text = if (yearsAgo <= 0) {
            context.getString(R.string.this_year)
        } else {
            context.resources.getQuantityString(R.plurals.years_ago, yearsAgo, yearsAgo)
        }

        // Headline + longer description
        holder.txtFact.text = event.text
        val extract = page?.extract?.trim().orEmpty()
        bindExtract(holder, if (extract.isNotEmpty() && !event.text.contains(extract)) extract else "")

        // Background image (or a gradient if there's none)
        holder.fallback.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            FALLBACK_GRADIENTS[position % FALLBACK_GRADIENTS.size]
        )
        bindImage(holder, page)

        // Actions
        val articleUrl = page?.content_urls?.desktop?.page
        holder.btnWiki.isEnabled = articleUrl != null
        holder.btnWiki.setOnClickListener {
            articleUrl ?: return@setOnClickListener
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(articleUrl)))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, R.string.no_link, Toast.LENGTH_SHORT).show()
            }
        }
        holder.btnShare.setOnClickListener {
            val text = buildString {
                append("On this day in ").append(yearLabel).append(": ").append(event.text)
                if (articleUrl != null) append("\n\n").append(articleUrl)
            }
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(send, context.getString(R.string.share_via)))
        }
    }

    private fun bindExtract(holder: FactViewHolder, extract: String) {
        holder.txtExtract.visibility = if (extract.isEmpty()) View.GONE else View.VISIBLE
        holder.txtExtract.text = extract
        holder.txtExtract.maxLines = COLLAPSED_LINES
        holder.txtShowMore.visibility = View.GONE
        holder.txtShowMore.setText(R.string.show_more)
        if (extract.isEmpty()) return

        // Only show the "Show more" toggle when the text is actually cut off.
        holder.txtExtract.post {
            val layout = holder.txtExtract.layout ?: return@post
            val lines = layout.lineCount
            if (lines > 0 && layout.getEllipsisCount(lines - 1) > 0) {
                holder.txtShowMore.visibility = View.VISIBLE
            }
        }

        val toggle = View.OnClickListener {
            if (holder.txtShowMore.visibility != View.VISIBLE) return@OnClickListener
            val expanded = holder.txtExtract.maxLines > COLLAPSED_LINES
            holder.txtExtract.maxLines = if (expanded) COLLAPSED_LINES else EXPANDED_LINES
            holder.txtShowMore.setText(if (expanded) R.string.show_more else R.string.show_less)
        }
        holder.txtExtract.setOnClickListener(toggle)
        holder.txtShowMore.setOnClickListener(toggle)
    }

    private fun bindImage(holder: FactViewHolder, page: WikiPage?) {
        val thumbUrl = fixUrl(page?.thumbnail?.source)
        // Full-size image looks much sharper full-screen; SVGs can't be decoded, so use the PNG thumbnail.
        val originalUrl = fixUrl(page?.originalimage?.source)
            ?.takeUnless { it.endsWith(".svg", ignoreCase = true) || it.endsWith(".tif", true) || it.endsWith(".tiff", true) }
        val primaryUrl = originalUrl ?: thumbUrl

        holder.imgBackground.dispose()
        holder.imgBackground.setImageDrawable(null)
        holder.fallback.visibility = View.VISIBLE
        if (primaryUrl == null) return

        val loader = getImageLoader(holder.itemView.context)
        holder.imgBackground.load(primaryUrl, loader) {
            listener(
                onSuccess = { _, _ -> holder.fallback.visibility = View.INVISIBLE },
                onError = { _, _ ->
                    // Retry with the smaller thumbnail if the original failed.
                    if (primaryUrl != thumbUrl && thumbUrl != null) {
                        holder.imgBackground.load(thumbUrl, loader) {
                            listener(onSuccess = { _, _ -> holder.fallback.visibility = View.INVISIBLE })
                        }
                    }
                }
            )
        }
    }

    override fun getItemCount() = events.size
}
