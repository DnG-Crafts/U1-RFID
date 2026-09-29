package dngsoftware.u1rfid;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.io.File;

@Database(entities = {Filament.class, CatalogTombstone.class}, version = 2, exportSchema = false)
public abstract class filamentDB extends RoomDatabase {
    public abstract MatDB matDB();
    public static volatile filamentDB INSTANCE;

    public static filamentDB getInstance(Context context) {
        closeInstance();
        synchronized (filamentDB.class) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(), filamentDB.class, "filament_database")
                    .addMigrations(MIGRATION_1_2)
                    .allowMainThreadQueries()
                    .build();

        }
        return INSTANCE;
    }

    // Adds the catalogue bookkeeping columns and the tombstone table. The rows
    // themselves are adopted on first launch, see FilamentCatalog.adoptUntrackedRows.
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE filament_table ADD COLUMN catalog_key TEXT");
            database.execSQL("ALTER TABLE filament_table ADD COLUMN catalog_revision INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE filament_table ADD COLUMN catalog_signature TEXT");
            database.execSQL("CREATE TABLE IF NOT EXISTS `catalog_tombstone` ("
                    + "`catalog_key` TEXT NOT NULL, `revision` INTEGER NOT NULL, PRIMARY KEY(`catalog_key`))");
        }
    };

    public static File getDatabaseFile(Context context) {
        return context.getDatabasePath("filament_database");
    }

    public static void closeInstance() {
        if (INSTANCE != null && INSTANCE.isOpen()) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }


}