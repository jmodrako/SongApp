package com.tooploox.songapp.common;

import android.databinding.BindingAdapter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.IdRes;
import android.view.View;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import static com.squareup.picasso.MemoryPolicy.NO_CACHE;
import static com.squareup.picasso.MemoryPolicy.NO_STORE;

public class DataBindingUtils {
    private static final String IMAGE_URL = "imageUrl";
    private static final String IMAGE_PLACEHOLDER = "imagePlaceholder";
    private static final String IMAGE_COLOR_PLACEHOLDER = "imageColorPlaceholder";
    private static final String IMAGE_DRAWABLE_PLACEHOLDER = "imageDrawablePlaceholder";
    private static final String IMAGE_DRAWABLE = "imageDrawable";
    private static final String IMAGE_WITH_CACHE = "withCache";
    private static final String VIEW_HIDE = "hide";

    @BindingAdapter("android:src")
    public static void setImageResource(ImageView imageView, int resource) {
        if (imageView != null && resource != -1) imageView.setImageResource(resource);
    }

    @BindingAdapter(IMAGE_URL)
    public static void loadImage(ImageView imageView, String imageUrl) {
        if (imageView == null || imageUrl == null || imageUrl.length() == 0) return;
        Picasso.with(imageView.getContext()).load(imageUrl).into(imageView);
    }

    @BindingAdapter(value = {IMAGE_DRAWABLE, IMAGE_WITH_CACHE})
    public static void loadImageFromDrawable(ImageView imageView, @IdRes int imageDrawable, boolean withCache) {
        if (imageView == null || imageDrawable == 0) return;

        final RequestCreator rc = Picasso.with(null).load(imageDrawable);
        if (!withCache) rc.memoryPolicy(NO_CACHE, NO_STORE);
        rc.into(imageView);
    }

    @BindingAdapter(value = {IMAGE_URL, IMAGE_PLACEHOLDER})
    public static void loadImageWithPlaceholder(ImageView imageView, String imageUrl, Drawable placeholderDrawable) {
        if (imageView != null) Picasso.with(imageView.getContext()).load(imageUrl).placeholder(placeholderDrawable)
                .error(placeholderDrawable).into(imageView);
    }

    @BindingAdapter(value = {IMAGE_URL, IMAGE_COLOR_PLACEHOLDER})
    public static void loadImageWithColorPlaceholder(final ImageView imageView, String imageUrl, int colorPlaceholderDrawable) {
        if (imageView == null || imageUrl == null || imageUrl.length() == 0) return;
        Picasso.with(imageView.getContext()).load(imageUrl).placeholder(new ColorDrawable(colorPlaceholderDrawable)).into(imageView);
    }

    @BindingAdapter(value = {IMAGE_URL, IMAGE_DRAWABLE_PLACEHOLDER})
    public static void loadImageWithDrawablePlaceholder(final ImageView imageView, String imageUrl, Drawable drawablePlaceholderDrawable) {
        if (imageView == null || imageUrl == null || imageUrl.length() == 0) return;
        Picasso.with(imageView.getContext()).load(imageUrl)
                .placeholder(drawablePlaceholderDrawable)
                .error(drawablePlaceholderDrawable)
                .into(imageView);
    }

    @BindingAdapter(VIEW_HIDE)
    public static void hide(View view, boolean hide) {
        if (view == null) return;
        view.setVisibility(hide ? View.GONE : View.VISIBLE);
    }
}
