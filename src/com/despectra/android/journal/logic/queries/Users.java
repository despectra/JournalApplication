package com.despectra.android.journal.logic.queries;

import android.content.ContentValues;
import android.database.Cursor;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.logic.queries.common.DelegatingInterface;
import org.json.JSONObject;

/**
 * Created by Dmitry on 08.06.14.
 */
public class Users {
    public static long preAddUser(DelegatingInterface delegatingInterface, JSONObject userData, int userLevel) throws Exception {
        String login = userData.getString("login");
        Cursor loginCheckData = delegatingInterface.getLocalStorageManager().getResolver().query(
                Contract.Users.URI,
                new String[]{Contract.Users._ID},
                Contract.Users.FIELD_LOGIN + " = ?",
                new String[]{userData.getString("login")},
                null
        );
        if (loginCheckData.getCount() > 0) {
            throw new Exception("Пользователь с логином" + login + " уже существует");
        }
        ContentValues tempUser = new ContentValues();
        tempUser.put(Contract.Users.FIELD_LOGIN, login);
        tempUser.put(Contract.Users.FIELD_NAME, userData.getString("name"));
        tempUser.put(Contract.Users.FIELD_MIDDLENAME, userData.getString("middlename"));
        tempUser.put(Contract.Users.FIELD_SURNAME, userData.getString("surname"));
        tempUser.put(Contract.Users.FIELD_LEVEL, userLevel);
        return delegatingInterface.getLocalStorageManager().insertTempEntity(Contract.Users.HOLDER, tempUser);
    }

    public static void commitAddUser(DelegatingInterface delegatingInterface, long localUserId, long remoteUserId) {
        delegatingInterface.getLocalStorageManager().persistTempEntity(Contract.Users.HOLDER, localUserId, remoteUserId);
    }

    public static void rollbackAddUser(DelegatingInterface delegatingInterface, long localUserId) {
        delegatingInterface.getLocalStorageManager().deleteEntityByLocalId(Contract.Users.HOLDER, localUserId);
    }

    public static void preDeleteUser(DelegatingInterface delegatingInterface, long localUserId) {
        delegatingInterface.getLocalStorageManager().markEntityAsDeleting(Contract.Users.HOLDER, localUserId);
    }

    public static void commitDeleteUser(DelegatingInterface delegatingInterface, long localUserId) {
        delegatingInterface.getLocalStorageManager().deleteEntityByLocalId(Contract.Users.HOLDER, localUserId);
    }

    public static void rollbackDeleteUser(DelegatingInterface delegatingInterface, long localUserId) {
        delegatingInterface.getLocalStorageManager().markEntityAsIdle(Contract.Users.HOLDER, localUserId);
    }
}
