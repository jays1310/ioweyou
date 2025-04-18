// ContactUtils.java
package com.example.ioweyou;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

public class ContactUtils {

    public static String getContactNumber(Context context, Uri contactUri) {
        String contactNumber = null;

        Cursor cursor = context.getContentResolver().query(
                contactUri,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {
            contactNumber = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            );
            cursor.close();
        }

        return contactNumber;
    }
}
