package com.example.practice

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.widget.EditText
import android.widget.Toast

internal object ContactArgs {
    const val ID = "contact_id"
    const val NAME = "contact_name"
    const val PHONE = "contact_phone"
    const val DELETE = "delete_contact"
}

internal fun compactPhone(input: String): String = input.filterNot { it in " -()" }

class DetailActivity : PracticeActivity() {
    private lateinit var nameField: EditText
    private lateinit var phoneField: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getStringExtra(ContactArgs.ID)
        val form = page(if (id == null) "添加联系人" else "联系人详情").card()
        nameField = form.field("姓名").apply {
            filters = arrayOf(InputFilter.LengthFilter(80))
            setText(savedInstanceState?.getString(ContactArgs.NAME) ?: intent.getStringExtra(ContactArgs.NAME))
        }
        phoneField = form.field("电话号码", InputType.TYPE_CLASS_PHONE).apply {
            filters = arrayOf(InputFilter.LengthFilter(32))
            setText(savedInstanceState?.getString(ContactArgs.PHONE) ?: intent.getStringExtra(ContactArgs.PHONE))
        }
        form.button("保存") { save(id) }
        if (id != null) {
            form.button("打开拨号盘", ::dial)
            form.button("删除联系人") {
                AlertDialog.Builder(this)
                    .setMessage("确定删除该联系人？")
                    .setPositiveButton("删除") { _, _ ->
                        setResult(Activity.RESULT_OK, Intent()
                            .putExtra(ContactArgs.ID, id)
                            .putExtra(ContactArgs.DELETE, true))
                        finish()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }
        form.button("返回") { finish() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ContactArgs.NAME, nameField.text.toString())
        outState.putString(ContactArgs.PHONE, phoneField.text.toString())
    }

    private fun save(id: String?) {
        val name = nameField.text.toString().trim()
        if (name.isEmpty()) nameField.error = "请输入姓名"
        val phone = validPhone()
        if (name.isEmpty() || phone == null) return
        setResult(Activity.RESULT_OK, Intent()
            .putExtra(ContactArgs.ID, id)
            .putExtra(ContactArgs.NAME, name)
            .putExtra(ContactArgs.PHONE, phone))
        finish()
    }

    private fun dial() {
        val phone = validPhone() ?: return
        // ACTION_DIAL 只预填系统拨号盘，是否拨出由用户确认，无需电话权限。
        try {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "此设备未安装拨号应用。", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validPhone(): String? {
        val phone = compactPhone(phoneField.text.toString().trim())
        if (phone.matches(Regex("\\+?[0-9]{3,15}"))) return phone
        phoneField.error = "请输入 3 至 15 位数字，可带国际区号 +"
        return null
    }
}
