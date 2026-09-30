package com.grootcleaning.automation.engine

import com.grootcleaning.models.AutomationTask

class TaskQueue(private val tasks: List<AutomationTask>) {
    private var index = 0
    fun size() = tasks.size
    fun current(): AutomationTask? = tasks.getOrNull(index)
    fun currentIndex() = index
    fun setIndex(value: Int) { index = value.coerceIn(0, tasks.size) }
    fun moveNext(): AutomationTask? { index++; return current() }
}
