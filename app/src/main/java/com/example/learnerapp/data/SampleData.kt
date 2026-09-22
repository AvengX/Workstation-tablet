package com.example.learnerapp.data

import com.example.learnerapp.model.ChecklistItem
import com.example.learnerapp.model.Task
import com.example.learnerapp.model.TaskScheduleItem
import com.example.learnerapp.model.TaskStep

object SampleData {

    val sampleSteps: List<TaskStep> = listOf(
        TaskStep(
            stepNumber = 1,
            instruction = "Take the assigned item, box and packing materials.",
            photoPlaceholderLabel = "Step 1 Photo Placeholder:\nAssigned item, box, and packing materials"
        ),
        TaskStep(
            stepNumber = 2,
            instruction = "Check the item and quantity against the packing instruction.",
            photoPlaceholderLabel = "Step 2 Photo Placeholder:\nItem and packing instruction sheet"
        ),
        TaskStep(
            stepNumber = 3,
            instruction = "Place the item in the box. Add the required protective material.",
            photoPlaceholderLabel = "Step 3 Photo Placeholder:\nItem placed inside box with bubble wrap"
        ),
        TaskStep(
            stepNumber = 4,
            instruction = "Close and seal the box.",
            photoPlaceholderLabel = "Step 4 Photo Placeholder:\nBox flaps closed and taped with packing tape"
        ),
        TaskStep(
            stepNumber = 5,
            instruction = "Attach the supplied parcel label in the correct position.",
            photoPlaceholderLabel = "Step 5 Photo Placeholder:\nCompleted parcel with shipping label affixed"
        )
    )

    val sampleChecklist: List<ChecklistItem> = listOf(
        ChecklistItem(id = 1, text = "Correct item and quantity", isChecked = false),
        ChecklistItem(id = 2, text = "Item is protected", isChecked = false),
        ChecklistItem(id = 3, text = "Box is sealed", isChecked = false),
        ChecklistItem(id = 4, text = "Correct label is attached", isChecked = false)
    )

    val sampleSchedule: List<TaskScheduleItem> = listOf(
        TaskScheduleItem(timeCategory = "NOW", title = "Pack one parcel", isCurrent = true),
        TaskScheduleItem(timeCategory = "NEXT", title = "Sort supplies", isCurrent = false),
        TaskScheduleItem(timeCategory = "LATER", title = "Clean work area", isCurrent = false)
    )

    val sampleTask: Task = Task(
        id = "pack_one_parcel",
        title = "Pack one parcel",
        steps = sampleSteps,
        checklist = sampleChecklist,
        completionPhrase = "I have finished packing this parcel. Please check."
    )
}
