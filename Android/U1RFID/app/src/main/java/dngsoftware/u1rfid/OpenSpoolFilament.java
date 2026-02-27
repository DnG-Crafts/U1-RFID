package dngsoftware.u1rfid;

import androidx.annotation.NonNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.List;

public class OpenSpoolFilament {
    private final JSONObject json;

    public OpenSpoolFilament() {
        this.json = new JSONObject();
        try {
            json.put("id", String.valueOf(System.currentTimeMillis()));
            json.put("protocol", "openspool");
            json.put("version", "1.0");
            json.put("brand", "Generic");
            json.put("type", "PLA");
            json.put("subtype", "Basic");
            json.put("alpha", "FF");
            json.put("color_hex", "0000FF");
        } catch (Exception ignored) {}
    }

    public OpenSpoolFilament(String jsonString) throws JSONException {
        this.json = new JSONObject(jsonString);
    }

    public void setID(String id) throws JSONException {
        json.put("id", id);
    }

    public void setType(String brand, String type, String subtype) throws JSONException {
        json.put("brand", brand);
        json.put("type", type);
        json.put("subtype", subtype);
    }

    public void setColor(String hex, String alpha) throws JSONException {
        json.put("color_hex", hex.replace("#", ""));
        json.put("alpha", alpha);
    }

    public void setAdditionalColors(List<String> hexColors) throws JSONException {
        JSONArray array = new JSONArray();
        for (String color : hexColors) {
            array.put(color.replace("#", ""));
        }
        json.put("additional_color_hexes", array);
    }

    public void setPhysicals(double diameter, int weightGram) throws JSONException {
        json.put("diameter", diameter);
        json.put("weight", weightGram);
    }

    public void setTemps(int minExtruder, int maxExtruder, int minBed, int maxBed) throws JSONException {
        json.put("min_temp", minExtruder);
        json.put("max_temp", maxExtruder);
        json.put("bed_min_temp", minBed);
        json.put("bed_max_temp", maxBed);
    }

    public String getID() { return json.optString("id", String.valueOf(System.currentTimeMillis())); }
    public String getBrand() { return json.optString("brand", "Generic"); }
    public String getType() { return json.optString("type", "PLA"); }
    public String getSubType() { return json.optString("subtype", "Basic"); }
    public String getColorHex() { return json.optString("color_hex", "0000FF").replace("#", ""); }
    public String getAlpha() { return json.optString("alpha", "FF"); }
    public int getMinTemp() { return json.optInt("min_temp", 190); }
    public int getMaxTemp() { return json.optInt("max_temp", 210); }
    public int getBedMinTemp() { return json.optInt("bed_min_temp", 50); }
    public int getBedMaxTemp() { return json.optInt("bed_max_temp", 60); }
    public int getWeight() { return json.optInt("weight", 1000); }
    public double getDiameter() { return json.optDouble("diameter", 1.75); }

    @NonNull
    @Override
    public String toString() {
        return json.toString();
    }

}