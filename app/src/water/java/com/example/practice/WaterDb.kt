package com.example.practice

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class WaterDay(val date: String, val milliliters: Int)

internal fun dayFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    isLenient = false
}

internal fun parseDay(date: String): Date? {
    if (!date.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}"))) return null
    // 用 UTC 把纯日期映射到折线坐标，日期间距不受夏令时影响。
    val format = dayFormat().apply { timeZone = TimeZone.getTimeZone("UTC") }
    return runCatching { format.parse(date) }.getOrNull()
}

class WaterDb(context: Context) : SQLiteOpenHelper(context.applicationContext, "water.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE water (day TEXT PRIMARY KEY NOT NULL, ml INTEGER NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        error("Missing migration from $oldVersion to $newVersion")
    }

    fun save(entry: WaterDay) {
        val values = ContentValues().apply {
            put("day", entry.date)
            put("ml", entry.milliliters)
        }
        // day 是日期主键，REPLACE 在一条语句里实现当天的新增或更新。
        writableDatabase.insertWithOnConflict("water", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            .also { if (it == -1L) throw SQLiteException("Save failed") }
    }

    fun recent(): List<WaterDay> =
        readableDatabase.query("water", arrayOf("day", "ml"), null, null, null, null, "day DESC", "7")
            .use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(WaterDay(cursor.getString(0), cursor.getInt(1)))
                }.asReversed()
            }
}
