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
            this.subtypes = subtypes;
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
            "ABS", "ASA", "HIPS", "PA", "PC", "PLA", "PLA+", "PVA", "PP", "TPU",
            "PETG", "BVOH", "PA6", "PAHT", "PPS", "PET", "PCTG", "PEBA", "PBT"
    };


    private static final Map<String, FilamentProfile> registry = new HashMap<>();

    static {
      
        registry.put("PLA", new FilamentProfile(190, 225, 40, 60, Arrays.asList(
                "Basic", "Silk", "Matte", "Tough", "Wood", "Bamboo",
                "Cork", "Copper", "Bronze", "Steel", "Marble", "Sparkle", "SnapSpeed", "Polylite",
                "Galaxy", "Glow-in-the-Dark", "Rainbow", "Dual-Tone", "Tri-Color", "Thermochromic", "Translucent",
                "Photochromic", "CF", "GF", "High-Speed", "Lightweight", "Conductive"
        )));

        registry.put("PLA+", new FilamentProfile(190, 225, 40, 60, Arrays.asList(
                "Basic", "Silk", "Matte", "Tough", "Wood", "Bamboo",
                "Cork", "Copper", "Bronze", "Steel", "Marble", "Sparkle",
                "Galaxy", "Glow-in-the-Dark", "Rainbow", "Dual-Tone", "Tri-Color", "Thermochromic", "Translucent",
                "Photochromic", "CF", "GF", "High-Speed", "Lightweight", "Conductive"
        )));

        registry.put("PETG", new FilamentProfile(230, 255, 70, 90, Arrays.asList(
                "Basic", "Translucent", "Transparent", "Matte", "CF", "GF",
                "Flame-Retardant", "ESD-Safe", "Food-Safe", "High-Speed"
        )));

        registry.put("ABS", new FilamentProfile(230, 270, 90, 110, Arrays.asList(
                "Basic", "Matte", "Aerosol-Smoothable", "CF", "GF",
                "Kevlar", "ESD-Safe", "Flame-Retardant", "High-Impact"
        )));
        
        registry.put("ASA", new FilamentProfile(240, 265, 90, 110, Arrays.asList(
                "Basic", "Matte", "UV-Resistant", "Aerosol-Smoothable", "CF", "GF"
        )));

        registry.put("TPU", new FilamentProfile(210, 245, 30, 60, Arrays.asList(
                "Basic", "98A", "95A", "95A HF", "85A", "75A", "Foaming (LW)", "Conductive", "High-Speed", "Anti-Static"
        )));
        
        registry.put("PEBA", new FilamentProfile(220, 250, 30, 50, Arrays.asList(
                "High-Rebound", "Lightweight", "Medical-Grade", "Low-Temperature-Flex"
        )));

        registry.put("PA", new FilamentProfile(240, 280, 70, 100, Arrays.asList(
                "Basic", "CF", "GF", "Kevlar", "Moisture-Conditioned"
        )));
        
        registry.put("PA6", new FilamentProfile(250, 290, 80, 110, Arrays.asList(
                "Industrial-Strength", "CF", "GF", "Glass-Bead", "Mineral"
        )));
        
        registry.put("PAHT", new FilamentProfile(270, 310, 90, 120, Arrays.asList(
                "High-Temp", "CF", "GF", "ESD-Safe"
        )));

        registry.put("PC", new FilamentProfile(270, 310, 100, 120, Arrays.asList(
                "Basic", "Transparent", "Optical-Grade", "CF", "Flame-Retardant", "Heat-Resistant"
        )));
        
        registry.put("PPS", new FilamentProfile(300, 340, 100, 120, Arrays.asList(
                "Basic", "CF", "GF", "Chemical-Resistant"
        )));
        
        registry.put("PCTG", new FilamentProfile(250, 275, 70, 85, Arrays.asList(
                "High-Clarity", "High-Impact", "Chemical-Resistant", "Food-Safe"
        )));
        
        registry.put("PBT", new FilamentProfile(230, 260, 70, 90, Arrays.asList(
                "Basic", "Glass", "Low-Friction", "Hydrolysis-Resistant"
        )));
        
        registry.put("PP", new FilamentProfile(220, 250, 80, 105, Arrays.asList(
                "Basic", "GF", "Chemical-Resistant", "Living-Hinge-Grade"
        )));
        
        registry.put("PET", new FilamentProfile(245, 270, 70, 90, Arrays.asList(
                "Basic", "Recycled", "GF", "CF"
        )));

        registry.put("PVA", new FilamentProfile(185, 210, 45, 60, List.of(
                "Soluble"
        )));
        
        registry.put("BVOH", new FilamentProfile(190, 220, 45, 60, Arrays.asList(
                "Fast-Dissolving", "High-Adhesion"
        )));
        
        registry.put("HIPS", new FilamentProfile(230, 250, 90, 110, Arrays.asList(
                "Limonene-Soluble", "Standard-Impact"
        )));
    }

    public static FilamentProfile getProfile(String type) {
        return registry.get(type.toUpperCase());
    }
}