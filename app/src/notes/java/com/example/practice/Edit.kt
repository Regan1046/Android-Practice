package com.example.practice

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.widget.EditText

internal object NoteArgs {
    const val ID = "note_id"
    const val TITLE = "note_title"
    const val BODY = "note_body"
}

class EditActivity : PracticeActivity() {
    private lateinit var titleField: EditText
    private lateinit var bodyField: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = if (intent.hasExtra(NoteArgs.ID)) R.string.notes_edit else R.string.notes_new
        val form = page(getString(title)).card()
        titleField = form.field(getString(R.string.notes_title_hint)).apply {
            setSingleLine(true)
            filters = arrayOf(InputFilter.LengthFilter(80))
            setText(savedInstanceState?.getString(NoteArgs.TITLE) ?: intent.getStringExtra(NoteArgs.TITLE))
        }
        bodyField = form.field(
            getString(R.string.notes_body_hint),
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        ).apply {
            setSingleLine(false)
            minLines = 6
            gravity = Gravity.TOP or Gravity.START
            filters = arrayOf(InputFilter.LengthFilter(4000))
            setText(savedInstanceState?.getString(NoteArgs.BODY) ?: intent.getStringExtra(NoteArgs.BODY))
        }
        form.button(getString(R.string.notes_save), ::returnNote)
        form.button(getString(android.R.string.cancel)) { finish() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // 旋转时优先恢复编辑中的草稿，而不是再次读取启动时的旧值。
        outState.putString(NoteArgs.TITLE, titleField.text.toString())
        outState.putString(NoteArgs.BODY, bodyField.text.toString())
    }

    private fun returnNote() {
        val title = titleField.text.toString().trim()
        if (title.isBlank()) {
            titleField.error = getString(R.string.notes_title_error)
            return
        }
        val result = Intent()
            .putExtra(NoteArgs.ID, intent.getStringExtra(NoteArgs.ID))
            .putExtra(NoteArgs.TITLE, title)
            .putExtra(NoteArgs.BODY, bodyField.text.toString().trim())
        // 编辑页只返回改动。列表页在收到 RESULT_OK 后负责写入本地数据。
        setResult(Activity.RESULT_OK, result)
        finish()
    }
}
