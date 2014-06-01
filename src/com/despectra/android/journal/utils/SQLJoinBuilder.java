package com.despectra.android.journal.utils;

/**
 * Created by Dmitry on 29.05.14.
 */
public class SQLJoinBuilder {
    private String mJoinString;
    public SQLJoinBuilder(String initTable) {
        mJoinString = new String();
        mJoinString += initTable;
    }

    public SQLJoinBuilder join(String table) {
        mJoinString += " JOIN " + table;
        return this;
    }

    public SQLJoinBuilder on(String predicate) {
        mJoinString += " ON " + predicate;
        return this;
    }

    public SQLJoinBuilder onEq(String column1, String column2) {
        mJoinString += " ON " + column1 + " = " + column2;
        return this;
    }

    public String create() {
        return mJoinString;
    }
}
