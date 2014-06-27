package com.despectra.android.journal.logic.queries;

import android.database.Cursor;
import com.despectra.android.journal.logic.helper.ApiAction;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.logic.local.TableModel;
import com.despectra.android.journal.logic.queries.common.DelegatingInterface;
import com.despectra.android.journal.logic.queries.common.QueryExecDelegate;
import com.despectra.android.journal.utils.Utils;
import org.json.JSONObject;

import java.util.Map;

/**
 * Created by Андрей on 27.06.14.
 */
public class Lessons extends QueryExecDelegate {

    public static final int GET_BY_TSG_ON_INTERVAL = 0;

    private Contract.EntityTable mTable;

    public Lessons(DelegatingInterface holderInterface, Map<String, Object> configs) {
        super(holderInterface, configs);
        mTable = TableModel.getTable(Contract.Lessons.TABLE);
    }

    public JSONObject getByTeacherSubjectGroupOnInterval(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        long localTSGId = request.getLong("LOCAL_teacher_subject_group_id");
        request.remove("LOCAL_teacher_subject_group_id");
        String fromDay = request.getString("from_day");
        String toDay = request.getString("to_day");

        JSONObject response = null;//TODO server request
        if (Utils.isApiJsonSuccess(response)) {
            updateLessons(response, GET_BY_TSG_ON_INTERVAL);
        }
        return response;
    }

    private void updateLessons(JSONObject response, int selectionMode) {
        Cursor existingLessonsIds;
        switch (selectionMode) {
            case GET_BY_TSG_ON_INTERVAL:
                existingLessonsIds = getLocalStorageManager().getResolver().query()
                break;
            default:
                return;
        }
    }
}
