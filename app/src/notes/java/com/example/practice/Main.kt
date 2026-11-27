package com.example.practice

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import org.json.JSONException
import java.util.UUID

class MainActivity : PracticeActivity() {
    private val store by lazy { NoteStore(this) }
    private var notes = emptyList<Note>()
    private var loadError = false
    private lateinit var search: EditText
    private lateinit var list: LinearLayout

    // 每次创建 Activity 都注册回调，编辑期间若父页面重建，结果仍会交给新实例。
    private val editor = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data ?: return@registerForActivityResult
        if (result.resultCode != Activity.RESULT_OK || loadError) return@registerForActivityResult
        val title = data.getStringExtra(NoteArgs.TITLE)?.trim().orEmpty()
        if (title.isBlank()) return@registerForActivityResult
        val id = data.getStringExtra(NoteArgs.ID) ?: UUID.randomUUID().toString()
        val note = Note(id, title, data.getStringExtra(NoteArgs.BODY).orEmpty())
        // 编辑同一个 ID 会替换旧记录，并把它移到列表顶部。
        save(listOf(note) + notes.filter { it.id != id })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notes = try {
            store.read()
        } catch (_: JSONException) {
            loadError = true
            emptyList()
        } catch (_: ClassCastException) {
            loadError = true
            emptyList()
        }
        val content = page(getString(R.string.notes_title), getString(R.string.notes_desc))
        val controls = content.card()
        search = controls.field(getString(R.string.notes_search)).apply {
            setSingleLine(true)
            setText(savedInstanceState?.getString("query"))
            isEnabled = !loadError
        }
        controls.button(getString(R.string.notes_new)) { openEditor(null) }.isEnabled = !loadError
        list = content.card()
        search.doAfterTextChanged { render() }
        render()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("query", search.text.toString())
    }

    private fun openEditor(note: Note?) {
        val intent = Intent(this, EditActivity::class.java)
        if (note != null) {
            // 显式 Intent 指定应用内的编辑页，extras 提供回填所需的内容。
            intent.putExtra(NoteArgs.ID, note.id)
            intent.putExtra(NoteArgs.TITLE, note.title)
            intent.putExtra(NoteArgs.BODY, note.body)
        }
        editor.launch(intent)
    }

    private fun render() {
        list.removeAllViews()
        list.heading(getString(R.string.notes_results))
        val query = search.text.toString().trim()
        val shown = notes.filter {
            it.title.contains(query, ignoreCase = true) || it.body.contains(query, ignoreCase = true)
        }
        when {
            loadError -> list.text(getString(R.string.notes_error))
            notes.isEmpty() -> list.text(getString(R.string.notes_empty))
            shown.isEmpty() -> list.text(getString(R.string.notes_none))
        }
        shown.forEach { note ->
            val row = list.card()
            row.heading(note.title)
            if (note.body.isNotBlank()) row.text(note.body).apply {
                maxLines = 3
                ellipsize = TextUtils.TruncateAt.END
            }
            row.button(getString(R.string.notes_edit)) { openEditor(note) }.contentDescription =
                getString(R.string.notes_edit_name, note.title)
            row.button(getString(R.string.notes_delete)) { confirmDelete(note) }.contentDescription =
                getString(R.string.notes_delete_name, note.title)
        }
    }

    private fun confirmDelete(note: Note) {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.notes_confirm, note.title))
            .setPositiveButton(R.string.notes_delete) { _, _ ->
                save(notes.filter { it.id != note.id })
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun save(newNotes: List<Note>) {
        store.write(newNotes)
        notes = newNotes
        render()
    }
}
