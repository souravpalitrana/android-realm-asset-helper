package com.eggheadgames.realmassethelper;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OsUtil {

    private static final String VERSION_PATTERN = "_\\d+\\.realm";
    private String cachedAssetPath;

    public String loadDatabaseToLocalStorage(Context context, String databaseName) {
        String asset = findAsset(context, "", databaseName);

        File file = new File(generateDatabaseFileName(context, databaseName));
        if (file.exists()) {
            file.delete();
        }
        try {
            InputStream is = context.getAssets().open(asset);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(buffer);
            fos.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return file.getName();
    }

    public String generateDatabaseFileName(Context context, String databaseName) {
        return context.getFilesDir() + File.separator + databaseName + ".realm";
    }

    public String getFileNameForDatabase(Context context, String databaseName) {
        return new File(generateDatabaseFileName(context, databaseName)).getName();
    }

    public Integer getCurrentDbVersion(Context context, String databaseName) {
        int currentVersion = PreferenceManager.getDefaultSharedPreferences(context).getInt(Constants.PREFERENCES_DB_VERSION + databaseName, -1);
        return currentVersion == -1 ? null : currentVersion;
    }

    public int getAssetsDbVersion(Context context, String databaseName) {
        String dbAsset = findAsset(context, "", databaseName);
        Pattern pattern = Pattern.compile(VERSION_PATTERN);
        Matcher matcher = pattern.matcher(dbAsset);

        if(matcher.find()) {
            String version = matcher.group().substring(1, matcher.group().indexOf('.'));
            return Integer.parseInt(version);
        }
        return 0;
    }

    public void storeDatabaseVersion(Context context, int version, String databaseName) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(Constants.PREFERENCES_DB_VERSION + databaseName, version).commit();
    }

    public boolean isEmpty(String string) {
        return TextUtils.isEmpty(string);
    }

    /**
     * expected asset name <databaseName>_xx.realm or <databaseName>.realm
     */
    public boolean isDatabaseAssetExists(Context context, String databaseName) {
        return !TextUtils.isEmpty(findAsset(context, "", databaseName));
    }

    public void clearCache() {
        cachedAssetPath = null;
    }

    private String findAsset(Context context, String path, String databaseName) {
        if (!TextUtils.isEmpty(cachedAssetPath)) {
            return cachedAssetPath;
        } else {
            try {
                String[] list;
                list = context.getAssets().list(path);
                if (list.length > 0) {
                    for (String file : list) {
                        String asset = findAsset(context,
                                TextUtils.isEmpty(path) ? file : path + File.separator + file,
                                databaseName);
                        if (!TextUtils.isEmpty(asset)) {
                            return asset;
                        }
                    }
                } else {
                    //it's a file
                    String fileName = new File(path).getName();
                    if (!TextUtils.isEmpty(fileName)) {
                        if (fileName.matches(databaseName + VERSION_PATTERN) || fileName.matches(databaseName + ".realm")) {
                            cachedAssetPath = path;
                            return path;
                        }
                    }
                }
            } catch (IOException e) {
                return null;
            }
            return null;
        }
    }
}
