package com.salmansaleem.edusphere

data class Classroom(
    val classroomId: String,
    val name: String,
    val instructorName: String,
    val imagePath: String?
)

data class Announcement(
    val announcementId: String,
    val classroomId: String,
    val uid: String,
    val name: String,
    val text: String,
    val timestamp: String
)
data class Comment(
    val commentId: String,
    val announcementId: String,
    val classroomId: String,
    val uid: String,
    val name: String,
    val text: String,
    val timestamp: String
)