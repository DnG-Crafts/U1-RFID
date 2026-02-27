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

}