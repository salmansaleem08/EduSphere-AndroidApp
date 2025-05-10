package com.salmansaleem.edusphere

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AttachmentsAdapter(
    private val attachments: MutableList<Map<String, String>>,
    private val onAttachmentClick: (String) -> Unit
) : RecyclerView.Adapter<AttachmentsAdapter.AttachmentViewHolder>() {

    class AttachmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val attachmentName: TextView = itemView.findViewById(R.id.tv_attachment_name)
        val attachmentIcon: ImageView = itemView.findViewById(R.id.iv_attachment_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttachmentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_attachment, parent, false)
        return AttachmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttachmentViewHolder, position: Int) {
        val attachment = attachments[position]
        holder.attachmentName.text = attachment["name"]
        holder.attachmentName.setOnClickListener { onAttachmentClick(attachment["path"]!!) }
    }

    override fun getItemCount(): Int = attachments.size

    fun updateAttachments(newAttachments: List<Map<String, String>>) {
        attachments.clear()
        attachments.addAll(newAttachments)
        notifyDataSetChanged()
    }
}
