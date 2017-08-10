package com.github.calendar.content;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.CalendarContract;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.github.calendar.CalendarUtils;

public abstract class EventsQueryHandler extends AsyncQueryHandler {

    private static final String SORT = CalendarContract.Events.DTSTART + " ASC";
    private static final String AND = " AND ";
    private static final String OR = " OR ";
    private static final String INT_TRUE = "1";
    private static final String INT_FALSE = "0";
    private static final String ALL_DAY = CalendarContract.Events.ALL_DAY + "=?";
    private static final String DELETED = CalendarContract.Events.DELETED + "=?";
    private static final String NOT_CALENDAR_ID = CalendarContract.Events.CALENDAR_ID + "!=?";
    private static final String START_WITHIN = "(" +
            CalendarContract.Events.DTSTART + ">=?" + AND +
            CalendarContract.Events.DTSTART + "<?" +
            ")";

    private static final String START_BEF_END_WITHIN_AFTER = "(" +
            CalendarContract.Events.DTSTART + "<?" + AND +
            CalendarContract.Events.DTEND + ">?" +
            ")";

    private static final String SELECTION_NON_ALL_DAY_EVENTS = "(" +
            ALL_DAY + AND +
            "(" + START_WITHIN + OR + START_BEF_END_WITHIN_AFTER + ")" +
            ")";

    private static final String SELECTION_ALL_DAY_EVENTS = "(" +
            ALL_DAY + AND +
            "(" + START_WITHIN + OR + START_BEF_END_WITHIN_AFTER + ")" +
            ")";

    private static final String SELECTION = "(" +
            DELETED + AND + "(" + SELECTION_NON_ALL_DAY_EVENTS + OR + SELECTION_ALL_DAY_EVENTS + ")" +
            ")";

    private final Collection<String> mExcludedCalendarIds;

    public EventsQueryHandler(ContentResolver cr,
                              Collection<String> excludedCalendarIds) {
        super(cr);
        mExcludedCalendarIds = excludedCalendarIds;
    }

    public final void startQuery(Object cookie, long startTimeMillis, long endTimeMillis) {
        final String utcStart = String.valueOf(CalendarUtils.toUtcTimeZone(startTimeMillis)),
                utcEnd = String.valueOf(CalendarUtils.toUtcTimeZone(endTimeMillis)),
                localStart = String.valueOf(startTimeMillis),
                localEnd = String.valueOf(endTimeMillis);
        List<String> args = new ArrayList<String>() {{
            add(INT_FALSE);
            add(INT_FALSE);
            add(localStart);
            add(localEnd);
            add(localStart);
            add(localStart);
            add(INT_TRUE);
            add(utcStart);
            add(utcEnd);
            add(utcStart);
            add(utcStart);
        }};
        StringBuilder sb = new StringBuilder(SELECTION);
        if (!mExcludedCalendarIds.isEmpty()) {
            Iterator<String> iterator = mExcludedCalendarIds.iterator();
            sb.append(AND).append("(");
            while (iterator.hasNext()) {
                args.add(iterator.next());
                sb.append(NOT_CALENDAR_ID);
                if (iterator.hasNext()) {
                    sb.append(AND);
                }
            }
            sb.append(")");
        }
        startQuery(0, cookie, CalendarContract.Events.CONTENT_URI,
                EventCursor.PROJECTION, sb.toString(), args.toArray(new String[args.size()]), SORT);
    }

    @Override
    protected final void onQueryComplete(int token, Object cookie, Cursor cursor) {
        handleQueryComplete(token, cookie, new EventCursor(cursor));
    }

    protected abstract void handleQueryComplete(int token, Object cookie, EventCursor cursor);
}
