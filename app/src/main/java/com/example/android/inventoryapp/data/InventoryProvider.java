package com.example.android.inventoryapp.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.example.android.inventoryapp.data.InventoryContract.ItemsEntry;

/**
 * {@link ContentProvider} for Inventory app.
 */
public class InventoryProvider extends ContentProvider {

    /**
     * Tag for the log messages
     */
    public static final String LOG_TAG = InventoryProvider.class.getSimpleName();

    /**
     * URI matcher code for the content URI for the items table
     */
    private static final int ITEMS = 100;

    /**
     * URI matcher code for the content URI for a single item in the items table
     */
    private static final int ITEM_ID = 101;

    /**
     * Quantity to be used when null is found
     */
    private static final int DEFAULT_QUANTITY = 0;

    /**
     * Price to be used when null is found
     */
    private static final int DEFAULT_PRICE = 0;

    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {

        // The calls to addURI() go here, for all of the content URI patterns that the provider
        // should recognize. All paths added to the UriMatcher have a corresponding code to return
        // when a match is found.

        // The content URI of the form "content://com.example.android.items/items" will map to the
        // integer code {@link #ITEMS}. This URI is used to provide access to MULTIPLE rows
        // of the items table.
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_ITEMS, ITEMS);

        // The content URI of the form "content://com.example.android.items/items/#" will map to the
        // integer code {@link #ITEM_ID}. This URI is used to provide access to ONE single row
        // of the items table.
        // In this case, the "#" wildcard is used where "#" can be substituted for an integer.
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_ITEMS + "/#", ITEM_ID);

    }

    /**
     * Database helper object
     */
    private InventoryDbHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new InventoryDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                // For the ITEMS code, query the items table
                cursor = database.query(ItemsEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case ITEM_ID:
                // For the ITEM_ID code, extract out the ID from the URI.
                selection = ItemsEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                cursor = database.query(ItemsEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Set notification URI on the Cursor,
        // so we know what content URI the Cursor was created for.
        // If the data at this URI changes, then we know we need to update the Cursor.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        // Return the cursor
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                return insertItem(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Insert an item into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */
    private Uri insertItem(Uri uri, ContentValues values) {
        // Check that the name is not null
        String name = values.getAsString(ItemsEntry.COLUMN_ITEM_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Item requires a name");
        }

        // Check that the quantity is valid
        Integer quantity = values.getAsInteger(ItemsEntry.COLUMN_ITEM_QUANTITY);
        if (quantity == null) {
            values.remove(ItemsEntry.COLUMN_ITEM_QUANTITY);
            values.put(ItemsEntry.COLUMN_ITEM_QUANTITY, DEFAULT_QUANTITY);
        } else if (!ItemsEntry.isValidQuantity(quantity)) {
            throw new IllegalArgumentException("Item requires valid quantity");
        }

        // Check that the price is valid
        Integer price = values.getAsInteger(ItemsEntry.COLUMN_ITEM_PRICE);
        if (price == null) {
            values.remove(ItemsEntry.COLUMN_ITEM_PRICE);
            values.put(ItemsEntry.COLUMN_ITEM_PRICE, DEFAULT_PRICE);
        } else if (!ItemsEntry.isValidPrice(price)) {
            throw new IllegalArgumentException("Item requires valid price");
        }

        // Check that the supplier email is valid
        String supplier = values.getAsString(ItemsEntry.COLUMN_ITEM_SUPPLIER);
        if ((supplier != null) && (!ItemsEntry.isValidSupplier(supplier))) {
            throw new IllegalArgumentException("Item requires valid supplier email");
        }

        // No need to check the image, any value is valid (including null).

        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Insert the new item with the given values
        long id = database.insert(ItemsEntry.TABLE_NAME, null, values);
        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Notify all listeners that the data has changed for the item content URI
        getContext().getContentResolver().notifyChange(uri, null);

        // Return the new URI with the ID (of the newly inserted row) appended at the end
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection,
                      String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                return updateItem(uri, contentValues, selection, selectionArgs);
            case ITEM_ID:
                selection = ItemsEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateItem(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Update items in the database with the given content values.
     * Return the number of rows that were successfully updated.
     */
    private int updateItem(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // If the {@link ItemsEntry#COLUMN_ITEM_NAME} key is present,
        // check that the name value is not null.
        if (values.containsKey(ItemsEntry.COLUMN_ITEM_NAME)) {
            String name = values.getAsString(ItemsEntry.COLUMN_ITEM_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Pet requires a name");
            }
        }

        // If the {@link ItemsEntry#COLUMN_ITEM_QUANTITY} key is present,
        // check that the quantity value is valid.
        if (values.containsKey(ItemsEntry.COLUMN_ITEM_QUANTITY)) {
            Integer quantity = values.getAsInteger(ItemsEntry.COLUMN_ITEM_QUANTITY);
            if (quantity == null) {
                values.remove(ItemsEntry.COLUMN_ITEM_QUANTITY);
                values.put(ItemsEntry.COLUMN_ITEM_QUANTITY, DEFAULT_QUANTITY);
            } else if (!ItemsEntry.isValidQuantity(quantity)) {
                throw new IllegalArgumentException("Item requires valid quantity");
            }
        }

        // If the {@link ItemsEntry#COLUMN_ITEM_PRICE} key is present,
        // check that the price value is valid.
        if (values.containsKey(ItemsEntry.COLUMN_ITEM_PRICE)) {
            Integer price = values.getAsInteger(ItemsEntry.COLUMN_ITEM_PRICE);
            if (price == null) {
                values.remove(ItemsEntry.COLUMN_ITEM_PRICE);
                values.put(ItemsEntry.COLUMN_ITEM_PRICE, DEFAULT_PRICE);
            } else if (!ItemsEntry.isValidPrice(price)) {
                throw new IllegalArgumentException("Item requires valid price");
            }
        }

        // If the {@link ItemsEntry#COLUMN_ITEM_SUPPLIER} key is present,
        // check that the supplier email is valid.
        if (values.containsKey(ItemsEntry.COLUMN_ITEM_SUPPLIER)) {
            String supplier = values.getAsString(ItemsEntry.COLUMN_ITEM_SUPPLIER);
            if ((supplier != null) && (!ItemsEntry.isValidSupplier(supplier))) {
                throw new IllegalArgumentException("Item requires valid supplier email");
            }
        }

        // No need to check the image, any value is valid (including null).

        // If there are no values to update, then don't try to update the database
        if (values.size() == 0) {
            return 0;
        }

        // Otherwise, get writeable database to update the data
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Perform the update on the database and get the number of rows affected
        int rowsUpdated = database.update(ItemsEntry.TABLE_NAME, values, selection, selectionArgs);

        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows updated
        return rowsUpdated;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Track the number of rows that were deleted
        int rowsDeleted;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                // Delete all rows that match the selection and selection args
                rowsDeleted = database.delete(ItemsEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case ITEM_ID:
                // Delete a single row given by the ID in the URI
                selection = ItemsEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(ItemsEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        // If 1 or more rows were deleted, then notify all listeners that the data at the
        // given URI has changed
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows deleted
        return rowsDeleted;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                return ItemsEntry.CONTENT_LIST_TYPE;
            case ITEM_ID:
                return ItemsEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }
}
