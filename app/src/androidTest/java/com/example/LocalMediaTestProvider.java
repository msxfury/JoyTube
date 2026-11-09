package com.example;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.Bundle;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.io.FileOutputStream;
import java.io.InputStream;

/** Test-only provider exposing generated fixture files through content:// URIs. */
public final class LocalMediaTestProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }

    @Override public Bundle call(String method, String arg, Bundle extras) {
        if (!"create_fixture".equals(method) || extras == null) return super.call(method, arg, extras);
        String name = extras.getString("name");
        String kind = extras.getString("kind");
        File target = file(new Uri.Builder().appendPath(name).build());
        File parent = target.getParentFile();
        if (parent != null) parent.mkdirs();
        try (FileOutputStream output = new FileOutputStream(target)) {
            if ("mp4".equals(kind)) {
                try (InputStream input = safeContext().getAssets().open("joytube_fixture_2s.mp4")) {
                    byte[] buffer = new byte[128 * 1024];
                    int count;
                    while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
                }
            } else if ("large_mp4".equals(kind)) {
                byte[] block = new byte[128 * 1024];
                for (int i = 0; i < block.length; i++) block[i] = (byte) ((i * 31 + 17) & 0xff);
                byte[] header = new byte[]{0, 0, 0, 24, 0x66, 0x74, 0x79, 0x70, 0x69, 0x73, 0x6f, 0x6d};
                System.arraycopy(header, 0, block, 0, header.length);
                for (int i = 0; i < 512; i++) output.write(block);
            } else if ("empty_mp4".equals(kind)) {
                // Deliberately zero bytes.
            } else if ("forged_mp4".equals(kind)) {
                byte[] bytes = new byte[4096];
                java.util.Arrays.fill(bytes, (byte) 0x55);
                output.write(bytes);
            } else if ("unsupported".equals(kind)) {
                output.write(new byte[1024 * 1024]);
            }
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
        Bundle result = new Bundle();
        result.putString("name", name);
        result.putLong("size", target.length());
        return result;
    }

    private File file(Uri uri) {
        return new File(new File(safeContext().getCacheDir(), "joytube_test_media"), uri.getLastPathSegment());
    }

    @Override public String getType(Uri uri) {
        String name = file(uri).getName().toLowerCase();
        if (name.endsWith(".mp4") || name.endsWith(".m4v")) return "video/mp4";
        if (name.endsWith(".m4a")) return "audio/mp4";
        if (name.endsWith(".mp3")) return "audio/mpeg";
        if (name.endsWith(".mkv")) return "video/x-matroska";
        return "application/octet-stream";
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        File target = file(uri);
        String[] columns = projection == null ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE} : projection;
        MatrixCursor cursor = new MatrixCursor(columns);
        ArrayList<Object> row = new ArrayList<>();
        for (String column : columns) {
            if (OpenableColumns.DISPLAY_NAME.equals(column)) row.add(target.getName());
            else if (OpenableColumns.SIZE.equals(column)) row.add(target.length());
            else row.add(null);
        }
        cursor.addRow(row);
        return cursor;
    }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return ParcelFileDescriptor.open(file(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    private android.content.Context safeContext() {
        android.content.Context value = getContext();
        if (value == null) throw new IllegalStateException("Provider context is missing");
        return value;
    }

    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException("Read-only test provider"); }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { throw new UnsupportedOperationException("Read-only test provider"); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { throw new UnsupportedOperationException("Read-only test provider"); }
}
