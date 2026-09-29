package dngsoftware.u1rfid;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "filament_table")
public class Filament {

        @SuppressWarnings("UnusedDeclaration")
        @PrimaryKey(autoGenerate = true)
        public int dbKey;

        @SuppressWarnings("UnusedDeclaration")
        @ColumnInfo(name = "filament_position")
        public int position;

        @SuppressWarnings("UnusedDeclaration")
        @ColumnInfo(name = "filament_name")
        public String filamentName;

        @SuppressWarnings("UnusedDeclaration")
        @ColumnInfo(name = "filament_id")
        public String filamentID;

        @SuppressWarnings("UnusedDeclaration")
        @ColumnInfo(name = "filament_vendor")
        public String filamentVendor;

        @SuppressWarnings("UnusedDeclaration")
        @ColumnInfo(name = "filament_param")
        public String filamentParam;

        // Set for rows owned by the stock catalogue, null for filaments the user added.
        @SuppressWarnings("UnusedDeclaration")
        @ColumnInfo(name = "catalog_key")
        public String catalogKey;

        @SuppressWarnings("UnusedDeclaration")
        @ColumnInfo(name = "catalog_revision", defaultValue = "0")
        public int catalogRevision;

        // Contents as the catalogue last wrote them; differs once the user edits the row.
        @SuppressWarnings("UnusedDeclaration")
        @ColumnInfo(name = "catalog_signature")
        public String catalogSignature;

}