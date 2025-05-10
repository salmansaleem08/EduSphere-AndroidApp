package com.salmansaleem.edusphere

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageView
import com.squareup.picasso.Picasso
import java.io.File


class CommentAdapter(private val comments: List<Comment>) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userNameTextView: TextView = itemView.findViewById(R.id.txt_user_name)
        val commentContentTextView: TextView = itemView.findViewById(R.id.txt_comment_content)
        val profileImageView: ImageView = itemView.findViewById(R.id.img_user_profile)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.userNameTextView.text = comment.name
        holder.commentContentTextView.text = comment.text
        // Load profile image
        if (comment.profileImagePath != null && File(comment.profileImagePath).exists()) {
            Picasso.get()
                .load(File(comment.profileImagePath))
                .placeholder(R.drawable.user_profile_placeholder)
                .error(R.drawable.user_profile_placeholder)
                .into(holder.profileImageView)
        } else {
            Picasso.get()
                .load(R.drawable.user_profile_placeholder)
                .into(holder.profileImageView)
        }
    }

    override fun getItemCount(): Int = comments.size
}