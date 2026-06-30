package com.krelinnbios.neodblite.data.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

/**
 * 把「字符串数组」字段解析得更宽容：NeoDB 对部分条目（如影视的演职人员）
 * 会把 author/director/actor/artist 返回成对象数组（含 name/role 等），
 * 而非纯字符串数组，直接按 List<String> 解析会抛
 * `Expected a string but was BEGIN_OBJECT`，导致搜索/详情整页失败。
 *
 * 该反序列化器对每个元素：字符串原样取用；对象取其 name 字段；其余忽略，
 * 从而无论实例返回哪种形态都能安全降级为名称列表。
 */
class FlexibleStringListDeserializer : JsonDeserializer<List<String>> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): List<String> {
        if (json == null || json.isJsonNull) return emptyList()
        if (!json.isJsonArray) return emptyList()
        val result = ArrayList<String>()
        for (element in json.asJsonArray) {
            when {
                element == null || element.isJsonNull -> Unit
                element.isJsonPrimitive -> {
                    element.asString.takeIf { it.isNotBlank() }?.let { result.add(it) }
                }
                element.isJsonObject -> {
                    val obj = element.asJsonObject
                    val name = obj.get("name")?.takeIf { it.isJsonPrimitive }?.asString
                        ?: obj.get("localized_name")
                            ?.takeIf { it.isJsonArray && it.asJsonArray.size() > 0 }
                            ?.asJsonArray?.firstOrNull()
                            ?.takeIf { it.isJsonObject }
                            ?.asJsonObject?.get("text")
                            ?.takeIf { it.isJsonPrimitive }?.asString
                    name?.takeIf { it.isNotBlank() }?.let { result.add(it) }
                }
                else -> Unit
            }
        }
        return result
    }
}
