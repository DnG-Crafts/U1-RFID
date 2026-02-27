package dngsoftware.u1rfid;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.text.InputFilter;
import android.text.Spanned;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@SuppressLint("GetInstance")
public class Utils {

    public static String[] materialWeights = {
            "1 KG",
            "750 G",
            "600 G",
            "500 G",
            "250 G"
    };

    public static String GetMaterialWeightByInt(int materialLength) {
        switch (materialLength) {
            case 1000:
                return "1 KG";
            case 750:
                return "750 G";
            case 600:
                return "600 G";
            case 500:
                return "500 G";
            case 250:
                return "250 G";
        }
        return "1 KG";
    }

    public static int GetMaterialIntWeight(String materialWeight) {
        switch (materialWeight) {
            case "1 KG":
                return 1000;
            case "750 G":
                return 750;
            case "600 G":
                return 600;
            case "500 G":
                return 500;
            case "250 G":
                return 250;
        }
        return 1000;
    }

    public static void populateDatabase(MatDB db) {
        try {
            List<OpenSpoolFilament> osfList = new ArrayList<>();
            addToList(osfList, "Snapmaker", "PLA", "Matte", 190, 220, 50, 60);
            addToList(osfList, "Snapmaker", "PLA", "SnapSpeed", 210, 230, 50, 60);
            addToList(osfList, "Snapmaker", "PLA", "Basic", 190, 210, 50, 60);
            addToList(osfList, "Snapmaker", "PLA", "Support", 180, 200, 50, 60);
            addToList(osfList, "Snapmaker", "PETG", "Basic", 230, 250, 70, 80);
            addToList(osfList, "Snapmaker", "PETG", "HF", 240, 260, 70, 80);
            addToList(osfList, "Snapmaker", "TPU", "95A", 210, 230, 30, 50);
            addToList(osfList, "Snapmaker", "TPU", "95A HF", 220, 240, 30, 50);
            addToList(osfList, "Snapmaker", "PVA", "Basic", 180, 200, 50, 60);
            addToList(osfList, "Snapmaker", "ABS", "Basic", 240, 260, 90, 110);
            addToList(osfList, "Polymaker", "PLA", "Polylite", 190, 230, 40, 60);
            addToList(osfList, "Polymaker", "PLA", "PolySonic", 210, 240, 40, 60);
            addToList(osfList, "Polymaker", "PLA", "PolyTerra", 190, 230, 30, 60);
            addToList(osfList, "Polymaker", "ABS", "Polylite", 245, 265, 90, 100);
            addToList(osfList, "Polymaker", "PETG", "Polylite", 230, 240, 70, 80);
            addToList(osfList, "Generic", "PLA", "Basic", 200, 220, 50, 60);
            addToList(osfList, "Generic", "PETG", "Basic", 230, 250, 70, 85);
            addToList(osfList, "Generic", "ABS", "Basic", 230, 260, 100, 110);
            addToList(osfList, "Generic", "TPU", "95A", 220, 240, 40, 60);
            addToList(osfList, "Generic", "TPU", "95A HF", 230, 250, 40, 60);
            addToList(osfList, "Generic", "ASA", "Basic", 240, 260, 100, 110);
            addToList(osfList, "Generic", "BVOH", "Basic", 190, 210, 50, 60);
            addToList(osfList, "Generic", "EVA", "Basic", 180, 210, 30, 50);
            addToList(osfList, "Generic", "HIPS", "Basic", 220, 240, 90, 110);
            addToList(osfList, "Generic", "PA", "Basic", 260, 290, 80, 100);
            addToList(osfList, "Generic", "PA", "CF", 270, 300, 80, 100);
            addToList(osfList, "Generic", "PC", "Basic", 270, 300, 100, 120);
            addToList(osfList, "Generic", "PCTG", "Basic", 250, 270, 70, 80);
            addToList(osfList, "Generic", "PE", "Basic", 220, 250, 70, 100);
            addToList(osfList, "Generic", "PE", "CF", 230, 260, 70, 100);
            addToList(osfList, "Generic", "PHA", "Basic", 190, 210, 40, 60);
            addToList(osfList, "Generic", "PLA", "Silk", 205, 225, 50, 60);
            addToList(osfList, "Generic", "PLA", "CF", 210, 230, 50, 60);
            addToList(osfList, "Generic", "PVA", "Basic", 190, 210, 50, 60);
            addToList(osfList, "Generic", "PLA", "Support", 190, 210, 50, 60);
            for (int i = 0; i < osfList.size(); i++) {
                OpenSpoolFilament osf = osfList.get(i);
                Filament dbItem = new Filament();
                dbItem.position = i;
                dbItem.filamentVendor = osf.getBrand();
                dbItem.filamentName = osf.getType();
                dbItem.filamentID = String.valueOf(i);
                dbItem.filamentParam = osf.toString();
                db.addItem(dbItem);
            }
        } catch (Exception ignored) {}
    }

    private static void addToList(List<OpenSpoolFilament> list, String brand, String type, String sub, int minE, int maxE, int minB, int maxB) {
        try {
            OpenSpoolFilament f = new OpenSpoolFilament();
            f.setType(brand, type, sub);
            f.setTemps(minE, maxE, minB, maxB);
            f.setPhysicals(1.75, 1000);
            f.setColor("0000FF", "FF");
            f.setID(String.valueOf(list.size()));
            list.add(f);
        } catch (Exception ignored) {}
    }

    @SuppressWarnings("unchecked")
    public static void setSpinnerSelection(Spinner spinner, String value) {
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
        if (adapter != null && value != null) {
            int position = adapter.getPosition(value);
            if (position >= 0) {
                spinner.setSelection(position);
            }
        }
    }

    public static List<String> getUniqueValues(List<Filament> list, String key) {
        Set<String> set = new LinkedHashSet<>();
        for (Filament f : list) {
            try {
                if (f.filamentParam != null) {
                    JSONObject json = new JSONObject(f.filamentParam);
                    String val = json.optString(key, "");
                    if (!val.isEmpty()) {
                        set.add(val);
                    }
                }
            } catch (Exception ignored) {}
        }
        return new ArrayList<>(set);
    }

    public static String bytesToHex(byte[] data, boolean space) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            if (space) {
                sb.append(String.format("%02X ", b));
            } else {
                sb.append(String.format("%02X", b));
            }
        }
        return sb.toString();
    }

    public static void SetPermissions(Context context) {
        String[] REQUIRED_PERMISSIONS = {Manifest.permission.NFC, Manifest.permission.INTERNET,
                Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        Activity activity = (Activity) context;
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            String[] permsArray = permissionsToRequest.toArray(new String[0]);
            ActivityCompat.requestPermissions(activity, permsArray, 200);
        }
    }

    public static void playBeep() {
        try {
            ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 50);
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 300);
            toneGenerator.stopTone();
            Thread.sleep(300);
            toneGenerator.release();
        } catch (Exception ignored) {
        }
    }

    public static class HexInputFilter implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            StringBuilder filtered = new StringBuilder();
            for (int i = start; i < end; i++) {
                char character = source.charAt(i);
                if (Character.isDigit(character) || (character >= 'a' && character <= 'f') || (character >= 'A' && character <= 'F')) {
                    filtered.append(character);
                }
            }
            return filtered.toString();
        }
    }

    public static boolean isValidHexCode(String hexCode) {
        Pattern pattern = Pattern.compile("^[0-9a-fA-F]{8}$");
        Matcher matcher = pattern.matcher(hexCode);
        return matcher.matches();
    }

    public static String rgbToHexA(int r, int g, int b, int a) {
        return String.format("%02X%02X%02X%02X", a, r, g, b);
    }

    public static int getContrastColor(@ColorInt int backgroundColor) {
        int red = Color.red(backgroundColor);
        int green = Color.green(backgroundColor);
        int blue = Color.blue(backgroundColor);
        double luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255.0;
        return (luminance > 0.5) ? Color.BLACK : Color.WHITE;
    }

    public static boolean arrayContains(String[] array, String string) {
        if (array == null || string == null) {
            return false;
        }
        for (String s : array) {
            if (s.contains(string.trim())) {
                return true;
            }
        }
        return false;
    }

    public static void copyFileToUri(Context context, File sourceFile, Uri destinationUri) throws IOException {
        try (InputStream in = new FileInputStream(sourceFile);
             OutputStream out = context.getContentResolver().openOutputStream(destinationUri)) {
            if (out == null) {
                throw new IOException("Failed to open output stream for URI: " + destinationUri);
            }
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }

    public static void copyFile(File sourceFile, File destinationFile) throws IOException {
        try (InputStream in = new FileInputStream(sourceFile);
             OutputStream out = new FileOutputStream(destinationFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }

    public static void copyUriToFile(Context context, Uri sourceUri, File destinationFile) throws IOException {
        try (InputStream in = context.getContentResolver().openInputStream(sourceUri);
             OutputStream out = new FileOutputStream(destinationFile)) {
            if (in == null) {
                throw new IOException("Failed to open input stream for URI: " + sourceUri);
            }
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }

    public static String GetSetting(Context context, String sKey, String sDefault) {
        SharedPreferences sharedPref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        return sharedPref.getString(sKey, sDefault);
    }

    public static boolean GetSetting(Context context, String sKey, boolean bDefault) {
        SharedPreferences sharedPref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        return sharedPref.getBoolean(sKey, bDefault);
    }

    public static int GetSetting(Context context, String sKey, int iDefault) {
        SharedPreferences sharedPref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        return sharedPref.getInt(sKey, iDefault);
    }

    public static long GetSetting(Context context, String sKey, long lDefault) {
        SharedPreferences sharedPref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        return sharedPref.getLong(sKey, lDefault);
    }

    public static void SaveSetting(Context context, String sKey, String sValue) {
        SharedPreferences sharedPref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(sKey, sValue);
        editor.apply();
    }

    public static void SaveSetting(Context context, String sKey, boolean bValue) {
        SharedPreferences sharedPref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(sKey, bValue);
        editor.apply();
    }

    public static void SaveSetting(Context context, String sKey, int iValue) {
        SharedPreferences sharedPref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(sKey, iValue);
        editor.apply();
    }

    public static void SaveSetting(Context context, String sKey, long lValue) {
        SharedPreferences sharedPref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong(sKey, lValue);
        editor.apply();
    }

    public static void setThemeMode(boolean enabled)
    {
        if (enabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }else{
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    public static void setNfcLaunchMode(Context context, boolean allowLaunch ) {
        ComponentName componentName = new ComponentName(context, LaunchActivity.class);
        PackageManager packageManager = context.getPackageManager();
        if (allowLaunch) {
            packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        }
        else {
            packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
    }

    public static String getPageDefinition(int page, int type) {
        if (page == 0) return "UID 0-2 / Internal";
        if (page == 1) return "UID 3-6";
        if (page == 2) return "Internal / BCC / Lock Bytes";
        if (page == 3) return "Capability Container (CC)";
        if (type == 213 || type == 215 || type == 216) {
            int cfgStart = (type == 213) ? 41 : (type == 215) ? 131 : 227;
            if (page >= 4 && page < cfgStart) return "User Data / NDEF";
            if (page == cfgStart) return "Config (Mirror / Auth0)";
            if (page == cfgStart + 1) return "Access (PROT / CFGLCK)";
            if (page == cfgStart + 2) return "PWD (Password)";
            if (page == cfgStart + 3) return "PACK / Target ID";
            return "End of Memory";
        }
        if (type == 100) {
            if (page >= 4 && page <= 39) return "User Data";
            if (page >= 40 && page <= 43) return "3DES Keys (Write-Only)";
            if (page == 44) return "Auth Start (AUTH0)";
            if (page == 45) return "Auth Config (AUTH1)";
            return "Internal";
        }
        return "Unknown";
    }

    public static int[] presetColors() {
        return new int[]{
                Color.parseColor("#25C4DA"),
                Color.parseColor("#0099A7"),
                Color.parseColor("#0B359A"),
                Color.parseColor("#0A4AB6"),
                Color.parseColor("#11B6EE"),
                Color.parseColor("#90C6F5"),
                Color.parseColor("#FA7C0C"),
                Color.parseColor("#F7B30F"),
                Color.parseColor("#E5C20F"),
                Color.parseColor("#B18F2E"),
                Color.parseColor("#8D766D"),
                Color.parseColor("#6C4E43"),
                Color.parseColor("#E62E2E"),
                Color.parseColor("#EE2862"),
                Color.parseColor("#EA2A2B"),
                Color.parseColor("#E83D89"),
                Color.parseColor("#AE2E65"),
                Color.parseColor("#611C8B"),
                Color.parseColor("#8D60C7"),
                Color.parseColor("#B287C9"),
                Color.parseColor("#006764"),
                Color.parseColor("#018D80"),
                Color.parseColor("#42B5AE"),
                Color.parseColor("#1D822D"),
                Color.parseColor("#54B351"),
                Color.parseColor("#72E115"),
                Color.parseColor("#474747"),
                Color.parseColor("#668798"),
                Color.parseColor("#B1BEC6"),
                Color.parseColor("#58636E"),
                Color.parseColor("#F8E911"),
                Color.parseColor("#F6D311"),
                Color.parseColor("#F2EFCE"),
                Color.parseColor("#FFFFFF"),
                Color.parseColor("#000000")
        };
    }
}