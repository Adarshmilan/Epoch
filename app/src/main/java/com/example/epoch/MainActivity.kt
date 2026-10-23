package com.example.epoch

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

interface WikiApi {
    @GET("feed/onthisday/events/{month}/{day}")
    suspend fun getEvents(
        @Path("month") month: String,
        @Path("day") day: String
    ): HistoryResponse
}

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var topBar: View
    private lateinit var txtDate: TextView
    private lateinit var txtCounter: TextView
    private lateinit var loadingView: View
    private lateinit var errorView: View
    private lateinit var imgError: ImageView
    private lateinit var txtErrorTitle: TextView
    private lateinit var txtErrorMessage: TextView
    private lateinit var swipeHint: View

    private val adapter = FactAdapter()
    private var hintAnimator: ObjectAnimator? = null
    private var loadJob: Job? = null

    /** The day currently shown (year is ignored). */
    private val selectedDate: Calendar = Calendar.getInstance()

    private val api: WikiApi by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "Epoch/1.0 (Android; https://github.com/Adarshmilan/Epoch)")
                        .build()
                )
            }
            .build()
        Retrofit.Builder()
            .baseUrl("https://en.wikipedia.org/api/rest_v1/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WikiApi::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Light status/navigation bar icons on our dark UI.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewPager = findViewById(R.id.viewPager)
        topBar = findViewById(R.id.topBar)
        txtDate = findViewById(R.id.txtDate)
        txtCounter = findViewById(R.id.txtCounter)
        loadingView = findViewById(R.id.loadingView)
        errorView = findViewById(R.id.errorView)
        imgError = findViewById(R.id.imgError)
        txtErrorTitle = findViewById(R.id.txtErrorTitle)
        txtErrorMessage = findViewById(R.id.txtErrorMessage)
        swipeHint = findViewById(R.id.swipeHint)

        if (savedInstanceState != null) {
            setSelectedDay(savedInstanceState.getInt(KEY_MONTH), savedInstanceState.getInt(KEY_DAY))
        }

        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 1
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateCounter(position)
                if (position > 0) hideSwipeHint()
            }
        })

        applyInsets()

        findViewById<View>(R.id.dateButton).setOnClickListener { showDatePicker() }
        findViewById<MaterialButton>(R.id.btnCalendar).setOnClickListener { showDatePicker() }
        findViewById<MaterialButton>(R.id.btnRetry).setOnClickListener { fetchData() }

        fetchData()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_MONTH, selectedDate.get(Calendar.MONTH))
        outState.putInt(KEY_DAY, selectedDate.get(Calendar.DAY_OF_MONTH))
    }

    private fun applyInsets() {
        val topBarBasePadding = topBar.paddingTop
        val hintBaseMargin = (swipeHint.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            topBar.updatePadding(
                left = topBar.paddingLeft.coerceAtLeast(bars.left),
                top = topBarBasePadding + bars.top + dp(8)
            )
            (swipeHint.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin =
                hintBaseMargin + bars.bottom
            swipeHint.requestLayout()
            adapter.bottomInset = bars.bottom
            insets
        }
    }

    private fun fetchData() {
        val month = (selectedDate.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val day = selectedDate.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        txtDate.text = SimpleDateFormat("MMMM d", Locale.getDefault()).format(selectedDate.time)

        showLoading()
        loadJob?.cancel()
        loadJob = lifecycleScope.launch {
            try {
                val events = api.getEvents(month, day).events
                    .filter { it.text.isNotBlank() }
                    .sortedByDescending { it.year }
                Log.d(TAG, "Fetched ${events.size} events for $month/$day")
                if (events.isEmpty()) {
                    showError(empty = true)
                } else {
                    showContent(events)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Fetch error", e)
                showError(empty = false)
            }
        }
    }

    private fun showLoading() {
        loadingView.visibility = View.VISIBLE
        errorView.visibility = View.GONE
        viewPager.visibility = View.INVISIBLE
        txtCounter.visibility = View.GONE
        hideSwipeHint()
    }

    private fun showContent(events: List<HistoricalEvent>) {
        adapter.submit(events)
        viewPager.setCurrentItem(0, false)
        loadingView.visibility = View.GONE
        errorView.visibility = View.GONE
        viewPager.visibility = View.VISIBLE
        viewPager.alpha = 0f
        viewPager.animate().alpha(1f).setDuration(250).start()
        txtCounter.visibility = View.VISIBLE
        updateCounter(0)
        if (events.size > 1) showSwipeHint()
    }

    private fun showError(empty: Boolean) {
        loadingView.visibility = View.GONE
        viewPager.visibility = View.INVISIBLE
        txtCounter.visibility = View.GONE
        errorView.visibility = View.VISIBLE
        imgError.setImageResource(if (empty) R.drawable.ic_calendar else R.drawable.ic_cloud_off)
        txtErrorTitle.setText(if (empty) R.string.empty_title else R.string.error_title)
        txtErrorMessage.setText(if (empty) R.string.empty_message else R.string.error_message)
    }

    private fun updateCounter(position: Int) {
        txtCounter.text = getString(R.string.counter_format, position + 1, adapter.itemCount)
    }

    private fun showSwipeHint() {
        swipeHint.visibility = View.VISIBLE
        swipeHint.alpha = 1f
        hintAnimator?.cancel()
        hintAnimator = ObjectAnimator.ofFloat(swipeHint, View.TRANSLATION_Y, 0f, -dp(8).toFloat()).apply {
            duration = 700
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }
    }

    private fun hideSwipeHint() {
        if (swipeHint.visibility != View.VISIBLE) return
        hintAnimator?.cancel()
        hintAnimator = null
        swipeHint.animate().alpha(0f).setDuration(200).withEndAction {
            swipeHint.visibility = View.GONE
            swipeHint.translationY = 0f
        }.start()
    }

    private fun showDatePicker() {
        // MaterialDatePicker works in UTC milliseconds.
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                if (isLeapDay()) LEAP_YEAR else Calendar.getInstance().get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            )
        }
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.pick_date)
            .setSelection(utc.timeInMillis)
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            val picked = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
            setSelectedDay(picked.get(Calendar.MONTH), picked.get(Calendar.DAY_OF_MONTH))
            fetchData()
        }
        picker.show(supportFragmentManager, "date_picker")
    }

    /** Uses a leap year so Feb 29 and month-end days never roll over. */
    private fun setSelectedDay(month: Int, day: Int) {
        selectedDate.clear()
        selectedDate.set(LEAP_YEAR, month, day)
    }

    private fun isLeapDay() =
        selectedDate.get(Calendar.MONTH) == Calendar.FEBRUARY && selectedDate.get(Calendar.DAY_OF_MONTH) == 29

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        hintAnimator?.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "Epoch"
        private const val KEY_MONTH = "month"
        private const val KEY_DAY = "day"
        private const val LEAP_YEAR = 2024
    }
}
