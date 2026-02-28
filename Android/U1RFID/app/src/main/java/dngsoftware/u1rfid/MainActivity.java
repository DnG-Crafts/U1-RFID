package dngsoftware.u1rfid;

import static android.view.View.TEXT_ALIGNMENT_CENTER;
import dngsoftware.u1rfid.databinding.ActivityMainBinding;
import dngsoftware.u1rfid.databinding.AddDialogBinding;
import dngsoftware.u1rfid.databinding.PickerDialogBinding;
import dngsoftware.u1rfid.databinding.TagDialogBinding;
import static dngsoftware.u1rfid.FilamentRegistry.filamentTypes;
import static dngsoftware.u1rfid.FilamentRegistry.filamentVendors;
import static dngsoftware.u1rfid.Utils.*;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
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
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.navigation.NavigationView;
import org.json.JSONObject;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback, NavigationView.OnNavigationItemSelectedListener {
    private MatDB matDb;
    private filamentDB rdb;
    private NfcAdapter nfcAdapter;
    Tag currentTag = null;
    int tagType;
    ArrayAdapter<String> sadapter;
    String MaterialID, MaterialWeight = "1 KG", MaterialColor = "FF0000FF";
    Dialog pickerDialog, addDialog, tagDialog;
    AlertDialog inputDialog;
    tagAdapter recycleAdapter;
    RecyclerView recyclerView;
    private Toast currentToast;
    tagItem[] tagItems;
    int SelectedSize;
    boolean userSelect = false;
    private ActivityMainBinding main;
    Bitmap gradientBitmap;
    private ExecutorService executorService;
    private Handler mainHandler;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        SetPermissions(this);

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        setupActivityResultLaunchers();

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        MenuItem launchItem = navigationView.getMenu().findItem(R.id.nav_launch);
        SwitchCompat launchSwitch = Objects.requireNonNull(launchItem.getActionView()).findViewById(R.id.drawer_switch);
        launchSwitch.setChecked(GetSetting(this, "autoLaunch", true));
        launchSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            setNfcLaunchMode(this, isChecked);
            SaveSetting(this, "autoLaunch", isChecked);
        });

        MenuItem readItem = navigationView.getMenu().findItem(R.id.nav_read);
        SwitchCompat readSwitch = Objects.requireNonNull(readItem.getActionView()).findViewById(R.id.drawer_switch);
        readSwitch.setChecked(GetSetting(this, "autoread", false));
        readSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SaveSetting(this, "autoread", isChecked);
        });

        MenuItem darkItem = navigationView.getMenu().findItem(R.id.nav_dark);
        SwitchCompat darkSwitch = Objects.requireNonNull(darkItem.getActionView()).findViewById(R.id.drawer_switch);
        darkSwitch.setChecked(GetSetting(this, "enabledm", false));
        darkSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SaveSetting(this, "enabledm", isChecked);
            setThemeMode(isChecked);
        });

        main.colorview.setOnClickListener(view -> openPicker());
        main.colorview.setBackgroundColor(Color.argb(255, 0, 0, 255));
        main.txtcolor.setText(MaterialColor);
        main.txtcolor.setTextColor(getContrastColor(Color.parseColor("#" + MaterialColor)));
        main.readbutton.setOnClickListener(view -> readTag(currentTag));
        main.writebutton.setOnClickListener(view -> writeTag(currentTag));

        main.menubutton.setOnClickListener(view -> drawerLayout.openDrawer(GravityCompat.START));
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
            } catch (Exception ignored) {}
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
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            try {
                nfcAdapter.disableReaderMode(this);
            } catch (Exception ignored) {
            }
        }
        if (pickerDialog != null && pickerDialog.isShowing()) {
            pickerDialog.dismiss();
        }
        if (inputDialog != null && inputDialog.isShowing()) {
            inputDialog.dismiss();
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
    public void onTagDiscovered(Tag tag) {
        try {
            mainHandler.post(() -> {
                byte[] uid = tag.getId();
                if (uid.length >= 6) {
                    currentTag = tag;
                    showToast(getString(R.string.tag_found) + bytesToHex(uid, false), Toast.LENGTH_SHORT);
                    tagType = getTagType(NfcA.get(currentTag));
                    main.tagid.setText(bytesToHex(uid, true));
                    if (tagType == 100 || tagType == 213) {
                        showToast(getString(R.string.incompatible_tag), Toast.LENGTH_SHORT);
                    }
                    main.lbltagid.setVisibility(View.VISIBLE);
                    if (GetSetting(this, "autoread", false)) {
                        readTag(currentTag);
                    }
                }
                else {
                    currentTag = null;
                    main.tagid.setText("");
                    main.lbltagid.setVisibility(View.INVISIBLE);
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
                        showToast(R.string.unknown_or_empty_tag, Toast.LENGTH_SHORT);
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
                                main.brand.post(() -> {
                                    setSpinnerSelection(main.type, filament.getType());
                                    main.type.post(() -> {
                                        try {
                                            JSONObject json = new JSONObject(jsonString);
                                            setSpinnerSelection(main.subtype, json.optString("subtype", "Basic"));
                                        } catch (Exception ignored) {}
                                    });
                                });

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
        });
    }


    public void writeTag(Tag tag) {
        try {
            Filament filament = matDb.getFilamentById(MaterialID);

            if (filament == null){
                showToast(getString(R.string.filament_not_found_in_db), Toast.LENGTH_SHORT);
                return;
            }

            OpenSpoolFilament osf = new OpenSpoolFilament(filament.filamentParam);
            osf.setColor(MaterialColor.substring(2), MaterialColor.substring(0, 2));
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
                showToast(R.string.data_written_to_tag, Toast.LENGTH_SHORT);
                playBeep();
            } else {
                NdefFormatable ndefFmt = NdefFormatable.get(tag);
                if (ndefFmt != null) {
                    ndefFmt.connect();
                    ndefFmt.format(message);
                    if (ndefFmt.isConnected()) ndefFmt.close();
                    showToast(R.string.data_written_to_tag, Toast.LENGTH_SHORT);
                } else {
                    showToast(R.string.invalid_tag_type, Toast.LENGTH_SHORT);
                }
            }

        } catch (Exception e) {
            showToast(R.string.error_writing_to_tag, Toast.LENGTH_SHORT);
        }
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
            pickerDialog.setTitle(R.string.pick_color);
            PickerDialogBinding dl = PickerDialogBinding.inflate(getLayoutInflater());
            View rv = dl.getRoot();
            colorDialog = dl;
            pickerDialog.setContentView(rv);
            gradientBitmap = null;

            dl.btncls.setOnClickListener(v -> {
                MaterialColor = dl.txtcolor.getText().toString();

                    if (dl.txtcolor.getText().toString().length() == 8) {
                        try {
                            int color = Color.argb(dl.alphaSlider.getProgress(), dl.redSlider.getProgress(), dl.greenSlider.getProgress(), dl.blueSlider.getProgress());
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
            addDialog.setTitle(R.string.add_filament);
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


    private void updateColorDisplay(PickerDialogBinding dl, int currentAlpha,int currentRed,int currentGreen,int currentBlue) {
        int color = Color.argb(currentAlpha, currentRed, currentGreen, currentBlue);
        dl.colorDisplay.setBackgroundColor(color);
        String hexCode = rgbToHexA(currentRed, currentGreen, currentBlue, currentAlpha);
        dl.txtcolor.setText(hexCode);
        dl.txtcolor.setTextColor(getContrastColor(Color.parseColor("#" + hexCode)));
    }


    private void setupPresetColors(PickerDialogBinding dl) {
        dl.presetColorGrid.removeAllViews();
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
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
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
            tagDialog.setTitle(R.string.tag_memory);
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

}