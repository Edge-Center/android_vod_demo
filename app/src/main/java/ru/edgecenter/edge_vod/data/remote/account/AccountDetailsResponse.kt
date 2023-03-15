package ru.edgecenter.edge_vod.data.remote.account

import com.google.gson.annotations.SerializedName

class AccountDetailsResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("email") val email: String,
    @SerializedName("users") val users: List<Any>,
    @SerializedName("currentUser") val currentUser: Int,
    @SerializedName("capabilities") val capabilities: Any?,
    @SerializedName("status") val status: String,
    @SerializedName("serviceStatuses") val serviceStatuses: Any?,
    @SerializedName("paidFeatures") val paidFeatures: Any?,
    @SerializedName("freeFeatures") val freeFeatures: Any?,
    @SerializedName("promotion") val promotion: Any?,
    @SerializedName("entryBaseDomain") val baseDomain: String?,
    @SerializedName("signup_process") val signUpProcess: String?,
    @SerializedName("delete_request") val deleteRequest: Any?,
    @SerializedName("deleted") val deleted: Boolean,
    @SerializedName("phone") val phone: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("companyName") val companyName: String?,
    @SerializedName("website") val website: String?,
    @SerializedName("promo_code") val promoCode: String?,
    @SerializedName("bill_type") val billType: String?,
    @SerializedName("custom_id") val customId: String?,
    @SerializedName("country_code") val countryCode: String?,
    @SerializedName("is_test") val isTest: Boolean,
)