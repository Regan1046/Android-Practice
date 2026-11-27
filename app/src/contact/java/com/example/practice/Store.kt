package com.example.practice

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal data class Contact(val id: String, val name: String, val phone: String)

internal class ContactStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("contacts", Context.MODE_PRIVATE)

    fun read(): List<Contact> {
        val array = JSONArray(prefs.getString("people", "[]").orEmpty())
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            Contact(item.getString("id"), item.getString("name"), item.getString("phone"))
        }
    }

    fun save(contacts: List<Contact>) {
        // 仅保存本应用添加的联系人。主键独立于姓名和号码，编辑时仍能定位原记录。
        val array = JSONArray()
        contacts.forEach {
            array.put(JSONObject().put("id", it.id).put("name", it.name).put("phone", it.phone))
        }
        prefs.edit().putString("people", array.toString()).apply()
    }
}
