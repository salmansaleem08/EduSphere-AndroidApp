package com.salmansaleem.edusphere

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import java.io.File
import de.hdodenhof.circleimageview.CircleImageView

class AnnouncementAdapter(private val announcements: List<Announcement>,
                          private val onAnnouncementClick: (Announcement) -> Unit
     ) :
    RecyclerView.Adapter<AnnouncementAdapter.AnnouncementViewHolder>() {

    class AnnouncementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.tv_comment_name)
        val timeTextView: TextView = itemView.findViewById(R.id.tv_comment_time)
        val announcementTextView: TextView = itemView.findViewById(R.id.announcement)
        val profileImageView: ImageView = itemView.findViewById(R.id.iv_profile)
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
        // Load profile image
        if (announcement.profileImagePath != null && File(announcement.profileImagePath).exists()) {
            Picasso.get()
                .load(File(announcement.profileImagePath))
                .placeholder(R.drawable.user_profile_placeholder)
                .error(R.drawable.user_profile_placeholder)
                .into(holder.profileImageView)
        } else {
            Picasso.get()
                .load(R.drawable.user_profile_placeholder)
                .into(holder.profileImageView)
        }
        // Set click listener on the item view
        holder.itemView.setOnClickListener {
            onAnnouncementClick(announcement)
        }
    }

    override fun getItemCount(): Int = announcements.size
}