package com.example.myapplication

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "todos")
data class TodoItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val priority: Priority = Priority.MEDIUM,
    val done: Boolean = false,
    val position: Int = 0
)

class Converters {
    @TypeConverter
    fun fromPriority(p: Priority): String = p.name

    @TypeConverter
    fun toPriority(s: String): Priority = Priority.valueOf(s)
}

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY position ASC, id ASC")
    fun observeAll(): Flow<List<TodoItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TodoItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TodoItem>)

    @Update
    suspend fun update(item: TodoItem)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM todos WHERE done = 1")
    suspend fun getDone(): List<TodoItem>

    @Query("DELETE FROM todos WHERE done = 1")
    suspend fun deleteDone()

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM todos")
    suspend fun nextPosition(): Int
}

@Database(entities = [TodoItem::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "todo.db"
                ).build().also { INSTANCE = it }
            }
    }
}