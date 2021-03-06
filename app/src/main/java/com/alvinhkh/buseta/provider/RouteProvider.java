package com.alvinhkh.buseta.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;

public class RouteProvider extends ContentProvider {

    private RouteOpenHelper mHelper;

    private static final String AUTHORITY = "com.alvinhkh.buseta.RouteProvider";
    private static final String BASE_PATH_STOP = "stop";
    public static final Uri CONTENT_URI_STOP = Uri.parse("content://" + AUTHORITY
            + "/" + BASE_PATH_STOP);
    private static final String BASE_PATH_STOP_FILTER = BASE_PATH_STOP + "/filter";
    public static final Uri CONTENT_URI_STOP_FILTER = Uri.parse("content://" + AUTHORITY
            + "/" + BASE_PATH_STOP_FILTER);
    private static final String BASE_PATH_BOUND = "bound";
    public static final Uri CONTENT_URI_BOUND = Uri.parse("content://" + AUTHORITY
            + "/" + BASE_PATH_BOUND);
    private static final String BASE_PATH_BOUND_FILTER = BASE_PATH_BOUND + "/filter";
    public static final Uri CONTENT_URI_BOUND_FILTER = Uri.parse("content://" + AUTHORITY
            + "/" + BASE_PATH_BOUND_FILTER);

    // used for the UriMatcher
    private static final int STOPS = 10;
    private static final int STOP_ID = 11;
    private static final int STOP_FILTER = 20;
    private static final int BOUNDS = 30;
    private static final int BOUND_ID = 31;
    private static final int BOUND_FILTER = 40;
    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_STOP, STOPS);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_STOP + "/#", STOP_ID);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_STOP_FILTER, STOP_FILTER);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_BOUND, BOUNDS);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_BOUND + "/#", BOUND_ID);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_BOUND_FILTER, BOUND_FILTER);
    }

    @Override
    public boolean onCreate() {
        mHelper = new RouteOpenHelper(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        // Uisng SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        // check if the caller has requested a column which does not exists
        checkColumns(projection);

        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
            case STOPS:
                // Set the table
                queryBuilder.setTables(RouteStopTable.TABLE_NAME);
                break;
            case STOP_ID:
                // Set the table
                queryBuilder.setTables(RouteStopTable.TABLE_NAME);
                // adding the ID to the original query
                queryBuilder.appendWhere(RouteStopTable.COLUMN_ID + "=" + uri.getLastPathSegment());
                break;
            case BOUNDS:
                // Set the table
                queryBuilder.setTables(RouteBoundTable.TABLE_NAME);
                break;
            case BOUND_ID:
                // Set the table
                queryBuilder.setTables(RouteBoundTable.TABLE_NAME);
                // adding the ID to the original query
                queryBuilder.appendWhere(RouteBoundTable.COLUMN_ID + "=" + uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = mHelper.getWritableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection,
                selectionArgs, null, null, sortOrder);
        // make sure that potential listeners are getting notified
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = mHelper.getWritableDatabase();
        long id;
        switch (uriType) {
            case STOPS:
                id = sqlDB.insert(RouteStopTable.TABLE_NAME, null, values);
                break;
            case BOUNDS:
                id = sqlDB.insert(RouteBoundTable.TABLE_NAME, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        switch (uriType) {
            case STOPS:
                return Uri.parse(BASE_PATH_STOP + "/#" + id);
            case BOUNDS:
                return Uri.parse(BASE_PATH_BOUND + "/#" + id);
            default:
                return null;
        }
    }

    @Override
    public int bulkInsert(Uri uri, @NonNull ContentValues[] values){
        int uriType = sURIMatcher.match(uri);
        int numInserted = 0;
        String tableName;
        switch (uriType) {
            case STOPS:
                tableName = RouteStopTable.TABLE_NAME;
                break;
            case BOUNDS:
                tableName = RouteBoundTable.TABLE_NAME;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        SQLiteDatabase sqlDB = mHelper.getWritableDatabase();
        sqlDB.beginTransaction();
        try {
            for (ContentValues cv : values) {
                long newID = sqlDB.insertOrThrow(tableName, null, cv);
                if (newID <= 0)
                    throw new SQLException("Failed to insert row into " + uri);
                numInserted++;
            }
            sqlDB.setTransactionSuccessful();
            getContext().getContentResolver().notifyChange(uri, null);
        } finally {
            sqlDB.endTransaction();
        }
        return numInserted;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = mHelper.getWritableDatabase();
        int rowsDeleted;
        switch (uriType) {
            case STOPS:
                rowsDeleted = sqlDB.delete(RouteStopTable.TABLE_NAME, selection,
                        selectionArgs);
                break;
            case STOP_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(RouteStopTable.TABLE_NAME,
                            RouteStopTable.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsDeleted = sqlDB.delete(RouteStopTable.TABLE_NAME,
                            RouteStopTable.COLUMN_ID + "=" + id
                                    + " and " + selection,
                            selectionArgs);
                }
                break;
            case STOP_FILTER:
                rowsDeleted = sqlDB.delete(RouteStopTable.TABLE_NAME, RouteStopTable.COLUMN_ID +
                                " IN ( " + "SELECT " + RouteStopTable.COLUMN_ID + " FROM " +
                                RouteStopTable.TABLE_NAME +
                                " LEFT JOIN " + FollowTable.TABLE_NAME +
                                " ON (" +
                                FollowTable.COLUMN_ROUTE + "=" + RouteStopTable.COLUMN_ROUTE + " AND " +
                                FollowTable.COLUMN_BOUND + "=" + RouteStopTable.COLUMN_BOUND +
                                ")" + " WHERE " + FollowTable.COLUMN_ID + " IS NULL)",
                        null);
                break;
            case BOUNDS:
                rowsDeleted = sqlDB.delete(RouteBoundTable.TABLE_NAME, selection,
                        selectionArgs);
                break;
            case BOUND_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(RouteBoundTable.TABLE_NAME,
                            RouteBoundTable.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsDeleted = sqlDB.delete(RouteBoundTable.TABLE_NAME,
                            RouteBoundTable.COLUMN_ID + "=" + id
                                    + " and " + selection,
                            selectionArgs);
                }
                break;
            case BOUND_FILTER:
                rowsDeleted = sqlDB.delete(RouteBoundTable.TABLE_NAME, RouteBoundTable.COLUMN_ID +
                                " IN ( " + "SELECT " + RouteBoundTable.COLUMN_ID + " FROM " +
                                RouteBoundTable.TABLE_NAME +
                                " LEFT JOIN " + FollowTable.TABLE_NAME +
                                " ON (" +
                                FollowTable.COLUMN_ROUTE + "=" + RouteBoundTable.COLUMN_ROUTE +
                                ")" + " WHERE " + FollowTable.COLUMN_ID + " IS NULL)",
                        null);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {

        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = mHelper.getWritableDatabase();
        int rowsUpdated;
        switch (uriType) {
            case STOPS:
                rowsUpdated = sqlDB.update(RouteStopTable.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);
                break;
            case STOP_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(RouteStopTable.TABLE_NAME,
                            values,
                            RouteStopTable.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsUpdated = sqlDB.update(RouteStopTable.TABLE_NAME,
                            values,
                            RouteStopTable.COLUMN_ID + "=" + id
                                    + " and "
                                    + selection,
                            selectionArgs);
                }
                break;
            case BOUNDS:
                rowsUpdated = sqlDB.update(RouteBoundTable.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);
                break;
            case BOUND_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(RouteBoundTable.TABLE_NAME,
                            values,
                            RouteBoundTable.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsUpdated = sqlDB.update(RouteBoundTable.TABLE_NAME,
                            values,
                            RouteBoundTable.COLUMN_ID + "=" + id
                                    + " and "
                                    + selection,
                            selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    private void checkColumns(String[] projection) {
        String[] available = {
                RouteStopTable.COLUMN_ID,
                RouteStopTable.COLUMN_DATE,
                RouteStopTable.COLUMN_ROUTE,
                RouteStopTable.COLUMN_BOUND,
                RouteStopTable.COLUMN_ORIGIN,
                RouteStopTable.COLUMN_ORIGIN_EN,
                RouteStopTable.COLUMN_DESTINATION,
                RouteStopTable.COLUMN_DESTINATION_EN,
                RouteStopTable.COLUMN_STOP_SEQ,
                RouteStopTable.COLUMN_STOP_CODE,
                RouteStopTable.COLUMN_STOP_NAME,
                RouteStopTable.COLUMN_STOP_NAME_EN,
                RouteStopTable.COLUMN_STOP_FARE,
                RouteStopTable.COLUMN_STOP_LAT,
                RouteStopTable.COLUMN_STOP_LONG,

                RouteBoundTable.COLUMN_ID,
                RouteBoundTable.COLUMN_DATE,
                RouteBoundTable.COLUMN_ROUTE,
                RouteBoundTable.COLUMN_BOUND,
                RouteBoundTable.COLUMN_ORIGIN,
                RouteBoundTable.COLUMN_ORIGIN_EN,
                RouteBoundTable.COLUMN_DESTINATION,
                RouteBoundTable.COLUMN_DESTINATION_EN,
        };
        if (projection != null) {
            HashSet<String> requestedColumns = new HashSet<>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<>(Arrays.asList(available));
            // check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in projection");
            }
        }
    }
}