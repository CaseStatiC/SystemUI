/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui;

import android.annotation.StringDef;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;

public final class Prefs {
    public static final String TAG = "Prefs";
    private Prefs() {} // no instantation

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
        Key.SEARCH_APP_WIDGET_ID,
        Key.DEBUG_MODE_ENABLED,
        Key.HOTSPOT_TILE_LAST_USED,
        Key.COLOR_INVERSION_TILE_LAST_USED,
        Key.DND_TILE_VISIBLE,
        Key.DND_TILE_COMBINED_ICON,
        Key.DND_CONFIRMED_PRIORITY_INTRODUCTION,
        Key.DND_CONFIRMED_SILENCE_INTRODUCTION,
        Key.DND_FAVORITE_BUCKET_INDEX,
        Key.DND_NONE_SELECTED,
        Key.DND_FAVORITE_ZEN,
    })
    public @interface Key {
        String SEARCH_APP_WIDGET_ID = "searchAppWidgetId";
        String SEARCH_APP_WIDGET_PACKAGE = "searchAppWidgetPackage";
        String DEBUG_MODE_ENABLED = "debugModeEnabled";
        String HOTSPOT_TILE_LAST_USED = "HotspotTileLastUsed";
        String COLOR_INVERSION_TILE_LAST_USED = "ColorInversionTileLastUsed";
        String DND_TILE_VISIBLE = "DndTileVisible";
        String DND_TILE_COMBINED_ICON = "DndTileCombinedIcon";
        String DND_CONFIRMED_PRIORITY_INTRODUCTION = "DndConfirmedPriorityIntroduction";
        String DND_CONFIRMED_SILENCE_INTRODUCTION = "DndConfirmedSilenceIntroduction";
        String DND_FAVORITE_BUCKET_INDEX = "DndCountdownMinuteIndex";
        String DND_NONE_SELECTED = "DndNoneSelected";
        String DND_FAVORITE_ZEN = "DndFavoriteZen";
    }

    public static boolean getBoolean(Context context, @Key String key, boolean defaultValue) {
        Log.d(TAG, "getBoolean: ");
        return get(context).getBoolean(key, defaultValue);
    }

    public static void putBoolean(Context context, @Key String key, boolean value) {
        Log.d(TAG, "putBoolean: ");
        get(context).edit().putBoolean(key, value).apply();
    }

    public static int getInt(Context context, @Key String key, int defaultValue) {
        Log.d(TAG, "getInt: ");
        return get(context).getInt(key, defaultValue);
    }

    public static void putInt(Context context, @Key String key, int value) {
        Log.d(TAG, "putInt: ");
        get(context).edit().putInt(key, value).apply();
    }

    public static long getLong(Context context, @Key String key, long defaultValue) {
        Log.d(TAG, "getLong: ");
        return get(context).getLong(key, defaultValue);
    }

    public static void putLong(Context context, @Key String key, long value) {
        Log.d(TAG, "putLong: ");
        get(context).edit().putLong(key, value).apply();
    }

    public static String getString(Context context, @Key String key, String defaultValue) {
        Log.d(TAG, "getString: ");
        return get(context).getString(key, defaultValue);
    }

    public static void putString(Context context, @Key String key, String value) {
        Log.d(TAG, "putString: ");
        get(context).edit().putString(key, value).apply();
    }

    public static Map<String, ?> getAll(Context context) {
        Log.d(TAG, "getAll: ");
        return get(context).getAll();
    }

    public static void remove(Context context, @Key String key) {
        Log.d(TAG, "remove: ");
        get(context).edit().remove(key).apply();
    }

    public static void registerListener(Context context,
            OnSharedPreferenceChangeListener listener) {
        Log.d(TAG, "registerListener: ");
        get(context).registerOnSharedPreferenceChangeListener(listener);
    }

    public static void unregisterListener(Context context,
            OnSharedPreferenceChangeListener listener) {
        Log.d(TAG, "unregisterListener: ");
        get(context).unregisterOnSharedPreferenceChangeListener(listener);
    }

    private static SharedPreferences get(Context context) {
        Log.d(TAG, "get: ");
        return context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
    }
}
