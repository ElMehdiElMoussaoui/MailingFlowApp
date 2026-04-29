package com.example.appmailing.TheContact

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.appmailing.R

class ContactsAdapter(
    private val onContactClick: (Contact) -> Unit,
    private val onDeleteClick: (Contact) -> Unit,
    private val onEditClick: (Contact) -> Unit,
    private val onSelectionChanged: (Int) -> Unit
) : ListAdapter<Contact, ContactsAdapter.ContactViewHolder>(ContactDiffCallback()) {

    private var isSelectionMode = false
    private val selectedContactIds = mutableSetOf<Int>()

    companion object {
        private const val PAYLOAD_SELECTION = "PAYLOAD_SELECTION"
        private const val PAYLOAD_SELECTION_MODE = "PAYLOAD_SELECTION_MODE"
    }

    fun setSelectionMode(enabled: Boolean) {
        if (isSelectionMode == enabled) return
        isSelectionMode = enabled
        if (!enabled) selectedContactIds.clear()
        notifyItemRangeChanged(0, itemCount, PAYLOAD_SELECTION_MODE)
        onSelectionChanged(selectedContactIds.size)
    }

    fun toggleSelection(contactId: Int, position: Int) {
        if (selectedContactIds.contains(contactId)) {
            selectedContactIds.remove(contactId)
        } else {
            selectedContactIds.add(contactId)
        }
        notifyItemChanged(position, PAYLOAD_SELECTION)
        onSelectionChanged(selectedContactIds.size)
    }

    fun selectAll() {
        selectedContactIds.clear()
        for (contact in currentList) {
            selectedContactIds.add(contact.id)
        }
        notifyItemRangeChanged(0, itemCount, PAYLOAD_SELECTION)
        onSelectionChanged(selectedContactIds.size)
    }

    fun clearSelection() {
        if (selectedContactIds.isEmpty()) return
        selectedContactIds.clear()
        notifyItemRangeChanged(0, itemCount, PAYLOAD_SELECTION)
        onSelectionChanged(0)
    }

    fun getSelectedIds(): List<Int> = selectedContactIds.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            val contact = getItem(position)
            if (payloads.contains(PAYLOAD_SELECTION_MODE)) {
                holder.cbSelect.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
                holder.layoutActions.visibility = if (isSelectionMode) View.GONE else View.VISIBLE
                holder.cbSelect.isChecked = selectedContactIds.contains(contact.id)
            } else if (payloads.contains(PAYLOAD_SELECTION)) {
                holder.cbSelect.isChecked = selectedContactIds.contains(contact.id)
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = getItem(position)
        val context = holder.itemView.context

        holder.tvName.text = contact.fullName
        holder.tvEmail.text = contact.email
        holder.tvPhone.text = contact.phone
        holder.tvPhone.visibility = if (contact.phone.isNotBlank()) View.VISIBLE else View.GONE

        // Fast initials extraction
        val firstSpace = contact.fullName.indexOf(' ')
        holder.tvInitials.text = if (firstSpace != -1) {
            val first = contact.fullName.getOrNull(0)?.toString() ?: ""
            val second = contact.fullName.getOrNull(firstSpace + 1)?.toString() ?: ""
            (first + second).uppercase()
        } else {
            contact.fullName.take(2).uppercase()
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

        holder.cbSelect.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
        holder.layoutActions.visibility = if (isSelectionMode) View.GONE else View.VISIBLE
        holder.cbSelect.isChecked = selectedContactIds.contains(contact.id)

        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(contact.id, holder.bindingAdapterPosition)
            } else {
                onContactClick(contact)
            }
        }

        holder.itemView.setOnLongClickListener {
            if (!isSelectionMode) {
                setSelectionMode(true)
                toggleSelection(contact.id, holder.bindingAdapterPosition)
                true
            } else false
        }

        holder.cbSelect.setOnClickListener {
            toggleSelection(contact.id, holder.bindingAdapterPosition)
        }

        holder.btnEdit.setOnClickListener { onEditClick(contact) }
        holder.btnDelete.setOnClickListener { onDeleteClick(contact) }
    }

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

    class ContactDiffCallback : DiffUtil.ItemCallback<Contact>() {
        override fun areItemsTheSame(oldItem: Contact, newItem: Contact): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean = oldItem == newItem
    }
}
