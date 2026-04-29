package com.example.appmailing.TheContact

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.appmailing.R

class ContactsAdapter(
    private val onContactClick: (Contact) -> Unit,
    private val onDeleteClick: (Contact) -> Unit,
    private val onEditClick: (Contact) -> Unit,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    private var filteredContacts = listOf<Contact>()
    private var isSelectionMode = false
    private val selectedContactIds = mutableSetOf<Int>()

    fun filter(all: List<Contact>, query: String, emailF: EmailStatus?, sendF: SendingStatus?) {
        filteredContacts = all.filter { c ->
            val matchQuery = query.isBlank() || 
                    c.fullName.contains(query, true) || 
                    c.email.contains(query, true)
            
            val matchEmail = emailF == null || c.emailStatus == emailF
            val matchSend  = sendF == null || c.sendingStatus == sendF
            
            matchQuery && matchEmail && matchSend
        }
        notifyDataSetChanged()
    }

    fun setSelectionMode(enabled: Boolean) {
        isSelectionMode = enabled
        if (!enabled) selectedContactIds.clear()
        notifyDataSetChanged()
    }

    fun toggleSelection(contactId: Int) {
        if (selectedContactIds.contains(contactId)) {
            selectedContactIds.remove(contactId)
        } else {
            selectedContactIds.add(contactId)
        }
        notifyDataSetChanged()
        onSelectionChanged(selectedContactIds.size)
    }

    fun selectAll() {
        selectedContactIds.clear()
        selectedContactIds.addAll(filteredContacts.map { it.id })
        notifyDataSetChanged()
        onSelectionChanged(selectedContactIds.size)
    }

    fun clearSelection() {
        selectedContactIds.clear()
        notifyDataSetChanged()
        onSelectionChanged(0)
    }

    fun getSelectedIds(): List<Int> = selectedContactIds.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = filteredContacts[position]
        val context = holder.itemView.context

        holder.tvName.text = contact.fullName
        holder.tvEmail.text = contact.email
        holder.tvPhone.text = contact.phone
        holder.tvPhone.visibility = if (contact.phone.isNotBlank()) View.VISIBLE else View.GONE

        val parts = contact.fullName.trim().split(" ")
        holder.tvInitials.text = when {
            parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
            parts.isNotEmpty() -> parts[0].take(2).uppercase()
            else -> "?"
        }

        if (contact.emailStatus == EmailStatus.CONFORME) {
            holder.ivEmailStatus.setImageResource(R.drawable.ic_check_circle)
            holder.ivEmailStatus.setColorFilter(ContextCompat.getColor(context, R.color.green_500))
        } else {
            holder.ivEmailStatus.setImageResource(R.drawable.ic_error_circle)
            holder.ivEmailStatus.setColorFilter(ContextCompat.getColor(context, R.color.red_400))
        }

        when (contact.sendingStatus) {
            SendingStatus.SENT -> {
                holder.ivSendingStatus.setImageResource(R.drawable.ic_email_sent)
                holder.ivSendingStatus.setColorFilter(ContextCompat.getColor(context, R.color.blue_primary))
            }
            SendingStatus.FAILED -> {
                holder.ivSendingStatus.setImageResource(R.drawable.ic_error_circle)
                holder.ivSendingStatus.setColorFilter(ContextCompat.getColor(context, R.color.red_400))
            }
            else -> {
                holder.ivSendingStatus.setImageResource(R.drawable.ic_email_unsent)
                holder.ivSendingStatus.setColorFilter(ContextCompat.getColor(context, R.color.grey_400))
            }
        }

        // Selection Logic
        holder.cbSelect.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
        holder.layoutActions.visibility = if (isSelectionMode) View.GONE else View.VISIBLE
        holder.cbSelect.isChecked = selectedContactIds.contains(contact.id)

        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(contact.id)
            } else {
                onContactClick(contact)
            }
        }

        holder.itemView.setOnLongClickListener {
            if (!isSelectionMode) {
                setSelectionMode(true)
                toggleSelection(contact.id)
                true
            } else false
        }

        holder.cbSelect.setOnClickListener {
            toggleSelection(contact.id)
        }

        holder.btnEdit.setOnClickListener { onEditClick(contact) }
        holder.btnDelete.setOnClickListener { onDeleteClick(contact) }
    }

    override fun getItemCount() = filteredContacts.size

    class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInitials: TextView = view.findViewById(R.id.tvInitials)
        val tvName: TextView = view.findViewById(R.id.tvContactName)
        val tvEmail: TextView = view.findViewById(R.id.tvContactEmail)
        val tvPhone: TextView = view.findViewById(R.id.tvContactPhone)
        val ivEmailStatus: ImageView = view.findViewById(R.id.ivEmailStatus)
        val ivSendingStatus: ImageView = view.findViewById(R.id.ivSendingStatus)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        val cbSelect: CheckBox = view.findViewById(R.id.cbSelect)
        val layoutActions: View = view.findViewById(R.id.layoutActions)
    }
}
