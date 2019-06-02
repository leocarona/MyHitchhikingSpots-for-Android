package com.myhitchhikingspots.utilities;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;

/**
 * Created by leoboaventura on 25/04/2017.
 */

public class IconUtils {

    /**
     * Demonstrates converting any Drawable to an Icon, for use as a marker icon.
     */
    public static Icon drawableToIcon(@NonNull Context context, @DrawableRes int id) {
        return drawableToIcon(context, id, -1);
    }

    /**
     * Demonstrates converting any Drawable to an Icon, for use as a marker icon.
     */
    public static Icon drawableToIcon(@NonNull Context context, @DrawableRes int id, @ColorInt int colorRes, double width, double height) {
        Drawable vectorDrawable = ResourcesCompat.getDrawable(context.getResources(), id, context.getTheme());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, (int) (canvas.getWidth() * width), (int) (canvas.getHeight() * height));
        if (colorRes != -1)
            DrawableCompat.setTint(vectorDrawable, colorRes);
        vectorDrawable.draw(canvas);
        return IconFactory.getInstance(context).fromBitmap(bitmap);
    }

    /**
     * Demonstrates converting any Drawable to an Icon, for use as a marker icon.
     */
    public static Icon drawableToIcon(@NonNull Context context, @DrawableRes int id, @ColorInt int colorRes) {
        return drawableToIcon(context, id, colorRes, 1, 1);
    }

    /**
     * Demonstrates converting any Drawable to an Icon, for use as a marker icon.
     */
    public static Icon drawableToIcon(@NonNull Context context, @DrawableRes int id, @Nullable ColorStateList colorRes) {
        if (colorRes == null)
            return drawableToIcon(context, id, -1);

        Drawable vectorDrawable = ResourcesCompat.getDrawable(context.getResources(), id, context.getTheme());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        DrawableCompat.setTintList(vectorDrawable, colorRes);
        vectorDrawable.draw(canvas);
        return IconFactory.getInstance(context).fromBitmap(bitmap);
    }
}