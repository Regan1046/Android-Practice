package com.example.practice

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal data class Expense(val id: Long, val title: String, val cents: Long)
internal data class Ledger(val expenses: List<Expense>, val cents: Long)

internal class ExpenseDb(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "expenses.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        // INTEGER 保存“分”，加总时不会引入 Double 的浮点误差。
        db.execSQL(
            """
            CREATE TABLE expense (
                _id INTEGER PRIMARY KEY,
                title TEXT NOT NULL,
                cents INTEGER NOT NULL CHECK(cents > 0)
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun save(id: Long?, title: String, cents: Long) {
        // ContentValues 提供列和值。WHERE 始终用 ? 参数，编辑时按主键定位。
        val values = ContentValues(2).apply {
            put("title", title)
            put("cents", cents)
        }
        if (id == null) writableDatabase.insertOrThrow("expense", null, values)
        else writableDatabase.update("expense", values, "_id = ?", arrayOf(id.toString()))
    }

    fun delete(id: Long) {
        writableDatabase.delete("expense", "_id = ?", arrayOf(id.toString()))
    }

    fun snapshot(write: (ExpenseDb) -> Unit): Ledger {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // 写入与随后的查询属于同一个事务，明细和合计来自同一份数据。
            write(this)
            val expenses = db.query("expense", arrayOf("_id", "title", "cents"), null,
                null, null, null, "_id DESC").use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(Expense(cursor.getLong(0), cursor.getString(1), cursor.getLong(2)))
                    }
                }
            }
            // SUM 对空表返回 NULL。COALESCE 将合计转换成 0。
            val totalCents = db.rawQuery("SELECT COALESCE(SUM(cents), 0) FROM expense", null)
                .use { cursor ->
                    cursor.moveToFirst()
                    cursor.getLong(0)
                }
            db.setTransactionSuccessful()
            return Ledger(expenses, totalCents)
        } finally {
            db.endTransaction()
        }
    }
}
