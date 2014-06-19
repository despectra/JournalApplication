package com.despectra.android.journal.logic.queries.common;

import com.despectra.android.journal.logic.queries.*;

import java.util.Map;

/**
 * Created by Dmitry on 02.06.14.
 */
public interface QueryExecutor {
    public Events forEvents(Map<String, Object> configs);
    public Groups forGroups(Map<String, Object> configs);
    public Students forStudents(Map<String, Object> configs);
    public Subjects forSubjects(Map<String, Object> configs);
    public Teachers forTeachers(Map<String, Object> configs);
    public Schedule forSchedule(Map<String, Object> configs);
}
