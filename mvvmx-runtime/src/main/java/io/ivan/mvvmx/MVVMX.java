package io.ivan.mvvmx;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.lang.reflect.Constructor;

public final class MVVMX {

    public static void bind(@NonNull FragmentActivity activity) {
        String classFullName = activity.getClass().getName() + "_ViewBinding";
        try {
            Class<?> clz = Class.forName(classFullName);
            Constructor<?> constructor = clz.getConstructor(activity.getClass());
            constructor.newInstance(activity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void bind(@NonNull Fragment fragment, LayoutInflater inflater, @Nullable ViewGroup container) {
        String classFullName = fragment.getClass().getName() + "_ViewBinding";
        try {
            Class<?> clz = Class.forName(classFullName);
            Constructor<?> constructor = clz.getConstructor(fragment.getClass(), LayoutInflater.class, ViewGroup.class);
            constructor.newInstance(fragment, inflater, container);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
