package com.salmansaleem.edusphere

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TasksAdapter(private val tasks: MutableList<Map<String, String>>) :
    RecyclerView.Adapter<TasksAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val taskTitle: TextView = itemView.findViewById(R.id.tv_task_title)
        val taskTime: TextView = itemView.findViewById(R.id.tv_task_time)
        val moreIcon: ImageView = itemView.findViewById(R.id.iv_more)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.taskTitle.text = task["name"]
        holder.taskTime.text = task["due_date"]
        holder.moreIcon.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, AssignmentSubmission::class.java)
            intent.putExtra("classroom_id", task["classroom_id"])
            intent.putExtra("assignment_id", task["assignment_id"])
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = tasks.size

    fun updateTasks(newTasks: List<Map<String, String>>) {
        tasks.clear()
        tasks.addAll(newTasks)
        notifyDataSetChanged()
    }
}
