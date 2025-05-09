package com.salmansaleem.edusphere

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso
import java.io.File


class ClassAdapter(private val classrooms: List<Classroom>) : RecyclerView.Adapter<ClassViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_class, parent, false)
        return ClassViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        holder.bind(classrooms[position])
    }

    override fun getItemCount(): Int = classrooms.size
}



class ClassViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val classTitle: TextView = itemView.findViewById(R.id.tv_class_title)
    private val instructorName: TextView = itemView.findViewById(R.id.tv_instructor_name)
    private val bgImage: ImageView = itemView.findViewById(R.id.bg_image)
    private val TAG = "ClassViewHolder"

//    fun bind(classroom: Classroom) {
//        classTitle.text = classroom.name
//        instructorName.text = classroom.instructorName
//        Log.d(TAG, "Binding classroom ${classroom.classroomId} with imageUrl: ${classroom.imagePath}")
//
//        // Load image from URL or local file
//        if (classroom.imagePath != null && classroom.imagePath.isNotEmpty()) {
//            Log.d(TAG, "Loading image from URL: ${classroom.imagePath}")
//            Picasso.get()
//                .load(classroom.imagePath)
//                .placeholder(R.drawable.class_placeholder)
//                .error(R.drawable.class_placeholder)
//                .into(bgImage)
//        } else {
//            // Check for local file as fallback
//            val localFile = File(itemView.context.filesDir, "${classroom.classroomId}_classroom.png")
//            if (localFile.exists()) {
//                Log.d(TAG, "Loading local image from file: ${localFile.absolutePath}")
//                Picasso.get()
//                    .load(localFile)
//                    .placeholder(R.drawable.class_placeholder)
//                    .error(R.drawable.class_placeholder)
//                    .into(bgImage)
//            } else {
//                Log.d(TAG, "No local image file for ${classroom.classroomId}, using placeholder")
//                Picasso.get()
//                    .load(R.drawable.class_placeholder)
//                    .into(bgImage)
//            }
//        }
//    }

    fun bind(classroom: Classroom) {
        classTitle.text = classroom.name
        instructorName.text = classroom.instructorName
        Log.d(TAG, "Binding classroom ${classroom.classroomId} with imagePath: ${classroom.imagePath}")

        // Load image from local file path
        if (classroom.imagePath != null && classroom.imagePath.isNotEmpty()) {
            val localFile = File(classroom.imagePath)
            if (localFile.exists()) {
                Log.d(TAG, "Loading local image from file: ${localFile.absolutePath}")
                Picasso.get()
                    .load(localFile)
                    .placeholder(R.drawable.class_placeholder)
                    .error(R.drawable.class_placeholder)
                    .into(bgImage)
            } else {
                Log.d(TAG, "Local image file does not exist for ${classroom.classroomId}, using placeholder")
                Picasso.get()
                    .load(R.drawable.class_placeholder)
                    .into(bgImage)
            }
        } else {
            Log.d(TAG, "No image path for ${classroom.classroomId}, using placeholder")
            Picasso.get()
                .load(R.drawable.class_placeholder)
                .into(bgImage)
        }
    }
}