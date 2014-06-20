package com.despectra.android.journal.logic.queries;

import android.content.ContentValues;
import android.database.Cursor;
import com.despectra.android.journal.logic.helper.ApiAction;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.logic.local.LocalStorageManager;
import com.despectra.android.journal.logic.queries.common.DelegatingInterface;
import com.despectra.android.journal.logic.queries.common.QueryExecDelegate;
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
        JSONArray scheduleItems = response.getJSONArray("schedule");
        for (int i = 0; i < scheduleItems.length(); i++) {
            JSONObject item = scheduleItems.getJSONObject(i);
            item.put("tsg", getLocalStorageManager().getLocalIdByRemote(TSG.HOLDER, item.getLong("tsg")));
            item.put("color", Utils.getRandomHoloColor(getContext()));
        }
        getLocalStorageManager().updateEntityWithJSONArray(LocalStorageManager.MODE_REPLACE,
                localSchedule,
                Contract.Schedule.HOLDER,
                response.getJSONArray("schedule"),
                "id",
                new String[]{"tsg", "day", "lesson_number", "color"},
                new String[]{Contract.Schedule.FIELD_TSG_ID, Contract.Schedule.FIELD_DAY,
                        Contract.Schedule.FIELD_LESSON_NUMBER, Contract.Schedule.FIELD_COLOR}
        );
    }

    /*****************   ADDITION   *********************/

    public JSONObject addScheduleItem(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        int day = request.getInt("day");
        int lessonNum = request.getInt("lesson_number");
        int color = request.getInt("color");
        request.remove("color");
        long localTSGId = request.getLong("teacher_subject_group_id");
        request.remove("teacher_subject_group_id");
        long localScheduleItemId = preAddScheduleItem(day, lessonNum, localTSGId, color);
        JSONObject response = getApplicationServer().executeGetApiQuery(action);
        if (Utils.isApiJsonSuccess(response)) {
            //commit
            commitAddScheduleItem(localScheduleItemId, response);
        } else {
            rollbackAddScheduleItem(localScheduleItemId);
        }
        return response;
    }

    private long preAddScheduleItem(int day, int lessonNumber, long localTSGId, int color) {
        ContentValues cv = new ContentValues();
        cv.put(Contract.Schedule.FIELD_DAY, day);
        cv.put(Contract.Schedule.FIELD_LESSON_NUMBER, lessonNumber);
        cv.put(Contract.Schedule.FIELD_TSG_ID, localTSGId);
        cv.put(Contract.Schedule.FIELD_COLOR, color);
        return getLocalStorageManager().insertTempEntity(Contract.Schedule.HOLDER, cv);
    }

    private void commitAddScheduleItem(long localScheduleItemId, JSONObject response) throws Exception {
        getLocalStorageManager().persistTempEntity(Contract.Schedule.HOLDER, localScheduleItemId, response.getLong("schedule_item_id"));
    }

    private void rollbackAddScheduleItem(long localScheduleItemId) {
        getLocalStorageManager().deleteEntityByLocalId(Contract.Schedule.HOLDER, localScheduleItemId);
    }

}
