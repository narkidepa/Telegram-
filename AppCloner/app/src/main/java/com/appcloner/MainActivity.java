package com.appcloner;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int PICK_ICON = 100;
    
    private ListView listApps;
    private EditText editAppName, editPackageName;
    private TextView textStatus, textSelectedApp, textPreview;
    private ImageView imgIcon, imgPreviewIcon;
    private Button btnClone, btnQuickClone, btnRefresh, btnChangeIcon;
    
    private List<AppInfo> installedApps = new ArrayList<>();
    private AppInfo selectedApp;
    private int cloneCounter = 1;
    private Bitmap selectedIconBitmap = null;
    private File iconFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        loadInstalledApps();
    }

    private void initViews() {
        listApps = findViewById(R.id.list_apps);
        editAppName = findViewById(R.id.edit_app_name);
        editPackageName = findViewById(R.id.edit_package_name);
        textStatus = findViewById(R.id.text_status);
        textSelectedApp = findViewById(R.id.text_selected_app);
        textPreview = findViewById(R.id.text_preview);
        imgIcon = findViewById(R.id.img_icon);
        imgPreviewIcon = findViewById(R.id.img_preview_icon);
        btnClone = findViewById(R.id.btn_clone);
        btnQuickClone = findViewById(R.id.btn_quick_clone);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnChangeIcon = findViewById(R.id.btn_change_icon);

        btnClone.setOnClickListener(v -> cloneApp(true));
        btnQuickClone.setOnClickListener(v -> cloneApp(false));
        btnRefresh.setOnClickListener(v -> loadInstalledApps());
        
        btnChangeIcon.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_ICON);
        });

        editAppName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) updatePreview();
        });
        
        editPackageName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) updatePreview();
        });

        listApps.setOnItemClickListener((parent, view, position, id) -> {
            selectedApp = installedApps.get(position);
            selectedIconBitmap = null;
            displayAppInfo(selectedApp);
        });

        listApps.setOnItemLongClickListener((parent, view, position, id) -> {
            AppInfo app = installedApps.get(position);
            openAppSettings(app.packageName);
            return true;
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_ICON && resultCode == RESULT_OK && data != null) {
            try {
                Uri imageUri = data.getData();
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                selectedIconBitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
                
                imgPreviewIcon.setImageBitmap(selectedIconBitmap);
                
                iconFile = new File(getCacheDir(), "custom_icon.png");
                FileOutputStream fos = new FileOutputStream(iconFile);
                selectedIconBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
                
                updatePreview();
                Toast.makeText(this, "Icon selected! Will be applied during clone.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Error selecting icon: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadInstalledApps() {
        PackageManager pm = getPackageManager();
        List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_META_DATA);
        
        installedApps.clear();
        for (PackageInfo pi : packages) {
            if ((pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                AppInfo app = new AppInfo();
                app.packageName = pi.packageName;
                app.appName = pi.applicationInfo.loadLabel(pm).toString();
                app.icon = pi.applicationInfo.loadIcon(pm);
                app.apkPath = pi.applicationInfo.sourceDir;
                installedApps.add(app);
            }
        }

        AppListAdapter adapter = new AppListAdapter(this, installedApps);
        listApps.setAdapter(adapter);
        textStatus.setText("Found " + installedApps.size() + " apps\nClick to select");
    }

    private void displayAppInfo(AppInfo app) {
        textSelectedApp.setText("Selected: " + app.appName);
        imgIcon.setImageDrawable(app.icon);
        
        imgPreviewIcon.setImageDrawable(app.icon);
        
        String newPackageName = app.packageName + ".clone" + cloneCounter;
        String newAppName = app.appName + " Clone";
        
        editPackageName.setText(newPackageName);
        editAppName.setText(newAppName);
        
        updatePreview();
    }

    private void updatePreview() {
        if (selectedApp == null) return;
        
        String newName = editAppName.getText().toString().trim();
        if (newName.isEmpty()) newName = selectedApp.appName + " Clone";
        
        String newPackage = editPackageName.getText().toString().trim();
        if (newPackage.isEmpty()) newPackage = selectedApp.packageName + ".clone" + cloneCounter;
        
        textPreview.setText("PREVIEW:\n" + newName + "\nPackage: " + newPackage);
        
        if (selectedIconBitmap != null) {
            imgPreviewIcon.setImageBitmap(selectedIconBitmap);
        }
    }

    private void openAppSettings(String packageName) {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + packageName));
        startActivity(intent);
    }

    private void cloneApp(boolean customNames) {
        if (selectedApp == null) {
            Toast.makeText(this, "Select an app first!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String newPackageName = customNames ? 
                editPackageName.getText().toString().trim() : 
                selectedApp.packageName + ".clone" + cloneCounter;
            
            String newAppName = customNames ? 
                editAppName.getText().toString().trim() : 
                selectedApp.appName + " Clone";

            if (newPackageName.isEmpty()) {
                newPackageName = selectedApp.packageName + ".clone" + cloneCounter;
            }
            if (newAppName.isEmpty()) {
                newAppName = selectedApp.appName + " Clone";
            }

            textStatus.setText("Cloning: " + newAppName + "\nPlease wait...");

            File apkFile = new File(selectedApp.apkPath);
            File outputDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            String fileName = sanitizeFileName(newAppName) + "_v" + cloneCounter + ".apk";
            File newApk = new File(outputDir, fileName);

            copyFile(apkFile, newApk);

            if (iconFile != null && iconFile.exists()) {
                File iconInOutput = new File(outputDir, sanitizeFileName(newAppName) + "_icon.png");
                copyFile(iconFile, iconInOutput);
            }

            File configFile = new File(outputDir, sanitizeFileName(newAppName) + "_CLONE_INFO.txt");
            String config = "CLONED APP INFO\n" +
                    "===============\n" +
                    "Original App: " + selectedApp.appName + "\n" +
                    "Original Package: " + selectedApp.packageName + "\n" +
                    "\n" +
                    "New App Name: " + newAppName + "\n" +
                    "New Package: " + newPackageName + "\n" +
                    "\n" +
                    "Custom Icon: " + (iconFile != null ? "Yes" : "No") + "\n" +
                    "\n" +
                    "INSTRUCTIONS:\n" +
                    "=============\n" +
                    "1. Install the cloned APK\n" +
                    "2. Use APK Editor Pro to:\n" +
                    "   - Change package name to: " + newPackageName + "\n" +
                    "   - Change app name to: " + newAppName + "\n" +
                    "   - Change icon (if custom icon selected)\n" +
                    "3. Save and install modified APK\n" +
                    "\n" +
                    "APK Location: " + newApk.getAbsolutePath() + "\n" +
                    "Icon Location: " + (iconFile != null ? iconFile.getAbsolutePath() : "Not selected");
            
            FileOutputStream configFos = new FileOutputStream(configFile);
            configFos.write(config.getBytes());
            configFos.close();

            cloneCounter++;
            
            textStatus.setText("SUCCESS!\n\nAPK: " + newApk.getName() + "\n" +
                    "Info: " + configFile.getName() + "\n\n" +
                    "Check Downloads folder!");
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(newApk), "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            Toast.makeText(this, "APK Ready! Check Downloads for info file.", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            textStatus.setText("ERROR: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void copyFile(File src, File dst) throws Exception {
        FileInputStream fis = new FileInputStream(src);
        FileOutputStream fos = new FileOutputStream(dst);
        byte[] buffer = new byte[4096];
        int length;
        while ((length = fis.read(buffer)) > 0) {
            fos.write(buffer, 0, length);
        }
        fos.close();
        fis.close();
    }

    public static class AppInfo {
        public String packageName;
        public String appName;
        public String apkPath;
        public Drawable icon;
    }
}
