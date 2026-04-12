package com.sporadic.reminder.domain.usecase

import android.content.Context
import android.net.Uri
import javax.inject.Inject

class ImportDataUseCase @Inject constructor() {
    suspend fun import(context: Context, uri: Uri, merge: Boolean) {
        TODO("Implemented in Task 17")
    }
}
