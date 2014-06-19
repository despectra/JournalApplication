package com.despectra.android.journal.logic.queries;

import com.despectra.android.journal.logic.queries.common.DelegatingInterface;
import com.despectra.android.journal.logic.queries.common.QueryExecDelegate;

import java.util.Map;

/**
 * Created by Dmitry on 19.06.14.
 */
public class Schedule extends QueryExecDelegate {
    public Schedule(DelegatingInterface holderInterface, Map<String, Object> configs) {
        super(holderInterface, configs);
    }
}
