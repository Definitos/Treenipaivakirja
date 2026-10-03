package fi.ville.treenipaivakirja.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** Liike, esim. "Penkkipunnerrus". */
@Entity(tableName = "exercises", indices = [Index(value = ["name"], unique = true)])
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

/** Yksi sarja: tietty liike, päivä, toistot ja kilot. */
@Entity(
    tableName = "sets",
    foreignKeys = [ForeignKey(
        entity = Exercise::class,
        parentColumns = ["id"],
        childColumns = ["exerciseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("exerciseId"), Index("epochDay")]
)
data class WorkoutSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val epochDay: Long,          // LocalDate.toEpochDay()
    val reps: Int,
    val weight: Double,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM exercises ORDER BY name COLLATE NOCASE")
    fun exercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findExercise(name: String): Exercise?

    @Insert
    suspend fun insertExercise(exercise: Exercise): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<WorkoutSet>)

    @Delete
    suspend fun deleteSet(set: WorkoutSet)

    @Query("SELECT * FROM sets WHERE epochDay = :day ORDER BY createdAt")
    fun setsForDay(day: Long): Flow<List<WorkoutSet>>

    @Query("SELECT * FROM sets WHERE exerciseId = :exerciseId ORDER BY epochDay, createdAt")
    fun setsForExercise(exerciseId: Long): Flow<List<WorkoutSet>>

    @Query("SELECT DISTINCT epochDay FROM sets")
    fun trainingDays(): Flow<List<Long>>
}

@Database(entities = [Exercise::class, WorkoutSet::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): WorkoutDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "treenit.db"
                ).build().also { instance = it }
            }
    }
}
