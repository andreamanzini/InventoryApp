package com.example.android.inventoryapp.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public final class InventoryContract {

    public static final String CONTENT_AUTHORITY = "com.example.android.inventory";
    /**
     * Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
     * the content provider.
     */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    /**
     * Possible path (appended to base content URI for possible URI's)
     */
    public static final String PATH_ITEMS = "items";

    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    private InventoryContract() {
    }

    /**
     * Inner class that defines constant values for the items database table.
     * Each entry in the table represents a single item.
     */
    public static final class ItemsEntry implements BaseColumns {

        /**
         * The content URI to access the invenotry data in the provider
         */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_ITEMS);

        /**
         * The MIME type of the {@link #CONTENT_URI} for a list of items.
         */
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_ITEMS;

        /**
         * The MIME type of the {@link #CONTENT_URI} for a single item.
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_ITEMS;

        /**
         * Name of database table for items
         */
        public final static String TABLE_NAME = "items";

        /**
         * Unique ID number for the item (only for use in the database table).
         * <p>
         * Type: INTEGER
         */
        public final static String _ID = BaseColumns._ID;

        /**
         * Name of the item.
         * <p>
         * Type: TEXT
         */
        public final static String COLUMN_ITEM_NAME = "name";

        /**
         * Price of the item
         * <p>
         * Type: INTEGER
         */
        public final static String COLUMN_ITEM_PRICE = "price";

        /**
         * Quantity of the item available.
         * <p>
         * Type: INTEGER
         */
        public final static String COLUMN_ITEM_QUANTITY = "quantity";

        /**
         * Supplier email address.
         * <p>
         * Type: TEXT
         */
        public final static String COLUMN_ITEM_SUPPLIER = "supplier";

        /**
         * Image of the item
         * <p>
         * Type: BLOB
         */
        public final static String COLUMN_ITEM_IMAGE = "image";

        /**
         * Returns whether or not the given quantity is valid
         */
        public static boolean isValidQuantity(int quantity) {
            return (quantity >= 0);
        }

        /**
         * Return whether or not the given price is valid
         *
         * @param price is stored in cents
         */
        public static boolean isValidPrice(int price) {
            return (price >= 0);
        }

        /**
         * Returns whether or not the given supplier email is valid
         */
        public static boolean isValidSupplier(String supplier) {
            return (supplier.contains("@"));
        }

    }

}
