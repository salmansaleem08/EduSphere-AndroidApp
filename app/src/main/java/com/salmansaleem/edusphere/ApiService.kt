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
}

data class FetchImageRequest(val uid: String)

data class ProfileImageResponse(
    val success: Boolean,
    val image_url: String? = null,
    val error: String? = null,
    val message: String? = null
)
