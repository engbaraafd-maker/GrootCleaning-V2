package com.grootcleaning.automation.detection

object TextCatalog {
    val storage = listOf("storage", "storage & cache", "storage and cache", "التخزين", "مساحة التخزين", "التخزين وذاكرة التخزين المؤقت")
    val clearCache = listOf("clear cache", "clear cached data", "مسح ذاكرة التخزين المؤقت", "محو ذاكرة التخزين المؤقت")
    val clearData = listOf("clear data", "clear storage", "مسح البيانات", "محو البيانات", "مسح مساحة التخزين", "محو مساحة التخزين")
    val manageSpace = listOf("manage space", "manage storage", "إدارة المساحة", "إدارة التخزين")
    val all = listOf("all", "الكل")
    val clearAllData = listOf("clear all data", "clear all", "محو جميع البيانات", "مسح جميع البيانات")
    val removeAccount = listOf("remove account", "إزالة الحساب")
    val confirm = listOf("ok", "confirm", "yes", "delete", "موافق", "تأكيد", "نعم", "حذف")
    val cancel = listOf("cancel", "إلغاء", "لا")
}

fun normalizeUiText(value: CharSequence?): String = value?.toString()?.trim()?.lowercase()?.replace(Regex("\\s+"), " ") ?: ""

fun containsAny(text: String, options: List<String>): Boolean {
    val n = normalizeUiText(text)
    return options.any { n == normalizeUiText(it) || n.contains(normalizeUiText(it)) }
}
