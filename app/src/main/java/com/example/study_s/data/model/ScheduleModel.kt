
package com.example.study_s.data.model

data class ScheduleModel(
    val scheduleId: String = "",
    val userId: String = "",
    val content: String = "",
    val year: Int = 0,
    val month: Int = 0,
    val day: Int = 0,
    val hour: Int = 0,
    val minute: Int = 0,
    val needsDailySummary: Boolean = false
)
