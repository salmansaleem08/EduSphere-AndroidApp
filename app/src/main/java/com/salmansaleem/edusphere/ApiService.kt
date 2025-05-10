package com.salmansaleem.edusphere

import com.google.firebase.database.*
import okhttp3.*
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

// Retrofit API Service
// Retrofit API Service
interface ApiService {
    @Multipart
    @POST("upload_profile_image.php")
    fun uploadProfileImage(
        @Part("uid") uid: RequestBody,
        @Part image: MultipartBody.Part
    ): Call<ProfileImageResponse>

    @POST("fetch_profile_image.php")
    fun fetchProfileImage(@Body request: FetchImageRequest): Call<ProfileImageResponse>


    // New classroom image endpoints
    @Multipart
    @POST("upload_classroom_image.php")
    fun uploadClassroomImage(
        @Part("classroom_id") classroomId: RequestBody,
        @Part image: MultipartBody.Part
    ): Call<ClassroomImageResponse>

    @POST("fetch_classroom_image.php")
    fun fetchClassroomImage(@Body request: FetchClassroomImageRequest): Call<ClassroomImageResponse>



    @Multipart
    @POST("upload_assignment_image.php")
    fun uploadAssignmentImage(
        @Part("assignment_id") assignmentId: RequestBody,
        @Part image: MultipartBody.Part
    ): Call<AssignmentImageResponse>


    @POST("fetch_assignment_image.php")
    fun fetchAssignmentImage(@Body request: FetchAssignmentImageRequest): Call<AssignmentImageResponse>



    @Multipart
    @POST("upload_submission_image.php")
    fun uploadSubmissionImage(
        @Part("submission_id") submissionId: RequestBody,
        @Part image: MultipartBody.Part
    ): Call<SubmissionImageResponse>

//    @POST("fetch_assignment_image.php") // Reusing endpoint with submission_id
//    fun fetchSubmissionImage(@Body request: FetchSubmissionImageRequest): Call<AssignmentImageResponse>



//    @Multipart
//    @POST("upload_submission_image")
//    fun uploadSubmissionImage(
//        @Part("submission_id") submissionId: RequestBody,
//        @Part image: MultipartBody.Part
//    ): Call<ImageUploadResponse>


    @POST("fetch_submission_image.php")
    fun fetchSubmissionImage(@Body request: FetchSubmissionImageRequest): Call<AssignmentImageResponse>
}

data class FetchImageRequest(val uid: String)

data class ProfileImageResponse(
    val success: Boolean,
    val image_url: String? = null,
    val error: String? = null,
    val message: String? = null
)

data class FetchClassroomImageRequest(val classroom_id: String)
data class ClassroomImageResponse(
    val success: Boolean,
    val error: String?,
    val image_url: String?,
    val message: String?
)


data class FetchAssignmentImageRequest(val assignment_id: String)

data class AssignmentImageResponse(
    val success: Boolean,
    val image_url: String? = null,
    val error: String? = null,
    val message: String? = null
)
data class FetchSubmissionImageRequest(val submission_id: String)

data class ImageUploadResponse(
    val success: Boolean,
    val image_url: String?
)


data class SubmissionImageResponse(
    val success: Boolean,
    val image_url: String? = null,
    val error: String? = null,
    val message: String? = null
)