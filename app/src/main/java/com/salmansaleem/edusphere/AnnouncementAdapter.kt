package com.salmansaleem.edusphere

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AnnouncementAdapter(private val announcements: List<Announcement>) :
    RecyclerView.Adapter<AnnouncementAdapter.AnnouncementViewHolder>() {

    class AnnouncementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.tv_comment_name)
        val timeTextView: TextView = itemView.findViewById(R.id.tv_comment_time)
        val announcementTextView: TextView = itemView.findViewById(R.id.announcement)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnnouncementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_announcement, parent, false)
        return AnnouncementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AnnouncementViewHolder, position: Int) {
        val announcement = announcements[position]
        holder.nameTextView.text = announcement.name
        holder.timeTextView.text = announcement.timestamp
        holder.announcementTextView.text = announcement.text
    }

    override fun getItemCount(): Int = announcements.size
}