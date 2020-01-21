package com.example.android.inventoryapp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventoryapp.data.InventoryContract.ItemsEntry;

import java.text.DecimalFormat;

/**
 * {@link InventoryCursorAdapter} is an adapter for a list or grid view
 * that uses a {@link Cursor} of item data as its data source. This adapter knows
 * how to create list items for each row of item data in the {@link Cursor}.
 */
public class InventoryCursorAdapter extends CursorAdapter {

    private static final String CURRENCY = "€";

    /**
     * Constructs a new {@link InventoryCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public InventoryCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    public static String formatPrice(int cents) {

        double eur = ((double) cents) / 100;
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
        return decimalFormat.format(eur);

    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Inflate a list item view using the layout specified in list_item.xml
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the item data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current item can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        // Find the sale Button
        Button button = (Button) view.findViewById(R.id.sale_button);
        // Get cursor current position and set it as Button Tag
        button.setTag(cursor.getPosition());
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = v.getContext();
                Integer position = (Integer) v.getTag();
                sellItem(context, position);
            }
        });

        // Find individual views that we want to modify in the list item layout
        TextView nameTextView = (TextView) view.findViewById(R.id.name);
        TextView quantityTextView = (TextView) view.findViewById(R.id.quantity);
        TextView priceTextView = (TextView) view.findViewById(R.id.price);

        // Find the columns of item attributes that we're interested in
        //int id = cursor.getColumnIndex(ItemsEntry._ID);
        int nameColumnIndex = cursor.getColumnIndex(ItemsEntry.COLUMN_ITEM_NAME);
        int quantityColumnIndex = cursor.getColumnIndex(ItemsEntry.COLUMN_ITEM_QUANTITY);
        int priceColumnIndex = cursor.getColumnIndex(ItemsEntry.COLUMN_ITEM_PRICE);

        // Read the item attributes from the Cursor for the current item
        String itemName = cursor.getString(nameColumnIndex);
        String itemQuantity = String.valueOf(cursor.getInt(quantityColumnIndex));
        String itemPrice = formatPrice(cursor.getInt(priceColumnIndex)) + CURRENCY;

        // Update the TextViews with the attributes for the current item
        nameTextView.setText(itemName);
        quantityTextView.setText(itemQuantity);
        priceTextView.setText(itemPrice);

        // Set the cursor as the tag of the view
        view.setTag(cursor);
    }

    private void sellItem(Context context, int position) {

        // Get the cursor and move to position stored in view's tag
        Cursor cursor = getCursor();
        cursor.moveToPosition(position);

        // Obtain old quantity
        int idColumnIndex = cursor.getColumnIndex(ItemsEntry._ID);
        int id = cursor.getInt(idColumnIndex);
        int quantityColumnIndex = cursor.getColumnIndex(ItemsEntry.COLUMN_ITEM_QUANTITY);
        int itemQuantity = cursor.getInt(quantityColumnIndex);


        // Decrease itemQuantity
        if (itemQuantity > 0) {
            itemQuantity -= 1;

            // Build the Uri to the specific decreased item
            Uri currentItemUri = ContentUris.withAppendedId(ItemsEntry.CONTENT_URI, id);

            // Update quantity value in database
            ContentValues values = new ContentValues();
            values.put(ItemsEntry.COLUMN_ITEM_QUANTITY, itemQuantity);
            context.getContentResolver().update(currentItemUri, values, null, null);

        } else {
            Toast.makeText(context, "Item terminated", Toast.LENGTH_SHORT).show();
        }
    }
}
