package com.despectra.android.journal.logic.queries.common;

import com.despectra.android.journal.logic.queries.*;

/**
 * Created by Dmitry on 02.06.14.
 */
public interface QueryExecutor {
    public Events forEvents();
    public Groups forGroups();
    public Students forStudents();
    public Subjects forSubjects();
    public Teachers forTeachers();
}
