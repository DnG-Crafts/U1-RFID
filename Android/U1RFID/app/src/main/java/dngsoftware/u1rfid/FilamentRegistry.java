package dngsoftware.u1rfid;
import java.util.*;

public class FilamentRegistry {

    public static class FilamentProfile {
        public final int minNozzleTemp;
        public final int maxNozzleTemp;
        public final int minBedTemp;
        public final int maxBedTemp;
        public final List<String> subtypes;

        public FilamentProfile(int minNozzle, int maxNozzle, int minBed, int maxBed, List<String> subtypes) {
            this.minNozzleTemp = minNozzle;
            this.maxNozzleTemp = maxNozzle;
            this.minBedTemp = minBed;
            this.maxBedTemp = maxBed;
            // Listed here by family for editing, shown alphabetically for picking.
            List<String> sorted = new ArrayList<>(subtypes);
            Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);
            this.subtypes = Collections.unmodifiableList(sorted);
        }
    }

    public static String[] filamentVendors = {
            "3Dgenius",
            "3DJake",
            "3DXTECH",
            "3D BEST-Q",
            "3D Hero",
            "3D-Fuel",
            "Aceaddity",
            "AddNorth",
            "Amazon Basics",
            "AMOLEN",
            "Ankermake",
            "Anycubic",
            "Atomic",
            "AzureFilm",
            "BASF",
            "Bblife",
            "BCN3D",
            "Beyond Plastic",
            "California Filament",
            "Capricorn",
            "CC3D",
            "colorFabb",
            "Comgrow",
            "Cookiecad",
            "Creality",
            "CERPRiSE",
            "Das Filament",
            "DO3D",
            "DOW",
            "DSM",
            "Duramic",
            "ELEGOO",
            "Eryone",
            "Essentium",
            "eSUN",
            "Extrudr",
            "Fiberforce",
            "Fiberlogy",
            "FilaCube",
            "Filamentive",
            "Fillamentum",
            "FLASHFORGE",
            "Formfutura",
            "Francofil",
            "FilamentOne",
            "Fil X",
            "GEEETECH",
            "Generic",
            "Giantarm",
            "Gizmo Dorks",
            "GreenGate3D",
            "HATCHBOX",
            "Hello3D",
            "IC3D",
            "IEMAI",
            "IIID Max",
            "INLAND",
            "iProspect",
            "iSANMATE",
            "Justmaker",
            "Keene Village Plastics",
            "Kexcelled",
            "LDO",
            "MakerBot",
            "MatterHackers",
            "MIKA3D",
            "NinjaTek",
            "Nobufil",
            "Novamaker",
            "OVERTURE",
            "OVVNYXE",
            "Polymaker",
            "Priline",
            "Printed Solid",
            "Protopasta",
            "Prusament",
            "Push Plastic",
            "R3D",
            "Re-pet3D",
            "Recreus",
            "Regen",
            "Sain SMART",
            "SliceWorx",
            "Snapmaker",
            "SnoLabs",
            "Spectrum",
            "SUNLU",
            "TTYT3D",
            "Tianse",
            "UltiMaker",
            "Valment",
            "Verbatim",
            "VO3D",
            "Voxelab",
            "VOXELPLA",
            "YOOPAI",
            "Yousu",
            "Ziro",
            "Zyltech"};


    public static final String[] filamentTypes = {
            "ABS", "ASA", "HIPS", "PA", "PC", "PLA", "PVA", "PP", "TPU",
            "PETG", "BVOH", "PA6", "PAHT", "PPS", "PET", "PCTG", "PEBA", "PBT",
            "EVA", "PE", "PHA"
    };


    private static final Map<String, FilamentProfile> registry = new HashMap<>();

    static {
      
        registry.put("PLA", new FilamentProfile(190, 225, 40, 60, Arrays.asList(
                "Basic", "PLA+", "Rapid PLA+", "Support", "Silk", "Matte", "Tough", "Wood", "Bamboo",
                "Cork", "Copper", "Bronze", "Steel", "Marble", "Sparkle",
                "SnapSpeed", "Polylite", "PolySonic", "PolyTerra",
                "Galaxy", "Glow in the Dark", "Rainbow", "Dual Tone", "Tri Color", "Thermochromic", "Translucent",
                "Photochromic", "CF", "GF", "High Speed", "PRO", "Lightweight", "Conductive"
        )));


        registry.put("PETG", new FilamentProfile(230, 255, 70, 90, Arrays.asList(
                "Basic", "Translucent", "Transparent", "Matte", "Polylite", "CF", "GF",
                "Flame Retardant", "ESD Safe", "Food Safe", "High Speed", "Rapid", "HF", "PRO"
        )));

        registry.put("ABS", new FilamentProfile(230, 270, 90, 110, Arrays.asList(
                "Basic", "Matte", "Polylite", "Aerosol Smoothable", "CF", "GF",
                "Kevlar", "ESD Safe", "Flame Retardant", "High Impact"
        )));
        
        registry.put("ASA", new FilamentProfile(240, 265, 90, 110, Arrays.asList(
                "Basic", "Matte", "UV Resistant", "Aerosol Smoothable", "CF", "GF"
        )));

        registry.put("TPU", new FilamentProfile(210, 245, 30, 60, Arrays.asList(
                "Basic", "98A", "95A", "95A HF", "Rapid 95A", "85A", "75A", "Foaming (LW)", "Conductive", "High Speed", "Anti Static"
        )));
        
        registry.put("PEBA", new FilamentProfile(220, 250, 30, 50, Arrays.asList(
                "High Rebound", "Lightweight", "Medical Grade", "Low Temperature Flex"
        )));

        registry.put("PA", new FilamentProfile(240, 280, 70, 100, Arrays.asList(
                "Basic", "CF", "GF", "Kevlar", "Moisture Conditioned"
        )));
        
        registry.put("PA6", new FilamentProfile(250, 290, 80, 110, Arrays.asList(
                "Industrial Strength", "CF", "GF", "Glass Bead", "Mineral"
        )));
        
        registry.put("PAHT", new FilamentProfile(270, 310, 90, 120, Arrays.asList(
                "High Temp", "CF", "GF", "ESD Safe"
        )));

        registry.put("PC", new FilamentProfile(270, 310, 100, 120, Arrays.asList(
                "Basic", "Transparent", "Optical Grade", "CF", "Flame Retardant", "Heat Resistant"
        )));
        
        registry.put("PPS", new FilamentProfile(300, 340, 100, 120, Arrays.asList(
                "Basic", "CF", "GF", "Chemical Resistant"
        )));
        
        registry.put("PCTG", new FilamentProfile(250, 275, 70, 85, Arrays.asList(
                "Basic", "High Clarity", "High Impact", "Chemical Resistant", "Food Safe"
        )));
        
        registry.put("PBT", new FilamentProfile(230, 260, 70, 90, Arrays.asList(
                "Basic", "Glass", "Low Friction", "Hydrolysis Resistant"
        )));
        
        registry.put("PP", new FilamentProfile(220, 250, 80, 105, Arrays.asList(
                "Basic", "GF", "Chemical Resistant", "Living Hinge Grade"
        )));
        
        registry.put("PET", new FilamentProfile(245, 270, 70, 90, Arrays.asList(
                "Basic", "Recycled", "GF", "CF"
        )));

        registry.put("PVA", new FilamentProfile(185, 210, 45, 60, Arrays.asList(
                "Basic", "Soluble"
        )));
        
        registry.put("BVOH", new FilamentProfile(190, 220, 45, 60, Arrays.asList(
                "Basic", "Fast Dissolving", "High Adhesion"
        )));
        
        registry.put("HIPS", new FilamentProfile(230, 250, 90, 110, Arrays.asList(
                "Basic", "Limonene Soluble", "Standard Impact"
        )));

        registry.put("EVA", new FilamentProfile(180, 210, 30, 50, List.of(
                "Basic"
        )));

        registry.put("PE", new FilamentProfile(220, 250, 70, 100, Arrays.asList(
                "Basic", "CF", "GF"
        )));

        registry.put("PHA", new FilamentProfile(190, 210, 40, 60, List.of(
                "Basic"
        )));
    }

    public static FilamentProfile getProfile(String type) {
        return registry.get(type.toUpperCase());
    }


    // OpenSpool consumers treat "type" as a base material name and reject anything else.
    // The U1 extended firmware routes tags through OpenRFID, which validates the type
    // (after folding a CF/GF subtype into it) against this list and drops the whole tag
    // when it does not match. Kept in sync with OpenRFID src/filament/valid_materials.py.
    private static final Set<String> openSpoolBaseMaterials = new HashSet<>(Arrays.asList(
            "ABS", "ABS-CF", "ABS-GF", "ASA", "ASA-CF", "ASA-GF", "ASA-AERO", "BVOH",
            "CoPE", "EVA", "FLEX", "HIPS", "PA", "PA-CF", "PA-GF", "PA6", "PA6-CF",
            "PA6-GF", "PA11", "PA11-CF", "PA11-GF", "PA12", "PA12-CF", "PA12-GF",
            "PAHT", "PAHT-CF", "PAHT-GF", "PC", "PC-ABS", "PC-CF", "PC-PBT", "PCL",
            "PCTG", "PE", "PE-CF", "PE-GF", "PEI-1010", "PEI-1010-CF", "PEI-1010-GF",
            "PEI-9085", "PEI-9085-CF", "PEI-9085-GF", "PEEK", "PEEK-CF", "PEEK-GF",
            "PEKK", "PEKK-CF", "PES", "PET", "PET-CF", "PET-GF", "PETG", "PETG-CF",
            "PETG-GF", "PHA", "PI", "PLA", "PLA-AERO", "PLA-CF", "POM", "PP", "PP-CF",
            "PP-GF", "PPA-CF", "PPA-GF", "PPS", "PPS-CF", "PPSU", "PSU", "PVA", "PVB",
            "PVDF", "SBS", "TPI", "TPU"
    ));

    // Type names offered by the picker that are variants of a base material rather than
    // a material of their own. They are written to a tag as the base material, with the
    // variant name carried over into the subtype so nothing is lost.
    private static final Map<String, String> openSpoolTypeAliases = new LinkedHashMap<>();

    static {
        openSpoolTypeAliases.put("PLA+", "PLA");
    }

    // The base material a tag should carry for the given picker type.
    public static String getOpenSpoolType(String type) {
        if (type == null) return null;
        String alias = openSpoolTypeAliases.get(type);
        return alias != null ? alias : type;
    }

    // The variant name to fold into the subtype, or null when the type needs no aliasing.
    // Only legacy data reaches this now that the picker offers base materials alone.
    public static String getOpenSpoolTypeVariant(String type) {
        return type != null && openSpoolTypeAliases.containsKey(type) ? type : null;
    }

    // Whether a tag written with this type/subtype will survive the printer's type check.
    // OpenRFID promotes a bare "CF" or "GF" subtype into the type before validating it, so a
    // valid base material can still be rejected once its subtype is folded in. An aliased
    // type keeps its variant at the front of the subtype, which stops that promotion.
    public static boolean isOpenSpoolTypeSupported(String type, String subtype) {
        String resolved = getOpenSpoolType(type);
        if (resolved == null) return false;
        boolean aliased = getOpenSpoolTypeVariant(type) != null;
        if (!aliased && ("CF".equals(subtype) || "GF".equals(subtype))) resolved = resolved + "-" + subtype;
        return openSpoolBaseMaterials.contains(resolved);
    }
}