package com.github.calendar.widget;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;

import com.github.calendar.CalendarUtils;
import com.github.calendar.content.EventCursor;

public class EventCalendarView extends ViewPager {

    private final MonthView.OnDateChangeListener mDateChangeListener =
            new MonthView.OnDateChangeListener() {
                @Override
                public void onSelectedDayChange(long dayMillis) {
                    mPagerAdapter.setSelectedDay(getCurrentItem(), dayMillis, false);
                    notifyDayChange(dayMillis);
                }
            };
    private MonthViewPagerAdapter mPagerAdapter;
    private OnChangeListener mListener;
    private CalendarAdapter mCalendarAdapter;

    public interface OnChangeListener {
        void onSelectedDayChange(long dayMillis);
    }

    public static abstract class CalendarAdapter {
        private EventCalendarView mCalendarView;

        void setCalendarView(EventCalendarView calendarView) {
            mCalendarView = calendarView;
        }

        protected void loadEvents(long monthMillis) {
        }

        public final void bindEvents(long monthMillis, EventCursor cursor) {
            mCalendarView.swapCursor(monthMillis, cursor);
        }
    }

    public EventCalendarView(Context context) {
        this(context, null);
    }

    public EventCalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        View child = mPagerAdapter.mViews.get(getCurrentItem());
        if (child != null) {
            child.measure(widthMeasureSpec, heightMeasureSpec);
            int height = child.getMeasuredHeight();
            setMeasuredDimension(getMeasuredWidth(), height);
        }
    }

    public void setOnChangeListener(OnChangeListener listener) {
        mListener = listener;
    }

    public void setSelectedDay(long dayMillis) {
        int position = getCurrentItem();
        if (CalendarUtils.monthBefore(dayMillis, mPagerAdapter.mSelectedDayMillis)) {
            mPagerAdapter.setSelectedDay(position - 1, dayMillis, true);
            setCurrentItem(position - 1, true);
        } else if (CalendarUtils.monthAfter(dayMillis, mPagerAdapter.mSelectedDayMillis)) {
            mPagerAdapter.setSelectedDay(position + 1, dayMillis, true);
            setCurrentItem(position + 1, true);
        } else {
            mPagerAdapter.setSelectedDay(position, dayMillis, true);
        }
    }

    public void setCalendarAdapter(CalendarAdapter adapter) {
        mCalendarAdapter = adapter;
        mCalendarAdapter.setCalendarView(this);
        loadEvents(getCurrentItem());
    }

    public void deactivate() {
        mPagerAdapter.deactivate();
    }

    public void invalidateData() {
        mPagerAdapter.invalidate();
        loadEvents(getCurrentItem());
    }

    public void reset() {
        deactivate();
        init();
        loadEvents(getCurrentItem());
    }

    private void init() {
        mPagerAdapter = new MonthViewPagerAdapter(mDateChangeListener);
        setAdapter(mPagerAdapter);
        setCurrentItem(mPagerAdapter.getCount() / 2);
        addOnPageChangeListener(new SimpleOnPageChangeListener() {
            public boolean mDragging = false;

            @Override
            public void onPageSelected(int position) {
                if (mDragging) {
                    toFirstDay(position);
                    notifyDayChange(mPagerAdapter.getMonth(position));
                }
                mDragging = false;
                if (getVisibility() != VISIBLE) {
                    onPageScrollStateChanged(SCROLL_STATE_IDLE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager.SCROLL_STATE_IDLE) {
                    syncPages(getCurrentItem());
                    loadEvents(getCurrentItem());
                } else if (state == SCROLL_STATE_DRAGGING) {
                    mDragging = true;
                }
            }
        });
    }

    private void toFirstDay(int position) {
        mPagerAdapter.setSelectedDay(position,
                CalendarUtils.monthFirstDay(mPagerAdapter.getMonth(position)), true);
    }

    private void notifyDayChange(long dayMillis) {
        if (mListener != null) {
            mListener.onSelectedDayChange(dayMillis);
        }
    }

    private void syncPages(int position) {
        int first = 0, last = mPagerAdapter.getCount() - 1;
        if (position == last) {
            mPagerAdapter.shiftLeft();
            setCurrentItem(first + 1, false);
        } else if (position == 0) {
            mPagerAdapter.shiftRight();
            setCurrentItem(last - 1, false);
        } else {
            if (position > 0) {
                mPagerAdapter.bind(position - 1);
            }
            if (position < mPagerAdapter.getCount() - 1) {
                mPagerAdapter.bind(position + 1);
            }
        }
    }

    private void loadEvents(int position) {
        if (mCalendarAdapter != null && mPagerAdapter.getCursor(position) == null) {
            mCalendarAdapter.loadEvents(mPagerAdapter.getMonth(position));
        }
    }

    private void swapCursor(long monthMillis, EventCursor cursor) {
        mPagerAdapter.swapCursor(monthMillis, cursor, new PagerContentObserver(monthMillis));
    }

    class PagerContentObserver extends ContentObserver {

        private final long monthMillis;

        public PagerContentObserver(long monthMillis) {
            super(new Handler());
            this.monthMillis = monthMillis;
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            mPagerAdapter.swapCursor(monthMillis, null, null);
            if (CalendarUtils.sameMonth(monthMillis, mPagerAdapter.getMonth(getCurrentItem()))) {
                loadEvents(getCurrentItem());
            }
        }
    }
}
