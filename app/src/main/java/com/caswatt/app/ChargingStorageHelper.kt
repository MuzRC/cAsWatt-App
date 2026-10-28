package com.caswatt.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ChargingStorageHelper {
    private const val PREFS_NAME = "caswatt_prefs"
    private const val KEY_DAILY_LOGS = "daily_charging_logs"
    private const val KEY_SESSION_LOGS = "session_charging_logs"

    fun loadDailyRecords(context: Context, targetList: MutableList<DailyRecord>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_DAILY_LOGS, null) ?: return
        try {
            targetList.clear()
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                targetList.add(
                    DailyRecord(
                        date = obj.getString("date"),
                        count = obj.getInt("count"),
                        totalMah = obj.getDouble("totalMah"),
                        peakWatt = obj.getDouble("peakWatt")
                    )
                )
            }
        } catch (_: Exception) {}
    }

    fun saveDailyRecords(context: Context, records: List<DailyRecord>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        for (rec in records) {
            array.put(JSONObject().apply {
                put("date", rec.date)
                put("count", rec.count)
                put("totalMah", rec.totalMah)
                put("peakWatt", rec.peakWatt)
            })
        }
        prefs.edit().putString(KEY_DAILY_LOGS, array.toString()).apply()
    }

    fun loadSessionRecords(context: Context, targetList: MutableList<SessionRecord>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_SESSION_LOGS, null) ?: return
        try {
            targetList.clear()
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val sTime = obj.getLong("startTime")
                val eTime = obj.getLong("endTime")

                // Tapis sesi hantu 1 Jan 1970 atau rekod rosak
                if (sTime < 1577836800000L || eTime <= sTime) continue

                targetList.add(
                    SessionRecord(
                        id = obj.optLong("id", sTime),
                        startTime = sTime,
                        endTime = eTime,
                        startLevel = obj.getInt("startLevel"),
                        endLevel = obj.getInt("endLevel"),
                        peakWatt = obj.getDouble("peakWatt"),
                        totalMah = obj.getDouble("totalMah"),
                        energyWh = obj.getDouble("energyWh"),
                        costMyr = obj.optDouble("costMyr", 0.0),
                        cableQuality = obj.optString("cableQuality", "Standard"),
                        avgWatt = obj.optDouble("avgWatt", 0.0),
                        startTemp = obj.optDouble("startTemp", 0.0),
                        endTemp = obj.optDouble("endTemp", 0.0)
                    )
                )
            }
        } catch (_: Exception) {}
    }

    fun saveSessionRecords(context: Context, records: List<SessionRecord>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        for (rec in records) {
            array.put(JSONObject().apply {
                put("id", rec.id)
                put("startTime", rec.startTime)
                put("endTime", rec.endTime)
                put("startLevel", rec.startLevel)
                put("endLevel", rec.endLevel)
                put("peakWatt", rec.peakWatt)
                put("totalMah", rec.totalMah)
                put("energyWh", rec.energyWh)
                put("costMyr", rec.costMyr)
                put("cableQuality", rec.cableQuality)
                put("avgWatt", rec.avgWatt)
                put("startTemp", rec.startTemp)
                put("endTemp", rec.endTemp)
            })
        }
        prefs.edit().putString(KEY_SESSION_LOGS, array.toString()).apply()
    }
}
