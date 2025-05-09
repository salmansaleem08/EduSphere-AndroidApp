package com.salmansaleem.edusphere

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import java.io.File

class PeopleAdapter(private var people: List<Person>) :
    RecyclerView.Adapter<PeopleAdapter.PersonViewHolder>() {

    class PersonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImageView: ImageView = itemView.findViewById(R.id.iv_profile)
        val nameTextView: TextView = itemView.findViewById(R.id.tv_name)
        val teacherLabelTextView: TextView = itemView.findViewById(R.id.tv_teacher_label)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_person, parent, false)
        return PersonViewHolder(view)
    }

    override fun onBindViewHolder(holder: PersonViewHolder, position: Int) {
        val person = people[position]
        // Set name with "(Teacher)" for teacher
        val nameText = if (person.isTeacher) {
            SpannableString("${person.name} (Teacher)").apply {
                setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(holder.itemView.context, R.color.light_gray)),
                    person.name.length,
                    length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        } else {
            SpannableString(person.name)
        }
        holder.nameTextView.text = nameText

        // Set teacher label visibility
        holder.teacherLabelTextView.visibility = if (person.isTeacher) View.VISIBLE else View.GONE

        // Load profile image
        if (person.profileImagePath != null && File(person.profileImagePath).exists()) {
            Picasso.get()
                .load(File(person.profileImagePath))
                .placeholder(R.drawable.user_profile_placeholder)
                .error(R.drawable.user_profile_placeholder)
                .into(holder.profileImageView)
        } else {
            Picasso.get()
                .load(R.drawable.user_profile_placeholder)
                .into(holder.profileImageView)
        }
    }

    override fun getItemCount(): Int = people.size

    fun updateList(newList: List<Person>) {
        people = newList
        notifyDataSetChanged()
    }
}
