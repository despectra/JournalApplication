package com.despectra.android.journal.logic.queries;

import android.content.ContentValues;
import android.database.Cursor;
import com.despectra.android.journal.logic.helper.ApiAction;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.logic.local.LocalStorageManager;
import com.despectra.android.journal.logic.local.TableModel;
import com.despectra.android.journal.logic.queries.common.DelegatingInterface;
import com.despectra.android.journal.logic.queries.common.QueryExecDelegate;
import com.despectra.android.journal.utils.JSONBuilder;
import com.despectra.android.journal.utils.Utils;
import com.despectra.android.journal.logic.local.Contract.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

/**
 * Created by Dmitry on 19.06.14.
 */
public class Schedule extends QueryExecDelegate {
    public Schedule(DelegatingInterface holderInterface, Map<String, Object> configs) {
        super(holderInterface, configs);
        mTable = TableModel.getTable(Contract.Schedule.TABLE);
    }

    /**********************   RETRIEVING    ********************************/

    public JSONObject getWeekScheduleForGroup(ApiAction action) throws Exception {
        String localGroupId = action.actionData.getString("LOCAL_group_id");
        action.actionData.remove("LOCAL_group_id");
        JSONObject response = getApplicationServer().executeGetApiQuery(action);
        if (Utils.isApiJsonSuccess(response)) {
            updateLocalWeekGroupSchedule(localGroupId, response);
            getLocalStorageManager().notifyUriForClients(Contract.Schedule.URI_FULL, action, "WeekScheduleFragment");
        }

        return response;
    }

    private void updateLocalWeekGroupSchedule(String localGroupId, JSONObject response) throws Exception {
        Cursor localSchedule = getLocalStorageManager().getResolver().query(
                Contract.Schedule.URI_TSG,
                new String[]{Contract.Schedule._ID, Contract.Schedule.REMOTE_ID},
                TSG.FIELD_GROUP_ID + " = ?",
                new String[]{localGroupId},
                null);
        JSONArray remoteSchedule = response.getJSONArray("schedule");
        for (int i = 0; i < remoteSchedule.length(); i++) {
            JSONObject item = remoteSchedule.getJSONObject(i);
            item.put("color", Utils.getRandomHoloColor(getContext()));
            item.put("teacher_subject_group_id",
                    getLocalStorageManager().getLocalIdByRemote(
                            TSG.HOLDER,
                            item.getLong("teacher_subject_group_id")));
        }
        LocalStorageManager.PreCallbacks callback = new LocalStorageManager.PreCallbacksAdapter(){
            @Override
            public boolean onPreUpdate(EntityTable table, long localId, ContentValues toUpdate) {
                if (table == Contract.Schedule.HOLDER) {
                    toUpdate.remove(Contract.Schedule.FIELD_COLOR);
                }
                return true;
            }
        };
        getLocalStorageManager().updateComplexEntityWithJsonResponse(LocalStorageManager.MODE_REPLACE,
                localSchedule,
                mTable,
                remoteSchedule,
                callback
        );
    }

    /*****************   ADDITION   *********************/

    public JSONObject addScheduleItem(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        int day = request.getInt("day");
        int lessonNum = request.getInt("lesson_number");
        int color = request.getInt("color");
        long localTSGId = request.getLong("LOCAL_teacher_subject_group_id");
        request.remove("color");
        request.remove("LOCAL_teacher_subject_group_id");
        long localScheduleItemId = getLocalStorageManager().preInsertEntity(mTable,
                new JSONBuilder()
                    .addKeyValue("day", day)
                    .addKeyValue("teacher_subject_group_id", localTSGId)
                    .addKeyValue("lesson_number", lessonNum)
                    .addKeyValue("color", color)
                    .create());
        getLocalStorageManager().notifyUri(Contract.Schedule.URI_FULL);
        JSONObject response = getApplicationServer().executeGetApiQuery(action);
        if (Utils.isApiJsonSuccess(response)) {
            //commit
            getLocalStorageManager().commitInsertingEntity(mTable, localScheduleItemId, response);
        } else {
            getLocalStorageManager().rollbackInsertingEntity(mTable, localScheduleItemId);
        }
        getLocalStorageManager().notifyUri(Contract.Schedule.URI_FULL);
        return response;
    }
}
