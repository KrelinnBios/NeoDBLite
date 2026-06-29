package com.krelinnbios.neodblite.data

import com.krelinnbios.neodblite.data.model.ItemBrief
import com.krelinnbios.neodblite.data.model.MarkInRequest
import com.krelinnbios.neodblite.data.model.MarkSchema
import com.krelinnbios.neodblite.data.model.NeoUser
import com.krelinnbios.neodblite.data.model.PagedMarks
import com.krelinnbios.neodblite.data.model.SearchResult
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * NeoDB REST API（含 Mastodon 兼容子集）。baseUrl 在 [NeoDBClient] 按实例 host 注入，
 * 鉴权 token 由拦截器统一附加。
 */
interface NeoDBApi {

    @GET("api/me")
    suspend fun me(): NeoUser

    @GET("api/catalog/search")
    suspend fun search(
        @Query("query") query: String,
        @Query("category") category: String?,
        @Query("page") page: Int
    ): SearchResult

    /** 趋势榜。cat 为路径段（book/movie/tv/music/game/podcast/performance）。 */
    @GET("api/trending/{cat}/")
    suspend fun trending(@Path("cat") cat: String): List<ItemBrief>

    /** 用条目自带的 api_url（相对 baseUrl）拉取详情，避免各类目路径差异。 */
    @GET
    suspend fun itemByPath(@Url path: String): ItemBrief
    /** 网页端 htmx 片段，用于读取条目公开短评/长评/笔记。 */
    @GET
    suspend fun htmlFragment(
        @Url path: String,
        @Header("HX-Request") hxRequest: String = "true"
    ): ResponseBody

    @GET("api/me/shelf/{type}")
    suspend fun shelf(
        @Path("type") type: String,
        @Query("category") category: String?,
        @Query("page") page: Int
    ): PagedMarks

    @GET("api/me/shelf/item/{uuid}")
    suspend fun getMark(@Path("uuid") uuid: String): Response<MarkSchema>

    @POST("api/me/shelf/item/{uuid}")
    suspend fun postMark(
        @Path("uuid") uuid: String,
        @Body body: MarkInRequest
    ): Response<ResponseBody>

    @DELETE("api/me/shelf/item/{uuid}")
    suspend fun deleteMark(@Path("uuid") uuid: String): Response<ResponseBody>
}
