package dngsoftware.u1rfid;

import org.json.JSONObject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * The stock filament catalogue, and the rules for carrying changes to it into a
 * database that is already populated.
 *
 * Each entry records the revision in which its contents last changed. On upgrade
 * only entries newer than the revision the database was last synced to are
 * considered, and an entry is applied only if the user has not edited that row.
 * An entry or removal marked forced is applied regardless, for changes that must
 * not be left behind - a filament type the app no longer supports, say.
 *
 * A row is recognised as edited by comparing its current contents against the
 * signature stored when the catalogue last wrote it. That means no separate
 * "modified" flag to keep in step, and an edit made by any code path counts.
 *
 * To publish a catalogue change: bump REVISION, add or amend entries and give
 * them the new revision, list anything withdrawn in REMOVALS, and set forced
 * only where a user's own edit must not be allowed to win.
 */
public final class FilamentCatalog {

    // Bump whenever ENTRIES or REMOVALS change.
    public static final int REVISION = 3;

    public static final class Entry {
        public final String key, id, brand, type, subtype;
        public final int extMin, extMax, bedMin, bedMax;
        public final int revision;
        public final boolean forced;

        Entry(String brand, String type, String subtype, int extMin, int extMax,
              int bedMin, int bedMax, int revision, boolean forced) {
            this.key = keyOf(brand, type, subtype);
            this.id = idOf(this.key);
            this.brand = brand;
            this.type = type;
            this.subtype = subtype;
            this.extMin = extMin;
            this.extMax = extMax;
            this.bedMin = bedMin;
            this.bedMax = bedMax;
            this.revision = revision;
            this.forced = forced;
        }

        public String signature() {
            return signatureOf(brand, type, subtype, extMin, extMax, bedMin, bedMax);
        }
    }

    public static final class Removal {
        public final String key;
        public final int revision;
        public final boolean forced;

        Removal(String brand, String type, String subtype, int revision, boolean forced) {
            this.key = keyOf(brand, type, subtype);
            this.revision = revision;
            this.forced = forced;
        }
    }

    public enum Action { INSERT, UPDATE, DELETE, SKIP }

    private static Entry e(String brand, String type, String subtype, int extMin, int extMax,
                           int bedMin, int bedMax, int revision) {
        return new Entry(brand, type, subtype, extMin, extMax, bedMin, bedMax, revision, false);
    }

    @SuppressWarnings("unused")
    private static Entry forced(String brand, String type, String subtype, int extMin, int extMax,
                                int bedMin, int bedMax, int revision) {
        return new Entry(brand, type, subtype, extMin, extMax, bedMin, bedMax, revision, true);
    }

    private static Removal r(String brand, String type, String subtype, int revision, boolean forced) {
        return new Removal(brand, type, subtype, revision, forced);
    }

    public static final List<Entry> ENTRIES = Arrays.asList(
        e("Snapmaker", "PLA", "Matte", 190, 220, 50, 60, 1),
        e("Snapmaker", "PLA", "SnapSpeed", 210, 230, 50, 60, 1),
        e("Snapmaker", "PLA", "Basic", 190, 210, 50, 60, 1),
        e("Snapmaker", "PLA", "Support", 180, 200, 50, 60, 1),
        e("Snapmaker", "PETG", "Basic", 230, 250, 70, 80, 1),
        e("Snapmaker", "PETG", "HF", 240, 260, 70, 80, 1),
        e("Snapmaker", "TPU", "95A", 210, 230, 30, 50, 1),
        e("Snapmaker", "TPU", "95A HF", 220, 240, 30, 50, 1),
        e("Snapmaker", "PVA", "Basic", 180, 200, 50, 60, 1),
        e("Snapmaker", "ABS", "Basic", 240, 260, 90, 110, 1),
        e("Polymaker", "PLA", "Polylite", 190, 230, 40, 60, 1),
        e("Polymaker", "PLA", "PolySonic", 210, 240, 40, 60, 1),
        e("Polymaker", "PLA", "PolyTerra", 190, 230, 30, 60, 1),
        e("Polymaker", "ABS", "Polylite", 245, 265, 90, 100, 1),
        e("Polymaker", "PETG", "Polylite", 230, 240, 70, 80, 1),
        e("ELEGOO", "PLA", "Basic", 190, 240, 60, 70, 2),
        e("ELEGOO", "PLA", "CF", 190, 240, 60, 70, 2),
        e("ELEGOO", "PLA", "Galaxy", 190, 220, 60, 70, 2),
        e("ELEGOO", "PLA", "Glow in the Dark", 190, 230, 60, 70, 2),
        e("ELEGOO", "PLA", "Marble", 190, 220, 60, 70, 2),
        e("ELEGOO", "PLA", "Matte", 190, 240, 60, 70, 2),
        e("ELEGOO", "PLA", "PRO", 190, 240, 60, 70, 2),
        e("ELEGOO", "PLA", "Silk", 190, 240, 60, 70, 2),
        e("ELEGOO", "PLA", "Sparkle", 190, 220, 60, 70, 2),
        e("ELEGOO", "PLA", "Translucent", 190, 240, 60, 70, 2),
        e("ELEGOO", "PLA", "Wood", 200, 240, 60, 70, 2),
        e("ELEGOO", "PLA", "PLA+", 190, 240, 60, 70, 3),
        e("ELEGOO", "PLA", "Rapid PLA+", 190, 240, 60, 70, 3),
        e("ELEGOO", "PETG", "Basic", 230, 270, 70, 80, 2),
        e("ELEGOO", "PETG", "CF", 240, 270, 70, 80, 2),
        e("ELEGOO", "PETG", "GF", 240, 270, 70, 80, 2),
        e("ELEGOO", "PETG", "HF", 230, 250, 70, 80, 2),
        e("ELEGOO", "PETG", "PRO", 230, 270, 70, 80, 2),
        e("ELEGOO", "PETG", "Rapid", 230, 270, 70, 80, 2),
        e("ELEGOO", "PETG", "Translucent", 230, 270, 70, 80, 2),
        e("ELEGOO", "ABS", "Basic", 240, 280, 90, 100, 2),
        e("ELEGOO", "ASA", "Basic", 240, 280, 90, 100, 2),
        e("ELEGOO", "ASA", "CF", 240, 280, 90, 100, 2),
        e("ELEGOO", "TPU", "95A", 200, 250, 35, 45, 2),
        e("ELEGOO", "TPU", "Rapid 95A", 200, 230, 35, 45, 2),
        e("ELEGOO", "PC", "Basic", 250, 270, 110, 120, 2),
        e("ELEGOO", "PC", "Flame Retardant", 260, 290, 110, 120, 2),
        e("ELEGOO", "PAHT", "CF", 260, 290, 60, 70, 2),
        e("ELEGOO", "PET", "CF", 260, 290, 70, 80, 2),
        e("Generic", "PLA", "Basic", 200, 220, 50, 60, 1),
        e("Generic", "PETG", "Basic", 230, 250, 70, 85, 1),
        e("Generic", "ABS", "Basic", 230, 260, 100, 110, 1),
        e("Generic", "TPU", "95A", 220, 240, 40, 60, 1),
        e("Generic", "TPU", "95A HF", 230, 250, 40, 60, 1),
        e("Generic", "ASA", "Basic", 240, 260, 100, 110, 1),
        e("Generic", "BVOH", "Basic", 190, 210, 50, 60, 1),
        e("Generic", "EVA", "Basic", 180, 210, 30, 50, 1),
        e("Generic", "HIPS", "Basic", 220, 240, 90, 110, 1),
        e("Generic", "PA", "Basic", 260, 290, 80, 100, 1),
        e("Generic", "PA", "CF", 270, 300, 80, 100, 1),
        e("Generic", "PC", "Basic", 270, 300, 100, 120, 1),
        e("Generic", "PCTG", "Basic", 250, 270, 70, 80, 1),
        e("Generic", "PE", "Basic", 220, 250, 70, 100, 1),
        e("Generic", "PE", "CF", 230, 260, 70, 100, 1),
        e("Generic", "PHA", "Basic", 190, 210, 40, 60, 1),
        e("Generic", "PLA", "Silk", 205, 225, 50, 60, 1),
        e("Generic", "PLA", "CF", 210, 230, 50, 60, 1),
        e("Generic", "PVA", "Basic", 190, 210, 50, 60, 1),
        e("Generic", "PLA", "Support", 190, 210, 50, 60, 1)
    );

    // Entries withdrawn from the catalogue. A forced removal is applied even to a
    // row the user has edited, for a filament the app can no longer write correctly.
    //
    // Revision 3 withdrew PLA+ as a filament type; it is a grade of PLA, and the
    // picker no longer offers it. These are forced because a row left behind would
    // name a type nothing can select or write.
    public static final List<Removal> REMOVALS = Arrays.asList(
        r("ELEGOO", "PLA+", "Basic", 3, true),
        r("ELEGOO", "PLA+", "Rapid", 3, true)
    );

    public static String keyOf(String brand, String type, String subtype) {
        return (brand + "|" + type + "|" + subtype).toLowerCase(java.util.Locale.ROOT);
    }

    /*
     * The filament id for a catalogue row, which is not its key. The key names the entry
     * for the catalogue's own bookkeeping and can be as long as it likes. The id is what
     * an ACE tag carries, and that round trips through sixteen bytes: writeAceTag lays
     * down twenty at page 33 and readAceTag takes sixteen back, so anything longer comes
     * back truncated and matches nothing.
     *
     * Eight hex digits of the key's hash. String.hashCode is specified by the language, so
     * the same entry keeps the same id everywhere and across releases.
     */
    public static String idOf(String key) {
        return String.format(java.util.Locale.ROOT, "%08x", key.hashCode());
    }

    private static String signatureOf(String brand, String type, String subtype,
                                      int extMin, int extMax, int bedMin, int bedMax) {
        return brand + "|" + type + "|" + subtype + "|"
                + extMin + "|" + extMax + "|" + bedMin + "|" + bedMax;
    }

    // The catalogue owns brand, type, subtype and the four temperatures. Colour,
    // weight and diameter are chosen per write, so changing them is not an edit.
    public static String signatureOf(Filament row) {
        try {
            JSONObject j = new JSONObject(row.filamentParam);
            return signatureOf(j.optString("brand"), j.optString("type"), j.optString("subtype"),
                    j.optInt("min_temp"), j.optInt("max_temp"),
                    j.optInt("bed_min_temp"), j.optInt("bed_max_temp"));
        } catch (Exception ignored) {
            return "";
        }
    }

    public static boolean isUserEdited(Filament row) {
        return row.catalogSignature != null && !row.catalogSignature.equals(signatureOf(row));
    }

    static Action decideEntry(Entry entry, Filament row, boolean withdrawnByUser) {
        if (row == null) {
            return (withdrawnByUser && !entry.forced) ? Action.SKIP : Action.INSERT;
        }
        if (isUserEdited(row) && !entry.forced) return Action.SKIP;
        return entry.signature().equals(signatureOf(row)) ? Action.SKIP : Action.UPDATE;
    }

    static Action decideRemoval(Removal removal, Filament row) {
        if (row == null) return Action.SKIP;
        if (isUserEdited(row) && !removal.forced) return Action.SKIP;
        return Action.DELETE;
    }

    public static Filament toFilament(Entry entry, int position) {
        Filament f = new Filament();
        f.position = position;
        f.filamentID = entry.id;
        f.filamentName = entry.type;
        f.filamentVendor = entry.brand;
        f.filamentParam = paramOf(entry, entry.id);
        f.catalogKey = entry.key;
        f.catalogRevision = entry.revision;
        f.catalogSignature = entry.signature();
        return f;
    }

    private static String paramOf(Entry entry, String id) {
        try {
            OpenSpoolFilament osf = new OpenSpoolFilament();
            osf.setID(id);
            osf.setType(entry.brand, entry.type, entry.subtype);
            osf.setTemps(entry.extMin, entry.extMax, entry.bedMin, entry.bedMax);
            osf.setPhysicals(1.75, 1000);
            osf.setColor("0000FF", "FF");
            return osf.toString();
        } catch (Exception ignored) {
            return "{}";
        }
    }

    /*
     * Adopts the rows of a database that predates this tracking. Anything matching a
     * catalogue entry is claimed and stamped with that entry's signature, so a row the
     * user had already edited reads as edited from here on. Anything else is left alone
     * as the user's own.
     *
     * Every entry is considered, not just the oldest, because an untracked database can
     * already hold later ones: a build carrying a newer catalogue but not yet this
     * tracking seeds them with no key. Matching only the oldest entries would leave
     * those unclaimed, and the sync below would then add each of them a second time.
     *
     * A row is compared against the entry as it stands now. If a later revision amends
     * an entry, a stock row upgrading straight from an untracked database reads as
     * edited and is left alone, which errs towards keeping what the user has.
     */
    public static void adoptUntrackedRows(MatDB db) {
        Set<String> withdrawn = new HashSet<>(db.getTombstoneKeys());
        for (Filament row : db.getAllItems()) {
            if (row.catalogKey != null) continue;
            String key = keyOf(row.filamentVendor, typeOf(row), subTypeOf(row));
            if (withdrawn.contains(key)) continue;
            for (Entry entry : ENTRIES) {
                if (!entry.key.equals(key)) continue;
                if (db.getFilamentByCatalogKey(entry.key) != null) {
                    // An earlier run already added this entry. Drop the untracked copy
                    // if it is still stock, but keep it if the user has changed it.
                    if (entry.signature().equals(signatureOf(row))) db.deleteItem(row);
                    break;
                }
                row.catalogKey = entry.key;
                row.catalogRevision = entry.revision;
                row.catalogSignature = entry.signature();
                db.updateItem(row);
                break;
            }
        }
    }

    private static String typeOf(Filament row) {
        try { return new JSONObject(row.filamentParam).optString("type"); }
        catch (Exception ignored) { return ""; }
    }

    private static String subTypeOf(Filament row) {
        try { return new JSONObject(row.filamentParam).optString("subtype"); }
        catch (Exception ignored) { return ""; }
    }

    // Brings a database up to REVISION. Returns the number of rows touched.
    public static int sync(MatDB db, int fromRevision) {
        int changed = 0;
        if (fromRevision >= REVISION) return changed;

        Set<String> withdrawn = new HashSet<>(db.getTombstoneKeys());
        int position = db.getItemCount();

        for (Entry entry : ENTRIES) {
            if (entry.revision <= fromRevision) continue;
            Filament row = db.getFilamentByCatalogKey(entry.key);
            switch (decideEntry(entry, row, withdrawn.contains(entry.key))) {
                case INSERT:
                    db.addItem(toFilament(entry, position++));
                    if (entry.forced) db.deleteTombstone(entry.key);
                    changed++;
                    break;
                case UPDATE:
                    Filament stock = toFilament(entry, row.position);
                    stock.dbKey = row.dbKey;
                    stock.filamentID = row.filamentID;
                    stock.filamentParam = paramOf(entry, row.filamentID);
                    db.updateItem(stock);
                    changed++;
                    break;
                default:
                    break;
            }
        }

        for (Removal removal : REMOVALS) {
            if (removal.revision <= fromRevision) continue;
            Filament row = db.getFilamentByCatalogKey(removal.key);
            db.deleteTombstone(removal.key);
            if (decideRemoval(removal, row) == Action.DELETE) {
                db.deleteItem(row);
                changed++;
            }
        }
        return changed;
    }

    // Restores every catalogue row to stock, leaving filaments the user added alone.
    public static void reset(MatDB db) {
        for (Filament row : db.getAllItems()) {
            if (row.catalogKey != null) db.deleteItem(row);
        }
        db.deleteAllTombstones();
        sync(db, 0);
    }

    private FilamentCatalog() {}
}
