package fi.ville.treenipaivakirja.data

import android.content.Context
import androidx.room.ColumnInfo
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
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

/** Treeniohjelma kirjastossa, esim. "Yläkroppa 1". */
@Entity(tableName = "templates")
data class Template(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

/** Ohjelman liike tavoitteineen. */
@Entity(
    tableName = "template_exercises",
    foreignKeys = [
        ForeignKey(entity = Template::class, parentColumns = ["id"], childColumns = ["templateId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Exercise::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("templateId"), Index("exerciseId")]
)
data class TemplateExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val exerciseId: Long,
    val position: Int,
    val targetSets: Int,
    val targetReps: Int,                                   // toistohaarukan alaraja (tai kiinteä)
    @ColumnInfo(defaultValue = "0") val targetRepsMax: Int = 0  // yläraja, 0 = ei haarukkaa
)

/** Päivälle suunniteltu/kirjattu liike (järjestys + tavoite). */
@Entity(
    tableName = "day_exercises",
    foreignKeys = [ForeignKey(entity = Exercise::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["epochDay", "exerciseId"], unique = true), Index("exerciseId")]
)
data class DayExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val exerciseId: Long,
    val position: Int,
    val targetSets: Int,
    val targetReps: Int,                                   // toistohaarukan alaraja (tai kiinteä)
    @ColumnInfo(defaultValue = "0") val targetRepsMax: Int = 0  // yläraja, 0 = ei haarukkaa
)

@Dao
interface WorkoutDao {
    // ----- Liikkeet -----
    @Query("SELECT * FROM exercises ORDER BY name COLLATE NOCASE")
    fun exercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findExercise(name: String): Exercise?

    @Insert
    suspend fun insertExercise(exercise: Exercise): Long

    // ----- Sarjat -----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<WorkoutSet>)

    @Delete
    suspend fun deleteSet(set: WorkoutSet)

    @Query("DELETE FROM sets WHERE epochDay = :day AND exerciseId = :exerciseId")
    suspend fun deleteSetsFor(day: Long, exerciseId: Long)

    @Query("SELECT * FROM sets WHERE epochDay = :day ORDER BY createdAt")
    fun setsForDay(day: Long): Flow<List<WorkoutSet>>

    @Query("SELECT * FROM sets WHERE exerciseId = :exerciseId ORDER BY epochDay, createdAt")
    fun setsForExercise(exerciseId: Long): Flow<List<WorkoutSet>>

    @Query("SELECT DISTINCT epochDay FROM sets")
    fun trainingDays(): Flow<List<Long>>

    /** Jokaisen liikkeen edellisen treenikerran sarjat ennen annettua päivää. */
    @Query(
        """
        SELECT s.* FROM sets s
        WHERE s.epochDay = (SELECT MAX(s2.epochDay) FROM sets s2
                            WHERE s2.exerciseId = s.exerciseId AND s2.epochDay < :day)
        ORDER BY s.createdAt
        """
    )
    fun previousSessions(day: Long): Flow<List<WorkoutSet>>

    // ----- Päivän liikkeet -----
    @Query("SELECT * FROM day_exercises WHERE epochDay = :day ORDER BY position, id")
    fun dayExercises(day: Long): Flow<List<DayExercise>>

    @Query("SELECT COALESCE(MAX(position), -1) FROM day_exercises WHERE epochDay = :day")
    suspend fun maxDayPosition(day: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDayExercises(items: List<DayExercise>)

    @Query("DELETE FROM day_exercises WHERE epochDay = :day AND exerciseId = :exerciseId")
    suspend fun deleteDayExercise(day: Long, exerciseId: Long)

    // ----- Ohjelmat -----
    @Query("SELECT * FROM templates ORDER BY name COLLATE NOCASE")
    fun templates(): Flow<List<Template>>

    @Query("SELECT * FROM template_exercises ORDER BY templateId, position")
    fun templateExercises(): Flow<List<TemplateExercise>>

    @Query("SELECT * FROM template_exercises WHERE templateId = :templateId ORDER BY position")
    suspend fun templateExercisesOf(templateId: Long): List<TemplateExercise>

    @Insert
    suspend fun insertTemplate(template: Template): Long

    @Update
    suspend fun updateTemplate(template: Template)

    @Query("DELETE FROM template_exercises WHERE templateId = :templateId")
    suspend fun clearTemplate(templateId: Long)

    @Insert
    suspend fun insertTemplateExercises(items: List<TemplateExercise>)

    @Query("DELETE FROM templates WHERE id = :templateId")
    suspend fun deleteTemplate(templateId: Long)
}

/** Version 2 skeema (jäädytetty, tarkistettu CI:ssä v2-käännöksessä). Käytetään migraatiossa 1 -> 2. */
object Schema2 {
    const val TEMPLATES =
        "CREATE TABLE IF NOT EXISTS `templates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)"
    const val TEMPLATE_EXERCISES =
        "CREATE TABLE IF NOT EXISTS `template_exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `templateId` INTEGER NOT NULL, `exerciseId` INTEGER NOT NULL, `position` INTEGER NOT NULL, `targetSets` INTEGER NOT NULL, `targetReps` INTEGER NOT NULL, FOREIGN KEY(`templateId`) REFERENCES `templates`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
    const val IDX_TE_TEMPLATE =
        "CREATE INDEX IF NOT EXISTS `index_template_exercises_templateId` ON `template_exercises` (`templateId`)"
    const val IDX_TE_EXERCISE =
        "CREATE INDEX IF NOT EXISTS `index_template_exercises_exerciseId` ON `template_exercises` (`exerciseId`)"
    const val DAY_EXERCISES =
        "CREATE TABLE IF NOT EXISTS `day_exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `epochDay` INTEGER NOT NULL, `exerciseId` INTEGER NOT NULL, `position` INTEGER NOT NULL, `targetSets` INTEGER NOT NULL, `targetReps` INTEGER NOT NULL, FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
    const val IDX_DE_DAY_EXERCISE =
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_day_exercises_epochDay_exerciseId` ON `day_exercises` (`epochDay`, `exerciseId`)"
    const val IDX_DE_EXERCISE =
        "CREATE INDEX IF NOT EXISTS `index_day_exercises_exerciseId` ON `day_exercises` (`exerciseId`)"
}

/**
 * Roomin generoimat CREATE-lauseet nykyiselle skeemalle niille tauluille, joita migraatiot muuttavat.
 * CI tarkistaa, että nämä vastaavat Roomia (ja siten että ALTER-lauseet tuottavat oikean skeeman).
 */
object SchemaCheck {
    const val TEMPLATE_EXERCISES_V3 =
        "CREATE TABLE IF NOT EXISTS `template_exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `templateId` INTEGER NOT NULL, `exerciseId` INTEGER NOT NULL, `position` INTEGER NOT NULL, `targetSets` INTEGER NOT NULL, `targetReps` INTEGER NOT NULL, `targetRepsMax` INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(`templateId`) REFERENCES `templates`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
    const val DAY_EXERCISES_V3 =
        "CREATE TABLE IF NOT EXISTS `day_exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `epochDay` INTEGER NOT NULL, `exerciseId` INTEGER NOT NULL, `position` INTEGER NOT NULL, `targetSets` INTEGER NOT NULL, `targetReps` INTEGER NOT NULL, `targetRepsMax` INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
}

/** v2 -> v3: toistohaarukan yläraja. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `template_exercises` ADD COLUMN `targetRepsMax` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `day_exercises` ADD COLUMN `targetRepsMax` INTEGER NOT NULL DEFAULT 0")
    }
}

/** v1 -> v2: ohjelmat ja päivän liikkeet. Vanhat sarjat säilyvät. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(Schema2.TEMPLATES)
        db.execSQL(Schema2.TEMPLATE_EXERCISES)
        db.execSQL(Schema2.IDX_TE_TEMPLATE)
        db.execSQL(Schema2.IDX_TE_EXERCISE)
        db.execSQL(Schema2.DAY_EXERCISES)
        db.execSQL(Schema2.IDX_DE_DAY_EXERCISE)
        db.execSQL(Schema2.IDX_DE_EXERCISE)
        // Olemassa olevat kirjaukset päivän liikkeiksi
        db.execSQL(
            "INSERT OR IGNORE INTO day_exercises (epochDay, exerciseId, position, targetSets, targetReps) " +
                "SELECT epochDay, exerciseId, 0, 0, 0 FROM sets GROUP BY epochDay, exerciseId ORDER BY MIN(createdAt)"
        )
    }
}

@Database(
    entities = [Exercise::class, WorkoutSet::class, Template::class, TemplateExercise::class, DayExercise::class],
    version = 3,
    exportSchema = false
)
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
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
    }
}
