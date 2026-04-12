# Sporadic Reminder App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a native Android 14+ app that delivers randomized reminders with full user customization, group management, notification actions (Done/Skip/Snooze), and a dashboard tracking activity.

**Architecture:** MVVM + Clean Architecture with four layers: UI (Jetpack Compose) -> ViewModel (StateFlow) -> Domain (Use Cases) -> Data (Room + System APIs). Scheduling uses AlarmManager with exact alarms, regenerated daily. Hilt for DI throughout.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3 (dynamic colors), Room, Hilt, AlarmManager, Navigation Compose

---

## File Structure

```
sporadic-reminder-app/
  build.gradle.kts                          # Project-level Gradle (plugins, versions)
  settings.gradle.kts                       # Module includes
  gradle.properties                         # JVM args, AndroidX opt-ins
  app/
    build.gradle.kts                        # App module dependencies
    src/
      main/
        AndroidManifest.xml
        java/com/sporadic/reminder/
          SporadicApp.kt                    # Hilt Application class
          MainActivity.kt                   # Single activity, hosts Compose
          di/
            AppModule.kt                    # Hilt bindings for use cases, schedulers
            DatabaseModule.kt               # Room database + DAO providers
          data/
            db/
              SporadicDatabase.kt           # Room database definition
              Converters.kt                 # TypeConverters for enums, LocalTime, Instant
            entity/
              ReminderEntity.kt
              ReminderGroupEntity.kt
              NotificationLogEntity.kt
              ScheduledAlarmEntity.kt
            dao/
              ReminderDao.kt
              ReminderGroupDao.kt
              NotificationLogDao.kt
              ScheduledAlarmDao.kt
            repository/
              ReminderRepository.kt
              GroupRepository.kt
              NotificationLogRepository.kt
              ScheduledAlarmRepository.kt
          domain/
            model/
              Priority.kt                   # Enum: LOW, DEFAULT, HIGH, URGENT
              DndBehavior.kt                # Enum: SKIP, SNOOZE
              NotificationAction.kt         # Enum: DONE, SKIPPED, SNOOZED
              AlarmStatus.kt                # Enum: PENDING, FIRED, SKIPPED, SNOOZED
            usecase/
              ScheduleRemindersUseCase.kt
              HandleNotificationActionUseCase.kt
              SyncGroupScheduleUseCase.kt
              ExportDataUseCase.kt
              ImportDataUseCase.kt
          scheduler/
            AlarmScheduler.kt               # AlarmManager wrapper
            BootReceiver.kt                 # BOOT_COMPLETED receiver
            DailySchedulerReceiver.kt       # Daily 4am regeneration trigger
            AlarmReceiver.kt                # Fires when a scheduled alarm triggers
          notification/
            NotificationPublisher.kt        # Builds and posts notifications
            DndStateChecker.kt              # Queries system DND state
            NotificationActionReceiver.kt   # Handles Done/Skip/Snooze from notification
            NotificationChannelManager.kt   # Creates/manages notification channels
          ui/
            navigation/
              SporadicNavGraph.kt           # Navigation routes and graph
              Screen.kt                     # Sealed class of routes
            theme/
              SporadicTheme.kt              # Material 3 theme with dynamic colors
            dashboard/
              DashboardScreen.kt
              DashboardViewModel.kt
            reminders/
              ReminderListScreen.kt
              ReminderListViewModel.kt
              ReminderEditScreen.kt
              ReminderEditViewModel.kt
            groups/
              GroupEditScreen.kt
              GroupEditViewModel.kt
            settings/
              SettingsScreen.kt
              SettingsViewModel.kt
          export/
            ExportImportManager.kt          # SAF file picker + JSON serialization
            ExportSchema.kt                 # JSON schema version and structure
        res/
          values/
            strings.xml
      test/
        java/com/sporadic/reminder/
          domain/usecase/
            ScheduleRemindersUseCaseTest.kt
            HandleNotificationActionUseCaseTest.kt
            SyncGroupScheduleUseCaseTest.kt
            ExportDataUseCaseTest.kt
            ImportDataUseCaseTest.kt
          scheduler/
            AlarmSchedulerTest.kt
          notification/
            DndStateCheckerTest.kt
          data/repository/
            ReminderRepositoryTest.kt
            GroupRepositoryTest.kt
      androidTest/
        java/com/sporadic/reminder/
          data/dao/
            ReminderDaoTest.kt
            ReminderGroupDaoTest.kt
            NotificationLogDaoTest.kt
            ScheduledAlarmDaoTest.kt
```

---

## Task 1: Project Scaffolding

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts` (project-level)
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/sporadic/reminder/SporadicApp.kt`
- Create: `app/src/main/java/com/sporadic/reminder/MainActivity.kt`
- Create: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Create project-level Gradle files**

`settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SporadicReminder"
include(":app")
```

`build.gradle.kts` (project root):
```kotlin
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.1.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.10" apply false
    id("com.google.devtools.ksp") version "2.1.10-1.0.31" apply false
    id("com.google.dagger.hilt.android") version "2.56.1" apply false
}
```

`gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 2: Create app-level build.gradle.kts**

`app/build.gradle.kts`:
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.sporadic.reminder"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sporadic.reminder"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2025.03.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Compose
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")

    // Room
    implementation("androidx.room:room-runtime:2.7.0")
    implementation("androidx.room:room-ktx:2.7.0")
    ksp("androidx.room:room-compiler:2.7.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.56.1")
    ksp("com.google.dagger:hilt-compiler:2.56.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Core
    implementation("androidx.core:core-ktx:1.16.0")

    // Serialization for export/import
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("io.mockk:mockk:1.13.16")
    testImplementation("com.google.truth:truth:1.4.4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.room:room-testing:2.7.0")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("com.google.truth:truth:1.4.4")
}
```

- [ ] **Step 3: Create AndroidManifest.xml**

`app/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY" />

    <application
        android:name=".SporadicApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.SporadicReminder">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.SporadicReminder">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <receiver
            android:name=".scheduler.BootReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>

        <receiver
            android:name=".scheduler.DailySchedulerReceiver"
            android:exported="false" />

        <receiver
            android:name=".scheduler.AlarmReceiver"
            android:exported="false" />

        <receiver
            android:name=".notification.NotificationActionReceiver"
            android:exported="false" />
    </application>
</manifest>
```

- [ ] **Step 4: Create Application class and MainActivity**

`app/src/main/java/com/sporadic/reminder/SporadicApp.kt`:
```kotlin
package com.sporadic.reminder

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SporadicApp : Application()
```

`app/src/main/java/com/sporadic/reminder/MainActivity.kt`:
```kotlin
package com.sporadic.reminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sporadic.reminder.ui.theme.SporadicTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SporadicTheme {
                // NavGraph will be added in Task 14
            }
        }
    }
}
```

`app/src/main/res/values/strings.xml`:
```xml
<resources>
    <string name="app_name">Sporadic</string>
</resources>
```

- [ ] **Step 5: Verify project builds**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/ build.gradle.kts settings.gradle.kts gradle.properties
git commit -m "feat: scaffold Android project with Compose, Room, Hilt dependencies"
```

---

## Task 2: Domain Enums

**Files:**
- Create: `app/src/main/java/com/sporadic/reminder/domain/model/Priority.kt`
- Create: `app/src/main/java/com/sporadic/reminder/domain/model/DndBehavior.kt`
- Create: `app/src/main/java/com/sporadic/reminder/domain/model/NotificationAction.kt`
- Create: `app/src/main/java/com/sporadic/reminder/domain/model/AlarmStatus.kt`

- [ ] **Step 1: Create enum classes**

`Priority.kt`:
```kotlin
package com.sporadic.reminder.domain.model

import android.app.NotificationManager

enum class Priority(val importance: Int) {
    LOW(NotificationManager.IMPORTANCE_LOW),
    DEFAULT(NotificationManager.IMPORTANCE_DEFAULT),
    HIGH(NotificationManager.IMPORTANCE_HIGH),
    URGENT(NotificationManager.IMPORTANCE_MAX);
}
```

`DndBehavior.kt`:
```kotlin
package com.sporadic.reminder.domain.model

enum class DndBehavior {
    SKIP,
    SNOOZE
}
```

`NotificationAction.kt`:
```kotlin
package com.sporadic.reminder.domain.model

enum class NotificationAction {
    DONE,
    SKIPPED,
    SNOOZED
}
```

`AlarmStatus.kt`:
```kotlin
package com.sporadic.reminder.domain.model

enum class AlarmStatus {
    PENDING,
    FIRED,
    SKIPPED,
    SNOOZED
}
```

- [ ] **Step 2: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/sporadic/reminder/domain/model/
git commit -m "feat: add domain enums (Priority, DndBehavior, NotificationAction, AlarmStatus)"
```

---

## Task 3: Room Entities and Type Converters

**Files:**
- Create: `app/src/main/java/com/sporadic/reminder/data/entity/ReminderEntity.kt`
- Create: `app/src/main/java/com/sporadic/reminder/data/entity/ReminderGroupEntity.kt`
- Create: `app/src/main/java/com/sporadic/reminder/data/entity/NotificationLogEntity.kt`
- Create: `app/src/main/java/com/sporadic/reminder/data/entity/ScheduledAlarmEntity.kt`
- Create: `app/src/main/java/com/sporadic/reminder/data/db/Converters.kt`

- [ ] **Step 1: Create type converters**

`Converters.kt`:
```kotlin
package com.sporadic.reminder.data.db

import androidx.room.TypeConverter
import com.sporadic.reminder.domain.model.AlarmStatus
import com.sporadic.reminder.domain.model.DndBehavior
import com.sporadic.reminder.domain.model.NotificationAction
import com.sporadic.reminder.domain.model.Priority
import java.time.Instant
import java.time.LocalTime

class Converters {
    @TypeConverter
    fun fromLocalTime(value: LocalTime?): String? = value?.toString()

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let { LocalTime.parse(it) }

    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun fromPriority(value: Priority): String = value.name

    @TypeConverter
    fun toPriority(value: String): Priority = Priority.valueOf(value)

    @TypeConverter
    fun fromDndBehavior(value: DndBehavior): String = value.name

    @TypeConverter
    fun toDndBehavior(value: String): DndBehavior = DndBehavior.valueOf(value)

    @TypeConverter
    fun fromNotificationAction(value: NotificationAction): String = value.name

    @TypeConverter
    fun toNotificationAction(value: String): NotificationAction = NotificationAction.valueOf(value)

    @TypeConverter
    fun fromAlarmStatus(value: AlarmStatus): String = value.name

    @TypeConverter
    fun toAlarmStatus(value: String): AlarmStatus = AlarmStatus.valueOf(value)
}
```

- [ ] **Step 2: Create entity classes**

`ReminderEntity.kt`:
```kotlin
package com.sporadic.reminder.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sporadic.reminder.domain.model.DndBehavior
import com.sporadic.reminder.domain.model.Priority
import java.time.LocalTime

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = ReminderGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("groupId")]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val notificationText: String,
    val notificationToneUri: String?,
    val vibrate: Boolean,
    val priority: Priority,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val notificationCount: Int,
    val activeDays: Int,
    val dndBehavior: DndBehavior,
    val isActive: Boolean,
    val groupId: Long?
)
```

`ReminderGroupEntity.kt`:
```kotlin
package com.sporadic.reminder.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalTime

@Entity(tableName = "reminder_groups")
data class ReminderGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val notificationCount: Int?,
    val activeDays: Int?
)
```

`NotificationLogEntity.kt`:
```kotlin
package com.sporadic.reminder.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sporadic.reminder.domain.model.NotificationAction
import java.time.Instant

@Entity(
    tableName = "notification_logs",
    foreignKeys = [
        ForeignKey(
            entity = ReminderEntity::class,
            parentColumns = ["id"],
            childColumns = ["reminderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("reminderId")]
)
data class NotificationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reminderId: Long,
    val scheduledTime: Instant,
    val firedTime: Instant,
    val action: NotificationAction?,
    val actionTimestamp: Instant?
)
```

`ScheduledAlarmEntity.kt`:
```kotlin
package com.sporadic.reminder.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sporadic.reminder.domain.model.AlarmStatus
import java.time.Instant

@Entity(
    tableName = "scheduled_alarms",
    foreignKeys = [
        ForeignKey(
            entity = ReminderEntity::class,
            parentColumns = ["id"],
            childColumns = ["reminderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("reminderId")]
)
data class ScheduledAlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reminderId: Long,
    val scheduledTime: Instant,
    val status: AlarmStatus
)
```

- [ ] **Step 3: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/sporadic/reminder/data/
git commit -m "feat: add Room entities and type converters"
```

---

## Task 4: Room Database and DAOs

**Files:**
- Create: `app/src/main/java/com/sporadic/reminder/data/dao/ReminderDao.kt`
- Create: `app/src/main/java/com/sporadic/reminder/data/dao/ReminderGroupDao.kt`
- Create: `app/src/main/java/com/sporadic/reminder/data/dao/NotificationLogDao.kt`
- Create: `app/src/main/java/com/sporadic/reminder/data/dao/ScheduledAlarmDao.kt`
- Create: `app/src/main/java/com/sporadic/reminder/data/db/SporadicDatabase.kt`
- Test: `app/src/androidTest/java/com/sporadic/reminder/data/dao/ReminderDaoTest.kt`
- Test: `app/src/androidTest/java/com/sporadic/reminder/data/dao/ScheduledAlarmDaoTest.kt`

- [ ] **Step 1: Create DAOs**

`ReminderDao.kt`:
```kotlin
package com.sporadic.reminder.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sporadic.reminder.data.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY name ASC")
    fun getAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE isActive = 1")
    suspend fun getActiveReminders(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE groupId = :groupId")
    suspend fun getByGroupId(groupId: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE isActive = 1 AND (activeDays & :dayBit) != 0")
    suspend fun getActiveForDay(dayBit: Int): List<ReminderEntity>

    @Insert
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Delete
    suspend fun delete(reminder: ReminderEntity)

    @Query("SELECT * FROM reminders")
    suspend fun getAllSync(): List<ReminderEntity>
}
```

`ReminderGroupDao.kt`:
```kotlin
package com.sporadic.reminder.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sporadic.reminder.data.entity.ReminderGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderGroupDao {
    @Query("SELECT * FROM reminder_groups ORDER BY name ASC")
    fun getAll(): Flow<List<ReminderGroupEntity>>

    @Query("SELECT * FROM reminder_groups WHERE id = :id")
    suspend fun getById(id: Long): ReminderGroupEntity?

    @Insert
    suspend fun insert(group: ReminderGroupEntity): Long

    @Update
    suspend fun update(group: ReminderGroupEntity)

    @Delete
    suspend fun delete(group: ReminderGroupEntity)

    @Query("SELECT * FROM reminder_groups")
    suspend fun getAllSync(): List<ReminderGroupEntity>
}
```

`NotificationLogDao.kt`:
```kotlin
package com.sporadic.reminder.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sporadic.reminder.data.entity.NotificationLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationLogDao {
    @Query("""
        SELECT * FROM notification_logs
        WHERE firedTime >= :startOfDay AND firedTime < :endOfDay
        ORDER BY firedTime DESC
    """)
    fun getLogsForDay(startOfDay: Long, endOfDay: Long): Flow<List<NotificationLogEntity>>

    @Query("""
        SELECT * FROM notification_logs
        WHERE reminderId = :reminderId
        ORDER BY firedTime DESC
        LIMIT :limit
    """)
    suspend fun getRecentForReminder(reminderId: Long, limit: Int = 50): List<NotificationLogEntity>

    @Insert
    suspend fun insert(log: NotificationLogEntity): Long

    @Update
    suspend fun update(log: NotificationLogEntity)

    @Query("SELECT * FROM notification_logs WHERE id = :id")
    suspend fun getById(id: Long): NotificationLogEntity?

    @Query("SELECT * FROM notification_logs")
    suspend fun getAllSync(): List<NotificationLogEntity>
}
```

`ScheduledAlarmDao.kt`:
```kotlin
package com.sporadic.reminder.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sporadic.reminder.data.entity.ScheduledAlarmEntity
import com.sporadic.reminder.domain.model.AlarmStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledAlarmDao {
    @Query("SELECT * FROM scheduled_alarms WHERE status = :status ORDER BY scheduledTime ASC")
    suspend fun getByStatus(status: AlarmStatus): List<ScheduledAlarmEntity>

    @Query("""
        SELECT * FROM scheduled_alarms
        WHERE scheduledTime >= :startOfDay AND scheduledTime < :endOfDay
        ORDER BY scheduledTime ASC
    """)
    fun getAlarmsForDay(startOfDay: Long, endOfDay: Long): Flow<List<ScheduledAlarmEntity>>

    @Query("SELECT * FROM scheduled_alarms WHERE id = :id")
    suspend fun getById(id: Long): ScheduledAlarmEntity?

    @Query("SELECT * FROM scheduled_alarms WHERE status = 'PENDING' AND scheduledTime > :now")
    suspend fun getPendingFutureAlarms(now: Long): List<ScheduledAlarmEntity>

    @Insert
    suspend fun insert(alarm: ScheduledAlarmEntity): Long

    @Insert
    suspend fun insertAll(alarms: List<ScheduledAlarmEntity>): List<Long>

    @Update
    suspend fun update(alarm: ScheduledAlarmEntity)

    @Query("DELETE FROM scheduled_alarms WHERE scheduledTime >= :startOfDay AND scheduledTime < :endOfDay")
    suspend fun deleteAlarmsForDay(startOfDay: Long, endOfDay: Long)

    @Query("DELETE FROM scheduled_alarms WHERE reminderId = :reminderId AND status = 'PENDING'")
    suspend fun deletePendingForReminder(reminderId: Long)
}
```

- [ ] **Step 2: Create Room database**

`SporadicDatabase.kt`:
```kotlin
package com.sporadic.reminder.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sporadic.reminder.data.dao.NotificationLogDao
import com.sporadic.reminder.data.dao.ReminderDao
import com.sporadic.reminder.data.dao.ReminderGroupDao
import com.sporadic.reminder.data.dao.ScheduledAlarmDao
import com.sporadic.reminder.data.entity.NotificationLogEntity
import com.sporadic.reminder.data.entity.ReminderEntity
import com.sporadic.reminder.data.entity.ReminderGroupEntity
import com.sporadic.reminder.data.entity.ScheduledAlarmEntity

@Database(
    entities = [
        ReminderEntity::class,
        ReminderGroupEntity::class,
        NotificationLogEntity::class,
        ScheduledAlarmEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class SporadicDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun reminderGroupDao(): ReminderGroupDao
    abstract fun notificationLogDao(): NotificationLogDao
    abstract fun scheduledAlarmDao(): ScheduledAlarmDao
}
```

- [ ] **Step 3: Write DAO instrumented tests**

`app/src/androidTest/java/com/sporadic/reminder/data/dao/ReminderDaoTest.kt`:
```kotlin
package com.sporadic.reminder.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.sporadic.reminder.data.db.SporadicDatabase
import com.sporadic.reminder.data.entity.ReminderEntity
import com.sporadic.reminder.domain.model.DndBehavior
import com.sporadic.reminder.domain.model.Priority
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class ReminderDaoTest {
    private lateinit var database: SporadicDatabase
    private lateinit var dao: ReminderDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SporadicDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.reminderDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    private fun createReminder(
        name: String = "Test",
        isActive: Boolean = true,
        activeDays: Int = 0b1111111,
        groupId: Long? = null
    ) = ReminderEntity(
        name = name,
        notificationText = "Test notification",
        notificationToneUri = null,
        vibrate = true,
        priority = Priority.DEFAULT,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(17, 0),
        notificationCount = 5,
        activeDays = activeDays,
        dndBehavior = DndBehavior.SKIP,
        isActive = isActive,
        groupId = groupId
    )

    @Test
    fun insertAndRetrieve() = runTest {
        val id = dao.insert(createReminder(name = "Water"))
        val result = dao.getById(id)
        assertThat(result).isNotNull()
        assertThat(result!!.name).isEqualTo("Water")
    }

    @Test
    fun getAllReturnsFlowSortedByName() = runTest {
        dao.insert(createReminder(name = "Zebra"))
        dao.insert(createReminder(name = "Apple"))
        val results = dao.getAll().first()
        assertThat(results.map { it.name }).isEqualTo(listOf("Apple", "Zebra"))
    }

    @Test
    fun getActiveForDay() = runTest {
        val monday = 0b0000001
        val tuesday = 0b0000010
        dao.insert(createReminder(name = "Mon", activeDays = monday, isActive = true))
        dao.insert(createReminder(name = "Tue", activeDays = tuesday, isActive = true))
        dao.insert(createReminder(name = "MonInactive", activeDays = monday, isActive = false))

        val results = dao.getActiveForDay(monday)
        assertThat(results).hasSize(1)
        assertThat(results[0].name).isEqualTo("Mon")
    }

    @Test
    fun deleteRemovesReminder() = runTest {
        val id = dao.insert(createReminder())
        val entity = dao.getById(id)!!
        dao.delete(entity)
        assertThat(dao.getById(id)).isNull()
    }
}
```

`app/src/androidTest/java/com/sporadic/reminder/data/dao/ScheduledAlarmDaoTest.kt`:
```kotlin
package com.sporadic.reminder.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.sporadic.reminder.data.db.SporadicDatabase
import com.sporadic.reminder.data.entity.ReminderEntity
import com.sporadic.reminder.data.entity.ScheduledAlarmEntity
import com.sporadic.reminder.domain.model.AlarmStatus
import com.sporadic.reminder.domain.model.DndBehavior
import com.sporadic.reminder.domain.model.Priority
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class ScheduledAlarmDaoTest {
    private lateinit var database: SporadicDatabase
    private lateinit var alarmDao: ScheduledAlarmDao
    private lateinit var reminderDao: ReminderDao
    private var reminderId: Long = 0

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SporadicDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        alarmDao = database.scheduledAlarmDao()
        reminderDao = database.reminderDao()
        reminderId = reminderDao.insert(
            ReminderEntity(
                name = "Test",
                notificationText = "Test",
                notificationToneUri = null,
                vibrate = false,
                priority = Priority.DEFAULT,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(17, 0),
                notificationCount = 3,
                activeDays = 0b1111111,
                dndBehavior = DndBehavior.SKIP,
                isActive = true,
                groupId = null
            )
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun getPendingFutureAlarms() = runTest {
        val now = Instant.now()
        val future = now.plusSeconds(3600)
        val past = now.minusSeconds(3600)

        alarmDao.insert(ScheduledAlarmEntity(reminderId = reminderId, scheduledTime = future, status = AlarmStatus.PENDING))
        alarmDao.insert(ScheduledAlarmEntity(reminderId = reminderId, scheduledTime = past, status = AlarmStatus.PENDING))
        alarmDao.insert(ScheduledAlarmEntity(reminderId = reminderId, scheduledTime = future, status = AlarmStatus.FIRED))

        val results = alarmDao.getPendingFutureAlarms(now.toEpochMilli())
        assertThat(results).hasSize(1)
        assertThat(results[0].status).isEqualTo(AlarmStatus.PENDING)
    }

    @Test
    fun deletePendingForReminder() = runTest {
        val time = Instant.now().plusSeconds(3600)
        alarmDao.insert(ScheduledAlarmEntity(reminderId = reminderId, scheduledTime = time, status = AlarmStatus.PENDING))
        alarmDao.insert(ScheduledAlarmEntity(reminderId = reminderId, scheduledTime = time, status = AlarmStatus.FIRED))

        alarmDao.deletePendingForReminder(reminderId)

        val pending = alarmDao.getByStatus(AlarmStatus.PENDING)
        val fired = alarmDao.getByStatus(AlarmStatus.FIRED)
        assertThat(pending).isEmpty()
        assertThat(fired).hasSize(1)
    }
}
```

- [ ] **Step 4: Run instrumented tests**

Run: `./gradlew connectedDebugAndroidTest`
Expected: All tests PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/sporadic/reminder/data/dao/ app/src/main/java/com/sporadic/reminder/data/db/SporadicDatabase.kt app/src/androidTest/
git commit -m "feat: add Room database, DAOs, and instrumented tests"
```

---

## Task 5: Repositories

**Files:**
- Create: `app/src/main/java/com/sporadic/reminder/data/repository/ReminderRepository.kt`
- Create: `app/src/main/java/com/sporadic/reminder/data/repository/GroupRepository.kt`
- Create: `app/src/main/java/com/sporadic/reminder/data/repository/NotificationLogRepository.kt`
- Create: `app/src/main/java/com/sporadic/reminder/data/repository/ScheduledAlarmRepository.kt`

- [ ] **Step 1: Create repositories**

Each repository is a thin wrapper over its DAO, exposing suspend functions and Flows. This keeps the DAO contract isolated from the domain layer.

`ReminderRepository.kt`:
```kotlin
package com.sporadic.reminder.data.repository

import com.sporadic.reminder.data.dao.ReminderDao
import com.sporadic.reminder.data.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    private val dao: ReminderDao
) {
    fun getAll(): Flow<List<ReminderEntity>> = dao.getAll()
    suspend fun getById(id: Long): ReminderEntity? = dao.getById(id)
    suspend fun getActiveForDay(dayBit: Int): List<ReminderEntity> = dao.getActiveForDay(dayBit)
    suspend fun getByGroupId(groupId: Long): List<ReminderEntity> = dao.getByGroupId(groupId)
    suspend fun insert(reminder: ReminderEntity): Long = dao.insert(reminder)
    suspend fun update(reminder: ReminderEntity) = dao.update(reminder)
    suspend fun delete(reminder: ReminderEntity) = dao.delete(reminder)
    suspend fun getAllSync(): List<ReminderEntity> = dao.getAllSync()
}
```

`GroupRepository.kt`:
```kotlin
package com.sporadic.reminder.data.repository

import com.sporadic.reminder.data.dao.ReminderGroupDao
import com.sporadic.reminder.data.entity.ReminderGroupEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor(
    private val dao: ReminderGroupDao
) {
    fun getAll(): Flow<List<ReminderGroupEntity>> = dao.getAll()
    suspend fun getById(id: Long): ReminderGroupEntity? = dao.getById(id)
    suspend fun insert(group: ReminderGroupEntity): Long = dao.insert(group)
    suspend fun update(group: ReminderGroupEntity) = dao.update(group)
    suspend fun delete(group: ReminderGroupEntity) = dao.delete(group)
    suspend fun getAllSync(): List<ReminderGroupEntity> = dao.getAllSync()
}
```

`NotificationLogRepository.kt`:
```kotlin
package com.sporadic.reminder.data.repository

import com.sporadic.reminder.data.dao.NotificationLogDao
import com.sporadic.reminder.data.entity.NotificationLogEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationLogRepository @Inject constructor(
    private val dao: NotificationLogDao
) {
    fun getLogsForDay(startOfDay: Long, endOfDay: Long): Flow<List<NotificationLogEntity>> =
        dao.getLogsForDay(startOfDay, endOfDay)
    suspend fun getRecentForReminder(reminderId: Long, limit: Int = 50): List<NotificationLogEntity> =
        dao.getRecentForReminder(reminderId, limit)
    suspend fun insert(log: NotificationLogEntity): Long = dao.insert(log)
    suspend fun update(log: NotificationLogEntity) = dao.update(log)
    suspend fun getById(id: Long): NotificationLogEntity? = dao.getById(id)
    suspend fun getAllSync(): List<NotificationLogEntity> = dao.getAllSync()
}
```

`ScheduledAlarmRepository.kt`:
```kotlin
package com.sporadic.reminder.data.repository

import com.sporadic.reminder.data.dao.ScheduledAlarmDao
import com.sporadic.reminder.data.entity.ScheduledAlarmEntity
import com.sporadic.reminder.domain.model.AlarmStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduledAlarmRepository @Inject constructor(
    private val dao: ScheduledAlarmDao
) {
    suspend fun getByStatus(status: AlarmStatus): List<ScheduledAlarmEntity> = dao.getByStatus(status)
    fun getAlarmsForDay(startOfDay: Long, endOfDay: Long): Flow<List<ScheduledAlarmEntity>> =
        dao.getAlarmsForDay(startOfDay, endOfDay)
    suspend fun getById(id: Long): ScheduledAlarmEntity? = dao.getById(id)
    suspend fun getPendingFutureAlarms(now: Long): List<ScheduledAlarmEntity> = dao.getPendingFutureAlarms(now)
    suspend fun insert(alarm: ScheduledAlarmEntity): Long = dao.insert(alarm)
    suspend fun insertAll(alarms: List<ScheduledAlarmEntity>): List<Long> = dao.insertAll(alarms)
    suspend fun update(alarm: ScheduledAlarmEntity) = dao.update(alarm)
    suspend fun deleteAlarmsForDay(startOfDay: Long, endOfDay: Long) = dao.deleteAlarmsForDay(startOfDay, endOfDay)
    suspend fun deletePendingForReminder(reminderId: Long) = dao.deletePendingForReminder(reminderId)
}
```

- [ ] **Step 2: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/sporadic/reminder/data/repository/
git commit -m "feat: add repository layer wrapping Room DAOs"
```

---

## Task 6: Hilt Dependency Injection

**Files:**
- Create: `app/src/main/java/com/sporadic/reminder/di/DatabaseModule.kt`
- Create: `app/src/main/java/com/sporadic/reminder/di/AppModule.kt`

- [ ] **Step 1: Create DI modules**

`DatabaseModule.kt`:
```kotlin
package com.sporadic.reminder.di

import android.content.Context
import androidx.room.Room
import com.sporadic.reminder.data.dao.NotificationLogDao
import com.sporadic.reminder.data.dao.ReminderDao
import com.sporadic.reminder.data.dao.ReminderGroupDao
import com.sporadic.reminder.data.dao.ScheduledAlarmDao
import com.sporadic.reminder.data.db.SporadicDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SporadicDatabase {
        return Room.databaseBuilder(
            context,
            SporadicDatabase::class.java,
            "sporadic_reminder.db"
        ).build()
    }

    @Provides
    fun provideReminderDao(db: SporadicDatabase): ReminderDao = db.reminderDao()

    @Provides
    fun provideReminderGroupDao(db: SporadicDatabase): ReminderGroupDao = db.reminderGroupDao()

    @Provides
    fun provideNotificationLogDao(db: SporadicDatabase): NotificationLogDao = db.notificationLogDao()

    @Provides
    fun provideScheduledAlarmDao(db: SporadicDatabase): ScheduledAlarmDao = db.scheduledAlarmDao()
}
```

`AppModule.kt`:
```kotlin
package com.sporadic.reminder.di

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    fun provideAlarmManager(@ApplicationContext context: Context): AlarmManager {
        return context.getSystemService(AlarmManager::class.java)
    }

    @Provides
    fun provideNotificationManager(@ApplicationContext context: Context): NotificationManager {
        return context.getSystemService(NotificationManager::class.java)
    }
}
```

- [ ] **Step 2: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/sporadic/reminder/di/
git commit -m "feat: add Hilt DI modules for database and system services"
```

---

## Task 7: ScheduleRemindersUseCase

**Files:**
- Create: `app/src/main/java/com/sporadic/reminder/domain/usecase/ScheduleRemindersUseCase.kt`
- Create: `app/src/main/java/com/sporadic/reminder/scheduler/AlarmScheduler.kt`
- Test: `app/src/test/java/com/sporadic/reminder/domain/usecase/ScheduleRemindersUseCaseTest.kt`

- [ ] **Step 1: Write the failing tests**

`ScheduleRemindersUseCaseTest.kt`:
```kotlin
package com.sporadic.reminder.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.sporadic.reminder.data.entity.ReminderEntity
import com.sporadic.reminder.data.entity.ReminderGroupEntity
import com.sporadic.reminder.data.entity.ScheduledAlarmEntity
import com.sporadic.reminder.data.repository.GroupRepository
import com.sporadic.reminder.data.repository.ReminderRepository
import com.sporadic.reminder.data.repository.ScheduledAlarmRepository
import com.sporadic.reminder.domain.model.AlarmStatus
import com.sporadic.reminder.domain.model.DndBehavior
import com.sporadic.reminder.domain.model.Priority
import com.sporadic.reminder.scheduler.AlarmScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class ScheduleRemindersUseCaseTest {
    private lateinit var useCase: ScheduleRemindersUseCase
    private val reminderRepo: ReminderRepository = mockk(relaxed = true)
    private val groupRepo: GroupRepository = mockk(relaxed = true)
    private val alarmRepo: ScheduledAlarmRepository = mockk(relaxed = true)
    private val alarmScheduler: AlarmScheduler = mockk(relaxed = true)

    @Before
    fun setup() {
        useCase = ScheduleRemindersUseCase(reminderRepo, groupRepo, alarmRepo, alarmScheduler)
    }

    private fun createReminder(
        id: Long = 1,
        startTime: LocalTime = LocalTime.of(9, 0),
        endTime: LocalTime = LocalTime.of(17, 0),
        notificationCount: Int = 5,
        activeDays: Int = 0b1111111,
        groupId: Long? = null
    ) = ReminderEntity(
        id = id,
        name = "Test",
        notificationText = "Test",
        notificationToneUri = null,
        vibrate = true,
        priority = Priority.DEFAULT,
        startTime = startTime,
        endTime = endTime,
        notificationCount = notificationCount,
        activeDays = activeDays,
        dndBehavior = DndBehavior.SKIP,
        isActive = true,
        groupId = groupId
    )

    @Test
    fun `generates correct number of alarms within time window`() = runTest {
        val reminder = createReminder(notificationCount = 3)
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()

        val capturedAlarms = slot<List<ScheduledAlarmEntity>>()
        coEvery { alarmRepo.insertAll(capture(capturedAlarms)) } returns listOf(1, 2, 3)

        useCase.scheduleForReminder(reminder, today, zone)

        val alarms = capturedAlarms.captured
        assertThat(alarms).hasSize(3)

        val startInstant = today.atTime(LocalTime.of(9, 0)).atZone(zone).toInstant()
        val endInstant = today.atTime(LocalTime.of(17, 0)).atZone(zone).toInstant()
        alarms.forEach { alarm ->
            assertThat(alarm.scheduledTime).isAtLeast(startInstant)
            assertThat(alarm.scheduledTime).isAtMost(endInstant)
            assertThat(alarm.status).isEqualTo(AlarmStatus.PENDING)
            assertThat(alarm.reminderId).isEqualTo(1)
        }
    }

    @Test
    fun `clears existing pending alarms before scheduling`() = runTest {
        val reminder = createReminder()
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()

        coEvery { alarmRepo.insertAll(any()) } returns listOf(1, 2, 3, 4, 5)

        useCase.scheduleForReminder(reminder, today, zone)

        coVerify { alarmRepo.deletePendingForReminder(reminder.id) }
    }

    @Test
    fun `registers alarms with AlarmScheduler`() = runTest {
        val reminder = createReminder(notificationCount = 2)
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()

        coEvery { alarmRepo.insertAll(any()) } returns listOf(10, 11)

        useCase.scheduleForReminder(reminder, today, zone)

        coVerify(exactly = 2) { alarmScheduler.scheduleExactAlarm(any(), any()) }
    }

    @Test
    fun `uses group schedule when group has shared schedule`() = runTest {
        val group = ReminderGroupEntity(
            id = 5,
            name = "Health",
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(20, 0),
            notificationCount = 2,
            activeDays = 0b1111111
        )
        val reminder = createReminder(groupId = 5, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(17, 0))

        coEvery { groupRepo.getById(5) } returns group

        val capturedAlarms = slot<List<ScheduledAlarmEntity>>()
        coEvery { alarmRepo.insertAll(capture(capturedAlarms)) } returns listOf(1, 2)

        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        useCase.scheduleForReminder(reminder, today, zone)

        val alarms = capturedAlarms.captured
        assertThat(alarms).hasSize(2) // group's count, not reminder's 5

        val groupStart = today.atTime(LocalTime.of(8, 0)).atZone(zone).toInstant()
        val groupEnd = today.atTime(LocalTime.of(20, 0)).atZone(zone).toInstant()
        alarms.forEach { alarm ->
            assertThat(alarm.scheduledTime).isAtLeast(groupStart)
            assertThat(alarm.scheduledTime).isAtMost(groupEnd)
        }
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.sporadic.reminder.domain.usecase.ScheduleRemindersUseCaseTest"`
Expected: FAIL — `ScheduleRemindersUseCase` class does not exist

- [ ] **Step 3: Implement AlarmScheduler**

`AlarmScheduler.kt`:
```kotlin
package com.sporadic.reminder.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager
) {
    fun scheduleExactAlarm(alarmId: Long, triggerAt: Instant) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt.toEpochMilli(),
            pendingIntent
        )
    }

    fun cancelAlarm(alarmId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
    }
}
```

- [ ] **Step 4: Implement ScheduleRemindersUseCase**

`ScheduleRemindersUseCase.kt`:
```kotlin
package com.sporadic.reminder.domain.usecase

import com.sporadic.reminder.data.entity.ReminderEntity
import com.sporadic.reminder.data.entity.ScheduledAlarmEntity
import com.sporadic.reminder.data.repository.GroupRepository
import com.sporadic.reminder.data.repository.ReminderRepository
import com.sporadic.reminder.data.repository.ScheduledAlarmRepository
import com.sporadic.reminder.domain.model.AlarmStatus
import com.sporadic.reminder.scheduler.AlarmScheduler
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlin.random.Random

class ScheduleRemindersUseCase @Inject constructor(
    private val reminderRepo: ReminderRepository,
    private val groupRepo: GroupRepository,
    private val alarmRepo: ScheduledAlarmRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend fun scheduleForReminder(
        reminder: ReminderEntity,
        date: LocalDate,
        zone: ZoneId
    ) {
        // Resolve effective schedule (group override or reminder's own)
        val effectiveStartTime: LocalTime
        val effectiveEndTime: LocalTime
        val effectiveCount: Int

        if (reminder.groupId != null) {
            val group = groupRepo.getById(reminder.groupId)
            effectiveStartTime = group?.startTime ?: reminder.startTime
            effectiveEndTime = group?.endTime ?: reminder.endTime
            effectiveCount = group?.notificationCount ?: reminder.notificationCount
        } else {
            effectiveStartTime = reminder.startTime
            effectiveEndTime = reminder.endTime
            effectiveCount = reminder.notificationCount
        }

        // Clear existing pending alarms for this reminder
        alarmRepo.deletePendingForReminder(reminder.id)

        // Generate random times within the window
        val startInstant = date.atTime(effectiveStartTime).atZone(zone).toInstant()
        val endInstant = date.atTime(effectiveEndTime).atZone(zone).toInstant()
        val windowMillis = endInstant.toEpochMilli() - startInstant.toEpochMilli()

        val alarms = (1..effectiveCount).map {
            val offsetMillis = Random.nextLong(windowMillis + 1)
            ScheduledAlarmEntity(
                reminderId = reminder.id,
                scheduledTime = Instant.ofEpochMilli(startInstant.toEpochMilli() + offsetMillis),
                status = AlarmStatus.PENDING
            )
        }

        // Persist and schedule
        val ids = alarmRepo.insertAll(alarms)
        ids.zip(alarms).forEach { (id, alarm) ->
            alarmScheduler.scheduleExactAlarm(id, alarm.scheduledTime)
        }
    }

    suspend fun scheduleAllForDay(date: LocalDate, zone: ZoneId) {
        val dayOfWeek = date.dayOfWeek.value // 1=Monday..7=Sunday
        val dayBit = 1 shl (dayOfWeek - 1)
        val reminders = reminderRepo.getActiveForDay(dayBit)
        reminders.forEach { reminder ->
            scheduleForReminder(reminder, date, zone)
        }
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.sporadic.reminder.domain.usecase.ScheduleRemindersUseCaseTest"`
Expected: All 4 tests PASS

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/sporadic/reminder/domain/usecase/ScheduleRemindersUseCase.kt app/src/main/java/com/sporadic/reminder/scheduler/AlarmScheduler.kt app/src/test/java/com/sporadic/reminder/domain/usecase/ScheduleRemindersUseCaseTest.kt
git commit -m "feat: add ScheduleRemindersUseCase with random time generation and AlarmScheduler"
```

---

## Task 8: HandleNotificationActionUseCase

**Files:**
- Create: `app/src/main/java/com/sporadic/reminder/domain/usecase/HandleNotificationActionUseCase.kt`
- Test: `app/src/test/java/com/sporadic/reminder/domain/usecase/HandleNotificationActionUseCaseTest.kt`

- [ ] **Step 1: Write the failing tests**

`HandleNotificationActionUseCaseTest.kt`:
```kotlin
package com.sporadic.reminder.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.sporadic.reminder.data.entity.NotificationLogEntity
import com.sporadic.reminder.data.entity.ScheduledAlarmEntity
import com.sporadic.reminder.data.repository.NotificationLogRepository
import com.sporadic.reminder.data.repository.ScheduledAlarmRepository
import com.sporadic.reminder.domain.model.AlarmStatus
import com.sporadic.reminder.domain.model.NotificationAction
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

class HandleNotificationActionUseCaseTest {
    private lateinit var useCase: HandleNotificationActionUseCase
    private val logRepo: NotificationLogRepository = mockk(relaxed = true)
    private val alarmRepo: ScheduledAlarmRepository = mockk(relaxed = true)

    @Before
    fun setup() {
        useCase = HandleNotificationActionUseCase(logRepo, alarmRepo)
    }

    private fun createAlarm(id: Long = 1, reminderId: Long = 10) = ScheduledAlarmEntity(
        id = id,
        reminderId = reminderId,
        scheduledTime = Instant.now().minusSeconds(60),
        status = AlarmStatus.FIRED
    )

    @Test
    fun `handleDone logs DONE action and keeps alarm FIRED`() = runTest {
        val alarm = createAlarm()
        coEvery { alarmRepo.getById(1) } returns alarm

        val logSlot = slot<NotificationLogEntity>()
        coEvery { logRepo.insert(capture(logSlot)) } returns 1

        useCase.handle(alarmId = 1, action = NotificationAction.DONE)

        assertThat(logSlot.captured.action).isEqualTo(NotificationAction.DONE)
        assertThat(logSlot.captured.reminderId).isEqualTo(10)
        assertThat(logSlot.captured.actionTimestamp).isNotNull()
    }

    @Test
    fun `handleSkipped logs SKIPPED action`() = runTest {
        val alarm = createAlarm()
        coEvery { alarmRepo.getById(1) } returns alarm

        val logSlot = slot<NotificationLogEntity>()
        coEvery { logRepo.insert(capture(logSlot)) } returns 1

        useCase.handle(alarmId = 1, action = NotificationAction.SKIPPED)

        assertThat(logSlot.captured.action).isEqualTo(NotificationAction.SKIPPED)
    }

    @Test
    fun `handleSnoozed logs SNOOZED action and updates alarm status`() = runTest {
        val alarm = createAlarm()
        coEvery { alarmRepo.getById(1) } returns alarm

        val logSlot = slot<NotificationLogEntity>()
        coEvery { logRepo.insert(capture(logSlot)) } returns 1

        useCase.handle(alarmId = 1, action = NotificationAction.SNOOZED)

        assertThat(logSlot.captured.action).isEqualTo(NotificationAction.SNOOZED)
        coVerify { alarmRepo.update(alarm.copy(status = AlarmStatus.SNOOZED)) }
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.sporadic.reminder.domain.usecase.HandleNotificationActionUseCaseTest"`
Expected: FAIL — class does not exist

- [ ] **Step 3: Implement HandleNotificationActionUseCase**

`HandleNotificationActionUseCase.kt`:
```kotlin
package com.sporadic.reminder.domain.usecase

import com.sporadic.reminder.data.entity.NotificationLogEntity
import com.sporadic.reminder.data.repository.NotificationLogRepository
import com.sporadic.reminder.data.repository.ScheduledAlarmRepository
import com.sporadic.reminder.domain.model.AlarmStatus
import com.sporadic.reminder.domain.model.NotificationAction
import java.time.Instant
import javax.inject.Inject

class HandleNotificationActionUseCase @Inject constructor(
    private val logRepo: NotificationLogRepository,
    private val alarmRepo: ScheduledAlarmRepository
) {
    suspend fun handle(alarmId: Long, action: NotificationAction) {
        val alarm = alarmRepo.getById(alarmId) ?: return

        logRepo.insert(
            NotificationLogEntity(
                reminderId = alarm.reminderId,
                scheduledTime = alarm.scheduledTime,
                firedTime = alarm.scheduledTime,
                action = action,
                actionTimestamp = Instant.now()
            )
        )

        if (action == NotificationAction.SNOOZED) {
            alarmRepo.update(alarm.copy(status = AlarmStatus.SNOOZED))
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.sporadic.reminder.domain.usecase.HandleNotificationActionUseCaseTest"`
Expected: All 3 tests PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/sporadic/reminder/domain/usecase/HandleNotificationActionUseCase.kt app/src/test/java/com/sporadic/reminder/domain/usecase/HandleNotificationActionUseCaseTest.kt
git commit -m "feat: add HandleNotificationActionUseCase with Done/Skip/Snooze handling"
```

---

## Task 9: SyncGroupScheduleUseCase

**Files:**
- Create: `app/src/main/java/com/sporadic/reminder/domain/usecase/SyncGroupScheduleUseCase.kt`
- Test: `app/src/test/java/com/sporadic/reminder/domain/usecase/SyncGroupScheduleUseCaseTest.kt`

- [ ] **Step 1: Write the failing tests**

`SyncGroupScheduleUseCaseTest.kt`:
```kotlin
package com.sporadic.reminder.domain.usecase

import com.sporadic.reminder.data.entity.ReminderEntity
import com.sporadic.reminder.data.entity.ReminderGroupEntity
import com.sporadic.reminder.data.repository.ReminderRepository
import com.sporadic.reminder.domain.model.DndBehavior
import com.sporadic.reminder.domain.model.Priority
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class SyncGroupScheduleUseCaseTest {
    private lateinit var useCase: SyncGroupScheduleUseCase
    private val reminderRepo: ReminderRepository = mockk(relaxed = true)
    private val scheduleUseCase: ScheduleRemindersUseCase = mockk(relaxed = true)

    @Before
    fun setup() {
        useCase = SyncGroupScheduleUseCase(reminderRepo, scheduleUseCase)
    }

    private fun createReminder(id: Long, groupId: Long) = ReminderEntity(
        id = id,
        name = "R$id",
        notificationText = "Text",
        notificationToneUri = null,
        vibrate = true,
        priority = Priority.DEFAULT,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(17, 0),
        notificationCount = 5,
        activeDays = 0b1111111,
        dndBehavior = DndBehavior.SKIP,
        isActive = true,
        groupId = groupId
    )

    @Test
    fun `reschedules all active group members for today`() = runTest {
        val members = listOf(createReminder(1, 5), createReminder(2, 5))
        coEvery { reminderRepo.getByGroupId(5) } returns members

        useCase.syncGroup(groupId = 5, date = LocalDate.now(), zone = ZoneId.systemDefault())

        coVerify(exactly = 2) { scheduleUseCase.scheduleForReminder(any(), any(), any()) }
    }

    @Test
    fun `skips inactive members`() = runTest {
        val members = listOf(
            createReminder(1, 5),
            createReminder(2, 5).copy(isActive = false)
        )
        coEvery { reminderRepo.getByGroupId(5) } returns members

        useCase.syncGroup(groupId = 5, date = LocalDate.now(), zone = ZoneId.systemDefault())

        coVerify(exactly = 1) { scheduleUseCase.scheduleForReminder(any(), any(), any()) }
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.sporadic.reminder.domain.usecase.SyncGroupScheduleUseCaseTest"`
Expected: FAIL

- [ ] **Step 3: Implement SyncGroupScheduleUseCase**

`SyncGroupScheduleUseCase.kt`:
```kotlin
package com.sporadic.reminder.domain.usecase

import com.sporadic.reminder.data.repository.ReminderRepository
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class SyncGroupScheduleUseCase @Inject constructor(
    private val reminderRepo: ReminderRepository,
    private val scheduleUseCase: ScheduleRemindersUseCase
) {
    suspend fun syncGroup(groupId: Long, date: LocalDate, zone: ZoneId) {
        val members = reminderRepo.getByGroupId(groupId)
        members.filter { it.isActive }.forEach { reminder ->
            scheduleUseCase.scheduleForReminder(reminder, date, zone)
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.sporadic.reminder.domain.usecase.SyncGroupScheduleUseCaseTest"`
Expected: All 2 tests PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/sporadic/reminder/domain/usecase/SyncGroupScheduleUseCase.kt app/src/test/java/com/sporadic/reminder/domain/usecase/SyncGroupScheduleUseCaseTest.kt
git commit -m "feat: add SyncGroupScheduleUseCase for group schedule propagation"
```

---

## Task 10: Notification System

**Files:**
- Create: `app/src/main/java/com/sporadic/reminder/notification/NotificationChannelManager.kt`
- Create: `app/src/main/java/com/sporadic/reminder/notification/DndStateChecker.kt`
- Create: `app/src/main/java/com/sporadic/reminder/notification/NotificationPublisher.kt`
- Create: `app/src/main/java/com/sporadic/reminder/notification/NotificationActionReceiver.kt`
- Test: `app/src/test/java/com/sporadic/reminder/notification/DndStateCheckerTest.kt`

- [ ] **Step 1: Write DndStateChecker test**

`DndStateCheckerTest.kt`:
```kotlin
package com.sporadic.reminder.notification

import android.app.NotificationManager
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class DndStateCheckerTest {
    private val notificationManager: NotificationManager = mockk()
    private val checker = DndStateChecker(notificationManager)

    @Test
    fun `returns true when DND is active`() {
        every { notificationManager.currentInterruptionFilter } returns NotificationManager.INTERRUPTION_FILTER_PRIORITY
        assertThat(checker.isDndActive()).isTrue()
    }

    @Test
    fun `returns false when interruption filter is ALL`() {
        every { notificationManager.currentInterruptionFilter } returns NotificationManager.INTERRUPTION_FILTER_ALL
        assertThat(checker.isDndActive()).isFalse()
    }

    @Test
    fun `returns true when interruption filter is NONE`() {
        every { notificationManager.currentInterruptionFilter } returns NotificationManager.INTERRUPTION_FILTER_NONE
        assertThat(checker.isDndActive()).isTrue()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.sporadic.reminder.notification.DndStateCheckerTest"`
Expected: FAIL

- [ ] **Step 3: Implement notification classes**

`DndStateChecker.kt`:
```kotlin
package com.sporadic.reminder.notification

import android.app.NotificationManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DndStateChecker @Inject constructor(
    private val notificationManager: NotificationManager
) {
    fun isDndActive(): Boolean {
        return notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
    }
}
```

`NotificationChannelManager.kt`:
```kotlin
package com.sporadic.reminder.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import com.sporadic.reminder.domain.model.Priority
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationChannelManager @Inject constructor(
    private val notificationManager: NotificationManager
) {
    fun createChannels() {
        Priority.entries.forEach { priority ->
            val channel = NotificationChannel(
                channelId(priority),
                channelName(priority),
                priority.importance
            ).apply {
                description = "Sporadic reminders with ${priority.name.lowercase()} priority"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun channelId(priority: Priority): String = "sporadic_${priority.name.lowercase()}"

    private fun channelName(priority: Priority): String = when (priority) {
        Priority.LOW -> "Low Priority Reminders"
        Priority.DEFAULT -> "Default Priority Reminders"
        Priority.HIGH -> "High Priority Reminders"
        Priority.URGENT -> "Urgent Priority Reminders"
    }
}
```

`NotificationPublisher.kt`:
```kotlin
package com.sporadic.reminder.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.sporadic.reminder.MainActivity
import com.sporadic.reminder.R
import com.sporadic.reminder.data.entity.ReminderEntity
import com.sporadic.reminder.domain.model.NotificationAction
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationPublisher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val channelManager: NotificationChannelManager
) {
    fun publish(reminder: ReminderEntity, alarmId: Long) {
        val contentIntent = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_ALARM_ID, alarmId)
                putExtra(EXTRA_REMINDER_ID, reminder.id)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelManager.channelId(reminder.priority))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(reminder.name)
            .setContentText(reminder.notificationText)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setVibrate(if (reminder.vibrate) longArrayOf(0, 250, 250, 250) else null)
            .apply {
                reminder.notificationToneUri?.let { setSound(Uri.parse(it)) }
            }
            .addAction(buildActionButton(alarmId, NotificationAction.DONE, "Done"))
            .addAction(buildActionButton(alarmId, NotificationAction.SKIPPED, "Skip"))
            .addAction(buildActionButton(alarmId, NotificationAction.SNOOZED, "Snooze"))
            .build()

        val notificationManager = context.getSystemService(android.app.NotificationManager::class.java)
        notificationManager.notify(alarmId.toInt(), notification)
    }

    private fun buildActionButton(
        alarmId: Long,
        action: NotificationAction,
        label: String
    ): NotificationCompat.Action {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ACTION, action.name)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            "${alarmId}_${action.ordinal}".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action.Builder(0, label, pendingIntent).build()
    }

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_ACTION = "extra_action"
    }
}
```

`NotificationActionReceiver.kt`:
```kotlin
package com.sporadic.reminder.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sporadic.reminder.domain.model.NotificationAction
import com.sporadic.reminder.domain.usecase.HandleNotificationActionUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {
    @Inject lateinit var handleAction: HandleNotificationActionUseCase

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(NotificationPublisher.EXTRA_ALARM_ID, -1)
        val actionName = intent.getStringExtra(NotificationPublisher.EXTRA_ACTION) ?: return
        val action = NotificationAction.valueOf(actionName)

        // Dismiss the notification
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancel(alarmId.toInt())

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleAction.handle(alarmId, action)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
```

- [ ] **Step 4: Create a placeholder notification icon**

Create `app/src/main/res/drawable/ic_notification.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M12,22c1.1,0 2,-0.9 2,-2h-4c0,1.1 0.89,2 2,2zM18,16v-5c0,-3.07 -1.64,-5.64 -4.5,-6.32V4c0,-0.83 -0.67,-1.5 -1.5,-1.5s-1.5,0.67 -1.5,1.5v0.68C7.63,5.36 6,7.92 6,11v5l-2,2v1h16v-1l-2,-2z"/>
</vector>
```

- [ ] **Step 5: Run DndStateChecker tests**

Run: `./gradlew testDebugUnitTest --tests "com.sporadic.reminder.notification.DndStateCheckerTest"`
Expected: All 3 tests PASS

- [ ] **Step 6: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/sporadic/reminder/notification/ app/src/main/res/drawable/ic_notification.xml app/src/test/java/com/sporadic/reminder/notification/
git commit -m "feat: add notification system (channels, publisher, DND checker, action receiver)"
```

---

## Task 11: Broadcast Receivers

**Files:**
- Create: `app/src/main/java/com/sporadic/reminder/scheduler/AlarmReceiver.kt`
- Create: `app/src/main/java/com/sporadic/reminder/scheduler/BootReceiver.kt`
- Create: `app/src/main/java/com/sporadic/reminder/scheduler/DailySchedulerReceiver.kt`

- [ ] **Step 1: Implement AlarmReceiver**

`AlarmReceiver.kt`:
```kotlin
package com.sporadic.reminder.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sporadic.reminder.data.repository.GroupRepository
import com.sporadic.reminder.data.repository.ReminderRepository
import com.sporadic.reminder.data.repository.ScheduledAlarmRepository
import com.sporadic.reminder.domain.model.AlarmStatus
import com.sporadic.reminder.domain.model.DndBehavior
import com.sporadic.reminder.notification.DndStateChecker
import com.sporadic.reminder.notification.NotificationPublisher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var alarmRepo: ScheduledAlarmRepository
    @Inject lateinit var reminderRepo: ReminderRepository
    @Inject lateinit var groupRepo: GroupRepository
    @Inject lateinit var dndChecker: DndStateChecker
    @Inject lateinit var notificationPublisher: NotificationPublisher
    @Inject lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        if (alarmId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleAlarm(alarmId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleAlarm(alarmId: Long) {
        val alarm = alarmRepo.getById(alarmId) ?: return
        var reminder = reminderRepo.getById(alarm.reminderId) ?: return

        // Group pooling: if reminder belongs to a group with a shared schedule,
        // randomly select a member reminder from the group to display instead.
        if (reminder.groupId != null) {
            val group = groupRepo.getById(reminder.groupId)
            if (group?.startTime != null) { // group has shared schedule
                val members = reminderRepo.getByGroupId(reminder.groupId).filter { it.isActive }
                if (members.isNotEmpty()) {
                    reminder = members.random()
                }
            }
        }

        if (dndChecker.isDndActive()) {
            when (reminder.dndBehavior) {
                DndBehavior.SKIP -> {
                    alarmRepo.update(alarm.copy(status = AlarmStatus.SKIPPED))
                }
                DndBehavior.SNOOZE -> {
                    val retryTime = Instant.now().plusSeconds(DND_RETRY_SECONDS)
                    val endOfWindow = LocalDate.now()
                        .atTime(reminder.endTime)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()

                    if (retryTime.isBefore(endOfWindow)) {
                        alarmScheduler.scheduleExactAlarm(alarmId, retryTime)
                        alarmRepo.update(alarm.copy(status = AlarmStatus.SNOOZED))
                    } else {
                        alarmRepo.update(alarm.copy(status = AlarmStatus.SKIPPED))
                    }
                }
            }
        } else {
            notificationPublisher.publish(reminder, alarmId)
            alarmRepo.update(alarm.copy(status = AlarmStatus.FIRED))
        }
    }

    companion object {
        const val DND_RETRY_SECONDS = 900L // 15 minutes
    }
}
```

- [ ] **Step 2: Implement BootReceiver**

`BootReceiver.kt`:
```kotlin
package com.sporadic.reminder.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sporadic.reminder.data.repository.ScheduledAlarmRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var alarmRepo: ScheduledAlarmRepository
    @Inject lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = Instant.now().toEpochMilli()
                val pendingAlarms = alarmRepo.getPendingFutureAlarms(now)
                pendingAlarms.forEach { alarm ->
                    alarmScheduler.scheduleExactAlarm(alarm.id, alarm.scheduledTime)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
```

- [ ] **Step 3: Implement DailySchedulerReceiver**

`DailySchedulerReceiver.kt`:
```kotlin
package com.sporadic.reminder.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sporadic.reminder.domain.usecase.ScheduleRemindersUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

@AndroidEntryPoint
class DailySchedulerReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduleUseCase: ScheduleRemindersUseCase

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val today = LocalDate.now()
                val zone = ZoneId.systemDefault()
                scheduleUseCase.scheduleAllForDay(today, zone)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val DAILY_REQUEST_CODE = 999_999

        fun scheduleDailyTrigger(context: Context) {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            val intent = Intent(context, DailySchedulerReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                DAILY_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = LocalDate.now().plusDays(1)
                .atTime(LocalTime.of(4, 0))
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }
}
```

- [ ] **Step 4: Initialize channels and daily trigger in SporadicApp**

Update `SporadicApp.kt`:
```kotlin
package com.sporadic.reminder

import android.app.Application
import com.sporadic.reminder.notification.NotificationChannelManager
import com.sporadic.reminder.scheduler.DailySchedulerReceiver
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SporadicApp : Application() {
    @Inject lateinit var channelManager: NotificationChannelManager

    override fun onCreate() {
        super.onCreate()
        channelManager.createChannels()
        DailySchedulerReceiver.scheduleDailyTrigger(this)
    }
}
```

- [ ] **Step 5: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/sporadic/reminder/scheduler/ app/src/main/java/com/sporadic/reminder/SporadicApp.kt
git commit -m "feat: add AlarmReceiver, BootReceiver, DailySchedulerReceiver with DND retry logic"
```

---

## Task 12: Theme and Navigation Shell

**Files:**
- Create: `app/src/main/java/com/sporadic/reminder/ui/theme/SporadicTheme.kt`
- Create: `app/src/main/java/com/sporadic/reminder/ui/navigation/Screen.kt`
- Create: `app/src/main/java/com/sporadic/reminder/ui/navigation/SporadicNavGraph.kt`
- Modify: `app/src/main/java/com/sporadic/reminder/MainActivity.kt`

- [ ] **Step 1: Create theme**

`SporadicTheme.kt`:
```kotlin
package com.sporadic.reminder.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme()
private val DarkColors = darkColorScheme()

@Composable
fun SporadicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
```

- [ ] **Step 2: Create navigation routes**

`Screen.kt`:
```kotlin
package com.sporadic.reminder.ui.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object ReminderList : Screen("reminders")
    data object ReminderEdit : Screen("reminders/{reminderId}") {
        fun createRoute(reminderId: Long) = "reminders/$reminderId"
        fun createNewRoute() = "reminders/-1"
    }
    data object GroupEdit : Screen("groups/{groupId}") {
        fun createRoute(groupId: Long) = "groups/$groupId"
        fun createNewRoute() = "groups/-1"
    }
    data object Settings : Screen("settings")
}
```

- [ ] **Step 3: Create navigation graph**

`SporadicNavGraph.kt`:
```kotlin
package com.sporadic.reminder.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

data class BottomNavItem(val screen: Screen, val label: String, val icon: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard, "Dashboard", Icons.Default.Home),
    BottomNavItem(Screen.ReminderList, "Reminders", Icons.Default.Notifications),
    BottomNavItem(Screen.Settings, "Settings", Icons.Default.Settings),
)

@Composable
fun SporadicNavGraph(navController: NavHostController = rememberNavController()) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.screen.route,
                        onClick = {
                            navController.navigate(item.screen.route) {
                                popUpTo(Screen.Dashboard.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                // DashboardScreen — implemented in Task 13
                Text("Dashboard")
            }
            composable(Screen.ReminderList.route) {
                // ReminderListScreen — implemented in Task 14
                Text("Reminders")
            }
            composable(
                route = Screen.ReminderEdit.route,
                arguments = listOf(navArgument("reminderId") { type = NavType.LongType })
            ) { backStackEntry ->
                val reminderId = backStackEntry.arguments?.getLong("reminderId") ?: -1L
                // ReminderEditScreen — implemented in Task 14
                Text("Edit Reminder $reminderId")
            }
            composable(
                route = Screen.GroupEdit.route,
                arguments = listOf(navArgument("groupId") { type = NavType.LongType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getLong("groupId") ?: -1L
                // GroupEditScreen — implemented in Task 15
                Text("Edit Group $groupId")
            }
            composable(Screen.Settings.route) {
                // SettingsScreen — implemented in Task 16
                Text("Settings")
            }
        }
    }
}
```

- [ ] **Step 4: Wire navigation into MainActivity**

Update `MainActivity.kt`:
```kotlin
package com.sporadic.reminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sporadic.reminder.ui.navigation.SporadicNavGraph
import com.sporadic.reminder.ui.theme.SporadicTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SporadicTheme {
                SporadicNavGraph()
            }
        }
    }
}
```

- [ ] **Step 5: Verify build and run on emulator**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

Launch on emulator. Verify: bottom nav bar with 3 tabs (Dashboard, Reminders, Settings), tapping switches between placeholder screens.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/sporadic/reminder/ui/ app/src/main/java/com/sporadic/reminder/MainActivity.kt
git commit -m "feat: add Material 3 theme with dynamic colors and bottom navigation shell"
```

---

## Task 13: Dashboard Screen

**Files:**
- Create: `app/src/main/java/com/sporadic/reminder/ui/dashboard/DashboardViewModel.kt`
- Create: `app/src/main/java/com/sporadic/reminder/ui/dashboard/DashboardScreen.kt`
- Modify: `app/src/main/java/com/sporadic/reminder/ui/navigation/SporadicNavGraph.kt`

- [ ] **Step 1: Create DashboardViewModel**

`DashboardViewModel.kt`:
```kotlin
package com.sporadic.reminder.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sporadic.reminder.data.entity.NotificationLogEntity
import com.sporadic.reminder.data.entity.ScheduledAlarmEntity
import com.sporadic.reminder.data.repository.NotificationLogRepository
import com.sporadic.reminder.data.repository.ScheduledAlarmRepository
import com.sporadic.reminder.domain.model.AlarmStatus
import com.sporadic.reminder.domain.model.NotificationAction
import com.sporadic.reminder.domain.usecase.HandleNotificationActionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class DashboardStats(
    val fired: Int = 0,
    val done: Int = 0,
    val skipped: Int = 0,
    val snoozed: Int = 0,
    val pending: Int = 0
)

data class DashboardUiState(
    val stats: DashboardStats = DashboardStats(),
    val todayLogs: List<NotificationLogEntity> = emptyList(),
    val todayAlarms: List<ScheduledAlarmEntity> = emptyList()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val logRepo: NotificationLogRepository,
    private val alarmRepo: ScheduledAlarmRepository,
    private val handleAction: HandleNotificationActionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        observeToday()
    }

    private fun observeToday() {
        val zone = ZoneId.systemDefault()
        val startOfDay = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfDay = LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        viewModelScope.launch {
            combine(
                logRepo.getLogsForDay(startOfDay, endOfDay),
                alarmRepo.getAlarmsForDay(startOfDay, endOfDay)
            ) { logs, alarms ->
                DashboardUiState(
                    stats = DashboardStats(
                        fired = alarms.count { it.status == AlarmStatus.FIRED },
                        done = logs.count { it.action == NotificationAction.DONE },
                        skipped = logs.count { it.action == NotificationAction.SKIPPED },
                        snoozed = logs.count { it.action == NotificationAction.SNOOZED },
                        pending = alarms.count { it.status == AlarmStatus.PENDING }
                    ),
                    todayLogs = logs,
                    todayAlarms = alarms
                )
            }.collect { _uiState.value = it }
        }
    }

    fun onAction(alarmId: Long, action: NotificationAction) {
        viewModelScope.launch {
            handleAction.handle(alarmId, action)
        }
    }
}
```

- [ ] **Step 2: Create DashboardScreen**

`DashboardScreen.kt`:
```kotlin
package com.sporadic.reminder.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sporadic.reminder.domain.model.NotificationAction
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Today", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))

        // Stats cards row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard("Pending", state.stats.pending, Modifier.weight(1f))
            StatCard("Done", state.stats.done, Modifier.weight(1f))
            StatCard("Skipped", state.stats.skipped, Modifier.weight(1f))
            StatCard("Snoozed", state.stats.snoozed, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Activity", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.todayAlarms.filter {
                it.status == com.sporadic.reminder.domain.model.AlarmStatus.FIRED
            }) { alarm ->
                val log = state.todayLogs.find { it.scheduledTime == alarm.scheduledTime }
                FiredReminderCard(
                    scheduledTime = alarm.scheduledTime
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("h:mm a")),
                    action = log?.action,
                    onDone = { viewModel.onAction(alarm.id, NotificationAction.DONE) },
                    onSkip = { viewModel.onAction(alarm.id, NotificationAction.SKIPPED) },
                    onSnooze = { viewModel.onAction(alarm.id, NotificationAction.SNOOZED) }
                )
            }
        }
    }
}

@Composable
private fun StatCard(label: String, count: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun FiredReminderCard(
    scheduledTime: String,
    action: NotificationAction?,
    onDone: () -> Unit,
    onSkip: () -> Unit,
    onSnooze: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Fired at $scheduledTime", style = MaterialTheme.typography.bodyMedium)
            if (action != null) {
                Text("Action: ${action.name}", style = MaterialTheme.typography.bodySmall)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onDone) { Text("Done") }
                    OutlinedButton(onClick = onSkip) { Text("Skip") }
                    OutlinedButton(onClick = onSnooze) { Text("Snooze") }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Wire into NavGraph**

In `SporadicNavGraph.kt`, replace the Dashboard placeholder:
```kotlin
composable(Screen.Dashboard.route) {
    DashboardScreen()
}
```

Add import: `import com.sporadic.reminder.ui.dashboard.DashboardScreen`

- [ ] **Step 4: Verify build and test on emulator**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

Launch on emulator. Verify: Dashboard tab shows "Today" header, 4 stat cards (all zeros initially), and an empty activity list.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/sporadic/reminder/ui/dashboard/ app/src/main/java/com/sporadic/reminder/ui/navigation/SporadicNavGraph.kt
git commit -m "feat: add Dashboard screen with stats cards and fired notifications list"
```

---

## Task 14: Reminder List and Edit Screens

**Files:**
- Create: `app/src/main/java/com/sporadic/reminder/ui/reminders/ReminderListViewModel.kt`
- Create: `app/src/main/java/com/sporadic/reminder/ui/reminders/ReminderListScreen.kt`
- Create: `app/src/main/java/com/sporadic/reminder/ui/reminders/ReminderEditViewModel.kt`
- Create: `app/src/main/java/com/sporadic/reminder/ui/reminders/ReminderEditScreen.kt`
- Modify: `app/src/main/java/com/sporadic/reminder/ui/navigation/SporadicNavGraph.kt`

- [ ] **Step 1: Create ReminderListViewModel**

`ReminderListViewModel.kt`:
```kotlin
package com.sporadic.reminder.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sporadic.reminder.data.entity.ReminderEntity
import com.sporadic.reminder.data.entity.ReminderGroupEntity
import com.sporadic.reminder.data.repository.GroupRepository
import com.sporadic.reminder.data.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReminderListUiState(
    val reminders: List<ReminderEntity> = emptyList(),
    val groups: List<ReminderGroupEntity> = emptyList()
)

@HiltViewModel
class ReminderListViewModel @Inject constructor(
    private val reminderRepo: ReminderRepository,
    private val groupRepo: GroupRepository
) : ViewModel() {

    val uiState: StateFlow<ReminderListUiState> = combine(
        reminderRepo.getAll(),
        groupRepo.getAll()
    ) { reminders, groups ->
        ReminderListUiState(reminders = reminders, groups = groups)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReminderListUiState())

    fun toggleActive(reminder: ReminderEntity) {
        viewModelScope.launch {
            reminderRepo.update(reminder.copy(isActive = !reminder.isActive))
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            reminderRepo.delete(reminder)
        }
    }
}
```

- [ ] **Step 2: Create ReminderListScreen**

`ReminderListScreen.kt`:
```kotlin
package com.sporadic.reminder.ui.reminders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sporadic.reminder.data.entity.ReminderEntity

@Composable
fun ReminderListScreen(
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToNewReminder: () -> Unit,
    onNavigateToGroupEdit: (Long) -> Unit,
    onNavigateToNewGroup: () -> Unit,
    viewModel: ReminderListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToNewReminder) {
                Icon(Icons.Default.Add, contentDescription = "Add reminder")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.groups.isNotEmpty()) {
                item {
                    Text(
                        "Groups",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                items(state.groups) { group ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToGroupEdit(group.id) }
                    ) {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            item {
                Text(
                    "Reminders",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            items(state.reminders) { reminder ->
                ReminderCard(
                    reminder = reminder,
                    onToggle = { viewModel.toggleActive(reminder) },
                    onClick = { onNavigateToEdit(reminder.id) }
                )
            }
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: ReminderEntity,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(reminder.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${reminder.startTime} - ${reminder.endTime} | ${reminder.notificationCount}x",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(checked = reminder.isActive, onCheckedChange = { onToggle() })
        }
    }
}
```

- [ ] **Step 3: Create ReminderEditViewModel**

`ReminderEditViewModel.kt`:
```kotlin
package com.sporadic.reminder.ui.reminders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sporadic.reminder.data.entity.ReminderEntity
import com.sporadic.reminder.data.entity.ReminderGroupEntity
import com.sporadic.reminder.data.repository.GroupRepository
import com.sporadic.reminder.data.repository.ReminderRepository
import com.sporadic.reminder.domain.model.DndBehavior
import com.sporadic.reminder.domain.model.Priority
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

data class ReminderEditUiState(
    val name: String = "",
    val notificationText: String = "",
    val notificationToneUri: String? = null,
    val vibrate: Boolean = true,
    val priority: Priority = Priority.DEFAULT,
    val startTime: LocalTime = LocalTime.of(9, 0),
    val endTime: LocalTime = LocalTime.of(17, 0),
    val notificationCount: Int = 5,
    val activeDays: Int = 0b1111111,
    val dndBehavior: DndBehavior = DndBehavior.SKIP,
    val groupId: Long? = null,
    val availableGroups: List<ReminderGroupEntity> = emptyList(),
    val isNew: Boolean = true,
    val isSaved: Boolean = false
)

@HiltViewModel
class ReminderEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reminderRepo: ReminderRepository,
    private val groupRepo: GroupRepository
) : ViewModel() {

    private val reminderId: Long = savedStateHandle["reminderId"] ?: -1L

    private val _uiState = MutableStateFlow(ReminderEditUiState())
    val uiState: StateFlow<ReminderEditUiState> = _uiState

    init {
        viewModelScope.launch {
            val groups = groupRepo.getAll().first()
            if (reminderId != -1L) {
                val reminder = reminderRepo.getById(reminderId)
                if (reminder != null) {
                    _uiState.value = ReminderEditUiState(
                        name = reminder.name,
                        notificationText = reminder.notificationText,
                        notificationToneUri = reminder.notificationToneUri,
                        vibrate = reminder.vibrate,
                        priority = reminder.priority,
                        startTime = reminder.startTime,
                        endTime = reminder.endTime,
                        notificationCount = reminder.notificationCount,
                        activeDays = reminder.activeDays,
                        dndBehavior = reminder.dndBehavior,
                        groupId = reminder.groupId,
                        availableGroups = groups,
                        isNew = false
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(availableGroups = groups)
            }
        }
    }

    fun updateName(value: String) { _uiState.value = _uiState.value.copy(name = value) }
    fun updateNotificationText(value: String) { _uiState.value = _uiState.value.copy(notificationText = value) }
    fun updateToneUri(value: String?) { _uiState.value = _uiState.value.copy(notificationToneUri = value) }
    fun updateVibrate(value: Boolean) { _uiState.value = _uiState.value.copy(vibrate = value) }
    fun updatePriority(value: Priority) { _uiState.value = _uiState.value.copy(priority = value) }
    fun updateStartTime(value: LocalTime) { _uiState.value = _uiState.value.copy(startTime = value) }
    fun updateEndTime(value: LocalTime) { _uiState.value = _uiState.value.copy(endTime = value) }
    fun updateNotificationCount(value: Int) { _uiState.value = _uiState.value.copy(notificationCount = value) }
    fun toggleDay(dayBit: Int) {
        _uiState.value = _uiState.value.copy(activeDays = _uiState.value.activeDays xor dayBit)
    }
    fun updateDndBehavior(value: DndBehavior) { _uiState.value = _uiState.value.copy(dndBehavior = value) }
    fun updateGroupId(value: Long?) { _uiState.value = _uiState.value.copy(groupId = value) }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            val entity = ReminderEntity(
                id = if (state.isNew) 0 else reminderId,
                name = state.name,
                notificationText = state.notificationText,
                notificationToneUri = state.notificationToneUri,
                vibrate = state.vibrate,
                priority = state.priority,
                startTime = state.startTime,
                endTime = state.endTime,
                notificationCount = state.notificationCount,
                activeDays = state.activeDays,
                dndBehavior = state.dndBehavior,
                isActive = true,
                groupId = state.groupId
            )
            if (state.isNew) {
                reminderRepo.insert(entity)
            } else {
                reminderRepo.update(entity)
            }
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }
}
```

- [ ] **Step 4: Create ReminderEditScreen**

`ReminderEditScreen.kt`:
```kotlin
package com.sporadic.reminder.ui.reminders

import android.media.RingtoneManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sporadic.reminder.domain.model.DndBehavior
import com.sporadic.reminder.domain.model.Priority
import java.time.LocalTime

private val DAYS = listOf("Mon" to 1, "Tue" to 2, "Wed" to 4, "Thu" to 8, "Fri" to 16, "Sat" to 32, "Sun" to 64)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReminderEditViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onNavigateBack()
    }

    val context = LocalContext.current
    val ringtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.getParcelableExtra<android.net.Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        viewModel.updateToneUri(uri?.toString())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "New Reminder" else "Edit Reminder") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::updateName,
                label = { Text("Reminder Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.notificationText,
                onValueChange = viewModel::updateNotificationText,
                label = { Text("Notification Text") },
                modifier = Modifier.fillMaxWidth()
            )

            // Tone picker
            Button(onClick = {
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    state.notificationToneUri?.let {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, android.net.Uri.parse(it))
                    }
                }
                ringtoneLauncher.launch(intent)
            }) {
                Text("Select Notification Tone")
            }

            // Vibrate toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vibrate")
                Switch(checked = state.vibrate, onCheckedChange = viewModel::updateVibrate)
            }

            // Priority
            Text("Priority")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                Priority.entries.forEachIndexed { index, priority ->
                    SegmentedButton(
                        selected = state.priority == priority,
                        onClick = { viewModel.updatePriority(priority) },
                        shape = SegmentedButtonDefaults.itemShape(index, Priority.entries.size)
                    ) {
                        Text(priority.name)
                    }
                }
            }

            // Time window
            // Note: Full TimePicker dialogs would be used in production.
            // For now, using simple text display with the times.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Start: ${state.startTime}")
                Text("End: ${state.endTime}")
            }

            // Notification count
            Text("Notifications per day: ${state.notificationCount}")
            Slider(
                value = state.notificationCount.toFloat(),
                onValueChange = { viewModel.updateNotificationCount(it.toInt()) },
                valueRange = 1f..20f,
                steps = 18
            )

            // Days of week
            Text("Active Days")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DAYS.forEach { (name, bit) ->
                    FilterChip(
                        selected = state.activeDays and bit != 0,
                        onClick = { viewModel.toggleDay(bit) },
                        label = { Text(name) }
                    )
                }
            }

            // DND Behavior
            Text("When Do Not Disturb is active:")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                DndBehavior.entries.forEachIndexed { index, behavior ->
                    SegmentedButton(
                        selected = state.dndBehavior == behavior,
                        onClick = { viewModel.updateDndBehavior(behavior) },
                        shape = SegmentedButtonDefaults.itemShape(index, DndBehavior.entries.size)
                    ) {
                        Text(behavior.name)
                    }
                }
            }

            // Group selection
            if (state.availableGroups.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                val selectedGroup = state.availableGroups.find { it.id == state.groupId }

                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selectedGroup?.name ?: "No group",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Group") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("No group") },
                            onClick = { viewModel.updateGroupId(null); expanded = false }
                        )
                        state.availableGroups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group.name) },
                                onClick = { viewModel.updateGroupId(group.id); expanded = false }
                            )
                        }
                    }
                }
            }

            // Save button
            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.name.isNotBlank() && state.notificationText.isNotBlank()
            ) {
                Text("Save")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
```

Note: add `import android.content.Intent` at the top of ReminderEditScreen.kt.

- [ ] **Step 5: Wire into NavGraph**

Update `SporadicNavGraph.kt` — replace the Reminders and ReminderEdit placeholders:

```kotlin
composable(Screen.ReminderList.route) {
    ReminderListScreen(
        onNavigateToEdit = { id -> navController.navigate(Screen.ReminderEdit.createRoute(id)) },
        onNavigateToNewReminder = { navController.navigate(Screen.ReminderEdit.createNewRoute()) },
        onNavigateToGroupEdit = { id -> navController.navigate(Screen.GroupEdit.createRoute(id)) },
        onNavigateToNewGroup = { navController.navigate(Screen.GroupEdit.createNewRoute()) }
    )
}
composable(
    route = Screen.ReminderEdit.route,
    arguments = listOf(navArgument("reminderId") { type = NavType.LongType })
) {
    ReminderEditScreen(onNavigateBack = { navController.popBackStack() })
}
```

Add imports:
```kotlin
import com.sporadic.reminder.ui.reminders.ReminderListScreen
import com.sporadic.reminder.ui.reminders.ReminderEditScreen
```

- [ ] **Step 6: Verify build and test on emulator**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

Launch on emulator. Verify: Reminders tab shows empty list with FAB. Tap FAB to open edit screen. All form fields visible. Save navigates back.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/sporadic/reminder/ui/reminders/ app/src/main/java/com/sporadic/reminder/ui/navigation/SporadicNavGraph.kt
git commit -m "feat: add Reminder list and edit screens with full form controls"
```

---

## Task 15: Group Edit Screen

**Files:**
- Create: `app/src/main/java/com/sporadic/reminder/ui/groups/GroupEditViewModel.kt`
- Create: `app/src/main/java/com/sporadic/reminder/ui/groups/GroupEditScreen.kt`
- Modify: `app/src/main/java/com/sporadic/reminder/ui/navigation/SporadicNavGraph.kt`

- [ ] **Step 1: Create GroupEditViewModel**

`GroupEditViewModel.kt`:
```kotlin
package com.sporadic.reminder.ui.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sporadic.reminder.data.entity.ReminderGroupEntity
import com.sporadic.reminder.data.repository.GroupRepository
import com.sporadic.reminder.domain.usecase.SyncGroupScheduleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

data class GroupEditUiState(
    val name: String = "",
    val useSharedSchedule: Boolean = false,
    val startTime: LocalTime = LocalTime.of(9, 0),
    val endTime: LocalTime = LocalTime.of(17, 0),
    val notificationCount: Int = 5,
    val activeDays: Int = 0b1111111,
    val isNew: Boolean = true,
    val isSaved: Boolean = false
)

@HiltViewModel
class GroupEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groupRepo: GroupRepository,
    private val syncGroupSchedule: SyncGroupScheduleUseCase
) : ViewModel() {

    private val groupId: Long = savedStateHandle["groupId"] ?: -1L

    private val _uiState = MutableStateFlow(GroupEditUiState())
    val uiState: StateFlow<GroupEditUiState> = _uiState

    init {
        if (groupId != -1L) {
            viewModelScope.launch {
                val group = groupRepo.getById(groupId)
                if (group != null) {
                    _uiState.value = GroupEditUiState(
                        name = group.name,
                        useSharedSchedule = group.startTime != null,
                        startTime = group.startTime ?: LocalTime.of(9, 0),
                        endTime = group.endTime ?: LocalTime.of(17, 0),
                        notificationCount = group.notificationCount ?: 5,
                        activeDays = group.activeDays ?: 0b1111111,
                        isNew = false
                    )
                }
            }
        }
    }

    fun updateName(value: String) { _uiState.value = _uiState.value.copy(name = value) }
    fun toggleSharedSchedule(value: Boolean) { _uiState.value = _uiState.value.copy(useSharedSchedule = value) }
    fun updateStartTime(value: LocalTime) { _uiState.value = _uiState.value.copy(startTime = value) }
    fun updateEndTime(value: LocalTime) { _uiState.value = _uiState.value.copy(endTime = value) }
    fun updateNotificationCount(value: Int) { _uiState.value = _uiState.value.copy(notificationCount = value) }
    fun toggleDay(dayBit: Int) {
        _uiState.value = _uiState.value.copy(activeDays = _uiState.value.activeDays xor dayBit)
    }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            val entity = ReminderGroupEntity(
                id = if (state.isNew) 0 else groupId,
                name = state.name,
                startTime = if (state.useSharedSchedule) state.startTime else null,
                endTime = if (state.useSharedSchedule) state.endTime else null,
                notificationCount = if (state.useSharedSchedule) state.notificationCount else null,
                activeDays = if (state.useSharedSchedule) state.activeDays else null
            )
            if (state.isNew) {
                groupRepo.insert(entity)
            } else {
                groupRepo.update(entity)
                // Re-sync alarms for all group members when schedule changes
                if (state.useSharedSchedule) {
                    syncGroupSchedule.syncGroup(groupId, LocalDate.now(), ZoneId.systemDefault())
                }
            }
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }
}
```

- [ ] **Step 2: Create GroupEditScreen**

`GroupEditScreen.kt`:
```kotlin
package com.sporadic.reminder.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val DAYS = listOf("Mon" to 1, "Tue" to 2, "Wed" to 4, "Thu" to 8, "Fri" to 16, "Sat" to 32, "Sun" to 64)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GroupEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: GroupEditViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "New Group" else "Edit Group") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::updateName,
                label = { Text("Group Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Shared Schedule")
                Switch(checked = state.useSharedSchedule, onCheckedChange = viewModel::toggleSharedSchedule)
            }

            if (state.useSharedSchedule) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Start: ${state.startTime}")
                    Text("End: ${state.endTime}")
                }

                Text("Notifications per day: ${state.notificationCount}")
                Slider(
                    value = state.notificationCount.toFloat(),
                    onValueChange = { viewModel.updateNotificationCount(it.toInt()) },
                    valueRange = 1f..20f,
                    steps = 18
                )

                Text("Active Days")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DAYS.forEach { (name, bit) ->
                        FilterChip(
                            selected = state.activeDays and bit != 0,
                            onClick = { viewModel.toggleDay(bit) },
                            label = { Text(name) }
                        )
                    }
                }
            }

            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.name.isNotBlank()
            ) {
                Text("Save")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
```

- [ ] **Step 3: Wire into NavGraph**

In `SporadicNavGraph.kt`, replace the GroupEdit placeholder:

```kotlin
composable(
    route = Screen.GroupEdit.route,
    arguments = listOf(navArgument("groupId") { type = NavType.LongType })
) {
    GroupEditScreen(onNavigateBack = { navController.popBackStack() })
}
```

Add import: `import com.sporadic.reminder.ui.groups.GroupEditScreen`

- [ ] **Step 4: Verify build and test on emulator**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

Launch on emulator. Navigate to group edit. Verify: name field, shared schedule toggle (shows/hides schedule fields), save works.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/sporadic/reminder/ui/groups/ app/src/main/java/com/sporadic/reminder/ui/navigation/SporadicNavGraph.kt
git commit -m "feat: add Group edit screen with shared schedule toggle"
```

---

## Task 16: Settings Screen

**Files:**
- Create: `app/src/main/java/com/sporadic/reminder/ui/settings/SettingsViewModel.kt`
- Create: `app/src/main/java/com/sporadic/reminder/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/sporadic/reminder/ui/navigation/SporadicNavGraph.kt`

- [ ] **Step 1: Create SettingsViewModel**

`SettingsViewModel.kt`:
```kotlin
package com.sporadic.reminder.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sporadic.reminder.domain.usecase.ExportDataUseCase
import com.sporadic.reminder.domain.usecase.ImportDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val exportStatus: String? = null,
    val importStatus: String? = null
)

enum class ThemeMode { LIGHT, DARK, SYSTEM }

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val exportUseCase: ExportDataUseCase,
    private val importUseCase: ImportDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    fun updateThemeMode(mode: ThemeMode) {
        _uiState.value = _uiState.value.copy(themeMode = mode)
    }

    fun export(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                exportUseCase.export(context, uri)
                _uiState.value = _uiState.value.copy(exportStatus = "Export successful")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(exportStatus = "Export failed: ${e.message}")
            }
        }
    }

    fun import(context: Context, uri: Uri, merge: Boolean) {
        viewModelScope.launch {
            try {
                importUseCase.import(context, uri, merge)
                _uiState.value = _uiState.value.copy(importStatus = "Import successful")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(importStatus = "Import failed: ${e.message}")
            }
        }
    }

    fun clearStatus() {
        _uiState.value = _uiState.value.copy(exportStatus = null, importStatus = null)
    }
}
```

- [ ] **Step 2: Create SettingsScreen**

`SettingsScreen.kt`:
```kotlin
package com.sporadic.reminder.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.export(context, it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.import(context, it, merge = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        // Theme
        Text("Theme", style = MaterialTheme.typography.titleMedium)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = state.themeMode == mode,
                    onClick = { viewModel.updateThemeMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size)
                ) {
                    Text(mode.name)
                }
            }
        }

        // Export/Import
        Text("Data", style = MaterialTheme.typography.titleMedium)
        Button(
            onClick = { exportLauncher.launch("sporadic_backup.json") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Export Data")
        }
        OutlinedButton(
            onClick = { importLauncher.launch(arrayOf("application/json")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Import Data")
        }

        state.exportStatus?.let { Text(it) }
        state.importStatus?.let { Text(it) }
    }
}
```

- [ ] **Step 3: Wire into NavGraph**

In `SporadicNavGraph.kt`, replace the Settings placeholder:

```kotlin
composable(Screen.Settings.route) {
    SettingsScreen()
}
```

Add import: `import com.sporadic.reminder.ui.settings.SettingsScreen`

- [ ] **Step 4: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/sporadic/reminder/ui/settings/ app/src/main/java/com/sporadic/reminder/ui/navigation/SporadicNavGraph.kt
git commit -m "feat: add Settings screen with theme toggle and export/import buttons"
```

---

## Task 17: Export/Import Use Cases

**Files:**
- Create: `app/src/main/java/com/sporadic/reminder/export/ExportSchema.kt`
- Create: `app/src/main/java/com/sporadic/reminder/domain/usecase/ExportDataUseCase.kt`
- Create: `app/src/main/java/com/sporadic/reminder/domain/usecase/ImportDataUseCase.kt`
- Test: `app/src/test/java/com/sporadic/reminder/domain/usecase/ExportDataUseCaseTest.kt`
- Test: `app/src/test/java/com/sporadic/reminder/domain/usecase/ImportDataUseCaseTest.kt`

Note: add `id("org.jetbrains.kotlin.plugin.serialization") version "2.1.10"` to the project-level `build.gradle.kts` plugins block and `id("org.jetbrains.kotlin.plugin.serialization")` to the app-level plugins block.

- [ ] **Step 1: Create export schema**

`ExportSchema.kt`:
```kotlin
package com.sporadic.reminder.export

import kotlinx.serialization.Serializable

@Serializable
data class ExportData(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val reminders: List<ExportReminder>,
    val groups: List<ExportGroup>,
    val logs: List<ExportLog>
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

@Serializable
data class ExportReminder(
    val name: String,
    val notificationText: String,
    val notificationToneUri: String?,
    val vibrate: Boolean,
    val priority: String,
    val startTime: String,
    val endTime: String,
    val notificationCount: Int,
    val activeDays: Int,
    val dndBehavior: String,
    val isActive: Boolean,
    val groupName: String?
)

@Serializable
data class ExportGroup(
    val name: String,
    val startTime: String?,
    val endTime: String?,
    val notificationCount: Int?,
    val activeDays: Int?
)

@Serializable
data class ExportLog(
    val reminderName: String,
    val scheduledTime: Long,
    val firedTime: Long,
    val action: String?,
    val actionTimestamp: Long?
)
```

- [ ] **Step 2: Write ExportDataUseCase test**

`ExportDataUseCaseTest.kt`:
```kotlin
package com.sporadic.reminder.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.sporadic.reminder.data.entity.ReminderEntity
import com.sporadic.reminder.data.entity.ReminderGroupEntity
import com.sporadic.reminder.data.repository.GroupRepository
import com.sporadic.reminder.data.repository.NotificationLogRepository
import com.sporadic.reminder.data.repository.ReminderRepository
import com.sporadic.reminder.domain.model.DndBehavior
import com.sporadic.reminder.domain.model.Priority
import com.sporadic.reminder.export.ExportData
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import java.time.LocalTime

class ExportDataUseCaseTest {
    private lateinit var useCase: ExportDataUseCase
    private val reminderRepo: ReminderRepository = mockk()
    private val groupRepo: GroupRepository = mockk()
    private val logRepo: NotificationLogRepository = mockk()

    @Before
    fun setup() {
        useCase = ExportDataUseCase(reminderRepo, groupRepo, logRepo)
    }

    @Test
    fun `buildExportData includes schema version and all data`() = runTest {
        coEvery { reminderRepo.getAllSync() } returns listOf(
            ReminderEntity(
                id = 1, name = "Water", notificationText = "Drink water",
                notificationToneUri = null, vibrate = true, priority = Priority.DEFAULT,
                startTime = LocalTime.of(9, 0), endTime = LocalTime.of(17, 0),
                notificationCount = 5, activeDays = 0b1111111,
                dndBehavior = DndBehavior.SKIP, isActive = true, groupId = null
            )
        )
        coEvery { groupRepo.getAllSync() } returns emptyList()
        coEvery { logRepo.getAllSync() } returns emptyList()

        val exportData = useCase.buildExportData()

        assertThat(exportData.schemaVersion).isEqualTo(ExportData.CURRENT_SCHEMA_VERSION)
        assertThat(exportData.reminders).hasSize(1)
        assertThat(exportData.reminders[0].name).isEqualTo("Water")

        // Verify it serializes to valid JSON
        val json = Json.encodeToString(ExportData.serializer(), exportData)
        assertThat(json).contains("\"schemaVersion\":1")
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.sporadic.reminder.domain.usecase.ExportDataUseCaseTest"`
Expected: FAIL

- [ ] **Step 4: Implement ExportDataUseCase**

`ExportDataUseCase.kt`:
```kotlin
package com.sporadic.reminder.domain.usecase

import android.content.Context
import android.net.Uri
import com.sporadic.reminder.data.repository.GroupRepository
import com.sporadic.reminder.data.repository.NotificationLogRepository
import com.sporadic.reminder.data.repository.ReminderRepository
import com.sporadic.reminder.export.ExportData
import com.sporadic.reminder.export.ExportGroup
import com.sporadic.reminder.export.ExportLog
import com.sporadic.reminder.export.ExportReminder
import kotlinx.serialization.json.Json
import javax.inject.Inject

class ExportDataUseCase @Inject constructor(
    private val reminderRepo: ReminderRepository,
    private val groupRepo: GroupRepository,
    private val logRepo: NotificationLogRepository
) {
    private val json = Json { prettyPrint = true }

    suspend fun buildExportData(): ExportData {
        val reminders = reminderRepo.getAllSync()
        val groups = groupRepo.getAllSync()
        val logs = logRepo.getAllSync()

        val groupNameMap = groups.associate { it.id to it.name }

        return ExportData(
            reminders = reminders.map { r ->
                ExportReminder(
                    name = r.name,
                    notificationText = r.notificationText,
                    notificationToneUri = r.notificationToneUri,
                    vibrate = r.vibrate,
                    priority = r.priority.name,
                    startTime = r.startTime.toString(),
                    endTime = r.endTime.toString(),
                    notificationCount = r.notificationCount,
                    activeDays = r.activeDays,
                    dndBehavior = r.dndBehavior.name,
                    isActive = r.isActive,
                    groupName = r.groupId?.let { groupNameMap[it] }
                )
            },
            groups = groups.map { g ->
                ExportGroup(
                    name = g.name,
                    startTime = g.startTime?.toString(),
                    endTime = g.endTime?.toString(),
                    notificationCount = g.notificationCount,
                    activeDays = g.activeDays
                )
            },
            logs = logs.map { l ->
                ExportLog(
                    reminderName = reminders.find { it.id == l.reminderId }?.name ?: "Unknown",
                    scheduledTime = l.scheduledTime.toEpochMilli(),
                    firedTime = l.firedTime.toEpochMilli(),
                    action = l.action?.name,
                    actionTimestamp = l.actionTimestamp?.toEpochMilli()
                )
            }
        )
    }

    suspend fun export(context: Context, uri: Uri) {
        val data = buildExportData()
        val jsonString = json.encodeToString(ExportData.serializer(), data)
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(jsonString.toByteArray())
        }
    }
}
```

- [ ] **Step 5: Implement ImportDataUseCase**

`ImportDataUseCase.kt`:
```kotlin
package com.sporadic.reminder.domain.usecase

import android.content.Context
import android.net.Uri
import com.sporadic.reminder.data.entity.ReminderEntity
import com.sporadic.reminder.data.entity.ReminderGroupEntity
import com.sporadic.reminder.data.repository.GroupRepository
import com.sporadic.reminder.data.repository.ReminderRepository
import com.sporadic.reminder.domain.model.DndBehavior
import com.sporadic.reminder.domain.model.Priority
import com.sporadic.reminder.export.ExportData
import kotlinx.serialization.json.Json
import java.time.LocalTime
import javax.inject.Inject

class ImportDataUseCase @Inject constructor(
    private val reminderRepo: ReminderRepository,
    private val groupRepo: GroupRepository
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun import(context: Context, uri: Uri, merge: Boolean) {
        val jsonString = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader().readText()
        } ?: throw IllegalStateException("Could not read file")

        val data = json.decodeFromString(ExportData.serializer(), jsonString)

        if (data.schemaVersion > ExportData.CURRENT_SCHEMA_VERSION) {
            throw IllegalStateException("Unsupported schema version: ${data.schemaVersion}")
        }

        // Import groups first (reminders reference them)
        val groupIdMap = mutableMapOf<String, Long>()
        for (exportGroup in data.groups) {
            if (merge) {
                val existing = groupRepo.getAllSync().find { it.name == exportGroup.name }
                if (existing != null) {
                    groupIdMap[exportGroup.name] = existing.id
                    continue
                }
            }
            val id = groupRepo.insert(
                ReminderGroupEntity(
                    name = exportGroup.name,
                    startTime = exportGroup.startTime?.let { LocalTime.parse(it) },
                    endTime = exportGroup.endTime?.let { LocalTime.parse(it) },
                    notificationCount = exportGroup.notificationCount,
                    activeDays = exportGroup.activeDays
                )
            )
            groupIdMap[exportGroup.name] = id
        }

        // Import reminders
        for (exportReminder in data.reminders) {
            if (merge) {
                val existing = reminderRepo.getAllSync().find { it.name == exportReminder.name }
                if (existing != null) continue
            }
            reminderRepo.insert(
                ReminderEntity(
                    name = exportReminder.name,
                    notificationText = exportReminder.notificationText,
                    notificationToneUri = exportReminder.notificationToneUri,
                    vibrate = exportReminder.vibrate,
                    priority = Priority.valueOf(exportReminder.priority),
                    startTime = LocalTime.parse(exportReminder.startTime),
                    endTime = LocalTime.parse(exportReminder.endTime),
                    notificationCount = exportReminder.notificationCount,
                    activeDays = exportReminder.activeDays,
                    dndBehavior = DndBehavior.valueOf(exportReminder.dndBehavior),
                    isActive = exportReminder.isActive,
                    groupId = exportReminder.groupName?.let { groupIdMap[it] }
                )
            )
        }
    }
}
```

- [ ] **Step 6: Run tests**

Run: `./gradlew testDebugUnitTest --tests "com.sporadic.reminder.domain.usecase.ExportDataUseCaseTest"`
Expected: All tests PASS

- [ ] **Step 7: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/sporadic/reminder/export/ app/src/main/java/com/sporadic/reminder/domain/usecase/ExportDataUseCase.kt app/src/main/java/com/sporadic/reminder/domain/usecase/ImportDataUseCase.kt app/src/test/java/com/sporadic/reminder/domain/usecase/ExportDataUseCaseTest.kt build.gradle.kts app/build.gradle.kts
git commit -m "feat: add JSON export/import with schema versioning and merge/replace support"
```

---

## Task 18: Permissions Handling

**Files:**
- Modify: `app/src/main/java/com/sporadic/reminder/MainActivity.kt`

- [ ] **Step 1: Add runtime permission requests**

Update `MainActivity.kt` to request POST_NOTIFICATIONS and SCHEDULE_EXACT_ALARM on launch:

```kotlin
package com.sporadic.reminder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.sporadic.reminder.ui.navigation.SporadicNavGraph
import com.sporadic.reminder.ui.theme.SporadicTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* User responded — no action needed, system handles it */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestPermissions()
        setContent {
            SporadicTheme {
                SporadicNavGraph()
            }
        }
    }

    private fun requestPermissions() {
        // POST_NOTIFICATIONS (Android 13+)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // SCHEDULE_EXACT_ALARM (Android 14+ requires explicit user grant)
        val alarmManager = getSystemService(android.app.AlarmManager::class.java)
        if (!alarmManager.canScheduleExactAlarms()) {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
        }
    }
}
```

- [ ] **Step 2: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Test on emulator**

Launch app. Verify: notification permission dialog appears. Exact alarm permission screen opens in system settings.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/sporadic/reminder/MainActivity.kt
git commit -m "feat: add runtime permission requests for notifications and exact alarms"
```

---

## Task 19: Final Integration Test

- [ ] **Step 1: Run full test suite**

Run: `./gradlew testDebugUnitTest`
Expected: All unit tests PASS

- [ ] **Step 2: Run instrumented tests**

Run: `./gradlew connectedDebugAndroidTest`
Expected: All instrumented tests PASS

- [ ] **Step 3: Run lint**

Run: `./gradlew lintDebug`
Expected: No errors (warnings acceptable)

- [ ] **Step 4: Full manual test on emulator**

1. Launch app — verify Dashboard shows with 0 stats
2. Navigate to Reminders tab — verify empty list with FAB
3. Tap FAB — create a reminder with: name "Hydrate", text "Drink water", 3 notifications, 9am-5pm, all days
4. Save — verify it appears in the list
5. Toggle reminder active/inactive — verify switch works
6. Navigate to Settings — verify theme toggle and export button
7. Tap Export — verify file picker opens
8. Navigate back to Dashboard — verify any pending alarms show

- [ ] **Step 5: Update CHANGELOG.md**

Add under `[Unreleased]`:
```markdown
### Added
- Reminder creation and management with full customization
- Reminder groups with optional shared schedules
- Random notification scheduling within time windows
- Dashboard with today's stats and activity log
- Notification actions: Done / Skip / Snooze
- DND-aware scheduling (skip or snooze per reminder)
- Boot recovery for scheduled alarms
- Export/import data as JSON
- Material 3 with dynamic colors and dark mode
- Runtime permission handling for notifications and exact alarms
```

- [ ] **Step 6: Commit**

```bash
git add CHANGELOG.md
git commit -m "docs: update CHANGELOG with MVP feature list"
```
