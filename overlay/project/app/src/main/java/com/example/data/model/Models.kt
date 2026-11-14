package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

enum class WorkType(val displayName: String) {
    ELECTRICAL("Electrical"),
    PLUMBING("Plumbing"),
    MECHANICAL("Mechanical");

    companion object {
        fun fromString(value: String): WorkType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
                ?: ELECTRICAL
        }
    }
}

data class SelectedItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val quantity: String,
    val workType: String = "Electrical",
    val category: String = ""
)

data class PaymentHistoryItem(
    val time: String,
    val createdAt: String,
    val count: Int
) {
    fun toJsonObject(): JSONObject {
        val obj = JSONObject()
        obj.put("time", time)
        obj.put("createdAt", createdAt)
        obj.put("count", count)
        return obj
    }

    companion object {
        fun fromJson(jsonStr: String): List<PaymentHistoryItem> {
            val list = mutableListOf<PaymentHistoryItem>()
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        PaymentHistoryItem(
                            time = obj.optString("time"),
                            createdAt = obj.optString("createdAt"),
                            count = obj.optInt("count", 1)
                        )
                    )
                }
            } catch (_: Exception) {}
            return list
        }

        fun toJson(list: List<PaymentHistoryItem>): String {
            val array = JSONArray()
            list.forEach { array.put(it.toJsonObject()) }
            return array.toString()
        }
    }
}

data class BillItemRow(
    val id: String = java.util.UUID.randomUUID().toString(),
    val qty: String = "",
    val description: String = "",
    val rate: String = ""
) {
    val amount: Double
        get() = (qty.toDoubleOrNull() ?: 0.0) * (rate.toDoubleOrNull() ?: 0.0)

    fun toJsonObject(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("qty", qty)
        obj.put("description", description)
        obj.put("rate", rate)
        return obj
    }

    companion object {
        fun fromJson(jsonStr: String): List<BillItemRow> {
            val list = mutableListOf<BillItemRow>()
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        BillItemRow(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                            qty = obj.optString("qty"),
                            description = obj.optString("description"),
                            rate = obj.optString("rate")
                        )
                    )
                }
            } catch (_: Exception) {}
            return list
        }

        fun toJson(list: List<BillItemRow>): String {
            val array = JSONArray()
            list.forEach { array.put(it.toJsonObject()) }
            return array.toString()
        }
    }
}
