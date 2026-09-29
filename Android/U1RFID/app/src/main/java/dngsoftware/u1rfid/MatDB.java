package dngsoftware.u1rfid;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface MatDB {

   @SuppressWarnings("UnusedDeclaration")
   @Insert
   void addItem(Filament item);

   @SuppressWarnings("UnusedDeclaration")
   @Update
   void updateItem(Filament item);

   @SuppressWarnings("UnusedDeclaration")
   @Query("UPDATE filament_table SET filament_position =:pos WHERE filament_id =:filamentID")
   void updatePosition(int pos, String filamentID);

   @SuppressWarnings("UnusedDeclaration")
   @Delete
   void deleteItem(Filament item);

   @SuppressWarnings("UnusedDeclaration")
   @Query("SELECT COUNT(dbKey) FROM filament_table")
   int getItemCount();

   @SuppressWarnings("UnusedDeclaration")
   @Query("SELECT * FROM filament_table ORDER BY filament_position ASC")
   List<Filament> getAllItems();

   @SuppressWarnings("UnusedDeclaration")
   @Query("DELETE FROM filament_table")
   void deleteAll();

   @SuppressWarnings("UnusedDeclaration")
   @Query("SELECT * FROM filament_table WHERE TRIM(filament_id) = TRIM(:filamentID)")
   Filament getFilamentById(String filamentID);

   @SuppressWarnings("UnusedDeclaration")
   @Query("SELECT * FROM filament_table WHERE catalog_key = :catalogKey LIMIT 1")
   Filament getFilamentByCatalogKey(String catalogKey);

   // The revision this database was last synced to, taken from the rows rather than
   // a preference so that importing a database brings its revision along with it.
   @SuppressWarnings("UnusedDeclaration")
   @Query("SELECT IFNULL(MAX(catalog_revision), 0) FROM filament_table")
   int getCatalogRevision();

   @SuppressWarnings("UnusedDeclaration")
   @Insert(onConflict = OnConflictStrategy.REPLACE)
   void addTombstone(CatalogTombstone tombstone);

   @SuppressWarnings("UnusedDeclaration")
   @Query("SELECT catalog_key FROM catalog_tombstone")
   List<String> getTombstoneKeys();

   @SuppressWarnings("UnusedDeclaration")
   @Query("DELETE FROM catalog_tombstone WHERE catalog_key = :catalogKey")
   void deleteTombstone(String catalogKey);

   @SuppressWarnings("UnusedDeclaration")
   @Query("DELETE FROM catalog_tombstone")
   void deleteAllTombstones();

}