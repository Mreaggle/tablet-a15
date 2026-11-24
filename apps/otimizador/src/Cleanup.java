package org.tableta15.otimizador;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.VpnService;
import android.os.Process;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class Cleanup {
    static final long MIB = 1024L * 1024L;
    private static final long LOW_SPACE = 256L * MIB;
    private static final long WORKING_SPACE = 16L * MIB;
    private static final int SYSTEM_FLAGS = ApplicationInfo.FLAG_SYSTEM
            | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP | ApplicationInfo.FLAG_PERSISTENT;

    interface Progress {
        void update(int completed, int total, String message);
    }

    static final class Result {
        long ramBefore;
        long ramAfter;
        long storageAvailable;
        long temporaryBytes;
        int temporaryFiles;
        int requests;
        boolean incomplete;
        boolean lowStorage;
        boolean storageRecoveryRequested;
    }

    private final Context context;

    Cleanup(Context context) {
        this.context = context;
    }

    Result run(Progress progress) {
        Result result = new Result();
        result.ramBefore = readAvailableMemory();
        ActivityManager manager = context.getSystemService(ActivityManager.class);
        List<String> targets = getTargetPackages();
        int work = targets.size() + 2;

        for (int index = 0; index < targets.size(); index++) {
            progress.update(index, work, "Encerrando apps em segundo plano…");
            try {
                manager.killBackgroundProcesses(targets.get(index));
                result.requests++;
            } catch (SecurityException error) {
                result.incomplete = true;
            }
        }

        progress.update(targets.size(), work, "Limpando arquivos temporários…");
        cleanCache(context.getCacheDir(), result);
        cleanCache(context.getExternalCacheDir(), result);

        progress.update(targets.size() + 1, work, "Verificando o espaço livre…");
        maintainStorage(result);
        result.ramAfter = readAvailableMemory();
        return result;
    }

    private List<String> getTargetPackages() {
        PackageManager packages = context.getPackageManager();
        List<ApplicationInfo> apps = packages.getInstalledApplications(0);
        Set<String> exceptions = getEssentialPackages(packages);
        Set<Integer> protectedUids = new HashSet<>();
        for (ApplicationInfo app : apps) {
            if (app.uid < Process.FIRST_APPLICATION_UID || (app.flags & SYSTEM_FLAGS) != 0
                    || exceptions.contains(app.packageName)) {
                protectedUids.add(app.uid);
            }
        }

        List<String> targets = new ArrayList<>();
        for (ApplicationInfo app : apps) {
            if (app.enabled && (app.flags & ApplicationInfo.FLAG_STOPPED) == 0
                    && !protectedUids.contains(app.uid)) {
                targets.add(app.packageName);
            }
        }
        return targets;
    }

    private Set<String> getEssentialPackages(PackageManager packages) {
        Set<String> exceptions = new HashSet<>();
        exceptions.add(context.getPackageName());

        String input = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.DEFAULT_INPUT_METHOD);
        ComponentName keyboard = input == null ? null : ComponentName.unflattenFromString(input);
        if (keyboard != null) exceptions.add(keyboard.getPackageName());

        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        for (ResolveInfo app : packages.queryIntentActivities(home, 0)) {
            exceptions.add(app.activityInfo.packageName);
        }

        for (ResolveInfo service : packages.queryIntentServices(
                new Intent(VpnService.SERVICE_INTERFACE), 0)) {
            exceptions.add(service.serviceInfo.packageName);
        }

        AccessibilityManager access = context.getSystemService(AccessibilityManager.class);
        for (AccessibilityServiceInfo service : access.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
            exceptions.add(service.getResolveInfo().serviceInfo.packageName);
        }
        return exceptions;
    }

    private void cleanCache(File directory, Result result) {
        if (directory == null || Files.isSymbolicLink(directory.toPath())) return;
        File[] files = directory.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (!Files.isSymbolicLink(file.toPath()) && file.isDirectory()) {
                cleanCache(file, result);
                file.delete();
            } else {
                long bytes = Files.isSymbolicLink(file.toPath()) ? 0 : file.length();
                if (file.delete()) {
                    result.temporaryFiles++;
                    result.temporaryBytes += bytes;
                } else {
                    result.incomplete = true;
                }
            }
        }
    }

    private void maintainStorage(Result result) {
        File internal = context.getFilesDir();
        result.storageAvailable = internal.getUsableSpace();
        if (result.storageAvailable == 0) {
            result.incomplete = true;
            return;
        }
        if (result.storageAvailable < LOW_SPACE) {
            StorageManager storage = context.getSystemService(StorageManager.class);
            try {
                UUID volume = storage.getUuidForPath(internal);
                long bytes = Math.min(WORKING_SPACE, storage.getAllocatableBytes(volume));
                if (bytes > 0) {
                    result.storageRecoveryRequested = true;
                    storage.allocateBytes(volume, bytes);
                }
            } catch (IOException | SecurityException error) {
                result.incomplete = true;
            }
            result.storageAvailable = internal.getUsableSpace();
        }
        result.lowStorage = result.storageAvailable < LOW_SPACE;
    }

    private static long readAvailableMemory() {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/meminfo"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("MemAvailable:")) {
                    return Long.parseLong(line.substring(13).trim().split("\\s+")[0]) * 1024L;
                }
            }
        } catch (IOException | NumberFormatException error) {
            return -1;
        }
        return -1;
    }
}
