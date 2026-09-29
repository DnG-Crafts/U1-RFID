package dngsoftware.u1rfid;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

// Records a catalogue row the user deleted, so an upgrade does not put it back.
// A forced entry overrides this and clears the tombstone.
@Entity(tableName = "catalog_tombstone")
public class CatalogTombstone {

        @NonNull
        @PrimaryKey
        @ColumnInfo(name = "catalog_key")
        public String catalogKey = "";

        @SuppressWarnings("UnusedDeclaration")
        @ColumnInfo(name = "revision")
        public int revision;

}
