package com.example.appmailing.statistique

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.appmailing.HomeScreen
import com.example.appmailing.R
import com.example.appmailing.TheContact.ContactsActivity
import com.example.appmailing.TheProducts.ProductsActivity
import com.example.appmailing.Thecampaing.CampaignActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.*

class StatistiqueActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase

    //  ADDED ONLY
    private var startDate: Long = 0L
    private var endDate: Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistique)

        database = AppDatabase.getDatabase(this)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        //  ADDED CALENDAR
        val btnCalendar = findViewById<ImageView>(R.id.btnCalendar)
        val tvDateRange = findViewById<TextView>(R.id.tvDateRange)

        btnCalendar.setOnClickListener { showCalendar() }
        tvDateRange.setOnClickListener { showCalendar() }

        setupBottomNav()
        setupDateHeader()
        observeStats()
    }

    private fun setupDateHeader() {
        val tvDateRange = findViewById<TextView>(R.id.tvDateRange)
        val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        val today = sdf.format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val weekAgo = sdf.format(calendar.time)

        tvDateRange?.text = "$weekAgo — $today"
    }

    //  CALENDAR FUNCTION
    private fun showCalendar() {

        val tvDateRange = findViewById<TextView>(R.id.tvDateRange)

        val start = Calendar.getInstance()
        val end = Calendar.getInstance()

        DatePickerDialog(this, { _, y, m, d ->
            start.set(y, m, d)

            DatePickerDialog(this, { _, y2, m2, d2 ->
                end.set(y2, m2, d2)

                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

                tvDateRange.text =
                    "${sdf.format(start.time)} — ${sdf.format(end.time)}"

                startDate = start.timeInMillis
                endDate = end.timeInMillis + 86400000

                observeStats()

            }, end.get(Calendar.YEAR), end.get(Calendar.MONTH), end.get(Calendar.DAY_OF_MONTH)).show()

        }, start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun observeStats() {

        val tvOpenRate = findViewById<TextView>(R.id.tvOpenRate)
        val tvClickRate = findViewById<TextView>(R.id.tvClickRate)
        val tvSuccessRate = findViewById<TextView>(R.id.tvSuccessPercent)

        val tvDonutPercent = findViewById<TextView>(R.id.tvDonutPercent)
        val tvDeliveredCount = findViewById<TextView>(R.id.tvDeliveredCount)
        val tvBouncedCount = findViewById<TextView>(R.id.tvBouncedCount)
        val tvTotalEmailsSent = findViewById<TextView>(R.id.tvTotalEmailsSent)

        val monOpen = findViewById<View>(R.id.monOpen)
        val monClick = findViewById<View>(R.id.monClick)
        val tueOpen = findViewById<View>(R.id.tueOpen)
        val tueClick = findViewById<View>(R.id.tueClick)
        val wedOpen = findViewById<View>(R.id.wedOpen)
        val wedClick = findViewById<View>(R.id.wedClick)
        val thuOpen = findViewById<View>(R.id.thuOpen)
        val thuClick = findViewById<View>(R.id.thuClick)
        val friOpen = findViewById<View>(R.id.friOpen)
        val friClick = findViewById<View>(R.id.friClick)

        database.sentEmailDao().getAllSentEmails().observe(this) { allEmails ->

            // ️ ONLY CHANGE (FILTER ADDED)
            val emails = (allEmails ?: emptyList()).filter {
                it.timestamp in startDate..endDate
            }

            val total = emails.size
            val successCount = emails.count { it.status == "SUCCESS" }
            val failedCount = total - successCount

            val openCount = emails.count { it.opened }
            val clickCount = emails.count { it.clicked }

            tvTotalEmailsSent?.text = total.toString()
            tvDeliveredCount?.text = successCount.toString()
            tvBouncedCount?.text = failedCount.toString()

            if (total > 0) {

                val successPercent = (successCount.toDouble() / total.toDouble() * 100).toInt()
                val openPercent = if (successCount > 0)
                    (openCount.toDouble() / successCount.toDouble() * 100).toInt() else 0
                val clickPercent = if (openCount > 0)
                    (clickCount.toDouble() / openCount.toDouble() * 100).toInt() else 0

                tvDonutPercent?.text = "$successPercent%"
                tvSuccessRate?.text = "$successPercent%"
                tvOpenRate?.text = "$openPercent%"
                tvClickRate?.text = "$clickPercent%"

                val calendar = Calendar.getInstance()

                val dailyStats = emails.groupBy {
                    calendar.timeInMillis = it.timestamp
                    calendar.get(Calendar.DAY_OF_WEEK)
                }

                fun getDayStats(day: Int): Pair<Int, Int> {
                    val dayEmails = dailyStats[day] ?: return 0 to 0
                    return dayEmails.count { it.opened } to dayEmails.count { it.clicked }
                }

                updateBarHeight(monOpen, calculateBarHeightPercent(getDayStats(Calendar.MONDAY).first, successCount))
                updateBarHeight(monClick, calculateBarHeightPercent(getDayStats(Calendar.MONDAY).second, successCount))
                updateBarHeight(tueOpen, calculateBarHeightPercent(getDayStats(Calendar.TUESDAY).first, successCount))
                updateBarHeight(tueClick, calculateBarHeightPercent(getDayStats(Calendar.TUESDAY).second, successCount))
                updateBarHeight(wedOpen, calculateBarHeightPercent(getDayStats(Calendar.WEDNESDAY).first, successCount))
                updateBarHeight(wedClick, calculateBarHeightPercent(getDayStats(Calendar.WEDNESDAY).second, successCount))
                updateBarHeight(thuOpen, calculateBarHeightPercent(getDayStats(Calendar.THURSDAY).first, successCount))
                updateBarHeight(thuClick, calculateBarHeightPercent(getDayStats(Calendar.THURSDAY).second, successCount))
                updateBarHeight(friOpen, calculateBarHeightPercent(getDayStats(Calendar.FRIDAY).first, successCount))
                updateBarHeight(friClick, calculateBarHeightPercent(getDayStats(Calendar.FRIDAY).second, successCount))

            } else {
                tvDonutPercent?.text = "0%"
                tvSuccessRate?.text = "0%"
                tvOpenRate?.text = "0%"
                tvClickRate?.text = "0%"

                resetBars(monOpen, monClick, tueOpen, tueClick, wedOpen, wedClick, thuOpen, thuClick, friOpen, friClick)
            }
        }
    }

    private fun calculateBarHeightPercent(count: Int, total: Int): Int {
        if (total == 0) return 5
        return (count.toDouble() / total.toDouble() * 100).toInt().coerceAtLeast(5)
    }

    private fun updateBarHeight(view: View?, percentage: Int) {
        view?.post {
            val params = view.layoutParams
            val maxHeightPx = (100 * resources.displayMetrics.density).toInt()
            params.height = (maxHeightPx * (percentage.coerceIn(5, 100) / 100.0)).toInt()
            view.layoutParams = params
        }
    }

    private fun resetBars(vararg views: View?) {
        views.forEach { updateBarHeight(it, 5) }
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_stats
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeScreen::class.java))
                    overridePendingTransition(0, 0); true
                }
                R.id.nav_campaigns -> {
                    startActivity(Intent(this, CampaignActivity::class.java))
                    overridePendingTransition(0, 0); true
                }
                R.id.nav_contacts -> {
                    startActivity(Intent(this, ContactsActivity::class.java))
                    overridePendingTransition(0, 0); true
                }
                R.id.nav_products -> {
                    startActivity(Intent(this, ProductsActivity::class.java))
                    overridePendingTransition(0, 0); true
                }
                R.id.nav_stats -> true
                else -> false
            }
        }
    }
}