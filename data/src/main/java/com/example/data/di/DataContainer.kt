package com.example.data.di

import android.content.Context
import com.example.data.repository.ScheduleRepository

class DataContainer(context: Context) {
    val scheduleRepository = ScheduleRepository(context)
}
