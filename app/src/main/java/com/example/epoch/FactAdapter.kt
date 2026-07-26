package com.example.epoch

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import okhttp3.OkHttpClient
import coil.ImageLoader
import coil.request.ImageRequest

class FactAdapter(private val events: List<HistoricalEvent>) :
    RecyclerView.Adapter<FactAdapter.FactViewHolder>() {

    companion object {
        private var imageLoader: ImageLoader? = null
        private var okHttpClient: OkHttpClient? = null

        private fun getClient(): OkHttpClient {
            if (okHttpClient == null) {
                okHttpClient = OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                            .build()
                        chain.proceed(request)
                    }
                    .build()
            }
            return okHttpClient!!
        }

        fun getImageLoader(context: android.content.Context): ImageLoader {
            if (imageLoader == null) {
                imageLoader = ImageLoader.Builder(context.applicationContext)
                    .okHttpClient(getClient())
                    .build()
            }
            return imageLoader!!
        }
    }

    class FactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgThumbnail: ImageView = view.findViewById(R.id.imgThumbnail)
        val txtYear: TextView = view.findViewById(R.id.txtYear)
        val txtFact: TextView = view.findViewById(R.id.txtFact)
        val btnWiki: Button = view.findViewById(R.id.btnWiki)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FactViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_fact, parent, false)
        return FactViewHolder(view)
    }

    override fun onBindViewHolder(holder: FactViewHolder, position: Int) {
        val event = events[position]
        holder.txtYear.text = event.year.toString()
        
        // Find the first page that has a thumbnail and potentially more description
        val pageWithInfo = event.pages.find { it.thumbnail?.source != null } ?: event.pages.firstOrNull()
        
        // Combine event text with page extract for more detail
        val description = StringBuilder(event.text)
        pageWithInfo?.extract?.let {
            if (it.isNotEmpty() && !event.text.contains(it)) {
                description.append("\n\n").append(it)
            }
        }
        holder.txtFact.text = description.toString()
        
        // Use a custom ImageLoader with User-Agent
        val context = holder.itemView.context
        
        // Prefer originalimage for better quality, fallback to thumbnail
        val rawUrl = pageWithInfo?.thumbnail?.source
        
        val url = rawUrl?.let {
            if (it.startsWith("//")) "https:$it" else it
        }

        android.util.Log.d("Epoch", "Loading image for ${event.year}: $url")

        val request = ImageRequest.Builder(context)
            .data(url)
            .setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .target(holder.imgThumbnail)
            .crossfade(true)
            .placeholder(android.R.color.darker_gray)
            .error(android.R.drawable.stat_notify_error)
            .build()
        
        getImageLoader(context).enqueue(request)

        holder.btnWiki.setOnClickListener {
            val url = pageWithInfo?.content_urls?.desktop?.page
            if (url != null) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                holder.itemView.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount() = events.size
}