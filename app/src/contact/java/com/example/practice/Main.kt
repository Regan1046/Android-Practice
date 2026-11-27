package com.example.practice

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import org.json.JSONException
import java.text.Collator
import java.util.Locale
import java.util.UUID

class MainActivity : PracticeActivity() {
    private val store by lazy { ContactStore(this) }
    private val collator = Collator.getInstance(Locale.SIMPLIFIED_CHINESE)
    private var contacts = emptyList<Contact>()
    private var loadError = false
    private lateinit var query: EditText
    private lateinit var list: LinearLayout

    // 在 onCreate 之前注册结果处理。配置变化后仍可以接收同一个详情页的结果。
    private val detail = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK || loadError) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        val id = data.getStringExtra(ContactArgs.ID)
        if (data.getBooleanExtra(ContactArgs.DELETE, false)) {
            save(contacts.filter { it.id != id })
        } else {
            val name = data.getStringExtra(ContactArgs.NAME) ?: return@registerForActivityResult
            val phone = data.getStringExtra(ContactArgs.PHONE) ?: return@registerForActivityResult
            val contact = Contact(id ?: UUID.randomUUID().toString(), name, phone)
            save(contacts.filter { it.id != contact.id } + contact)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contacts = try {
            store.read()
        } catch (_: JSONException) {
            loadError = true
            emptyList()
        } catch (_: ClassCastException) {
            loadError = true
            emptyList()
        }
        val content = page("本地通讯录", "添加联系人，按姓名或号码查找。")
        val search = content.card()
        query = search.field("搜索姓名或号码").apply {
            filters = arrayOf(InputFilter.LengthFilter(80))
            setText(savedInstanceState?.getString("query"))
            isEnabled = !loadError
        }
        search.button("添加联系人") { open(null) }.isEnabled = !loadError
        list = content.card()
        query.doAfterTextChanged { render() }
        render()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("query", query.text.toString())
    }

    private fun open(contact: Contact?) {
        val intent = Intent(this, DetailActivity::class.java)
        contact?.let {
            intent.putExtra(ContactArgs.ID, it.id)
            intent.putExtra(ContactArgs.NAME, it.name)
            intent.putExtra(ContactArgs.PHONE, it.phone)
        }
        detail.launch(intent)
    }

    private fun render() {
        list.removeAllViews()
        val text = query.text.toString().trim()
        val digits = compactPhone(text)
        val shown = contacts.filter {
            it.name.contains(text, ignoreCase = true) ||
                (digits.isNotEmpty() && it.phone.contains(digits))
        }.sortedWith { first, second -> collator.compare(first.name, second.name) }
        list.heading("联系人 · ${shown.size}")
        when {
            loadError -> list.text("无法读取本地通讯录。")
            contacts.isEmpty() -> list.text("还没有联系人，请先添加。")
            shown.isEmpty() -> list.text("没有找到匹配的联系人。")
        }
        shown.forEach { contact ->
            val row = list.card()
            row.heading(contact.name)
            row.text(contact.phone)
            row.button("查看详情") { open(contact) }.contentDescription = "查看${contact.name}的详情"
        }
    }

    private fun save(newContacts: List<Contact>) {
        store.save(newContacts)
        contacts = newContacts
        render()
    }
}
