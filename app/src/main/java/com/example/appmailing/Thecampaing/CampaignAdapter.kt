package com.example.appmailing.Thecampaing

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.appmailing.R
import java.text.SimpleDateFormat
import java.util.*

class CampaignAdapter(
    private var fullList: List<CampaignFullDetails>,
    private val onItemClick: (Campaign) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<CampaignAdapter.ViewHolder>() {

    private var displayList = fullList.toMutableList()
    private var currentFilterQuery: String = ""

    fun updateList(newList: List<CampaignFullDetails>) {
        fullList = newList
        applyFilterAndSort()
    }

    fun filter(query: String) {
        currentFilterQuery = query
        applyFilterAndSort()
    }

    private fun applyFilterAndSort() {
        displayList = if (currentFilterQuery.isEmpty()) {
            fullList.toMutableList()
        } else {
            fullList.filter {
                it.campaign.title.contains(currentFilterQuery, ignoreCase = true)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    fun sortByStatus(targetStatus: String) {
        val sortedList = fullList.sortedWith(compareByDescending<CampaignFullDetails> { 
            it.campaign.status.equals(targetStatus, ignoreCase = true) 
        }.thenByDescending { it.campaign.timestamp })
        
        fullList = sortedList
        applyFilterAndSort()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_campaign, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = displayList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = displayList[position]
        val campaign = item.campaign
        val product = item.product

        holder.tvTitle.text = campaign.title
        holder.tvRecipients.text = "${campaign.recipientCount} Recipients"
        
        // Product Line
        holder.tvProductLine.text = "Product: ${product?.name ?: "None"}"
        
        // Date Line
        holder.tvTimestamp.text = formatTimestamp(campaign.timestamp)
        
        // Status styling
        applyStatusStyle(holder.tvStatus, campaign.status)

        holder.itemView.setOnClickListener { onItemClick(campaign) }
        holder.itemView.setOnLongClickListener {
            onDeleteClick(campaign.id)
            true
        }
    }

    private fun formatTimestamp(timestampStr: String): String {
        return try {
            val millis = timestampStr.toLong()
            val sdf = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())
            sdf.format(Date(millis))
        } catch (e: Exception) {
            timestampStr
        }
    }

    private fun applyStatusStyle(tv: TextView, status: String) {
        val ctx = tv.context
        tv.text = status.uppercase()
        when (status.uppercase()) {
            "SENT" -> {
                tv.setBackgroundResource(R.drawable.status_sent_bg)
                tv.setTextColor(androidx.core.content.ContextCompat.getColor(ctx, android.R.color.holo_green_dark))
            }
            "FAILED" -> {
                tv.setBackgroundResource(R.drawable.status_failed_bg)
                tv.setTextColor(androidx.core.content.ContextCompat.getColor(ctx, android.R.color.holo_red_dark))
            }
            "PENDING" -> {
                tv.setBackgroundResource(R.drawable.status_pending_bg)
                tv.setTextColor(androidx.core.content.ContextCompat.getColor(ctx, android.R.color.holo_orange_dark))
            }
            else -> {
                tv.setBackgroundResource(R.drawable.status_pending_bg)
                tv.setTextColor(androidx.core.content.ContextCompat.getColor(ctx, android.R.color.darker_gray))
            }
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvStatus: TextView = view.findViewById(R.id.tvStatusBadge)
        val tvRecipients: TextView = view.findViewById(R.id.tvRecipients)
        val tvTitle: TextView = view.findViewById(R.id.tvCampaignTitle)
        val tvProductLine: TextView = view.findViewById(R.id.tvProductLine)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
    }
}