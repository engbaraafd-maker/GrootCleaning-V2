package com.grootcleaning.data

import androidx.room.TypeConverter
import com.grootcleaning.models.Operation
import com.grootcleaning.models.RunStatus
import com.grootcleaning.models.StepStatus
import com.grootcleaning.models.TaskStatus
import com.grootcleaning.models.VerificationLevel
import com.grootcleaning.models.VerificationResult

class GrootConverters {
    @TypeConverter fun operationToString(value: Operation?): String? = value?.name
    @TypeConverter fun stringToOperation(value: String?): Operation? = value?.let(Operation::valueOf)
    @TypeConverter fun taskStatusToString(value: TaskStatus?): String? = value?.name
    @TypeConverter fun stringToTaskStatus(value: String?): TaskStatus? = value?.let(TaskStatus::valueOf)
    @TypeConverter fun stepStatusToString(value: StepStatus?): String? = value?.name
    @TypeConverter fun stringToStepStatus(value: String?): StepStatus? = value?.let(StepStatus::valueOf)
    @TypeConverter fun runStatusToString(value: RunStatus?): String? = value?.name
    @TypeConverter fun stringToRunStatus(value: String?): RunStatus? = value?.let(RunStatus::valueOf)
    @TypeConverter fun verificationLevelToString(value: VerificationLevel?): String? = value?.name
    @TypeConverter fun stringToVerificationLevel(value: String?): VerificationLevel? = value?.let(VerificationLevel::valueOf)
    @TypeConverter fun verificationResultToString(value: VerificationResult?): String? = value?.name
    @TypeConverter fun stringToVerificationResult(value: String?): VerificationResult? = value?.let(VerificationResult::valueOf)
}
