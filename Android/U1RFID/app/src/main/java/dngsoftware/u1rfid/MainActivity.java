package dngsoftware.u1rfid;

import static android.view.View.TEXT_ALIGNMENT_CENTER;
import dngsoftware.u1rfid.databinding.ActivityMainBinding;
import dngsoftware.u1rfid.databinding.AddDialogBinding;
import dngsoftware.u1rfid.databinding.DryerDialogBinding;
import dngsoftware.u1rfid.databinding.PickerDialogBinding;
import dngsoftware.u1rfid.databinding.SettingsDialogBinding;
import dngsoftware.u1rfid.databinding.SpoolDialogBinding;
import dngsoftware.u1rfid.databinding.TagDialogBinding;
import static dngsoftware.u1rfid.FilamentRegistry.filamentTypes;
import static dngsoftware.u1rfid.FilamentRegistry.filamentVendors;
import static dngsoftware.u1rfid.Utils.*;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GravityCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback, NavigationView.OnNavigationItemSelectedListener {
    private MatDB matDb;
    private filamentDB rdb;
    private NfcAdapter nfcAdapter;
    Tag currentTag = null;
    // What Spoolman said about the tag in hand, kept against the UID it was fetched for.
    String cachedSpoolUid = "";
    JSONObject cachedSpool = null;
    // Spoolman's filament id for a local filament, once one has resolved to it.
    final Map<String, Integer> spoolmanFilamentIds = new HashMap<>();
    // What Spoolman holds for the filament chosen in the UI, when it holds anything.
    String cachedFilamentKey = "";
    JSONObject cachedSmFilament = null;
    JSONObject cachedSmSpool = null;
    // Whether the filament we settled on is the one meant, rather than merely the closest.
    boolean smMatchCertain = false;
    // A filament the user chose outright, which no amount of guessing may override.
    int pinnedSmFilamentId = 0;
    String pinnedForMaterialId = "";
    List<JSONObject> cachedSmCandidates = new ArrayList<>();
    int tagType;
    ArrayAdapter<String> sadapter;
    ColorMatcher matcher = null;
    String SelectedTool = null;
    String MaterialID, MaterialWeight = "1 KG", MaterialColor = "FF0000FF";
    Dialog pickerDialog, addDialog, tagDialog, settingsDialog, spoolDialog, dryerDialog ;
    AlertDialog inputDialog;
    tagAdapter recycleAdapter;
    RecyclerView recyclerView;
    private Toast currentToast;
    tagItem[] tagItems;
    int SelectedSize;
    boolean userSelect = false;
    private ActivityMainBinding main;
    Bitmap gradientBitmap;
    Context context;
    private ExecutorService executorService;
    // Spoolman is spoken to on its own thread. The shared one also carries the tag reads,
    // and a request to a slow or unreachable server would hold a read behind it for as
    // long as the timeouts allow, by which time the tag is out of the field.
    private ExecutorService spoolmanExecutor;
    private Handler mainHandler;
    private boolean isRunning = false;
    private final int INTERVAL = 5000;
    private ActivityResultLauncher<Intent> exportDirectoryChooser;
    private ActivityResultLauncher<Intent> importFileChooser;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Void> cameraLauncher;
    private static final int ACTION_EXPORT = 1;
    private static final int ACTION_IMPORT = 2;
    private int pendingAction = -1;
    NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private static final int PERMISSION_REQUEST_CODE = 2;
    private PickerDialogBinding colorDialog;
    private FrameLayout[] tools = new FrameLayout[4];
    private TextView[] select = new TextView[4];
    private TextView[] type = new TextView[4];
    private String[] subtype = new String[4];
    private String[] vendor = new String[4];
    private View[] check = new View[4];
    private String[] color = new String[4];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setThemeMode(GetSetting(this, "enabledm", false));
        Resources res = getApplicationContext().getResources();
        Locale locale = new Locale("en");
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        res.updateConfiguration(config, res.getDisplayMetrics());

        main = ActivityMainBinding.inflate(getLayoutInflater());
        View rv = main.getRoot();
        setContentView(rv);

        tools = new FrameLayout[]{main.tool1, main.tool2, main.tool3, main.tool4};
        select = new TextView[]{main.select1, main.select2, main.select3, main.select4};
        type = new TextView[]{main.type1, main.type2, main.type3, main.type4};
        check = new View[]{main.check1, main.check2, main.check3, main.check4};

        SetPermissions(this);

        executorService = Executors.newSingleThreadExecutor();
        spoolmanExecutor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        setupActivityResultLaunchers();

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        MenuItem readItem = navigationView.getMenu().findItem(R.id.nav_read);
        SwitchCompat readSwitch = Objects.requireNonNull(readItem.getActionView()).findViewById(R.id.drawer_switch);
        readSwitch.setChecked(GetSetting(this, "autoread", false));
        readSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SaveSetting(this, "autoread", isChecked);
        });


        MenuItem writeItem = navigationView.getMenu().findItem(R.id.nav_write);
        SwitchCompat writeSwitch = Objects.requireNonNull(writeItem.getActionView()).findViewById(R.id.drawer_switch);
        writeSwitch.setChecked(GetSetting(this, "writeprinter", false));
        writeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SaveSetting(this, "writeprinter", isChecked);
            if (isChecked) {
                main.toolFrame.setVisibility(View.VISIBLE);
                main.tempGrid.setVisibility(View.INVISIBLE);
                main.tagid.setVisibility(View.INVISIBLE);
                loadToolFrame();
            } else {
                main.toolFrame.setVisibility(View.GONE);
                main.tempGrid.setVisibility(View.VISIBLE);
                main.tagid.setVisibility(View.INVISIBLE);
                stopFrameUpdater();
            }
        });


        main.colorview.setOnClickListener(view -> openPicker());
        main.colorview.setBackgroundColor(Color.argb(255, 0, 0, 255));
        main.txtcolor.setText(MaterialColor);
        main.txtcolor.setTextColor(getContrastColor(Color.parseColor("#" + MaterialColor)));

        main.readbutton.setOnClickListener(view -> {
            readTag(currentTag);
        });

        main.writebutton.setOnClickListener(view -> {
            if (GetSetting(context, "writeprinter", false)) {
                writePrinter();
            } else if (GetSetting(this, "acetag", false)) {
                writeAceTag(currentTag);
            } else {
                writeTag(currentTag);
            }
        });


        main.menubutton.setOnClickListener(view -> {
            navigationView.getMenu().findItem(R.id.nav_dryer).setVisible(GetSetting(context, "acemod", false));
            drawerLayout.openDrawer(GravityCompat.START);
        });


        main.addbutton.setOnClickListener(view -> openAddDialog(false));
        main.editbutton.setOnClickListener(view -> openAddDialog(true));

        main.deletebutton.setOnClickListener(view -> {
            try {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                SpannableString titleText = new SpannableString(getString(R.string.delete_filament));
                titleText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.primary_brand)), 0, titleText.length(), 0);
                OpenSpoolFilament filament = new OpenSpoolFilament(matDb.getFilamentById(MaterialID).filamentParam);
                SpannableString messageText = new SpannableString(" " + filament.getBrand() + "\n " + filament.getType() + "\n " + filament.getSubType());
                messageText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.text_main)), 0, messageText.length(), 0);
                builder.setTitle(titleText);
                builder.setMessage(messageText);
                builder.setPositiveButton(R.string.delete, (dialog, which) -> {
                    if (matDb.getFilamentById(MaterialID) != null) {
                        matDb.deleteItem(matDb.getFilamentById(MaterialID));
                        loadMaterials();
                        dialog.dismiss();
                    }
                });

                builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                AlertDialog alert = builder.create();
                alert.show();
                if (alert.getWindow() != null) {
                    alert.getWindow().setBackgroundDrawableResource(R.color.background_alt);
                    alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.primary_brand));
                    alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.primary_brand));
                }
            } catch (Exception ignored) {
            }
        });

        setMatDb();

        main.colorspin.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    openPicker();
                    break;
                case MotionEvent.ACTION_UP:
                    v.performClick();
                    break;
                default:
                    break;
            }
            return false;
        });


        if (!GetSetting(this, "firm_notice", false)) {
            showFirmwareNotice();
        }

        if (GetSetting(context, "enablesm", false)) {
            main.smbutton.setVisibility(View.VISIBLE);
            executorService.execute(() -> matcher = new ColorMatcher(context));
        } else {
            main.smbutton.setVisibility(View.INVISIBLE);
        }
        main.smbutton.setOnClickListener(view ->
        {
            if (GetSetting(context, "enablesm", false)) {
                showSpoolmanMenu();
            }
        });

        if (GetSetting(context, "writeprinter", false)) {
            main.toolFrame.setVisibility(View.VISIBLE);
            main.tempGrid.setVisibility(View.INVISIBLE);
            loadToolFrame();
        } else {
            main.toolFrame.setVisibility(View.GONE);
            main.tempGrid.setVisibility(View.VISIBLE);
        }

    }


    @Override
    protected void onResume() {
        super.onResume();
        try {
            nfcAdapter = NfcAdapter.getDefaultAdapter(this);
            if (nfcAdapter != null && nfcAdapter.isEnabled()) {
                Bundle options = new Bundle();
                options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);
                nfcAdapter.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_A, options);
            }
        }catch (Exception ignored) {}
    }


    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (nfcAdapter != null) {
                nfcAdapter.disableReaderMode(this);
            }
        } catch (Exception ignored) {}
    }


    void setMatDb() {
        try {
            if (rdb != null && rdb.isOpen()) {
                rdb.close();
            }

            rdb = filamentDB.getInstance(this);
            matDb = rdb.matDB();

            if (matDb.getItemCount() == 0) {
                populateDatabase(matDb);
            }

            mainHandler.post(() -> {
                sadapter = new ArrayAdapter<>(this, R.layout.spinner_item, materialWeights);
                main.spoolsize.setAdapter(sadapter);
                main.spoolsize.setSelection(SelectedSize);
                main.spoolsize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                        SelectedSize = main.spoolsize.getSelectedItemPosition();
                        MaterialWeight = sadapter.getItem(position);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parentView) {
                    }
                });
                loadMaterials();
            });
        } catch (Exception ignored) {}
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopFrameUpdater();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
            spoolmanExecutor.shutdownNow();
        }
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            try {
                nfcAdapter.disableReaderMode(this);
            } catch (Exception ignored) {
            }
        }
        if (rdb != null && rdb.isOpen()) {
            rdb.close();
        }
        if (pickerDialog != null && pickerDialog.isShowing()) {
            pickerDialog.dismiss();
        }
        if (inputDialog != null && inputDialog.isShowing()) {
            inputDialog.dismiss();
        }
        if (spoolDialog != null && spoolDialog.isShowing()) {
            spoolDialog.dismiss();
        }
        if (settingsDialog != null && settingsDialog.isShowing()) {
            settingsDialog.dismiss();
        }
        if (addDialog != null && addDialog.isShowing()) {
            addDialog.dismiss();
        }
        if (tagDialog != null && tagDialog.isShowing()) {
            tagDialog.dismiss();
        }
        if (dryerDialog != null && dryerDialog.isShowing()) {
            dryerDialog.dismiss();
        }
    }


    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (pickerDialog != null && pickerDialog.isShowing()) {
            pickerDialog.dismiss();
            openPicker();
        }
        if (inputDialog != null && inputDialog.isShowing()) {
            inputDialog.dismiss();
        }
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_export) {
            showExportDialog();
        }else if (id == R.id.nav_import) {
            showImportDialog();
        }else if (id == R.id.nav_format) {
            formatTag(currentTag);
        } else if (id == R.id.nav_memory) {
            loadTagMemory();
        }else if (id == R.id.nav_settings) {
            openSettings();
        }else if (id == R.id.nav_dryer) {
            openDryer();
        }


        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }


    @Override
    public void onTagDiscovered(Tag tag) {
        try {
            mainHandler.post(() -> {
                byte[] uid = tag.getId();
                if (uid.length >= 6) {
                    currentTag = tag;
                    showToast(getString(R.string.tag_found) + bytesToHex(uid, false), Toast.LENGTH_SHORT);
                    tagType = getTagType(NfcA.get(currentTag));
                    updateTagLine();
                    main.tagid.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.twotone_nfc_24, 0, 0, 0);
                    main.tagid.setVisibility(View.VISIBLE);
                    if (tagType == 100) {
                        showToast(getString(R.string.incompatible_tag), Toast.LENGTH_SHORT);
                        return;
                    }
                    else if (tagType == 213 && !GetSetting(this, "acetag", false)) {
                        showToast(getString(R.string.incompatible_tag), Toast.LENGTH_SHORT);
                        return;
                    }
                    resolveSpoolForTag();
                    if (GetSetting(this, "autoread", false)) {
                        readTag(currentTag);
                    }
                }
                else {
                    currentTag = null;
                    cachedSpoolUid = "";
                    cachedSpool = null;
                    main.tagid.setVisibility(View.INVISIBLE);
                    main.tagid.setText("");
                    showToast(R.string.invalid_tag_type, Toast.LENGTH_SHORT);
                }
            });
        } catch (Exception ignored) {
        }
    }


    void loadMaterials()
    {
        try {
            List<Filament> allFilaments = matDb.getAllItems();
            List<String> brands = getUniqueValues(allFilaments, "brand");
            ArrayAdapter<String> brandAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, brands);
            main.brand.setAdapter(brandAdapter);
            main.brand.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String selectedBrand = brands.get(position);
                    List<Filament> brandFiltered = new ArrayList<>();
                    for (Filament f : allFilaments)
                        if (f.filamentVendor.equals(selectedBrand)) brandFiltered.add(f);
                    List<String> types = getUniqueValues(brandFiltered, "type");
                    main.type.setAdapter(new ArrayAdapter<>(MainActivity.this, R.layout.spinner_item, types));
                    main.type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> p2, View v2, int pos2, long id2) {
                            String selectedType = types.get(pos2);
                            List<String> subtypes = new ArrayList<>();
                            for (Filament f : brandFiltered) {
                                try {
                                    JSONObject json = new JSONObject(f.filamentParam);
                                    if (json.optString("type").equals(selectedType)) {
                                        subtypes.add(json.optString("subtype", "Basic"));
                                    }
                                } catch (Exception ignored) {
                                }
                            }
                            main.subtype.setAdapter(new ArrayAdapter<>(MainActivity.this, R.layout.spinner_item, subtypes));
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                        }
                    });
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            main.subtype.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String sub = parent.getItemAtPosition(position).toString();
                    for (Filament f : allFilaments) {
                        try {
                            JSONObject json = new JSONObject(f.filamentParam);
                            if (json.optString("brand").equals(main.brand.getSelectedItem().toString()) && json.optString("type").equals(main.type.getSelectedItem().toString()) && json.optString("subtype").equals(sub)) {
                                MaterialID = json.optString("id");
                                resolveSpoolmanForSelection();
                                main.extMax.setText(String.format(Locale.getDefault(),"%d°C", json.optInt("max_temp")));
                                main.extMin.setText(String.format(Locale.getDefault(),"%d°C", json.optInt("min_temp")));
                                main.bedMax.setText(String.format(Locale.getDefault(),"%d°C", json.optInt("bed_max_temp")));
                                main.bedMin.setText(String.format(Locale.getDefault(),"%d°C", json.optInt("bed_min_temp")));
                                break;
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        } catch (Exception ignored) {}
    }



    private void readTag(Tag tag) {
        if (tag == null) {
            showToast(R.string.no_nfc_tag_found, Toast.LENGTH_SHORT);
            return;
        }
        executorService.execute(() -> {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                try {
                    ndef.connect();
                    NdefMessage ndefMessage = ndef.getNdefMessage();
                    if (ndefMessage == null) {
                        ndef.close();
                        readAceTag(tag);
                        return;
                    }
                    for (NdefRecord record : ndefMessage.getRecords()) {
                        String mimeType = new String(record.getType(), StandardCharsets.US_ASCII);
                        if (mimeType.equals("application/json")) {
                            byte[] payload = record.getPayload();
                            String jsonString = new String(payload, StandardCharsets.UTF_8);
                            OpenSpoolFilament filament = new OpenSpoolFilament(jsonString);
                            mainHandler.post(() -> {
                                userSelect = true;
                                setSpinnerSelection(main.brand, filament.getBrand());
                                main.brand.postDelayed(() -> {
                                    setSpinnerSelection(main.type, filament.getType());
                                    main.type.postDelayed(() -> {
                                        try {
                                            JSONObject json = new JSONObject(jsonString);
                                            setSpinnerSelection(main.subtype, json.optString("subtype", "Basic"));
                                        } catch (Exception ignored) {}
                                    },200);
                                }, 200);

                                setSpinnerSelection(main.spoolsize, GetMaterialWeightByInt(filament.getWeight()));
                                String alpha = filament.getAlpha();
                                String colorHex = filament.getColorHex();
                                MaterialColor = alpha + colorHex;
                                int colorInt = Color.parseColor("#" + MaterialColor);
                                main.colorview.setBackgroundColor(colorInt);
                                main.txtcolor.setText(MaterialColor);
                                main.txtcolor.setTextColor(getContrastColor(colorInt));
                                showToast(R.string.data_read_from_tag, Toast.LENGTH_SHORT);
                                userSelect = false;
                            });
                            return;
                        }
                    }
                } catch (Exception ignored) {
                    showToast(R.string.error_reading_tag, Toast.LENGTH_SHORT);
                } finally {
                    try { ndef.close(); } catch (Exception ignored) {}
                }
            }
            else {
                readAceTag(tag);
            }


        });
    }



    public void writePrinter() {
        try {
            if (SelectedTool == null) {
                showToast("Select Toolhead first", Toast.LENGTH_SHORT);
                return;
            }
            if (GetSetting(context, "u1host", "").isEmpty()) {
                showToast("Set printer host first", Toast.LENGTH_SHORT);
                return;
            }


            if (GetSetting(context, "acemod", false)) {
                Filament filament = matDb.getFilamentById(MaterialID);
                if (filament == null) {
                    showToast(getString(R.string.filament_not_found_in_db), Toast.LENGTH_SHORT);
                    return;
                }
                OpenSpoolFilament osf = new OpenSpoolFilament(filament.filamentParam);
                applySpoolmanFilament(osf);
                String colour = effectiveColour();
                osf.setColor(colour.substring(2), colour.substring(0, 2));
                osf.setPhysicals(175, GetMaterialIntWeight(main.spoolsize.getSelectedItem().toString()));
                setFilamentAce(this, SelectedTool, osf.getBrand(),osf.getType(),osf.getSubType(),osf.getColorHex(),osf.getAlpha(), String.valueOf(GetSetting(context, "u1official", false)), GetMaterialLength(osf.getWeight()), String.valueOf((int)osf.getDiameter()),
                        String.valueOf(osf.getWeight()), String.valueOf(osf.getMinTemp()), String.valueOf(osf.getMaxTemp()), String.valueOf(osf.getBedMinTemp()), String.valueOf(osf.getBedMaxTemp()), success -> {
                            runOnUiThread(() -> {
                                if (success) {
                                    int toolNumber = Integer.parseInt(SelectedTool);
                                    updateCircleColor(select[toolNumber], main.txtcolor.getText().toString());
                                    type[toolNumber].setText(main.type.getSelectedItem().toString());
                                    SaveSetting(context, "tool" + (toolNumber + 1) + "_type", main.type.getSelectedItem().toString());
                                    SaveSetting(context, "tool" + (toolNumber + 1) + "_color", main.txtcolor.getText().toString());
                                    SaveSetting(context, "tool" + (toolNumber + 1) + "_id", MaterialID);
                                    showToast("Filament configuration saved successfully", Toast.LENGTH_SHORT);
                                } else {
                                    showToast("Failed to set filament configuration", Toast.LENGTH_SHORT);
                                }
                            });
                        });


            } else {
                String toolColour = effectiveColour();
                setFilament(this, SelectedTool, main.brand.getSelectedItem().toString(), main.type.getSelectedItem().toString(), main.subtype.getSelectedItem().toString(),
                        toolColour.substring(2) + toolColour.substring(0, 2), success -> {
                            runOnUiThread(() -> {
                                if (success) {
                                    int toolNumber = Integer.parseInt(SelectedTool);
                                    updateCircleColor(select[toolNumber], main.txtcolor.getText().toString());
                                    type[toolNumber].setText(main.type.getSelectedItem().toString());
                                    SaveSetting(context, "tool" + (toolNumber + 1) + "_type", main.type.getSelectedItem().toString());
                                    SaveSetting(context, "tool" + (toolNumber + 1) + "_color", main.txtcolor.getText().toString());
                                    SaveSetting(context, "tool" + (toolNumber + 1) + "_id", MaterialID);
                                    showToast("Filament configuration saved successfully", Toast.LENGTH_SHORT);
                                } else {
                                    showToast("Failed to set filament configuration", Toast.LENGTH_SHORT);
                                }
                            });
                        });
            }
        } catch (Exception ignored) {}
    }

    // Written, and then put on its spool. Those are the two halves of tagging a spool,
    // and they were two errands, the second of them easy to forget it existed.
    private void writtenToTag() {
        showToast(R.string.data_written_to_tag, Toast.LENGTH_SHORT);
        linkTagAfterWrite();
    }

    public void writeTag(Tag tag) {
        if (tag == null) {
            showToast(R.string.no_nfc_tag_found, Toast.LENGTH_SHORT);
            return;
        }
        try {
            Filament filament = matDb.getFilamentById(MaterialID);
            if (filament == null){
                showToast(getString(R.string.filament_not_found_in_db), Toast.LENGTH_SHORT);
                return;
            }
            OpenSpoolFilament osf = new OpenSpoolFilament(filament.filamentParam);
            applySpoolmanFilament(osf);
            String colour = effectiveColour();
            osf.setColor(colour.substring(2), colour.substring(0, 2));
            osf.setPhysicals(175,GetMaterialIntWeight(main.spoolsize.getSelectedItem().toString()));
            byte[] payload = osf.toString().getBytes(StandardCharsets.UTF_8);
            NdefRecord jsonRecord = NdefRecord.createMime(getString(R.string.application_json), payload);
            NdefMessage message = new NdefMessage(jsonRecord);
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                if (!ndef.isWritable()) {
                    showToast(getString(R.string.tag_is_read_only), Toast.LENGTH_SHORT);
                    if (ndef.isConnected()) ndef.close();
                    return;
                }
                int size = message.toByteArray().length;
                if (ndef.getMaxSize() < size) {
                    showToast(getString(R.string.tag_capacity_too_small), Toast.LENGTH_SHORT);
                    if (ndef.isConnected()) ndef.close();
                    return;
                }
                ndef.writeNdefMessage(message);
                if (ndef.isConnected()) ndef.close();
                writtenToTag();
                playBeep();
            } else {
                NdefFormatable ndefFmt = NdefFormatable.get(tag);
                if (ndefFmt != null) {
                    ndefFmt.connect();
                    ndefFmt.format(message);
                    if (ndefFmt.isConnected()) ndefFmt.close();
                    writtenToTag();
                } else {
                    showToast(R.string.invalid_tag_type, Toast.LENGTH_SHORT);
                }
            }

        } catch (Exception e) {
            showToast(R.string.error_writing_to_tag, Toast.LENGTH_SHORT);
        }
    }


    private void writeAceTag(Tag tag) {
        if (tag == null) {
            showToast(R.string.no_nfc_tag_found, Toast.LENGTH_SHORT);
            return;
        }
        executorService.execute(() -> {
            NfcA nfcA = NfcA.get(tag);
            if (nfcA != null) {
                try {
                    if (!nfcA.isConnected()) nfcA.connect();
                    Filament filament = matDb.getFilamentById(MaterialID);
                    if (filament == null){
                        showToast(getString(R.string.filament_not_found_in_db), Toast.LENGTH_SHORT);
                        return;
                    }
                    OpenSpoolFilament osf = new OpenSpoolFilament(filament.filamentParam);
                    byte[] buffer = new byte[144];
                    putAtPage(buffer, 4, new byte[]{123, 0, (byte) 229, 0}, 4);
                    putAtPage(buffer, 5, osf.getBrand().getBytes(StandardCharsets.UTF_8), 20);
                    putAtPage(buffer, 10, osf.getSubType().getBytes(StandardCharsets.UTF_8), 20);
                    putAtPage(buffer, 15, osf.getType().getBytes(StandardCharsets.UTF_8), 20);
                    putAtPage(buffer, 20, formatColor(MaterialColor), 4);
                    //putAtPage(buffer, 21, formatColor(MaterialColor1), 4);
                    //putAtPage(buffer, 22, formatColor(MaterialColor2), 4);
                    putAtPage(buffer, 24, doubleLE(osf.getMinTemp(), osf.getMaxTemp()), 4);
                    putAtPage(buffer, 29, doubleLE(osf.getBedMinTemp(), osf.getBedMaxTemp()), 4);
                    putAtPage(buffer, 30, doubleLE(175, GetMaterialLength(main.spoolsize.getSelectedItem().toString())), 4);
                    putAtPage(buffer, 31, new byte[]{(byte) 232, 3, 0, 0}, 4);
                    putAtPage(buffer, 33, osf.getID().getBytes(StandardCharsets.UTF_8), 20);
                    rawTagWrite(nfcA, 4, buffer, 144);
                    playBeep();
                    writtenToTag();
                } catch (Exception e) {
                    showToast(R.string.error_writing_to_tag, Toast.LENGTH_SHORT);
                } finally {
                    try {
                        if (nfcA.isConnected()) nfcA.close();
                    } catch (Exception ignored) {
                    }
                }
            } else {
                showToast(R.string.invalid_tag_type, Toast.LENGTH_SHORT);
            }
        });
    }


    private void readAceTag(Tag tag) {
        if (tag == null) {
            showToast(R.string.no_nfc_tag_found, Toast.LENGTH_SHORT);
            return;
        }
        executorService.execute(() -> {
            NfcA nfcA = NfcA.get(tag);
            if (nfcA != null) {
                try {
                    if (!nfcA.isConnected()) nfcA.connect();
                    byte[] header = rawTagRead(nfcA, 4, 4);
                    if (Arrays.equals(header, new byte[]{123, 0, (byte)229, 0})) {
                        byte[] colorData = rawTagRead(nfcA, 20, 4);
                        MaterialColor = String.format("%02X%02X%02X%02X", colorData[0], colorData[3], colorData[2], colorData[1]);
                        byte[] lengthData = rawTagRead(nfcA, 30, 4);
                        String materialWeight = GetMaterialWeight(((lengthData[3] & 0xFF) << 8) | (lengthData[2] & 0xFF));
                        String materialID = new String(rawTagRead(nfcA, 33, 16), StandardCharsets.UTF_8).trim();
                        Filament filament = matDb.getFilamentById(materialID);
                        if (filament == null){
                            showToast(getString(R.string.filament_not_found_in_db), Toast.LENGTH_SHORT);
                            return;
                        }
                        OpenSpoolFilament osf = new OpenSpoolFilament(filament.filamentParam);
                        mainHandler.post(() -> {
                            userSelect = true;
                            setSpinnerSelection(main.brand, osf.getBrand());
                            main.brand.postDelayed(() -> {
                                setSpinnerSelection(main.type, osf.getType());
                                main.type.postDelayed(() -> {
                                    try {
                                        setSpinnerSelection(main.subtype, osf.getSubType());
                                    } catch (Exception ignored) {}
                                },200);
                            },200);

                            setSpinnerSelection(main.spoolsize, materialWeight);
                            int colorInt = Color.parseColor("#" + MaterialColor);
                            main.colorview.setBackgroundColor(colorInt);
                            main.txtcolor.setText(MaterialColor);
                            main.txtcolor.setTextColor(getContrastColor(colorInt));
                            userSelect = false;
                        });
                    } else {
                        showToast(R.string.unknown_or_empty_tag, Toast.LENGTH_SHORT);
                    }
                } catch (Exception ignored) {
                    showToast(R.string.error_reading_tag, Toast.LENGTH_SHORT);
                } finally {
                    try {
                        if (nfcA.isConnected()) nfcA.close();
                    } catch (Exception ignored) {
                    }
                }
            } else {
                showToast(R.string.invalid_tag_type, Toast.LENGTH_SHORT);
            }
        });
    }


    private int getTagType(NfcA nfcA) {
        if (probePage(nfcA, (byte) 220)) return 216;
        if (probePage(nfcA, (byte) 125)) return 215;
        if (probePage(nfcA, (byte) 47)) return 100;
        return 213;
    }


    private boolean probePage(NfcA nfcA, byte pageNumber) {
        try {
            if (!nfcA.isConnected()) nfcA.connect();
            byte[] result = nfcA.transceive(new byte[]{(byte) 0x30, pageNumber});
            if (result != null && result.length == 16) {
                return true;
            }
        } catch (Exception ignored) {
        } finally {
            try {
                if (nfcA.isConnected()) nfcA.close();
            } catch (Exception ignored) {}
        }
        return false;
    }


    private void formatTag(Tag tag) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        SpannableString titleText = new SpannableString(getString(R.string.format_tag));
        titleText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.primary_brand)), 0, titleText.length(), 0);
        SpannableString messageText = new SpannableString(getString(R.string.this_will_erase_the_data_on_the_tag_and_format_it_for_writing));
        messageText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.text_main)), 0, messageText.length(), 0);
        builder.setTitle(titleText);
        builder.setMessage(messageText);
        builder.setPositiveButton(R.string.format, (dialog, which) -> {
            if (tag == null) {
                showToast(R.string.no_nfc_tag_found, Toast.LENGTH_SHORT);
                return;
            }
            executorService.execute(() -> {
                Ndef ndef = Ndef.get(tag);
                if (ndef != null) {
                    showToast(getString(R.string.tag_is_already_ndef_formatted), Toast.LENGTH_SHORT);
                } else {
                    NdefFormatable ndefFmt = NdefFormatable.get(tag);
                    if (ndefFmt != null) {
                        try {
                            ndefFmt.connect();
                            NdefRecord jsonRecord = NdefRecord.createMime(getString(R.string.application_json), new byte[]{123 ,125});
                            NdefMessage message = new NdefMessage(jsonRecord);
                            ndefFmt.format(message);
                            showToast(R.string.tag_formatted, Toast.LENGTH_SHORT);
                        } catch (Exception e) {
                            showToast(R.string.failed_to_format_tag_for_writing, Toast.LENGTH_SHORT);
                        } finally {
                            try {
                                if (ndefFmt.isConnected()) ndefFmt.close();
                            } catch (Exception ignored) {}
                        }
                    } else {
                        showToast(R.string.no_nfc_tag_found, Toast.LENGTH_SHORT);
                    }
                }
            });

        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        AlertDialog alert = builder.create();
        alert.show();
        if (alert.getWindow() != null) {
            alert.getWindow().setBackgroundDrawableResource(R.color.background_alt);
            alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.primary_brand));
            alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.primary_brand));
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    void openPicker() {
        try {
            pickerDialog = new Dialog(this, R.style.Theme_U1RFID);
            pickerDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            pickerDialog.setCanceledOnTouchOutside(false);
            PickerDialogBinding dl = PickerDialogBinding.inflate(getLayoutInflater());
            View rv = dl.getRoot();
            colorDialog = dl;
            pickerDialog.setContentView(rv);
            gradientBitmap = null;

            dl.btncls.setOnClickListener(v -> {
                    if (dl.txtcolor.getText().toString().length() == 8) {
                        try {
                            int color = Color.argb(dl.alphaSlider.getProgress(), dl.redSlider.getProgress(), dl.greenSlider.getProgress(), dl.blueSlider.getProgress());
                            MaterialColor = dl.txtcolor.getText().toString();
                            resolveSpoolmanForSelection();
                            main.colorview.setBackgroundColor(color);
                            main.txtcolor.setText(MaterialColor);
                            main.txtcolor.setTextColor(getContrastColor(Color.parseColor("#" + MaterialColor)));
                        } catch (Exception ignored) {
                        }
                    }

                pickerDialog.dismiss();
            });

            dl.redSlider.setProgress(Color.red(Color.parseColor("#" + MaterialColor)));
            dl.greenSlider.setProgress(Color.green(Color.parseColor("#" + MaterialColor)));
            dl.blueSlider.setProgress(Color.blue(Color.parseColor("#" + MaterialColor)));
            dl.alphaSlider.setProgress(Color.alpha(Color.parseColor("#" + MaterialColor)));

            setupPresetColors(dl);
            updateColorDisplay(dl, dl.alphaSlider.getProgress(), dl.redSlider.getProgress(), dl.greenSlider.getProgress(), dl.blueSlider.getProgress());

            setupGradientPicker(dl);

            dl.gradientPickerView.setOnTouchListener((v, event) -> {
                v.performClick();
                if (gradientBitmap == null) {
                    return false;
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                    float touchX = event.getX();
                    float touchY = event.getY();
                    int pixelX = Math.max(0, Math.min(gradientBitmap.getWidth() - 1, (int) touchX));
                    int pixelY = Math.max(0, Math.min(gradientBitmap.getHeight() - 1, (int) touchY));
                    int pickedColor = gradientBitmap.getPixel(pixelX, pixelY);
                    setSlidersFromColor(dl, Color.argb(255, Color.red(pickedColor), Color.green(pickedColor), Color.blue(pickedColor)));
                    return true;
                }
                return false;
            });

            setupCollapsibleSection(dl,
                    dl.rgbSlidersHeader,
                    dl.rgbSlidersContent,
                    dl.rgbSlidersToggleIcon,
                    GetSetting(this,"RGB_VIEW",false)
            );
            setupCollapsibleSection(dl,
                    dl.gradientPickerHeader,
                    dl.gradientPickerContent,
                    dl.gradientPickerToggleIcon,
                    GetSetting(this,"PICKER_VIEW",true)
            );
            setupCollapsibleSection(dl,
                    dl.presetColorsHeader,
                    dl.presetColorsContent,
                    dl.presetColorsToggleIcon,
                    GetSetting(this,"PRESET_VIEW",true)
            );
            setupCollapsibleSection(dl,
                    dl.photoColorHeader,
                    dl.photoColorContent,
                    dl.photoColorToggleIcon,
                    GetSetting(this, "PHOTO_VIEW", false)
            );

            SeekBar.OnSeekBarChangeListener rgbChangeListener = new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    updateColorDisplay(dl, dl.alphaSlider.getProgress(), dl.redSlider.getProgress(), dl.greenSlider.getProgress(), dl.blueSlider.getProgress());
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            };

            dl.redSlider.setOnSeekBarChangeListener(rgbChangeListener);
            dl.greenSlider.setOnSeekBarChangeListener(rgbChangeListener);
            dl.blueSlider.setOnSeekBarChangeListener(rgbChangeListener);

            dl.alphaSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                    updateColorDisplay(dl, dl.alphaSlider.getProgress(), dl.redSlider.getProgress(), dl.greenSlider.getProgress(), dl.blueSlider.getProgress());
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            dl.txtcolor.setOnClickListener(v -> showHexInputDialog(dl));

            dl.photoImage.setOnClickListener(v -> {
                Drawable drawable = ContextCompat.getDrawable(dl.photoImage.getContext(), R.drawable.camera);
                if (dl.photoImage.getDrawable() != null && drawable != null) {
                    if (Objects.equals(dl.photoImage.getDrawable().getConstantState(), drawable.getConstantState())) {
                        checkPermissionsAndCapture();
                    }
                } else {
                    checkPermissionsAndCapture();
                }
            });

            dl.clearImage.setOnClickListener(v -> {

                dl.photoImage.setImageResource( R.drawable.camera);
                dl.photoImage.setDrawingCacheEnabled(false);
                dl.photoImage.buildDrawingCache(false);
                dl.photoImage.setOnTouchListener(null);
                dl.clearImage.setVisibility(View.GONE);

            });

            pickerDialog.show();
        } catch (Exception ignored) {}
    }


    void openAddDialog(boolean edit) {
        try {
            addDialog = new Dialog(this, R.style.Theme_U1RFID);
            addDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            addDialog.setCanceledOnTouchOutside(false);
            AddDialogBinding dl = AddDialogBinding.inflate(getLayoutInflater());
            View rv = dl.getRoot();
            addDialog.setContentView(rv);
            dl.btncls.setOnClickListener(v -> addDialog.dismiss());


            dl.chkvendor.setOnClickListener(v -> {
                if (dl.chkvendor.isChecked()) {
                    dl.vendor.setVisibility(View.INVISIBLE);
                    dl.layoutVendor.setVisibility(View.VISIBLE);
                    dl.vendorborder.setVisibility(View.INVISIBLE);
                    dl.lblvendor.setVisibility(View.INVISIBLE);

                } else {
                    dl.vendor.setVisibility(View.VISIBLE);
                    dl.layoutVendor.setVisibility(View.INVISIBLE);
                    dl.vendorborder.setVisibility(View.VISIBLE);
                    dl.lblvendor.setVisibility(View.VISIBLE);

                }
            });

            if (edit) {
                dl.btnsave.setText(R.string.save);
                dl.lbltitle.setText(R.string.edit_filament);
            }
            else {
                dl.btnsave.setText(R.string.add);
                dl.lbltitle.setText(R.string.add_filament);
            }

           dl.btnsave.setOnClickListener(v -> {
               if (Objects.requireNonNull(dl.txtextmin.getText()).toString().isEmpty() || Objects.requireNonNull(dl.txtextmax.getText()).toString().isEmpty() || Objects.requireNonNull(dl.txtbedmin.getText()).toString().isEmpty() || Objects.requireNonNull(dl.txtbedmax.getText()).toString().isEmpty())
               {
                   showToast(R.string.fill_all_fields, Toast.LENGTH_SHORT);
                   return;
               }
               if (dl.chkvendor.isChecked() && Objects.requireNonNull(dl.txtvendor.getText()).toString().isEmpty()) {
                   showToast(R.string.fill_all_fields, Toast.LENGTH_SHORT);
                   return;
               }

               String vendor = dl.vendor.getSelectedItem().toString();
               if (dl.chkvendor.isChecked())
               {
                   vendor = Objects.requireNonNull(dl.txtvendor.getText()).toString().trim();
               }
               if (edit) {
                   updateFilament(vendor, dl.type.getSelectedItem().toString(), dl.subtype.getSelectedItem().toString(), dl.txtextmin.getText().toString(), dl.txtextmax.getText().toString(), dl.txtbedmin.getText().toString(), dl.txtbedmax.getText().toString());
               } else {
                   addFilament(vendor, dl.type.getSelectedItem().toString(), dl.subtype.getSelectedItem().toString(), dl.txtextmin.getText().toString(), dl.txtextmax.getText().toString(), dl.txtbedmin.getText().toString(), dl.txtbedmax.getText().toString());
               }

               addDialog.dismiss();
           });

            ArrayAdapter<String> vadapter = new ArrayAdapter<>(this, R.layout.spinner_item, filamentVendors);
            dl.vendor.setAdapter(vadapter);

            ArrayAdapter<String> tadapter = new ArrayAdapter<>(this, R.layout.spinner_item, filamentTypes);
            dl.type.setAdapter(tadapter);

            FilamentRegistry.FilamentProfile profile = FilamentRegistry.getProfile(dl.type.getSelectedItem().toString());
            dl.txtextmin.setText(String.valueOf(profile.minNozzleTemp));
            dl.txtextmax.setText(String.valueOf(profile.maxNozzleTemp));
            dl.txtbedmin.setText(String.valueOf(profile.minBedTemp));
            dl.txtbedmax.setText(String.valueOf(profile.maxBedTemp));
            ArrayAdapter<String> sadapter = new ArrayAdapter<>(this, R.layout.spinner_item, profile.subtypes);
            dl.subtype.setAdapter(sadapter);

            dl.type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                        String selectedType = parentView.getItemAtPosition(position).toString();
                        FilamentRegistry.FilamentProfile profile = FilamentRegistry.getProfile(selectedType);
                        dl.txtextmin.setText(String.valueOf(profile.minNozzleTemp));
                        dl.txtextmax.setText(String.valueOf(profile.maxNozzleTemp));
                        dl.txtbedmin.setText(String.valueOf(profile.minBedTemp));
                        dl.txtbedmax.setText(String.valueOf(profile.maxBedTemp));
                        ArrayAdapter<String> sadapter = new ArrayAdapter<>(parentView.getContext(), R.layout.spinner_item, profile.subtypes);
                        dl.subtype.setAdapter(sadapter);
                }
                @Override
                public void onNothingSelected(AdapterView<?> parentView) {}
            });

            if (edit) {
                Filament filament = matDb.getFilamentById(MaterialID);
                OpenSpoolFilament osf = new OpenSpoolFilament(filament.filamentParam);
                setSpinnerSelection(dl.type, osf.getType());
                try {
                    if (!arrayContains(filamentVendors, osf.getBrand())) {
                        dl.chkvendor.setChecked(true);
                        dl.layoutVendor.setVisibility(View.VISIBLE);
                        dl.vendorborder.setVisibility(View.INVISIBLE);
                        dl.lblvendor.setVisibility(View.INVISIBLE);
                        dl.vendor.setVisibility(View.INVISIBLE);
                        dl.txtvendor.setText(osf.getBrand());
                    } else {
                        dl.chkvendor.setChecked(false);
                        dl.layoutVendor.setVisibility(View.INVISIBLE);
                        dl.vendorborder.setVisibility(View.VISIBLE);
                        dl.lblvendor.setVisibility(View.VISIBLE);
                        dl.vendor.setVisibility(View.VISIBLE);
                        setSpinnerSelection(dl.vendor, osf.getBrand());
                    }
                } catch (Exception ignored) {
                    dl.chkvendor.setChecked(false);
                    dl.layoutVendor.setVisibility(View.INVISIBLE);
                    dl.vendorborder.setVisibility(View.VISIBLE);
                    dl.lblvendor.setVisibility(View.VISIBLE);
                    dl.vendor.setVisibility(View.VISIBLE);
                    dl.vendor.setSelection(47);
                    dl.type.setSelection(5);
                }
                setSpinnerSelection(dl.subtype, osf.getSubType());
                dl.txtextmin.setText(String.valueOf(osf.getMinTemp()));
                dl.txtextmax.setText(String.valueOf(osf.getMaxTemp()));
                dl.txtbedmin.setText(String.valueOf(osf.getBedMinTemp()));
                dl.txtbedmax.setText(String.valueOf(osf.getBedMaxTemp()));

            }else {
                dl.vendor.setSelection(47);
                dl.type.setSelection(5);
                FilamentRegistry.FilamentProfile fp = FilamentRegistry.getProfile("PLA");
                dl.txtextmin.setText(String.valueOf(fp.minNozzleTemp));
                dl.txtextmax.setText(String.valueOf(fp.maxNozzleTemp));
                dl.txtbedmin.setText(String.valueOf(fp.minBedTemp));
                dl.txtbedmax.setText(String.valueOf(fp.maxBedTemp));
            }
            addDialog.show();
        } catch (Exception ignored) {}
    }


    void addFilament(String tmpVendor, String tmpType, String tmpSubType, String tmpExtMin, String tmpExtMax, String tmpBedMin, String tmpBedMax) {
        try {
            OpenSpoolFilament osfilament = new OpenSpoolFilament();
            osfilament.setType(tmpVendor, tmpType, tmpSubType);
            osfilament.setPhysicals(1.75, 1000);
            osfilament.setTemps(Integer.parseInt(tmpExtMin), Integer.parseInt(tmpExtMax), Integer.parseInt(tmpBedMin), Integer.parseInt(tmpBedMax));
            Filament filament = new Filament();
            filament.position = matDb.getItemCount();
            filament.filamentID = osfilament.getID();
            filament.filamentName = tmpType;
            filament.filamentVendor = tmpVendor;
            filament.filamentParam = osfilament.toString();
            matDb.addItem(filament);
            loadMaterials();
        } catch (Exception ignored) {}
    }


    void updateFilament(String tmpVendor, String tmpType, String tmpSubType, String tmpExtMin, String tmpExtMax, String tmpBedMin, String tmpBedMax) {
        try {
            Filament currentFilament = matDb.getFilamentById(MaterialID);
            int tmpPosition = currentFilament.position;
            OpenSpoolFilament osfilament = new OpenSpoolFilament(currentFilament.filamentParam);
            osfilament.setType(tmpVendor, tmpType, tmpSubType);
            osfilament.setPhysicals(1.75, 1000);
            osfilament.setTemps(Integer.parseInt(tmpExtMin), Integer.parseInt(tmpExtMax), Integer.parseInt(tmpBedMin), Integer.parseInt(tmpBedMax));
            Filament filament = new Filament();
            filament.position = tmpPosition;
            filament.filamentID = osfilament.getID();
            filament.filamentName = tmpType;
            filament.filamentVendor = tmpVendor;
            filament.filamentParam = osfilament.toString();
            matDb.deleteItem(currentFilament);
            matDb.addItem(filament);
            loadMaterials();
        } catch (Exception ignored) {}
    }


    private void setupSpoolmanColors(PickerDialogBinding dl) {
        for (JSONObject f : cachedSmCandidates) {
            String hex = f.optString("color_hex", "").replace("#", "");
            String rgb = spoolmanRgb(hex);
            if (rgb.isEmpty()) continue;
            int color;
            try {
                color = Color.parseColor("#" + spoolmanAlpha(hex, "FF") + rgb);
            } catch (Exception ignored) {
                continue;
            }
            Button colorButton = new Button(this);
            FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                    (int) getResources().getDimension(R.dimen.preset_circle_size),
                    (int) getResources().getDimension(R.dimen.preset_circle_size)
            );
            int margin = (int) getResources().getDimension(R.dimen.preset_circle_margin);
            params.setMargins(margin, margin, margin, margin);
            colorButton.setLayoutParams(params);
            GradientDrawable circleDrawable = (GradientDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.circle_shape, null);
            assert circleDrawable != null;
            circleDrawable.setColor(color);
            colorButton.setBackground(circleDrawable);
            colorButton.setTag(color);
            colorButton.setContentDescription(f.optString("name"));
            int filamentId = f.optInt("id");
            colorButton.setOnClickListener(v -> {
                // Choosing the colour chooses the product, so the guess is not consulted.
                pinnedForMaterialId = MaterialID;
                pinnedSmFilamentId = filamentId;
                setSlidersFromColor(dl, (int) v.getTag());
                showToast(f.optString("name"), Toast.LENGTH_SHORT);
            });
            dl.presetColorGrid.addView(colorButton);
        }
    }


    private void updateColorDisplay(PickerDialogBinding dl, int currentAlpha,int currentRed,int currentGreen,int currentBlue) {
        int color = Color.argb(currentAlpha, currentRed, currentGreen, currentBlue);
        dl.colorDisplay.setBackgroundColor(color);
        String hexCode = rgbToHexA(currentRed, currentGreen, currentBlue, currentAlpha);
        dl.txtcolor.setText(hexCode);
        dl.txtcolor.setTextColor(getContrastColor(Color.parseColor("#" + hexCode)));
    }


    /*
     * The colours offered when picking one. With Spoolman connected and a filament
     * selected, those are the colours that filament is actually made in, and choosing one
     * settles which product this is as well as what shade it is. Otherwise, and for any
     * shade not among them, the sliders below still reach the whole range.
     */
    private void setupPresetColors(PickerDialogBinding dl) {
        dl.presetColorGrid.removeAllViews();
        if (!cachedSmCandidates.isEmpty()) {
            setupSpoolmanColors(dl);
            return;
        }
        for (int color : presetColors()) {
            Button colorButton = new Button(this);
            FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                    (int) getResources().getDimension(R.dimen.preset_circle_size),
                    (int) getResources().getDimension(R.dimen.preset_circle_size)
            );
            params.setMargins(
                    (int) getResources().getDimension(R.dimen.preset_circle_margin),
                    (int) getResources().getDimension(R.dimen.preset_circle_margin),
                    (int) getResources().getDimension(R.dimen.preset_circle_margin),
                    (int) getResources().getDimension(R.dimen.preset_circle_margin)
            );
            colorButton.setLayoutParams(params);
            GradientDrawable circleDrawable = (GradientDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.circle_shape, null);
            assert circleDrawable != null;
            circleDrawable.setColor(color);
            colorButton.setBackground(circleDrawable);
            colorButton.setTag(color);
            colorButton.setOnClickListener(v -> {
                int selectedColor = (int) v.getTag();
                setSlidersFromColor(dl, selectedColor);
            });
            dl.presetColorGrid.addView(colorButton);
        }
    }


    private void setSlidersFromColor(PickerDialogBinding dl, int argbColor) {
        dl.redSlider.setProgress(Color.red(argbColor));
        dl.greenSlider.setProgress(Color.green(argbColor));
        dl.blueSlider.setProgress(Color.blue(argbColor));
        dl.alphaSlider.setProgress(Color.alpha(argbColor));
        updateColorDisplay(dl, Color.alpha(argbColor), Color.red(argbColor), Color.green(argbColor), Color.blue(argbColor));
    }


    private void showHexInputDialog(PickerDialogBinding dl) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        builder.setTitle(R.string.enter_hex_color_aarrggbb);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        input.setHint(R.string.aarrggbb);
        input.setTextColor(Color.BLACK);
        input.setHintTextColor(Color.GRAY);
        input.setTextAlignment(TEXT_ALIGNMENT_CENTER);
        input.setText(rgbToHexA(dl.redSlider.getProgress(), dl.greenSlider.getProgress(), dl.blueSlider.getProgress(), dl.alphaSlider.getProgress()));
        InputFilter[] filters = new InputFilter[3];
        filters[0] = new Utils.HexInputFilter();
        filters[1] = new InputFilter.LengthFilter(8);
        filters[2] = new InputFilter.AllCaps();
        input.setFilters(filters);
        builder.setView(input);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.submit, (dialog, which) -> {
            String hexInput = input.getText().toString().trim();
            if (isValidHexCode(hexInput)) {
                setSlidersFromColor(dl, Color.parseColor("#" + hexInput));
            } else {
                showToast(R.string.invalid_hex_code_please_use_aarrggbb_format, Toast.LENGTH_LONG);
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        inputDialog = builder.create();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidthPx = displayMetrics.widthPixels;
        float density = getResources().getDisplayMetrics().density;
        int maxWidthDp = 100;
        int maxWidthPx = (int) (maxWidthDp * density);
        int dialogWidthPx = (int) (screenWidthPx * 0.80);
        if (dialogWidthPx > maxWidthPx) {
            dialogWidthPx = maxWidthPx;
        }
        Objects.requireNonNull(inputDialog.getWindow()).setLayout(dialogWidthPx, WindowManager.LayoutParams.WRAP_CONTENT);
        inputDialog.getWindow().setGravity(Gravity.CENTER); // Center the dialog on the screen
        inputDialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = inputDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = inputDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            positiveButton.setTextColor(Color.parseColor("#82B1FF"));
            negativeButton.setTextColor(Color.parseColor("#82B1FF"));
        });
        inputDialog.show();
    }


    void setupGradientPicker(PickerDialogBinding dl) {
        dl.gradientPickerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                dl.gradientPickerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int width = dl.gradientPickerView.getWidth();
                int height = dl.gradientPickerView.getHeight();
                if (width > 0 && height > 0) {
                    gradientBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(gradientBitmap);
                    Paint paint = new Paint();
                    float[] hsv = new float[3];
                    hsv[1] = 1.0f;
                    for (int y = 0; y < height; y++) {
                        hsv[2] = 1.0f - (float) y / height;
                        for (int x = 0; x < width; x++) {
                            hsv[0] = (float) x / width * 360f;
                            paint.setColor(Color.HSVToColor(255, hsv));
                            canvas.drawPoint(x, y, paint);
                        }
                    }
                    dl.gradientPickerView.setBackground(new BitmapDrawable(getResources(), gradientBitmap));
                }
            }
        });
    }


    private void setupCollapsibleSection(PickerDialogBinding dl, LinearLayout header, final ViewGroup content, final ImageView toggleIcon, boolean isExpandedInitially) {
        content.setVisibility(isExpandedInitially ? View.VISIBLE : View.GONE);
        toggleIcon.setImageResource(isExpandedInitially ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down);
        header.setOnClickListener(v -> {
            if (content.getVisibility() == View.VISIBLE) {
                content.setVisibility(View.GONE);
                toggleIcon.setImageResource(R.drawable.ic_arrow_down);
                if (header.getId() == dl.rgbSlidersHeader.getId()) {
                    SaveSetting(this,"RGB_VIEW",false);
                }
                else if (header.getId() == dl.gradientPickerHeader.getId()) {
                    SaveSetting(this,"PICKER_VIEW",false);
                }
                else if (header.getId() == dl.presetColorsHeader.getId()) {
                    SaveSetting(this,"PRESET_VIEW",false);
                }
                else if (header.getId() == dl.photoColorHeader.getId()) {
                    SaveSetting(this,"PHOTO_VIEW",false);
                }
            } else {
                content.setVisibility(View.VISIBLE);
                toggleIcon.setImageResource(R.drawable.ic_arrow_up);
                if (header.getId() == dl.rgbSlidersHeader.getId()) {
                    SaveSetting(this,"RGB_VIEW",true);
                }
                else if (header.getId() == dl.gradientPickerHeader.getId()) {
                    SaveSetting(this,"PICKER_VIEW",true);
                    if (gradientBitmap == null) {
                        setupGradientPicker(dl);
                    }
                }
                else if (header.getId() == dl.presetColorsHeader.getId()) {
                    SaveSetting(this,"PRESET_VIEW",true);
                }
                else if (header.getId() == dl.photoColorHeader.getId()) {
                    SaveSetting(this,"PHOTO_VIEW",true);
                }
            }
        });
    }


    private void setupActivityResultLaunchers() {
        try {
            exportDirectoryChooser = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        try {
                            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                                Uri treeUri = result.getData().getData();
                                if (treeUri != null) {
                                    getContentResolver().takePersistableUriPermission(
                                            treeUri,
                                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    );
                                    performSAFExport(treeUri);
                                } else {
                                    showToast(R.string.failed_to_get_export_directory, Toast.LENGTH_SHORT);
                                }
                            } else {
                                showToast(R.string.export_cancelled, Toast.LENGTH_SHORT);
                            }
                        } catch (Exception ignored) {
                        }
                    }
            );

            importFileChooser = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        try {
                            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                                Uri fileUri = result.getData().getData();
                                if (fileUri != null) {
                                    performSAFImport(fileUri);
                                } else {
                                    showToast(R.string.failed_to_select_import_file, Toast.LENGTH_SHORT);
                                }
                            } else {
                                showToast(R.string.import_cancelled, Toast.LENGTH_SHORT);
                            }

                        } catch (Exception ignored) {
                        }
                    }
            );

            requestPermissionLauncher = registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        try {

                            if (isGranted) {
                                if (pendingAction == ACTION_EXPORT) {
                                    performLegacyExport();
                                } else if (pendingAction == ACTION_IMPORT) {
                                    performLegacyImport();
                                }
                            } else {
                                showToast(R.string.storage_permission_denied_cannot_perform_action, Toast.LENGTH_LONG);
                            }
                            pendingAction = -1;

                        } catch (Exception ignored) {
                        }
                    }
            );

            cameraLauncher = registerForActivityResult(
                    new ActivityResultContracts.TakePicturePreview(),
                    bitmap -> {
                        try {
                            if (bitmap != null) {
                                colorDialog.photoImage.setImageBitmap(bitmap);
                                setupPhotoPicker(colorDialog.photoImage);
                            } else {
                                showToast(R.string.photo_capture_cancelled_or_failed, Toast.LENGTH_SHORT);
                            }
                        } catch (Exception ignored) {
                        }
                    }
            );
        } catch (Exception ignored) {
        }
    }


    private void checkPermissionAndStartAction(int actionType) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                if (actionType == ACTION_EXPORT) {
                    performLegacyExport();
                } else {
                    performLegacyImport();
                }
            } else {
                pendingAction = actionType;
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        } else {
            if (actionType == ACTION_EXPORT) {
                startSAFExportProcess();
            } else {
                startSAFImportProcess();
            }
        }
    }


    private void startSAFExportProcess() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.select_backup_folder));
        exportDirectoryChooser.launch(intent);
    }


    private void performSAFExport(Uri treeUri) {
        executorService.execute(() -> {
            try {
                File dbFile = filamentDB.getDatabaseFile(this);
                filamentDB.closeInstance();
                DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
                if (pickedDir == null || !pickedDir.exists() || !pickedDir.canWrite()) {
                    showToast(R.string.cannot_write_to_selected_directory, Toast.LENGTH_LONG);
                    return;
                }
                String dbBaseName = dbFile.getName().replace(".db", "");
                DocumentFile dbDestFile = pickedDir.createFile("application/octet-stream", dbBaseName + ".db");
                if (dbDestFile != null) {
                    copyFileToUri(this, dbFile, dbDestFile.getUri());
                } else {
                    showToast(R.string.failed_to_create_db_backup_file, Toast.LENGTH_LONG);
                    return;
                }
                showToast(R.string.database_exported_successfully, Toast.LENGTH_LONG);
            } catch (Exception e) {
                showToast(getString(R.string.database_saf_export_failed) + e.getMessage(), Toast.LENGTH_LONG);
            } finally {
                filamentDB.getInstance(this);
            }
        });
    }


    private void performLegacyExport() {
        executorService.execute(() -> {
            try {
                File dbFile = filamentDB.getDatabaseFile(this);
                filamentDB.closeInstance();
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                if (!downloadsDir.exists()) {
                    boolean val = downloadsDir.mkdirs();
                }
                String dbBaseName = dbFile.getName().replace(".db", "");
                File dbDestFile = new File(downloadsDir, dbBaseName + ".db");
                copyFile(dbFile, dbDestFile);
                showToast(R.string.database_exported_successfully_to_downloads_folder, Toast.LENGTH_LONG);
            } catch (Exception e) {
                showToast(getString(R.string.database_legacy_export_failed) + e.getMessage(), Toast.LENGTH_LONG);
            } finally {
                filamentDB.getInstance(this);
            }
        });
    }


    private void startSAFImportProcess() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        String[] mimeTypes = {"application/x-sqlite3", "application/vnd.sqlite3", "application/octet-stream"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        importFileChooser.launch(intent);
    }


    private void performSAFImport(Uri sourceUri) {
        if (!sourceUri.toString().toLowerCase().contains("filament_database")) {
            showToast(R.string.incorrect_database_file_selected, Toast.LENGTH_LONG);
            return;
        }
        executorService.execute(() -> {
            try {
                filamentDB.closeInstance();
                File dbFile = filamentDB.getDatabaseFile(this);
                File dbDir = dbFile.getParentFile();
                if (dbDir != null && !dbDir.exists()) {
                    boolean val = dbDir.mkdirs();
                }
                copyUriToFile(this, sourceUri, dbFile);
                filamentDB.getInstance(this);
                setMatDb();

                showToast(R.string.database_imported_successfully, Toast.LENGTH_LONG);
            } catch (Exception e) {
                showToast(getString(R.string.database_saf_import_failed) + e.getMessage(), Toast.LENGTH_LONG);
            } finally {
                if (filamentDB.INSTANCE == null) {
                    filamentDB.getInstance(this);
                    setMatDb();
                }
            }
        });
    }


    private void performLegacyImport() {
        executorService.execute(() -> {
            try {
                filamentDB.closeInstance();

                File dbFile = filamentDB.getDatabaseFile(this);
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File sourceDbFile = new File(downloadsDir, dbFile.getName());
                if (!dbFile.getName().toLowerCase().contains("filament_database")) {
                    showToast(R.string.incorrect_database_file_selected, Toast.LENGTH_LONG);
                    return;
                }
                if (!sourceDbFile.exists()) {
                    showToast(getString(R.string.backup_file_not_found_in_downloads) + sourceDbFile.getName(), Toast.LENGTH_LONG);
                    return;
                }
                File dbDir = dbFile.getParentFile();
                if (dbDir != null && !dbDir.exists()) {
                    boolean val = dbDir.mkdirs();
                }
                copyFile(sourceDbFile, dbFile);
                filamentDB.getInstance(this);
                setMatDb();

                showToast(R.string.database_imported_successfully, Toast.LENGTH_LONG);

            } catch (Exception e) {
                showToast(getString(R.string.database_legacy_import_failed) + e.getMessage(), Toast.LENGTH_LONG);
            } finally {
                if (filamentDB.INSTANCE == null) {
                    filamentDB.getInstance(this);
                    setMatDb();

                }
            }
        });
    }


    private void showImportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        SpannableString titleText = new SpannableString(getString(R.string.import_database));
        titleText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.primary_brand)), 0, titleText.length(), 0);
        SpannableString messageText = new SpannableString(getString(R.string.restore_database_from_file_filament_database_db));
        messageText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.text_main)), 0, messageText.length(), 0);
        builder.setTitle(titleText);
        builder.setMessage(messageText);
        builder.setPositiveButton(R.string.import_txt, (dialog, which) -> checkPermissionAndStartAction(ACTION_IMPORT));
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.color.background_alt);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.primary_brand));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.primary_brand));
        }
    }


    private void showExportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        SpannableString titleText = new SpannableString(getString(R.string.export_database));
        titleText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.primary_brand)), 0, titleText.length(), 0);
        SpannableString messageText = new SpannableString(getString(R.string.backup_database_to_file_filament_database_db));
        messageText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.text_main)), 0, messageText.length(), 0);
        builder.setTitle(titleText);
        builder.setMessage(messageText);
        builder.setPositiveButton(R.string.export, (dialog, which) -> new Thread(() -> {
            if (matDb.getItemCount() > 0) {
                mainHandler.post(() -> checkPermissionAndStartAction(ACTION_EXPORT));
            } else {
                mainHandler.post(() -> showToast(R.string.no_data_to_export, Toast.LENGTH_SHORT));
            }
        }).start());
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.color.background_alt);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.primary_brand));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.primary_brand));
        }
    }


    private void checkPermissionsAndCapture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
        }
        else {
            takePicture();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePicture();
            } else {
                showToast(R.string.camera_permission_is_required_to_take_photos, Toast.LENGTH_SHORT);
            }
        }
    }


    private void takePicture() {
        if (cameraLauncher != null) {
            cameraLauncher.launch(null);
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    private void setupPhotoPicker(ImageView imageView) {
        colorDialog.clearImage.setVisibility(View.VISIBLE);
        imageView.setDrawingCacheEnabled(true);
        imageView.buildDrawingCache(true);
        imageView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                Bitmap bitmap = imageView.getDrawingCache();
                float touchX = event.getX();
                float touchY = event.getY();
                if (touchX >= 0 && touchX < bitmap.getWidth() && touchY >= 0 && touchY < bitmap.getHeight()) {
                    try {
                        int pixel = bitmap.getPixel((int) touchX, (int) touchY);
                        int r = Color.red(pixel);
                        int g = Color.green(pixel);
                        int b = Color.blue(pixel);
                        colorDialog.colorDisplay.setBackgroundColor(Color.rgb(r, g, b));
                        colorDialog.txtcolor.setText(String.format("FF%06X", (0xFFFFFF & pixel)));
                        setSlidersFromColor(colorDialog, Color.argb(255, Color.red(pixel), Color.green(pixel), Color.blue(pixel)));
                    } catch (Exception ignored) {}
                }
            }
            return true;
        });
    }


    private void showToast(final Object content, final int duration) {
        mainHandler.post(() -> {
            if (currentToast != null) currentToast.cancel();
            if (content instanceof Integer) {
                currentToast = Toast.makeText(this, (Integer) content, duration);
            } else if (content instanceof String) {
                currentToast = Toast.makeText(this, (String) content, duration);
            } else {
                currentToast = Toast.makeText(this, String.valueOf(content), duration);
            }
            currentToast.show();
        });
    }


    void loadTagMemory() {
        try {
            tagDialog = new Dialog(this, R.style.Theme_U1RFID);
            tagDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            tagDialog.setCanceledOnTouchOutside(false);
            TagDialogBinding tdl = TagDialogBinding.inflate(getLayoutInflater());
            View rv = tdl.getRoot();
            tagDialog.setContentView(rv);
            tdl.btncls.setOnClickListener(v -> tagDialog.dismiss());
            tdl.btnread.setOnClickListener(v -> readTagMemory(tdl));
            recyclerView = tdl.recyclerView;
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            layoutManager.scrollToPosition(0);
            recyclerView.setLayoutManager(layoutManager);
            tagItems = new tagItem[0];
            recycleAdapter = new tagAdapter(this, tagItems);
            recyclerView.setAdapter(recycleAdapter);
            tagDialog.show();
            readTagMemory(tdl);
        } catch (Exception ignored) {}
    }


    void readTagMemory(TagDialogBinding tdl) {
        if (currentTag == null) {
            showToast(R.string.no_nfc_tag_found, Toast.LENGTH_SHORT);
            return;
        }
        executorService.execute(() -> {
            NfcA nfcA = NfcA.get(currentTag);
            if (nfcA != null) {
                try {
                    if (!nfcA.isConnected()) nfcA.connect();
                    int maxPages = (tagType == 216) ? 231 : (tagType == 215) ? 135 : 45;
                    if (tagType == 100) maxPages = 48;
                    mainHandler.post(() -> tdl.lbldesc.setText(tagType == 100 ? "UL-C" : "NTAG" + tagType));
                    tagItems = new tagItem[maxPages];
                    for (int i = 0; i < maxPages; i += 4) {
                        byte[] data = nfcA.transceive(new byte[]{0x30, (byte) i});
                        for (int offset = 0; offset < 4; offset++) {
                            int currentPage = i + offset;
                            if (currentPage >= maxPages) break;
                            byte[] pageData = new byte[4];
                            System.arraycopy(data, offset * 4, pageData, 0, 4);
                            String hexString = bytesToHex(pageData, true);
                            String definition = getPageDefinition(currentPage, tagType);
                            tagItems[currentPage] = new tagItem();
                            tagItems[currentPage].tKey = String.format(Locale.getDefault(), "Page %d | %s", currentPage, definition);
                            tagItems[currentPage].tValue = hexString;
                            if (currentPage < 2) {
                                tagItems[currentPage].tImage = AppCompatResources.getDrawable(this, R.drawable.locked);
                            } else if (definition.contains("User Data")) {
                                tagItems[currentPage].tImage = AppCompatResources.getDrawable(this, R.drawable.writable);
                            } else {
                                tagItems[currentPage].tImage = AppCompatResources.getDrawable(this, R.drawable.internal);
                            }
                        }
                    }
                    mainHandler.post(() -> {
                        recycleAdapter = new tagAdapter(this, tagItems);
                        recycleAdapter.setHasStableIds(true);
                        recyclerView.setAdapter(recycleAdapter);
                    });
                } catch (Exception ignored) {
                    showToast(R.string.error_reading_tag, Toast.LENGTH_SHORT);
                } finally {
                    try {
                        if (nfcA.isConnected()) nfcA.close();
                    } catch (Exception ignored) {
                    }
                }
            } else {
                showToast(R.string.invalid_tag_type, Toast.LENGTH_SHORT);
            }
        });
    }


    private void showFirmwareNotice() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        SpannableString titleText = new SpannableString(getString(R.string.extended_firmware_required));
        titleText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.primary_brand)), 0, titleText.length(), 0);
        SpannableString messageText = new SpannableString(String.format("%s\n\n%s\n\n", getString(R.string.extended_firmware_message), getString(R.string.extended_firmware_url)));
        messageText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.text_main)), 0, messageText.length(), 0);

        builder.setTitle(titleText);
        builder.setMessage(messageText);
        builder.setCancelable(false);

        builder.setPositiveButton(R.string.i_understand, (dialog, which) -> {
            SaveSetting(this,"firm_notice",true);
            dialog.dismiss();
        });

        builder.setNeutralButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

        AlertDialog cDialog = builder.create();
        cDialog.show();

        TextView messageView = cDialog.findViewById(android.R.id.message);
        if (messageView != null) {
            Linkify.addLinks(messageView, Linkify.WEB_URLS);
            messageView.setMovementMethod(LinkMovementMethod.getInstance());
            messageView.setLinkTextColor(ContextCompat.getColor(this, R.color.primary_variant));
        }

        if (cDialog.getWindow() != null) {
            cDialog.getWindow().setBackgroundDrawableResource(R.color.background_alt);
            cDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.primary_brand));
            cDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(ContextCompat.getColor(this, R.color.primary_brand));
        }
    }


    void openSpoolAdd() {
        spoolDialog = new Dialog(context, R.style.Theme_U1RFID);
        spoolDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        spoolDialog.setCanceledOnTouchOutside(false);
        SpoolDialogBinding sdl = SpoolDialogBinding.inflate(getLayoutInflater());
        View rv = sdl.getRoot();
        spoolDialog.setContentView(rv);

        sdl.btncls.setOnClickListener(v -> {
            hideKeyboard(v);
            spoolDialog.dismiss();
        });

        sdl.containerVendor.setVisibility(View.VISIBLE);
        sdl.containerFilament.setVisibility(View.GONE);
        sdl.containerSpool.setVisibility(View.GONE);

        sdl.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                hideKeyboard(sdl.tabLayout);
                sdl.containerVendor.setVisibility(View.GONE);
                sdl.containerFilament.setVisibility(View.GONE);
                sdl.containerSpool.setVisibility(View.GONE);
                switch (tab.getPosition()) {
                    case 0:
                        sdl.containerVendor.setVisibility(View.VISIBLE);
                        break;
                    case 1:
                        sdl.containerFilament.setVisibility(View.VISIBLE);
                        break;
                    case 2:
                        sdl.containerSpool.setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        View.OnClickListener dateListener = v -> {
            TextInputEditText target = (TextInputEditText) v;
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Date")
                    .build();
            picker.addOnPositiveButtonClickListener(selection -> {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                target.setText(sdf.format(new Date(selection)));
            });
            picker.show(getSupportFragmentManager(), "DATE_PICKER");
        };

        sdl.sFirstUsed.setOnClickListener(dateListener);
        sdl.sLastUsed.setOnClickListener(dateListener);

        sdl.containerMain.setOnClickListener(v -> hideKeyboard(sdl.containerMain));
        sdl.containerButton.setOnClickListener(v -> hideKeyboard(sdl.containerButton));

        sdl.vComment.setText(String.format("Created by %s", context.getString(R.string.app_name)));
        sdl.fComment.setText(String.format("Created by %s", context.getString(R.string.app_name)));
        sdl.sComment.setText(R.string.rfid_tagged_for_snapmaker_u1);

        Filament localData = matDb.getFilamentById(MaterialID);

        if (localData.filamentParam != null && !localData.filamentParam.isEmpty()) {
            try {
                OpenSpoolFilament osf = new OpenSpoolFilament(localData.filamentParam);
                sdl.fMaterial.setText(osf.getType());
                sdl.fDiameter.setText(String.valueOf(osf.getDiameter()));
                sdl.fTempExtruder.setText(String.valueOf(osf.getMaxTemp()));
                sdl.fTempBed.setText(String.valueOf(osf.getBedMaxTemp()));
                sdl.vName.setText(osf.getBrand());
                sdl.fDensity.setText(GetMaterialDensity(osf.getType()));
            } catch (Exception ignored) {
            }
        }else {
            showToast(getString(R.string.filament_not_found_in_db), Toast.LENGTH_SHORT);
            return;
        }

        sdl.sRemainingWeight.setText(String.format(Locale.getDefault(), "%d", Utils.GetMaterialIntWeight(MaterialWeight)));
        sdl.sInitialWeight.setText(String.format(Locale.getDefault(), "%d", Utils.GetMaterialIntWeight(MaterialWeight)));
        sdl.fColorHex.setText(MaterialColor.substring(2));
        String colorName = matcher.findNearestColor(MaterialColor.substring(2));
        if (colorName == null || colorName.isEmpty())
        {
            colorName = MaterialColor.substring(2);
        }
        // Spoolman's material is the material; the grade belongs in the filament's name.
        // Named as Spoolman's own records are, so a filament created here reads the same
        // as one entered there: "ELEGOO Rapid PLA+ Blue".
        sdl.fName.setText(String.format("%s %s %s", sdl.vName.getText(), subtypeForSpoolman(), colorName).trim());
        String[] directions = new String[] {"coaxial", "longitudinal"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, directions);
        sdl.fMultiColorDirection.setAdapter(adapter);

        // If this tag is already bound to a spool, show what Spoolman knows rather than
        // the defaults, so the figures on screen are the ones about to be written back.
        prefillFromSpoolman(sdl);

        sdl.btnadd.setOnClickListener(v -> {
            hideKeyboard(v);
            String smHost = GetSetting(context, "smhost", "");
            int smPort = GetSetting(context, "smport", 7912);

            if (!smHost.isEmpty()) {
                String baseUrl = "http://" + smHost + ":" + smPort + "/api/v1";

                spoolmanExecutor.execute(() -> {
                    try {
                        String vendorName = Objects.requireNonNull(sdl.vName.getText()).toString().trim();
                        int vendorId = -1;
                        String vRes = performSmRequest(context, baseUrl + "/vendor", "GET", null);

                        if (vRes != null) {
                            JSONArray vArray = new JSONArray(vRes);
                            for (int i = 0; i < vArray.length(); i++) {
                                JSONObject vo = vArray.getJSONObject(i);
                                if (vo.getString("name").equalsIgnoreCase(vendorName)) {
                                    vendorId = vo.getInt("id");
                                    break;
                                }
                            }
                        }

                        if (vendorId == -1) {
                            JSONObject vBody = new JSONObject();
                            vBody.put("name", vendorName);
                            vBody.put("comment", Objects.requireNonNull(sdl.vComment.getText()).toString());
                            vBody.put("empty_spool_weight", getDoubleOrNull(sdl.vEmptySpoolWeight));
                            putExternalId(vBody, sdl.vExternalId.getText());
                            String newV = performSmRequest(context, baseUrl + "/vendor", "POST", vBody.toString());
                            if (newV != null) vendorId = new JSONObject(newV).getInt("id");
                        }

                        String filamentName = Objects.requireNonNull(sdl.fName.getText()).toString().trim();
                        int filamentId = -1;

                        // The Spoolman filament this one resolved to earlier. Confirm it is
                        // still there and go straight to it, rather than listing them all
                        // and matching on name again.
                        Integer known = spoolmanFilamentIds.get(MaterialID);
                        if (known != null) {
                            String cached = performSmRequest(context, baseUrl + "/filament/" + known, "GET", null);
                            if (cached != null) filamentId = new JSONObject(cached).optInt("id", -1);
                        }

                        // Not by name. Spoolman names a filament however whoever entered it
                        // chose to: "ELEGOO Rapid PLA+ Blue" where this app would generate
                        // "Rapid PLA+ (Blue)". Match on what the two actually agree about,
                        // the same way the lookup on the read side does.
                        if (filamentId == -1) {
                            Filament localRow = matDb.getFilamentById(MaterialID);
                            if (localRow != null) {
                                OpenSpoolFilament local = new OpenSpoolFilament(localRow.filamentParam);
                                JSONObject matched = matchSpoolmanFilament(
                                        spoolmanCandidates(baseUrl, vendorName, local.getType(),
                                                local.getSubType()), MaterialColor);
                                if (matched != null) filamentId = matched.optInt("id", -1);
                            }
                        }


                        if (filamentId == -1) {
                            JSONObject fBody = new JSONObject();
                            fBody.put("name", filamentName);
                            fBody.put("vendor_id", vendorId);
                            fBody.put("material", Objects.requireNonNull(sdl.fMaterial.getText()).toString());
                            fBody.put("price", getDoubleOrNull(sdl.fPrice));
                            fBody.put("density", getDoubleOrNull(sdl.fDensity));
                            fBody.put("diameter", getDoubleOrNull(sdl.fDiameter));
                            fBody.put("weight", getDoubleOrNull(sdl.fWeight));
                            fBody.put("spool_weight", getDoubleOrNull(sdl.fSpoolWeight));
                            fBody.put("article_number", Objects.requireNonNull(sdl.fArticleNumber.getText()).toString());
                            fBody.put("comment", Objects.requireNonNull(sdl.fComment.getText()).toString());
                            fBody.put("settings_extruder_temp", getIntOrNull(sdl.fTempExtruder));
                            fBody.put("settings_bed_temp", getIntOrNull(sdl.fTempBed));
                            if (!Objects.requireNonNull(sdl.fMultiColorHexes.getText()).toString().isEmpty()) {
                                fBody.put("multi_color_hexes", sdl.fMultiColorHexes.getText().toString());
                                fBody.put("multi_color_direction", sdl.fMultiColorDirection.getText().toString());
                            }else {
                                fBody.put("color_hex", Objects.requireNonNull(sdl.fColorHex.getText()).toString().replace("#", ""));
                            }
                            putExternalId(fBody, sdl.fExternalId.getText());
                            String newF = performSmRequest(context, baseUrl + "/filament", "POST", fBody.toString());
                            if (newF != null) filamentId = new JSONObject(newF).getInt("id");
                        }

                        if (filamentId != -1) spoolmanFilamentIds.put(MaterialID, filamentId);

                        if (filamentId != -1) {
                            // Spoolman keeps one record per physical spool, so posting blindly
                            // turns every repeat visit into another duplicate. Look first, and
                            // let the user say which spool this is when there is more than one.
                            /*
                             * A tag bound to a spool usually is that spool, but not always:
                             * a tag outlives the roll it was stuck to, and gets moved to a
                             * fresh one when the old is spent. Taking the binding as final
                             * wrote the new roll over the old spool's record, so offer the
                             * choice instead, with the bound spool marked and preselected.
                             */
                            JSONObject bound = boundSpoolForCurrentTag(baseUrl);
                            List<JSONObject> existing = fetchSpools(baseUrl, filamentId);
                            int boundId = bound == null ? 0 : bound.optInt("id");
                            if (bound != null) {
                                boolean listed = false;
                                for (JSONObject sp : existing) {
                                    if (sp.optInt("id") == boundId) { listed = true; break; }
                                }
                                // The tag may have carried over from another filament entirely.
                                if (!listed) existing.add(0, bound);
                            }

                            if (existing.isEmpty()) {
                                submitSpool(baseUrl, sdl, filamentId, null);
                            } else {
                                int resolvedFilamentId = filamentId;
                                String message = getResources().getQuantityString(
                                        R.plurals.spools_already_recorded, existing.size(),
                                        existing.size(), filamentName);
                                mainHandler.post(() -> chooseSpool(existing, boundId, message, chosen ->
                                        spoolmanExecutor.execute(() ->
                                                // Binding takes the tag off whatever else held it.
                                                submitSpool(baseUrl, sdl, resolvedFilamentId, chosen))));
                            }
                        }
                    } catch (Exception e) {
                        showToast(getString(R.string.error_creating_spool), Toast.LENGTH_SHORT);
                    }
                });
            } else {
                showToast(getString(R.string.spoolman_host_is_not_set), Toast.LENGTH_SHORT);
            }
        });
        spoolDialog.show();
    }




    /*
     * Spoolman's external_id names the record a filament or vendor came from in the
     * external catalogue it syncs, and tools match on it. This app does not know that
     * key: it was sending its own filament id instead, and an empty string for a vendor,
     * so every filament created here claimed an identity that resolves to nothing.
     * Send it only when someone has supplied a real one.
     */
    private static void putExternalId(JSONObject body, CharSequence value) throws JSONException {
        String id = value == null ? "" : value.toString().trim();
        if (!id.isEmpty()) body.put("external_id", id);
    }


    private String subtypeForSpoolman() {
        try {
            Filament filament = matDb.getFilamentById(MaterialID);
            if (filament != null) return new OpenSpoolFilament(filament.filamentParam).getSubType();
        } catch (Exception ignored) {}
        return "";
    }


    private String spoolmanBaseUrl() {
        String smHost = GetSetting(context, "smhost", "");
        if (smHost.isEmpty() || !GetSetting(context, "enablesm", false)) return "";
        return "http://" + smHost + ":" + GetSetting(context, "smport", 7912) + "/api/v1";
    }

    /*
     * Asks Spoolman about the tag as it is read, rather than waiting for a dialog to be
     * opened. The answer is kept against the UID it was fetched for, so presenting a tag
     * costs one request and everything after it costs none, until a different tag turns
     * up or a write makes what we hold stale.
     */
    void resolveSpoolForTag() {
        String uid = currentCardUid();
        String baseUrl = spoolmanBaseUrl();
        if (uid.isEmpty() || baseUrl.isEmpty()) {
            cachedSpoolUid = "";
            cachedSpool = null;
            return;
        }
        if (uid.equals(cachedSpoolUid)) return;
        cachedSpoolUid = uid;
        cachedSpool = null;
        spoolmanExecutor.execute(() -> {
            JSONObject spool = findSpoolByCardUid(baseUrl, uid);
            if (!uid.equals(cachedSpoolUid)) return;   // a different tag arrived meanwhile
            cachedSpool = spool;
            mainHandler.post(this::updateTagLine);
            if (spool != null) {
                showToast(getString(R.string.loaded_spool_from_spoolman, spool.optInt("id")), Toast.LENGTH_SHORT);
            }
        });
    }

    /*
     * The tag line says what the tag is and, once Spoolman has been asked, which spool it
     * belongs to. A tag with no spool beside it is one that has not been linked yet - a
     * state that was only ever announced in a toast, and so was invisible a second later.
     */
    void updateTagLine() {
        if (currentTag == null) return;
        String uid = currentCardUid();
        String text = bytesToHex(currentTag.getId(), true);
        JSONObject spool = cachedSpool;
        if (spool != null && uid.equals(cachedSpoolUid)) {
            text += "   \u2192   " + getString(R.string.spool_ref, spool.optInt("id"));
        }
        main.tagid.setText(text);
    }

    /*
     * (1) A tag that has just been written belongs to a spool, so put it on one here
     * rather than leave it to be remembered as a separate errand through the Spoolman
     * button. The filament on screen is the filament just written, so the only open
     * question is which spool, and that is only a question when the filament has more
     * than one and the tag is not already on any of them.
     */
    void linkTagAfterWrite() {
        String baseUrl = spoolmanBaseUrl();
        String uid = currentCardUid();
        if (baseUrl.isEmpty() || uid.isEmpty()) return;
        spoolmanExecutor.execute(() -> {
            try {
                JSONObject filament = cachedSmFilament;
                if (filament == null) {
                    // Nothing resolved yet, so resolve it the same way the picker does.
                    Filament row = matDb.getFilamentById(MaterialID);
                    if (row == null) return;
                    OpenSpoolFilament osf = new OpenSpoolFilament(row.filamentParam);
                    filament = matchSpoolmanFilament(spoolmanCandidates(baseUrl, osf.getBrand(),
                            osf.getType(), osf.getSubType()), MaterialColor);
                }
                if (filament == null) return;   // Spoolman has no such filament; nothing to join

                // Already on a spool: bind again so a tag moved here leaves the old one.
                JSONObject bound = boundSpoolForCurrentTag(baseUrl);
                if (bound != null) {
                    linkAndRefresh(baseUrl, bound.optInt("id"), uid);
                    return;
                }
                List<JSONObject> spools = fetchSpools(baseUrl, filament.optInt("id"));
                if (spools.isEmpty()) return;   // no spool on record; the add form makes one
                if (spools.size() == 1) {
                    linkAndRefresh(baseUrl, spools.get(0).optInt("id"), uid);
                    return;
                }
                String message = getResources().getQuantityString(R.plurals.assign_tag_to_spool,
                        spools.size(), spools.size(), filament.optString("name"));
                mainHandler.post(() -> chooseSpool(spools, 0, message, chosen -> {
                    if (chosen == null) {
                        openSpoolAdd();
                        return;
                    }
                    spoolmanExecutor.execute(() -> linkAndRefresh(baseUrl, chosen, uid));
                }));
            } catch (Exception ignored) {}
        });
    }

    // Binds, then asks Spoolman again so the tag line shows the spool it now belongs to.
    private void linkAndRefresh(String baseUrl, int spoolId, String uid) {
        bindCardUid(baseUrl, spoolId, uid);
        cachedSpoolUid = "";
        cachedSpool = null;
        resolveSpoolForTag();
    }

    /*
     * Finds the Spoolman filament matching what is selected, and the spool of it if there
     * is exactly one. Without a tag there is no UID to go on, so match the way a person
     * would: same vendor, the same material allowing for the grade, the grade itself read
     * out of Spoolman's name, and the same colour.
     */
    // Every Spoolman filament that is this vendor, material and grade. Colour is what
    // tells them apart and it is not decided here.
    private List<JSONObject> spoolmanCandidates(String baseUrl, String brand, String type, String subtype) {
        List<JSONObject> out = new ArrayList<>();
        try {
            for (JSONObject f : fetchPaged(baseUrl + "/filament")) {
                JSONObject vendor = f.optJSONObject("vendor");
                String theirBrand = vendor != null ? vendor.optString("name", "") : "";
                if (!theirBrand.trim().equalsIgnoreCase(brand.trim())) continue;
                if (!FilamentRegistry.sameMaterial(type, f.optString("material", ""))) continue;
                String theirSubtype = FilamentRegistry.subtypeFromName(type, f.optString("name", ""));
                boolean subtypeAgrees = theirSubtype.isEmpty()
                        ? subtype.equalsIgnoreCase("Basic") : theirSubtype.equalsIgnoreCase(subtype);
                if (subtypeAgrees) out.add(f);
            }
        } catch (Exception ignored) {}
        return out;
    }

    /*
     * Picks between the candidates on colour. An exact colour is the one meant; anything
     * else is the closest of them, which is a guess and is marked as one. No distance
     * measure rescues a colour that simply is not among the products, so what the guess
     * is allowed to change is limited, and the picker exists to overrule it.
     */
    private JSONObject matchSpoolmanFilament(List<JSONObject> candidates, String colourHex) {
        try {
            JSONObject nearest = null;
            double nearestDistance = Double.MAX_VALUE;
            JSONObject colourless = null;
            smMatchCertain = false;

            for (JSONObject f : candidates) {

                // Colour ranks the candidates, it does not rule them out. Spoolman carries a
                // product's own colour, 2240AF for Elegoo's blue, where this app carries
                // whatever was picked here. Insisting the two be equal matched nothing at
                // all, so take the closest instead, and an exact one straight away.
                String theirColour = f.optString("color_hex", "").replace("#", "");
                String theirRgb = spoolmanRgb(theirColour);
                if (theirRgb.isEmpty() || colourHex.length() < 8) {
                    if (colourless == null) colourless = f;
                    continue;
                }
                String ourAlpha = colourHex.substring(0, 2), ourRgb = colourHex.substring(2, 8);
                String theirAlpha = spoolmanAlpha(theirColour, "FF");
                if (theirRgb.equalsIgnoreCase(ourRgb) && theirAlpha.equalsIgnoreCase(ourAlpha)) {
                    smMatchCertain = true;
                    return f;
                }
                double distance = colourDistance(ourRgb, ourAlpha, theirRgb, theirAlpha);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = f;
                }
            }
            // The only candidate there is, is the one meant, whatever its colour.
            if (candidates.size() == 1) smMatchCertain = true;
            return nearest != null ? nearest : colourless;
        } catch (Exception ignored) {}
        return null;
    }

    /*
     * How far apart two colours are. Transparency is not a colour, so it is measured
     * separately and counts for much less: two shades of blue differ in a way that
     * matters more than one of them being translucent. A whole alpha's difference is
     * worth about a sixth of the furthest two colours can be apart, enough to separate
     * a translucent from a solid of the same shade and not enough to outrank the shade.
     */
    private static final double ALPHA_WEIGHT = 0.25;

    private static double colourDistance(String aRgb, String aAlpha, String bRgb, String bAlpha) {
        try {
            int x = Integer.parseInt(aRgb, 16);
            int y = Integer.parseInt(bRgb, 16);
            int dr = ((x >> 16) & 0xFF) - ((y >> 16) & 0xFF);
            int dg = ((x >> 8) & 0xFF) - ((y >> 8) & 0xFF);
            int db = (x & 0xFF) - (y & 0xFF);
            double rgb = Math.sqrt(dr * dr + dg * dg + db * db);
            int da = Integer.parseInt(aAlpha, 16) - Integer.parseInt(bAlpha, 16);
            return rgb + ALPHA_WEIGHT * Math.abs(da);
        } catch (Exception ignored) {
            return Double.MAX_VALUE;
        }
    }

    // Spoolman writes a colour as RRGGBB or RRGGBBAA; this app carries AARRGGBB.
    private static String spoolmanRgb(String colourHex) {
        return colourHex.length() >= 6 ? colourHex.substring(0, 6) : "";
    }

    private static String spoolmanAlpha(String colourHex, String fallback) {
        return colourHex.length() >= 8 ? colourHex.substring(6, 8) : fallback;
    }

    // The colour to write: Spoolman's, when one of its filaments matched. It holds the
    // product's own shade, 2240AF for Elegoo's blue, where this app holds what was picked.
    String effectiveColour() {
        JSONObject f = cachedSmFilament;
        if (f == null || MaterialColor.length() < 8) return MaterialColor;
        String theirs = f.optString("color_hex", "").replace("#", "");
        String rgb = spoolmanRgb(theirs);
        if (rgb.isEmpty()) return MaterialColor;
        return spoolmanAlpha(theirs, MaterialColor.substring(0, 2)) + rgb;
    }

    /*
     * Lets the filament be chosen outright. Colour is what separates a vendor's products
     * from one another, and a colour picked here need not be near any of them, so the
     * closest is sometimes not the one wanted and no measure fixes that. The choice is
     * remembered against this filament and survives until a different one is selected.
     */
    /*
     * The three things there are to do with Spoolman, named. The button used to go
     * straight to matching a filament, which is the roundabout way to reach a spool that
     * Spoolman already holds and the only way that was offered.
     */
    void showSpoolmanMenu() {
        try {
            String[] items = {
                    getString(R.string.sm_pick_spool),
                    getString(R.string.sm_pick_filament),
                    getString(R.string.sm_new_spool),
            };
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            SpannableString titleText = new SpannableString(getString(R.string.spoolman));
            titleText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.primary_brand)), 0, titleText.length(), 0);
            builder.setTitle(titleText);
            builder.setItems(items, (dialog, which) -> {
                if (which == 0) chooseSpoolmanSpool();
                else if (which == 1) chooseSpoolmanFilament();
                else openSpoolAdd();
            });
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
            AlertDialog alert = builder.create();
            alert.show();
            if (alert.getWindow() != null) {
                alert.getWindow().setBackgroundDrawableResource(R.color.background_alt);
                alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(context, R.color.primary_brand));
            }
        } catch (Exception ignored) {}
    }

    /*
     * Spool first. Pick the spool out of Spoolman and let it say what the filament is,
     * rather than describe the filament here and hope the match lands on the right one of
     * several. For a spool Spoolman already holds this is both the shorter way round and
     * the certain one: the spool names its own filament, so nothing is guessed, and what
     * gets written afterwards is bound to that very spool.
     */
    void chooseSpoolmanSpool() {
        String baseUrl = spoolmanBaseUrl();
        if (baseUrl.isEmpty()) {
            showToast(getString(R.string.spoolman_host_is_not_set), Toast.LENGTH_SHORT);
            return;
        }
        showToast(getString(R.string.loading_spools), Toast.LENGTH_SHORT);
        spoolmanExecutor.execute(() -> {
            List<JSONObject> spools = fetchAllSpools(baseUrl);
            Collections.sort(spools, (a, b) -> {
                // Archived last; they are still pickable, just not what is usually wanted.
                int archived = Boolean.compare(a.optBoolean("archived"), b.optBoolean("archived"));
                if (archived != 0) return archived;
                int name = spoolTitle(a).compareToIgnoreCase(spoolTitle(b));
                return name != 0 ? name : Integer.compare(a.optInt("id"), b.optInt("id"));
            });
            mainHandler.post(() -> showSpoolList(spools));
        });
    }

    // "ELEGOO  Rapid PETG Red" - what a person scanning the list reads first.
    private static String spoolTitle(JSONObject spool) {
        JSONObject filament = spool.optJSONObject("filament");
        if (filament == null) return "";
        JSONObject vendor = filament.optJSONObject("vendor");
        String brand = vendor == null ? "" : vendor.optString("name", "");
        return (brand + "  " + filament.optString("name", "")).trim();
    }

    private void showSpoolList(List<JSONObject> all) {
        try {
            if (all.isEmpty()) {
                showToast(getString(R.string.no_spoolman_spools), Toast.LENGTH_SHORT);
                return;
            }
            List<JSONObject> shown = new ArrayList<>(all);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                    android.R.layout.simple_list_item_1, new ArrayList<>());
            int pad = Math.round(16 * getResources().getDisplayMetrics().density);

            EditText search = new EditText(context);
            search.setHint(R.string.search_spools);
            search.setSingleLine(true);
            search.setInputType(InputType.TYPE_CLASS_TEXT);
            search.setTextColor(ContextCompat.getColor(context, R.color.text_main));

            ListView list = new ListView(context);
            list.setAdapter(adapter);

            LinearLayout box = new LinearLayout(context);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(pad, pad / 2, pad, 0);
            box.addView(search);
            box.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    Math.round(getResources().getDisplayMetrics().heightPixels * 0.55f)));

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            SpannableString titleText = new SpannableString(getString(R.string.sm_pick_spool));
            titleText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.primary_brand)), 0, titleText.length(), 0);
            builder.setTitle(titleText);
            builder.setView(box);
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
            AlertDialog alert = builder.create();

            // Every word typed has to be in the row somewhere, in any order, so "elegoo
            // red" and "red elegoo" both find the same spool.
            Runnable refill = () -> {
                String query = search.getText() == null ? "" : search.getText().toString().trim();
                shown.clear();
                List<String> labels = new ArrayList<>();
                for (JSONObject spool : all) {
                    String row = spoolTitle(spool) + "   " + describeSpool(spool);
                    boolean hit = true;
                    for (String word : query.toLowerCase(Locale.getDefault()).split("\\s+")) {
                        if (!word.isEmpty() && !row.toLowerCase(Locale.getDefault()).contains(word)) {
                            hit = false;
                            break;
                        }
                    }
                    if (hit) {
                        shown.add(spool);
                        labels.add(row);
                    }
                }
                adapter.clear();
                adapter.addAll(labels);
                adapter.notifyDataSetChanged();
            };
            refill.run();

            search.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence c, int a, int b, int d) {}
                @Override public void onTextChanged(CharSequence c, int a, int b, int d) {}
                @Override public void afterTextChanged(Editable e) { refill.run(); }
            });
            list.setOnItemClickListener((parent, view, position, id) -> {
                if (position < shown.size()) {
                    JSONObject spool = shown.get(position);
                    alert.dismiss();
                    applySpoolmanSpool(spool);
                }
            });

            alert.show();
            if (alert.getWindow() != null) {
                alert.getWindow().setBackgroundDrawableResource(R.color.background_alt);
                alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(context, R.color.primary_brand));
            }
        } catch (Exception ignored) {}
    }

    /*
     * Takes the chosen spool as settled. Its filament is pinned, so none of the matching
     * that runs on a selection may overrule it, and the catalogue row that filament
     * corresponds to drives the spinners - from which the existing machinery applies
     * Spoolman's own colour and temperatures.
     */
    private void applySpoolmanSpool(JSONObject spool) {
        try {
            JSONObject filament = spool.optJSONObject("filament");
            if (filament == null) return;
            JSONObject vendor = filament.optJSONObject("vendor");
            String brand = vendor == null ? "" : vendor.optString("name", "");
            String material = filament.optString("material", "");
            String subtype = FilamentRegistry.subtypeFromName(material, filament.optString("name", ""));
            if (subtype.isEmpty()) subtype = "Basic";

            Filament local = findLocalFilament(brand, material, subtype);
            if (local == null) {
                // Writing it would mean inventing a catalogue entry for it, and a tag
                // written off an invented entry is not one the printer knows either.
                showToast(getString(R.string.no_local_filament_for,
                        (brand + " " + material + " " + subtype).trim()), Toast.LENGTH_LONG);
                return;
            }

            OpenSpoolFilament osf = new OpenSpoolFilament(local.filamentParam);
            MaterialID = local.filamentID;
            pinnedForMaterialId = MaterialID;
            pinnedSmFilamentId = filament.optInt("id");
            spoolmanFilamentIds.put(MaterialID, filament.optInt("id"));
            cachedFilamentKey = "";
            cachedSmFilament = filament;
            cachedSmSpool = spool;

            setSpinnerSelection(main.brand, osf.getBrand());
            main.brand.postDelayed(() -> {
                setSpinnerSelection(main.type, osf.getType());
                main.type.postDelayed(() -> setSpinnerSelection(main.subtype, osf.getSubType()), 200);
            }, 200);

            int grams = (int) Math.round(filament.optDouble("weight", 0));
            if (grams > 0) setSpinnerSelection(main.spoolsize, GetMaterialWeightByInt(grams));

            showToast(getString(R.string.spool_selected, spool.optInt("id")), Toast.LENGTH_SHORT);
        } catch (Exception ignored) {}
    }

    /*
     * The catalogue row a Spoolman filament corresponds to. Same brand and material with
     * the same grade is the one; the same brand and material with another grade will do,
     * since the grade is the part Spoolman keeps in a name and names vary. A row of some
     * other brand is not offered: writing that would put the wrong maker on the tag.
     */
    private Filament findLocalFilament(String brand, String material, String subtype) {
        Filament sameBrand = null;
        try {
            for (Filament f : matDb.getAllItems()) {
                JSONObject json = new JSONObject(f.filamentParam);
                if (!json.optString("brand").trim().equalsIgnoreCase(brand.trim())) continue;
                if (!FilamentRegistry.sameMaterial(json.optString("type"), material)) continue;
                if (json.optString("subtype", "Basic").equalsIgnoreCase(subtype)) return f;
                if (sameBrand == null) sameBrand = f;
            }
        } catch (Exception ignored) {}
        return sameBrand;
    }

    void chooseSpoolmanFilament() {
        String baseUrl = spoolmanBaseUrl();
        if (baseUrl.isEmpty() || MaterialID == null) {
            showToast(getString(R.string.spoolman_host_is_not_set), Toast.LENGTH_SHORT);
            return;
        }
        Filament row = matDb.getFilamentById(MaterialID);
        if (row == null) return;
        spoolmanExecutor.execute(() -> {
            try {
                OpenSpoolFilament osf = new OpenSpoolFilament(row.filamentParam);
                List<JSONObject> candidates = spoolmanCandidates(baseUrl, osf.getBrand(),
                        osf.getType(), osf.getSubType());
                mainHandler.post(() -> showSpoolmanFilamentPicker(candidates));
            } catch (Exception ignored) {}
        });
    }

    private void showSpoolmanFilamentPicker(List<JSONObject> candidates) {
        try {
            if (candidates.isEmpty()) {
                showToast(getString(R.string.no_spoolman_filaments), Toast.LENGTH_SHORT);
                openSpoolAdd();
                return;
            }
            String[] items = new String[candidates.size() + 1];
            items[0] = getString(R.string.spoolman_closest_colour);
            int checked = 0;
            for (int i = 0; i < candidates.size(); i++) {
                JSONObject f = candidates.get(i);
                String colour = f.optString("color_hex", "").replace("#", "");
                items[i + 1] = f.optString("name") + (colour.isEmpty() ? "" : "   #" + colour);
                if (f.optInt("id") == pinnedSmFilamentId) checked = i + 1;
            }
            final int[] choice = {checked};

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            SpannableString titleText = new SpannableString(getString(R.string.spoolman_filament));
            titleText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.primary_brand)), 0, titleText.length(), 0);
            builder.setTitle(titleText);
            builder.setSingleChoiceItems(items, checked, (dialog, which) -> choice[0] = which);
            builder.setPositiveButton(R.string.select, (dialog, which) -> {
                applyFilamentChoice(candidates, choice[0]);
                assignTagToSpool(candidates, choice[0]);
            });
            builder.setNeutralButton(R.string.add_spool_to_spoolman, (dialog, which) -> {
                applyFilamentChoice(candidates, choice[0]);
                openSpoolAdd();
            });
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
            AlertDialog alert = builder.create();
            alert.show();
            if (alert.getWindow() != null) {
                alert.getWindow().setBackgroundDrawableResource(R.color.background_alt);
                for (int b : new int[]{AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEUTRAL, AlertDialog.BUTTON_NEGATIVE}) {
                    if (alert.getButton(b) != null) {
                        alert.getButton(b).setTextColor(ContextCompat.getColor(context, R.color.primary_brand));
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    /*
     * Binds the tag in hand to one of that filament's spools and does nothing else. A U1
     * spool carries two tags, so this is done twice for every spool, and the only way to
     * do it before was to fill the whole add-a-spool form in again - laborious enough to
     * be skipped, which leaves the spool holding one of its two tags, and a chance to
     * write the form's figures over the spool's own each time it is not.
     */
    private void assignTagToSpool(List<JSONObject> candidates, int which) {
        String baseUrl = spoolmanBaseUrl();
        String uid = currentCardUid();
        if (baseUrl.isEmpty() || uid.isEmpty()) return;
        // Queued behind the lookup applyFilamentChoice just started, on the one Spoolman
        // thread, so the automatic choice has resolved by the time this reads it.
        spoolmanExecutor.execute(() -> {
            try {
                JSONObject filament = which == 0 ? cachedSmFilament : candidates.get(which - 1);
                if (filament == null) return;
                int filamentId = filament.optInt("id");
                JSONObject bound = boundSpoolForCurrentTag(baseUrl);
                List<JSONObject> spools = fetchSpools(baseUrl, filamentId);
                if (bound != null) {
                    boolean listed = false;
                    for (JSONObject sp : spools) {
                        if (sp.optInt("id") == bound.optInt("id")) { listed = true; break; }
                    }
                    if (!listed) spools.add(0, bound);
                }
                if (spools.isEmpty()) return;   // nothing to assign to; the add form makes one
                int boundId = bound == null ? 0 : bound.optInt("id");
                String message = getResources().getQuantityString(R.plurals.assign_tag_to_spool,
                        spools.size(), spools.size(), filament.optString("name"));
                mainHandler.post(() -> chooseSpool(spools, boundId, message, chosen -> {
                    if (chosen == null) {
                        openSpoolAdd();
                        return;
                    }
                    spoolmanExecutor.execute(() -> {
                        bindCardUid(baseUrl, chosen, uid);
                        cachedSpoolUid = "";
                        cachedSpool = null;
                        showToast(getString(R.string.tag_assigned_to_spool, chosen), Toast.LENGTH_SHORT);
                    });
                }));
            } catch (Exception ignored) {}
        });
    }

    private void applyFilamentChoice(List<JSONObject> candidates, int which) {
        pinnedForMaterialId = MaterialID;
        pinnedSmFilamentId = which == 0 ? 0 : candidates.get(which - 1).optInt("id");
        cachedFilamentKey = "";
        resolveSpoolmanForSelection();
    }


    void resolveSpoolmanForSelection() {
        String baseUrl = spoolmanBaseUrl();
        String key = MaterialID + "|" + MaterialColor;
        if (baseUrl.isEmpty() || MaterialID == null) {
            cachedFilamentKey = "";
            cachedSmFilament = null;
            cachedSmSpool = null;
            mainHandler.post(this::updateSpoolmanIndicators);
            return;
        }
        if (key.equals(cachedFilamentKey)) return;
        if (!MaterialID.equals(pinnedForMaterialId)) pinnedSmFilamentId = 0;
        cachedFilamentKey = key;
        cachedSmCandidates = new ArrayList<>();
        cachedSmFilament = null;
        cachedSmSpool = null;
        Filament row = matDb.getFilamentById(MaterialID);
        if (row == null) return;
        spoolmanExecutor.execute(() -> {
            try {
                OpenSpoolFilament osf = new OpenSpoolFilament(row.filamentParam);
                List<JSONObject> candidates = spoolmanCandidates(baseUrl, osf.getBrand(),
                        osf.getType(), osf.getSubType());
                if (!key.equals(cachedFilamentKey)) return;
                cachedSmCandidates = candidates;

                JSONObject f = null;
                if (pinnedSmFilamentId != 0) {
                    for (JSONObject c : candidates) {
                        if (c.optInt("id") == pinnedSmFilamentId) { f = c; break; }
                    }
                    smMatchCertain = f != null;
                }
                if (f == null) f = matchSpoolmanFilament(candidates, MaterialColor);
                if (!key.equals(cachedFilamentKey)) return;
                cachedSmFilament = f;
                if (f != null) {
                    spoolmanFilamentIds.put(MaterialID, f.optInt("id"));
                    List<JSONObject> spools = fetchSpools(baseUrl, f.optInt("id"));
                    if (!key.equals(cachedFilamentKey)) return;
                    if (spools.size() == 1) cachedSmSpool = spools.get(0);
                }
            } catch (Exception ignored) {}
            mainHandler.post(this::updateSpoolmanIndicators);
        });
    }

    /*
     * Two small marks, no more: the Spoolman button goes from dim to solid once something
     * of ours is known there, and a spool glyph sits beside the temperatures that came
     * from Spoolman rather than from the filament as defined here.
     */
    void updateSpoolmanIndicators() {
        try {
            boolean known = cachedSmFilament != null || cachedSpool != null;
            main.smbutton.setAlpha(known ? 1.0f : 0.35f);
            int glyph = cachedSmFilament != null ? R.drawable.twotone_spool_24 : 0;
            main.extMin.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, glyph, 0);
            main.bedMin.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, glyph, 0);
            if (cachedSmFilament != null) {
                applySpoolmanTemps();
                // Only when we know which filament this is. Overwriting a chosen colour
                // with a guessed one leaves no way back to the colour that was wanted.
                if (smMatchCertain) applySpoolmanColour();
            }
        } catch (Exception ignored) {}
    }

    // Spoolman holds the product's own shade, so show it rather than the one picked here.
    // The key moves with it, so settling on it does not look like a fresh selection.
    private void applySpoolmanColour() {
        String colour = effectiveColour();
        if (colour.equals(MaterialColor)) return;
        MaterialColor = colour;
        cachedFilamentKey = MaterialID + "|" + MaterialColor;
        int colorInt = Color.parseColor("#" + MaterialColor);
        main.colorview.setBackgroundColor(colorInt);
        main.txtcolor.setText(MaterialColor);
        main.txtcolor.setTextColor(getContrastColor(colorInt));
    }

    // Spoolman states one extruder and one bed temperature where this app carries a range.
    // Where it has them they are the ones to print at, so show them and write them.
    private void applySpoolmanTemps() {
        int ext = cachedSmFilament.optInt("settings_extruder_temp", 0);
        int bed = cachedSmFilament.optInt("settings_bed_temp", 0);
        if (ext > 0) {
            main.extMin.setText(String.format(Locale.getDefault(), "%d\u00B0C", ext));
            main.extMax.setText(String.format(Locale.getDefault(), "%d\u00B0C", ext));
        }
        if (bed > 0) {
            main.bedMin.setText(String.format(Locale.getDefault(), "%d\u00B0C", bed));
            main.bedMax.setText(String.format(Locale.getDefault(), "%d\u00B0C", bed));
        }
    }

    // Overlays what Spoolman holds onto the filament about to be written.
    void applySpoolmanFilament(OpenSpoolFilament osf) {
        JSONObject f = cachedSmFilament;
        if (f == null) return;
        try {
            int ext = f.optInt("settings_extruder_temp", 0);
            int bed = f.optInt("settings_bed_temp", 0);
            if (ext > 0 || bed > 0) {
                osf.setTemps(ext > 0 ? ext : osf.getMinTemp(), ext > 0 ? ext : osf.getMaxTemp(),
                        bed > 0 ? bed : osf.getBedMinTemp(), bed > 0 ? bed : osf.getBedMaxTemp());
            }
        } catch (Exception ignored) {}
    }

    /*
     * The spool this tag is actually recorded against, or none. Kept apart from the
     * spool a filament merely happens to have only one of: that one is a guess, and a
     * guess marked as "this tag" in the picker offers to move a tag that was never
     * there off a spool that never held it.
     */
    private JSONObject boundSpoolForCurrentTag(String baseUrl) {
        String uid = currentCardUid();
        if (uid.isEmpty()) return null;
        if (uid.equals(cachedSpoolUid) && cachedSpool != null) return cachedSpool;
        JSONObject spool = findSpoolByCardUid(baseUrl, uid);
        cachedSpoolUid = uid;
        cachedSpool = spool;
        return spool;
    }

    // For filling the dialog in, where the single spool of a matched filament is a
    // good enough source of figures to show.
    private JSONObject spoolForCurrentTag(String baseUrl) {
        JSONObject bound = boundSpoolForCurrentTag(baseUrl);
        return bound != null ? bound : cachedSmSpool;
    }

    private void prefillFromSpoolman(SpoolDialogBinding sdl) {
        String baseUrl = spoolmanBaseUrl();
        if (baseUrl.isEmpty()) return;
        spoolmanExecutor.execute(() -> {
            JSONObject spool = spoolForCurrentTag(baseUrl);
            if (spool == null) return;
            mainHandler.post(() -> {
                try {
                    if (!spool.isNull("remaining_weight")) {
                        sdl.sRemainingWeight.setText(String.format(Locale.getDefault(), "%d",
                                (int) spool.optDouble("remaining_weight")));
                    }
                    if (!spool.isNull("initial_weight")) {
                        sdl.sInitialWeight.setText(String.format(Locale.getDefault(), "%d",
                                (int) spool.optDouble("initial_weight")));
                    }
                    if (!spool.isNull("used_weight")) {
                        sdl.sUsedWeight.setText(String.format(Locale.getDefault(), "%d",
                                (int) spool.optDouble("used_weight")));
                    }
                    if (!spool.optString("location").isEmpty()) sdl.sLocation.setText(spool.optString("location"));
                    if (!spool.optString("lot_nr").isEmpty()) sdl.sLotNr.setText(spool.optString("lot_nr"));
                    if (!spool.optString("comment").isEmpty()) sdl.sComment.setText(spool.optString("comment"));
                    if (!spool.isNull("price")) {
                        sdl.sPrice.setText(String.format(Locale.getDefault(), "%.2f", spool.optDouble("price")));
                    }
                    if (!spool.optString("first_used").isEmpty()) {
                        sdl.sFirstUsed.setText(spool.optString("first_used").substring(0, 10));
                    }
                    if (!spool.optString("last_used").isEmpty()) {
                        sdl.sLastUsed.setText(spool.optString("last_used").substring(0, 10));
                    }
                    sdl.sArchived.setChecked(spool.optBoolean("archived"));
                } catch (Exception ignored) {}
            });
        });
    }


    // The scanned tag's UID as uppercase hex, the form SpoolLink and the printer
    // firmware both use. Empty when nothing has been scanned.
    private String currentCardUid() {
        return currentTag == null ? "" : bytesToHex(currentTag.getId(), false);
    }

    /*
     * Spoolman carries the tag binding in a custom field on the spool, "card_uids": a
     * JSON encoded string holding comma separated uppercase UIDs. SpoolLink and the
     * extended firmware's spoollink component both read and write it in that exact
     * shape, so writing it here links a tag for all three rather than only for us.
     * There is no way to hand the work to SpoolLink itself; it exports nothing an app
     * can call. Sharing the field is the interoperability, and it works whether or not
     * SpoolLink is installed.
     */
    private static List<String> parseCardUids(JSONObject spool) {
        List<String> uids = new ArrayList<>();
        JSONObject extra = spool == null ? null : spool.optJSONObject("extra");
        if (extra == null) return uids;
        String raw = extra.optString("card_uids", "").trim();
        // The value is JSON encoded. Ours is a quoted comma separated string, but a
        // JSON array is just as valid a thing to find in there, so read either rather
        // than mistake one for empty and write over the UIDs it holds.
        try {
            Object decoded = new JSONTokener(raw).nextValue();
            if (decoded instanceof JSONArray) {
                JSONArray arr = (JSONArray) decoded;
                for (int i = 0; i < arr.length(); i++) addCardUid(uids, arr.optString(i));
                return uids;
            }
            if (decoded instanceof String) raw = (String) decoded;
        } catch (Exception ignored) {}
        for (String uid : raw.split("[,;\\s]+")) addCardUid(uids, uid);
        return uids;
    }

    private static void addCardUid(List<String> uids, String uid) {
        String trimmed = uid == null ? "" : uid.trim().toUpperCase(java.util.Locale.ROOT);
        if (!trimmed.isEmpty() && !uids.contains(trimmed)) uids.add(trimmed);
    }

    /*
     * Spoolman answers a list a page at a time, and a request naming no page gets the
     * default one. Reading only that silently loses everything past it - invisible on a
     * small instance, wrong on a large one. Ask for a generous page and keep asking until
     * a short page says it was the last.
     *
     * Archived spools are asked for throughout: a spent spool is exactly where a recycled
     * tag was last recorded, and it is still a spool worth picking.
     */
    private static final int SM_PAGE = 500;

    private List<JSONObject> fetchPaged(String url) {
        List<JSONObject> out = new ArrayList<>();
        try {
            String join = url.contains("?") ? "&" : "?";
            for (int offset = 0; ; offset += SM_PAGE) {
                String res = performSmRequest(context,
                        url + join + "limit=" + SM_PAGE + "&offset=" + offset, "GET", null);
                if (res == null) break;
                JSONArray array = new JSONArray(res);
                for (int i = 0; i < array.length(); i++) out.add(array.getJSONObject(i));
                if (array.length() < SM_PAGE) break;
            }
        } catch (Exception ignored) {}
        return out;
    }

    private List<JSONObject> fetchAllSpools(String baseUrl) {
        return fetchPaged(baseUrl + "/spool?allow_archived=true");
    }

    // Every spool carrying this UID. More than one is the duplication to clear up.
    private List<JSONObject> findSpoolsByCardUid(String baseUrl, String cardUid) {
        List<JSONObject> found = new ArrayList<>();
        if (cardUid.isEmpty()) return found;
        String upper = cardUid.toUpperCase(java.util.Locale.ROOT);
        for (JSONObject spool : fetchAllSpools(baseUrl)) {
            if (parseCardUids(spool).contains(upper)) found.add(spool);
        }
        return found;
    }

    private JSONObject findSpoolByCardUid(String baseUrl, String cardUid) {
        List<JSONObject> found = findSpoolsByCardUid(baseUrl, cardUid);
        return found.isEmpty() ? null : found.get(0);
    }

    // Spoolman rejects a custom field it does not know about, so declare it first.
    private void ensureCardUidField(String baseUrl) {
        try {
            String res = performSmRequest(context, baseUrl + "/field/spool", "GET", null);
            if (res != null) {
                JSONArray fields = new JSONArray(res);
                for (int i = 0; i < fields.length(); i++) {
                    if ("card_uids".equals(fields.getJSONObject(i).optString("key"))) return;
                }
            }
            JSONObject body = new JSONObject();
            body.put("name", "Card UIDs");
            body.put("field_type", "text");
            performSmRequest(context, baseUrl + "/field/spool/card_uids", "POST", body.toString());
        } catch (Exception ignored) {}
    }

    private boolean writeCardUids(String baseUrl, int spoolId, List<String> uids) {
        try {
            ensureCardUidField(baseUrl);
            JSONObject extra = new JSONObject();
            extra.put("card_uids", JSONObject.quote(String.join(",", uids)));
            JSONObject body = new JSONObject();
            body.put("extra", extra);
            return performSmRequest(context, baseUrl + "/spool/" + spoolId,
                    "PATCH", body.toString()) != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    /*
     * Reads the spool back before its tags are changed. The copy in hand came either
     * from the list fetched before the dialog opened or from the reply to the update
     * just made, and neither is a promise about what "card_uids" holds now. Writing
     * the field out of a stale copy is what puts one tag where two belong.
     */
    private JSONObject reloadSpool(String baseUrl, int spoolId) {
        try {
            String res = performSmRequest(context, baseUrl + "/spool/" + spoolId, "GET", null);
            if (res != null) return new JSONObject(res);
        } catch (Exception ignored) {}
        return null;
    }

    // Takes this tag off a spool, for when it has been moved to another.
    private void unbindCardUid(String baseUrl, int spoolId, String cardUid) {
        if (spoolId <= 0 || cardUid.isEmpty()) return;
        JSONObject spool = reloadSpool(baseUrl, spoolId);
        if (spool == null) return;
        List<String> uids = parseCardUids(spool);
        if (!uids.remove(cardUid.toUpperCase(java.util.Locale.ROOT))) return;
        writeCardUids(baseUrl, spoolId, uids);
    }

    /*
     * Adds this tag to the spool, keeping the tags already on it: a U1 spool carries two,
     * one on each side, and the second has to join the first rather than take its place.
     * Tags outlive the roll they were stuck to and get peeled onto a fresh one, so the
     * UID may still be recorded against a spool it has left; take it off those, and one
     * physical tag stays the tag of exactly one spool.
     */
    private void bindCardUid(String baseUrl, int spoolId, String cardUid) {
        if (spoolId <= 0) return;
        /*
         * Every step below can fail quietly - no tag in range, the spool gone, the write
         * refused - and this whole path used to swallow all of it, so a spool that never
         * got its second tag still reported the update as a success and said nothing
         * about the tag at all. Each outcome now names itself, and the count says how
         * many tags the spool ended up holding, which is the thing being got wrong.
         */
        if (cardUid.isEmpty()) {
            showToast(getString(R.string.no_tag_to_link), Toast.LENGTH_LONG);
            return;
        }
        String upper = cardUid.toUpperCase(java.util.Locale.ROOT);
        for (JSONObject other : findSpoolsByCardUid(baseUrl, upper)) {
            int otherId = other.optInt("id");
            if (otherId != spoolId) unbindCardUid(baseUrl, otherId, upper);
        }
        JSONObject spool = reloadSpool(baseUrl, spoolId);
        if (spool == null) {
            showToast(getString(R.string.failed_to_link_tag, spoolId), Toast.LENGTH_LONG);
            return;
        }
        List<String> uids = parseCardUids(spool);
        if (uids.contains(upper)) {
            showToast(getResources().getQuantityString(R.plurals.tag_already_linked,
                    uids.size(), uids.size(), spoolId), Toast.LENGTH_LONG);
            return;
        }
        uids.add(upper);
        showToast(writeCardUids(baseUrl, spoolId, uids)
                ? getResources().getQuantityString(R.plurals.tag_linked_to_spool,
                        uids.size(), uids.size(), spoolId)
                : getString(R.string.failed_to_link_tag, spoolId), Toast.LENGTH_LONG);
    }

    // Spools already recorded against this filament, newest first. Anything Spoolman
    // cannot give us is treated as none, so the caller falls back to creating one.
    private List<JSONObject> fetchSpools(String baseUrl, int filamentId) {
        List<JSONObject> spools = new ArrayList<>();
        for (JSONObject spool : fetchAllSpools(baseUrl)) {
            JSONObject filament = spool.optJSONObject("filament");
            if (filament != null && filament.optInt("id", -1) == filamentId) spools.add(spool);
        }
        Collections.sort(spools, (a, b) -> Integer.compare(b.optInt("id"), a.optInt("id")));
        return spools;
    }

    private String describeSpool(JSONObject spool) {
        StringBuilder sb = new StringBuilder("#" + spool.optInt("id"));
        if (spool.has("remaining_weight") && !spool.isNull("remaining_weight")) {
            sb.append(String.format(Locale.getDefault(), "  %.0f g left", spool.optDouble("remaining_weight")));
        }
        String location = spool.optString("location", "");
        if (!location.isEmpty()) sb.append("  ").append(location);
        String lot = spool.optString("lot_nr", "");
        if (!lot.isEmpty()) sb.append("  lot ").append(lot);
        List<String> uids = parseCardUids(spool);
        if (!uids.isEmpty()) {
            sb.append("  ").append(getResources().getQuantityString(R.plurals.spool_tag_count,
                    uids.size(), uids.size()));
        }
        if (spool.optBoolean("archived")) sb.append("  (").append(getString(R.string.archived)).append(")");
        return sb.toString();
    }

    interface SpoolChoice { void onChosen(Integer spoolId); }

    // Offers the spools already on record plus the option to add another. Choosing one
    // updates it rather than creating a second record for the same physical spool.
    private void chooseSpool(List<JSONObject> spools, int boundId, String message, SpoolChoice choice) {
        try {
            String[] items = new String[spools.size() + 1];
            for (int i = 0; i < spools.size(); i++) {
                items[i] = describeSpool(spools.get(i))
                        + (spools.get(i).optInt("id") == boundId ? "   " + getString(R.string.this_tag) : "");
            }
            items[spools.size()] = getString(R.string.add_another_spool);

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            /*
             * A dialog shows a message or a list, never both: setMessage claims the content
             * area and the items are never laid out. This picker asked which spool the tag
             * belongs to and then showed no spools to pick, so cancelling was the only way
             * out of it - and every tag after a filament's first went unbound, because the
             * first tag creates the spool without the picker and each one after it comes
             * through here. The words go in the title, which sits above the list rather
             * than in place of it.
             */
            // Name the tag being assigned. With several spools of one filament on record,
            // which UID is going where is the whole question the picker is asking.
            String uid = currentCardUid();
            String heading = getString(R.string.spool_already_recorded);
            SpannableStringBuilder title = new SpannableStringBuilder(heading);
            title.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.primary_brand)),
                    0, heading.length(), 0);
            title.append("\n\n").append(message);
            if (!uid.isEmpty()) title.append("\n").append(getString(R.string.tag_uid, uid));

            TextView titleView = new TextView(context);
            titleView.setText(title);
            titleView.setTextColor(ContextCompat.getColor(context, R.color.text_main));
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            int pad = Math.round(16 * getResources().getDisplayMetrics().density);
            titleView.setPadding(pad, pad, pad, pad / 2);
            builder.setCustomTitle(titleView);
            builder.setItems(items, (dialog, which) ->
                    choice.onChosen(which < spools.size() ? spools.get(which).optInt("id") : null));
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
            AlertDialog alert = builder.create();
            alert.show();
            if (alert.getWindow() != null) {
                alert.getWindow().setBackgroundDrawableResource(R.color.background_alt);
                alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(context, R.color.primary_brand));
            }
        } catch (Exception ignored) {
            choice.onChosen(null);
        }
    }

    // Creates a spool, or updates the one chosen when spoolId is given.
    private void submitSpool(String baseUrl, SpoolDialogBinding sdl, int filamentId, Integer spoolId) {
        try {
            JSONObject sBody = new JSONObject();
            sBody.put("filament_id", filamentId);
            sBody.put("price", getDoubleOrNull(sdl.sPrice));
            if (!Objects.requireNonNull(sdl.sFirstUsed.getText()).toString().isEmpty()) {
                sBody.put("first_used", sdl.sFirstUsed.getText().toString());
            }
            if (!Objects.requireNonNull(sdl.sLastUsed.getText()).toString().isEmpty()) {
                sBody.put("last_used", sdl.sLastUsed.getText().toString());
            }
            sBody.put("initial_weight", getDoubleOrNull(sdl.sInitialWeight));
            if (getDoubleOrNull(sdl.sRemainingWeight) != JSONObject.NULL) {
                sBody.put("remaining_weight", getDoubleOrNull(sdl.sRemainingWeight));
            } else if (getDoubleOrNull(sdl.sUsedWeight) != JSONObject.NULL) {
                sBody.put("used_weight", getDoubleOrNull(sdl.sUsedWeight));
            }
            sBody.put("location", Objects.requireNonNull(sdl.sLocation.getText()).toString());
            sBody.put("lot_nr", Objects.requireNonNull(sdl.sLotNr.getText()).toString());
            sBody.put("comment", Objects.requireNonNull(sdl.sComment.getText()).toString());
            sBody.put("archived", sdl.sArchived.isChecked());

            String ret = spoolId == null
                    ? performSmRequest(context, baseUrl + "/spool", "POST", sBody.toString())
                    : performSmRequest(context, baseUrl + "/spool/" + spoolId, "PATCH", sBody.toString());
            if (ret != null) {
                showToast(getString(spoolId == null ? R.string.spool_created_successfully
                        : R.string.spool_updated_successfully), Toast.LENGTH_SHORT);
                mainHandler.post(() -> spoolDialog.dismiss());
                bindCardUid(baseUrl, new JSONObject(ret).optInt("id"), currentCardUid());
                cachedSpoolUid = "";
                cachedSpool = null;
            } else {
                showToast(getString(spoolId == null ? R.string.failed_to_create_spool
                        : R.string.failed_to_update_spool), Toast.LENGTH_SHORT);
            }
        } catch (Exception ignored) {
            showToast(getString(R.string.error_creating_spool), Toast.LENGTH_SHORT);
        }
    }


    public void openSettings() {
        settingsDialog = new Dialog(context, R.style.Theme_U1RFID);
        settingsDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        settingsDialog.setCanceledOnTouchOutside(false);
        SettingsDialogBinding sdl = SettingsDialogBinding.inflate(getLayoutInflater());
        View rv = sdl.getRoot();
        settingsDialog.setContentView(rv);

        sdl.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                sdl.containerGeneral.setVisibility(View.GONE);
                sdl.containerPrinter.setVisibility(View.GONE);
                sdl.containerSpoolman.setVisibility(View.GONE);
                switch (tab.getPosition()) {
                    case 0: sdl.containerGeneral.setVisibility(View.VISIBLE); break;
                    case 1: sdl.containerPrinter.setVisibility(View.VISIBLE); break;
                    case 2: sdl.containerSpoolman.setVisibility(View.VISIBLE); break;
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        sdl.readswitch.setChecked(GetSetting(context, "autoread", false));
        sdl.readswitch.setOnCheckedChangeListener((buttonView, isChecked) -> SaveSetting(context, "autoread", isChecked));
        sdl.aceswitch.setChecked(GetSetting(context, "acetag", false));
        sdl.aceswitch.setOnCheckedChangeListener((buttonView, isChecked) -> SaveSetting(context, "acetag", isChecked));
        sdl.modswitch.setChecked(GetSetting(context, "acemod", false));
        sdl.modswitch.setOnCheckedChangeListener((buttonView, isChecked) -> SaveSetting(context, "acemod", isChecked));
        sdl.launchswitch.setChecked(GetSetting(context, "autoLaunch", true));
        sdl.launchswitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            setNfcLaunchMode(context, isChecked);
            SaveSetting(context, "autoLaunch", isChecked);
        });
        sdl.themeswitch.setChecked(GetSetting(context, "enabledm", false));
        sdl.themeswitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SaveSetting(context, "enabledm", isChecked);
            setThemeMode(isChecked);
        });
        sdl.spoolswitch.setChecked(GetSetting(context, "enablesm", false));
        sdl.smhost.setText(GetSetting(context, "smhost", ""));
        sdl.smport.setText(String.valueOf(GetSetting(context, "smport", 7912)));
        sdl.u1host.setText(GetSetting(context, "u1host", ""));
        sdl.smhost.setEnabled(sdl.spoolswitch.isChecked());
        sdl.smport.setEnabled(sdl.spoolswitch.isChecked());
        sdl.spoolswitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sdl.smhost.setEnabled(isChecked);
            sdl.smport.setEnabled(isChecked);
            if (isChecked) {
                sdl.smhost.setTextColor(ContextCompat.getColor(context, R.color.text_main));
                sdl.smport.setTextColor(ContextCompat.getColor(context, R.color.text_main));
                main.smbutton.setVisibility(View.VISIBLE);
                if (matcher == null)
                {
                    executorService.execute(() -> matcher = new ColorMatcher(context));
                }
            }
            else {
                sdl.smhost.setTextColor(Color.GRAY);
                sdl.smport.setTextColor(Color.GRAY);
                main.smbutton.setVisibility(View.INVISIBLE);
            }
            SaveSetting(context, "enablesm", isChecked);
        });
        if (sdl.spoolswitch.isChecked()) {
            sdl.smhost.setTextColor(ContextCompat.getColor(context, R.color.text_main));
            sdl.smport.setTextColor(ContextCompat.getColor(context, R.color.text_main));
        }
        else {
            sdl.smhost.setTextColor(Color.GRAY);
            sdl.smport.setTextColor(Color.GRAY);
        }
        sdl.btncls.setOnClickListener(v -> settingsDialog.dismiss());
        sdl.btnsave.setOnClickListener(v -> settingsDialog.dismiss());
        settingsDialog.setOnDismissListener(dialogInterface -> {
            SaveSetting(context, "smhost", Objects.requireNonNull(sdl.smhost.getText()).toString());
            SaveSetting(context, "smport", Integer.parseInt(Objects.requireNonNull(sdl.smport.getText()).toString()));
            SaveSetting(context, "u1host", Objects.requireNonNull(sdl.u1host.getText()).toString());
        });
        settingsDialog.show();
    }


    public void openDryer() {
        dryerDialog = new Dialog(context, R.style.DialogTheme);
        dryerDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dryerDialog.setCanceledOnTouchOutside(false);
        DryerDialogBinding ddl = DryerDialogBinding.inflate(getLayoutInflater());
        View rv = ddl.getRoot();
        dryerDialog.setContentView(rv);

        String[] durationOptions = {"30 Minutes", "60 Minutes", "180 Minutes", "120 Minutes", "240 Minutes", "360 Minutes", "420 Minutes"};
        String[] tempOptions = {"55°C", "45°C", "40°C", "35°C", "30°C"};

        ArrayAdapter<String> durationAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, durationOptions);
        durationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<String> tempAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, tempOptions);
        tempAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ddl.duration.setAdapter(durationAdapter);
        ddl.temperature.setAdapter(tempAdapter);
        ddl.duration.setSelection(4);
        ddl.temperature.setSelection(0);

        ddl.btncls.setOnClickListener(v -> dryerDialog.dismiss());

        ddl.btnstart.setOnClickListener(v -> {
            if (GetSetting(context,"u1host","").isEmpty()) {
                showToast("Set printer host first", Toast.LENGTH_SHORT);
                return;
            }
            startDryer(this, ddl.temperature.getSelectedItem().toString().replace("°C",""), ddl.duration.getSelectedItem().toString().replace(" Minutes",""), success -> {
                runOnUiThread(() -> {
                    if (success) {
                        showToast("Drying started", Toast.LENGTH_SHORT);
                        dryerDialog.dismiss();
                    } else {
                        showToast("Failed to start drying", Toast.LENGTH_SHORT);
                    }
                });
            });
        });


        ddl.btnstop.setOnClickListener(v -> {
            if (GetSetting(context,"u1host","").isEmpty()) {
                showToast("Set printer host first", Toast.LENGTH_SHORT);
                return;
            }
            stopDryer(this, success -> {
                runOnUiThread(() -> {
                    if (success) {
                        showToast("Drying stopped", Toast.LENGTH_SHORT);
                        dryerDialog.dismiss();
                    } else {
                        showToast("Failed to stop drying", Toast.LENGTH_SHORT);
                    }
                });
            });
        });

        dryerDialog.show();
    }


    void loadToolFrame() {
        try {

            updateCircleColor(select[0], GetSetting(context, "tool1_color", "B6BBBC"));
            updateCircleColor(select[1], GetSetting(context, "tool2_color", "B6BBBC"));
            updateCircleColor(select[2], GetSetting(context, "tool3_color", "B6BBBC"));
            updateCircleColor(select[3], GetSetting(context, "tool4_color", "B6BBBC"));
            type[0].setText(GetSetting(context, "tool1_type", "?"));
            type[1].setText(GetSetting(context, "tool2_type", "?"));
            type[2].setText(GetSetting(context, "tool3_type", "?"));
            type[3].setText(GetSetting(context, "tool4_type", "?"));

           // startFrameUpdater();
            for (int i = 0; i < tools.length; i++) {
                final int index = i;
                tools[i].setOnClickListener(v -> {
                    for (int j = 0; j < tools.length; j++) {
                        if (j == index) {
                            tools[j].setBackgroundResource(R.drawable.tool_selected);
                            SelectedTool = String.valueOf(index);
                            main.tagid.setVisibility(View.VISIBLE);
                            main.tagid.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.twotone_tool_24, 0, 0, 0);
                            main.tagid.setText(String.format(Locale.getDefault(), "Toolhead: %d", (index + 1)));
                            check[j].setBackgroundResource(R.drawable.check_circle);
                        } else {
                            tools[j].setBackgroundResource(R.drawable.tool_unselected);
                            check[j].setBackgroundResource(0);
                        }
                    }
                });

                tools[i].setOnLongClickListener(v -> {
                    for (int j = 0; j < tools.length; j++) {
                        if (j == index) {
                            tools[j].setBackgroundResource(R.drawable.tool_selected);
                            SelectedTool = String.valueOf(index);
                            main.tagid.setVisibility(View.VISIBLE);
                            main.tagid.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.twotone_tool_24, 0, 0, 0);
                            main.tagid.setText(String.format(Locale.getDefault(), "Toolhead: %d", (index + 1)));
                            check[j].setBackgroundResource(R.drawable.check_circle);
                        } else {
                            tools[j].setBackgroundResource(R.drawable.tool_unselected);
                            check[j].setBackgroundResource(0);
                        }
                    }


                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    SpannableString titleText = new SpannableString("Select Action");
                    titleText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.primary_brand)), 0, titleText.length(), 0);
                    SpannableString messageText = new SpannableString("Clear the filament configuration from toolhead " + (index + 1) + " or load the filament information into the app?");
                    messageText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.text_main)), 0, messageText.length(), 0);
                    builder.setTitle(titleText);
                    builder.setMessage(messageText);

                    builder.setNegativeButton("Clear", (dialog, which) -> {
                        if (GetSetting(context, "u1host", "").isEmpty()) {
                            return;
                        }
                        int toolNumber = Integer.parseInt(SelectedTool);
                        updateCircleColor(select[toolNumber], "B6BBBC");
                        type[toolNumber].setText("?");
                        SaveSetting(context, "tool" + (toolNumber + 1) + "_type", "?");
                        SaveSetting(context, "tool" + (toolNumber + 1) + "_color", "FFB6BBBC");
                        SaveSetting(context, "tool" + (toolNumber + 1) + "_id", "0");
                        clearFilament(this, SelectedTool, success -> {
                            runOnUiThread(() -> {
                                if (success) {
                                    showToast("Filament configuration cleared successfully", Toast.LENGTH_SHORT);
                                } else {
                                    showToast("Failed to clear printer filament configuration", Toast.LENGTH_SHORT);
                                }
                            });
                        });
                    });

                    builder.setPositiveButton("Load", (dialog, which) -> {
                        try {
                            int toolNumber = Integer.parseInt(SelectedTool);
                            Filament filament;
                            filament = findFilament(matDb, vendor[toolNumber],type[toolNumber].getText().toString(),subtype[toolNumber]);
                            if (filament == null) {
                                filament = matDb.getFilamentById(GetSetting(context, "tool" + (toolNumber + 1) + "_id", ""));
                            }
                            if (filament == null) {
                                return;
                            }
                            OpenSpoolFilament osf = new OpenSpoolFilament(filament.filamentParam);
                            userSelect = true;
                            setSpinnerSelection(main.brand, osf.getBrand());
                            main.brand.postDelayed(() -> {
                                setSpinnerSelection(main.type, osf.getType());
                                main.type.postDelayed(() -> {
                                    try {
                                        setSpinnerSelection(main.subtype, osf.getSubType());
                                    } catch (Exception ignored) {
                                    }
                                }, 200);
                            }, 200);

                            try{
                                if (color[toolNumber] == null) {
                                    MaterialColor = GetSetting(this, "tool" + (toolNumber + 1) + "_color", "FF0000FF");
                                }
                                else {
                                    MaterialColor = color[toolNumber];
                                }
                            } catch (Exception e) {
                                MaterialColor = GetSetting(this, "tool" + (toolNumber + 1) + "_color", "FF0000FF");
                            }

                            int colorInt = Color.parseColor("#" + MaterialColor);
                            main.colorview.setBackgroundColor(colorInt);
                            main.txtcolor.setText(MaterialColor);
                            main.txtcolor.setTextColor(getContrastColor(colorInt));
                            userSelect = false;
                        } catch (Exception ignored) {
                            userSelect = false;
                        }
                    });

                    builder.setNeutralButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                    AlertDialog alert = builder.create();
                    alert.show();
                    if (alert.getWindow() != null) {
                        alert.getWindow().setBackgroundDrawableResource(R.color.background_alt);
                        alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.primary_brand));
                        alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.primary_brand));
                        alert.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(ContextCompat.getColor(this, R.color.primary_brand));
                    }
                    return true;
                });

            }

        } catch (Exception ignored) {}
    }


    private final Runnable updateToolFrame = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                executorService.execute(() -> {
                    try {
                        getFilament(context, (filamentInfo) -> {
                            runOnUiThread(() -> {
                                try {
                                    JSONObject root = new JSONObject(filamentInfo);
                                    JSONArray params = root.getJSONArray("params");
                                    String innerStringRaw = params.getString(0);
                                    String jsonReady = innerStringRaw.replaceFirst("//", "").trim();
                                    JSONObject data = new JSONObject(jsonReady);
                                    JSONArray vendors = data.getJSONArray("filament_vendor");
                                    JSONArray types = data.getJSONArray("filament_type");
                                    JSONArray subTypes = data.getJSONArray("filament_sub_type");
                                    JSONArray colors = data.getJSONArray("filament_color");
                                    for (int i = 0; i < 4; i++) {
                                        String filamentVendor = vendors.getString(i);
                                        String filamentType = types.getString(i).replace("NONE", "?");
                                        String filamentSubType = subTypes.getString(i);
                                        String filamentColor = Long.toHexString(colors.getLong(i)).toUpperCase();
                                        vendor[i] = filamentVendor;
                                        subtype[i] = filamentSubType;
                                        type[i].setText(filamentType);
                                        if (filamentType.equals("?")) {
                                            updateCircleColor(select[i], "B6BBBC");
                                            color[i] = "B6BBBC";
                                        } else {
                                            updateCircleColor(select[i], filamentColor);
                                            color[i] = filamentColor;
                                        }
                                    }
                                } catch (Exception e) {
                                    updateCircleColor(select[0], GetSetting(context, "tool1_color", "B6BBBC"));
                                    updateCircleColor(select[1], GetSetting(context, "tool2_color", "B6BBBC"));
                                    updateCircleColor(select[2], GetSetting(context, "tool3_color", "B6BBBC"));
                                    updateCircleColor(select[3], GetSetting(context, "tool4_color", "B6BBBC"));
                                    type[0].setText(GetSetting(context, "tool1_type", "?"));
                                    type[1].setText(GetSetting(context, "tool2_type", "?"));
                                    type[2].setText(GetSetting(context, "tool3_type", "?"));
                                    type[3].setText(GetSetting(context, "tool4_type", "?"));
                                }
                            });
                        });
                    } catch (Exception ignored) {}
                    mainHandler.postDelayed(this, INTERVAL);
                });
            }
        }
    };

    public void startFrameUpdater() {
        try {
            if (!isRunning) {
                isRunning = true;
                mainHandler.post(updateToolFrame);
            }
        } catch (Exception ignored) {}
    }

    public void stopFrameUpdater() {
        try {
            isRunning = false;
            mainHandler.removeCallbacks(updateToolFrame);
        } catch (Exception ignored) {}
    }

}