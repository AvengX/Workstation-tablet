package com.example.learnerapp.data.local

import androidx.room.withTransaction
import com.example.learnerapp.data.local.entities.ChecklistItemEntity
import com.example.learnerapp.data.local.entities.ChecklistProgressEntity
import com.example.learnerapp.data.local.entities.ScheduleItemEntity
import com.example.learnerapp.data.local.entities.TaskEntity
import com.example.learnerapp.data.local.entities.TaskProgressEntity
import com.example.learnerapp.data.local.entities.TaskStepEntity
import java.time.LocalDate

object DatabaseSeeder {

    const val TASK_ID_PACK_ONE_PARCEL = "pack_one_parcel"
    const val TASK_ID_SORT_SUPPLIES = "sort_supplies"
    const val TASK_ID_CLEAN_WORK_AREA = "clean_work_area"

    val initialTask = TaskEntity(
        id = TASK_ID_PACK_ONE_PARCEL,
        title = "Pack one parcel",
        description = "Collect item and packing materials at workstation.",
        helpPhrase = "Please ask a member of staff for help with this step.",
        completionPhrase = "I have finished packing this parcel. Please check."
    )

    val initialSteps = listOf(
        TaskStepEntity(
            id = "pack_one_parcel_step_1",
            taskId = TASK_ID_PACK_ONE_PARCEL,
            stepNumber = 1,
            instruction = "Take the assigned item, box and packing materials.",
            imagePath = null
        ),
        TaskStepEntity(
            id = "pack_one_parcel_step_2",
            taskId = TASK_ID_PACK_ONE_PARCEL,
            stepNumber = 2,
            instruction = "Check the item and quantity against the packing instruction.",
            imagePath = null
        ),
        TaskStepEntity(
            id = "pack_one_parcel_step_3",
            taskId = TASK_ID_PACK_ONE_PARCEL,
            stepNumber = 3,
            instruction = "Place the item in the box. Add the required protective material.",
            imagePath = null
        ),
        TaskStepEntity(
            id = "pack_one_parcel_step_4",
            taskId = TASK_ID_PACK_ONE_PARCEL,
            stepNumber = 4,
            instruction = "Close and seal the box.",
            imagePath = null
        ),
        TaskStepEntity(
            id = "pack_one_parcel_step_5",
            taskId = TASK_ID_PACK_ONE_PARCEL,
            stepNumber = 5,
            instruction = "Attach the supplied parcel label in the correct position.",
            imagePath = null
        )
    )

    val initialChecklist = listOf(
        ChecklistItemEntity(id = 1, taskId = TASK_ID_PACK_ONE_PARCEL, order = 1, text = "Correct item and quantity"),
        ChecklistItemEntity(id = 2, taskId = TASK_ID_PACK_ONE_PARCEL, order = 2, text = "Item is protected"),
        ChecklistItemEntity(id = 3, taskId = TASK_ID_PACK_ONE_PARCEL, order = 3, text = "Box is sealed"),
        ChecklistItemEntity(id = 4, taskId = TASK_ID_PACK_ONE_PARCEL, order = 4, text = "Correct label is attached")
    )

    val initialSchedule: List<ScheduleItemEntity>
        get() = listOf(
            ScheduleItemEntity(id = "schedule_now", date = LocalDate.now().toString(), taskId = TASK_ID_PACK_ONE_PARCEL, order = 1, category = "NOW"),
            ScheduleItemEntity(id = "schedule_next", date = LocalDate.now().toString(), taskId = TASK_ID_SORT_SUPPLIES, order = 2, category = "NEXT"),
            ScheduleItemEntity(id = "schedule_later", date = LocalDate.now().toString(), taskId = TASK_ID_CLEAN_WORK_AREA, order = 3, category = "LATER")
        )

    val initialOtherTasks = listOf(
        TaskEntity(
            id = TASK_ID_SORT_SUPPLIES,
            title = "Sort supplies",
            description = "Upcoming workstation task.",
            helpPhrase = "Please ask a member of staff for help.",
            completionPhrase = "I have finished sorting supplies."
        ),
        TaskEntity(
            id = TASK_ID_CLEAN_WORK_AREA,
            title = "Clean work area",
            description = "End of shift workstation clean.",
            helpPhrase = "Please ask a member of staff for help.",
            completionPhrase = "I have cleaned my work area."
        )
    )

    /**
     * Seeds database with initial Phase 2 sample data if database is empty.
     */
    suspend fun seedIfEmpty(database: AppDatabase) {
        if (database.taskDao().getTaskCount() == 0) {
            database.withTransaction {
                database.taskDao().insertTask(initialTask)
                database.taskDao().insertTasks(initialOtherTasks)
                database.taskStepDao().insertSteps(initialSteps)
                database.checklistDao().insertItems(initialChecklist)
                database.scheduleDao().insertScheduleItems(initialSchedule)

                // Initialize progress
                database.progressDao().insertOrUpdateProgress(
                    TaskProgressEntity(
                        id = "progress_pack_one_parcel",
                        taskId = TASK_ID_PACK_ONE_PARCEL,
                        scheduleItemId = "schedule_now",
                        currentStep = 1,
                        status = "NOT_STARTED",
                        startedAt = null,
                        completedAt = null
                    )
                )

                val initialChecklistProgress = initialChecklist.map {
                    ChecklistProgressEntity(
                        taskId = TASK_ID_PACK_ONE_PARCEL,
                        checklistItemId = it.id,
                        isChecked = false
                    )
                }
                database.progressDao().insertAllChecklistProgress(initialChecklistProgress)
            }
        }
    }
}
