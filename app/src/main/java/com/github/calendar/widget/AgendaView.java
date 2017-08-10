package com.github.calendar.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import com.github.calendar.CalendarUtils;
import com.github.calendar.R;
import com.github.calendar.ViewUtils;
import com.github.calendar.weather.WeatherPojo;

public class AgendaView extends RecyclerView {
    private static final String STATE_VIEW = "state:view";
    private static final String STATE_ADAPTER = "state:adapter";

    private OnDateChangeListener mListener;
    private AgendaAdapter mAdapter;
    private int mPendingScrollPosition = NO_POSITION;
    private long mPrevTimeMillis = CalendarUtils.NO_TIME_MILLIS;
    private Bundle mAdapterSavedState;
    private final int[] mColors;

    public interface OnDateChangeListener {
        void onSelectedDayChange(long dayMillis);
    }

    public AgendaView(Context context) {
        this(context, null);
    }

    public AgendaView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AgendaView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
        if (isInEditMode()) {
            mColors = new int[]{ContextCompat.getColor(context, android.R.color.transparent)};
            setAdapter(new AgendaAdapter(context) {});
        } else {
            mColors = ViewUtils.getCalendarColors(context);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle outState = new Bundle();
        outState.putParcelable(STATE_VIEW, super.onSaveInstanceState());
        if (mAdapter != null) {
            outState.putBundle(STATE_ADAPTER, mAdapter.saveState());
        }
        return outState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle savedState = (Bundle) state;
        mAdapterSavedState = savedState.getBundle(STATE_ADAPTER);
        super.onRestoreInstanceState(savedState.getParcelable(STATE_VIEW));
    }

    @Override
    public void onScrolled(int dx, int dy) {
        if (dy != 0) {
            loadMore();
            notifyDateChange();
        }
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);
        if (state == SCROLL_STATE_IDLE && mPendingScrollPosition != NO_POSITION) {
            mPendingScrollPosition = NO_POSITION;
            mAdapter.unlockBinding();
        }
    }

    @Override
    public void setAdapter(Adapter adapter) {
        if (adapter != null && !(adapter instanceof AgendaAdapter)) {
            throw new IllegalArgumentException("Adapter must be an instance of AgendaAdapter");
        }
        mAdapter = (AgendaAdapter) adapter;
        if (mAdapter != null) {
            if (mAdapterSavedState != null) {
                mAdapter.restoreState(mAdapterSavedState);
                mAdapterSavedState = null;
            } else {
                mAdapter.append(getContext());
                getLinearLayoutManager().scrollToPosition(mAdapter.getItemCount() / 2);
            }
            mAdapter.setCalendarColors(mColors);
        }
        super.setAdapter(mAdapter);
    }

    public void setOnDateChangeListener(OnDateChangeListener listener) {
        mListener = listener;
    }

    public void setSelectedDay(long dayMillis) {
        if (mAdapter == null) {
            return;
        }
        mPendingScrollPosition = mAdapter.getPosition(getContext(), dayMillis);
        if (mPendingScrollPosition >= 0) {
            mAdapter.lockBinding();
            smoothScrollToPosition(mPendingScrollPosition);
        }
    }

    public void setWeather(WeatherPojo weather) {
        if (mAdapter != null) {
            mAdapter.setWeather(weather);
        }
    }

    public void reset() {
        mPendingScrollPosition = NO_POSITION;
        mPrevTimeMillis = CalendarUtils.NO_TIME_MILLIS;
        mAdapterSavedState = null;
        if (mAdapter != null) {
            int originalCount = mAdapter.getItemCount();
            mAdapter.lockBinding();
            mAdapter.deactivate();
            mAdapter.notifyItemRangeRemoved(0, originalCount);
            mAdapter.append(getContext());
            mAdapter.notifyItemRangeInserted(0, mAdapter.getItemCount());
            setSelectedDay(CalendarUtils.today());
        }
    }

    public void invalidateData() {
        if (mAdapter != null) {
            mAdapter.invalidate();
        }
    }

    private void init() {
        setHasFixedSize(false);
        setLayoutManager(new AgendaLinearLayoutManager(getContext()));
        addItemDecoration(new DividerItemDecoration(getContext()));
        setItemAnimator(null);
    }

    private LinearLayoutManager getLinearLayoutManager() {
        return (LinearLayoutManager) getLayoutManager();
    }

    void loadMore() {
        if (mAdapter == null) {
            return;
        }
        if (getLinearLayoutManager().findFirstVisibleItemPosition() == 0) {
            mAdapter.prepend(getContext());
        } else if (getLinearLayoutManager().findLastVisibleItemPosition()
                == mAdapter.getItemCount() - 1) {
            mAdapter.append(getContext());
        }
    }

    private void notifyDateChange() {
        int position = getLinearLayoutManager().findFirstVisibleItemPosition();
        if (position < 0) {
            return;
        }
        long timeMillis = mAdapter.getAdapterItem(position).mTimeMillis;
        if (mPrevTimeMillis != timeMillis) {
            mPrevTimeMillis = timeMillis;
            if (mPendingScrollPosition == NO_POSITION && mListener != null) {
                mListener.onSelectedDayChange(timeMillis);
            }
        }
    }

    static class DividerItemDecoration extends ItemDecoration {
        private final Paint mPaint;
        private final int mSize;

        public DividerItemDecoration(Context context) {
            mSize = context.getResources().getDimensionPixelSize(R.dimen.divider_size);
            mPaint = new Paint();
            mPaint.setColor(ContextCompat.getColor(context, R.color.colorDivider));
            mPaint.setStrokeWidth(mSize);
        }

        @Override
        public void onDrawOver(Canvas c, RecyclerView parent, State state) {
            int top, left = 0, right = parent.getMeasuredWidth();
            for (int i = 0; i < parent.getChildCount(); i++) {
                top = parent.getChildAt(i).getTop() - mSize / 2;
                c.drawLine(left, top, right, top, mPaint);
            }
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, State state) {
            if (parent.getChildAdapterPosition(view) > 0) {
                outRect.top = mSize;
            }
        }
    }

    static class AgendaLinearLayoutManager extends LinearLayoutManager {

        public AgendaLinearLayoutManager(Context context) {
            super(context);
        }

        @Override
        public void smoothScrollToPosition(RecyclerView recyclerView,
                                           RecyclerView.State state,
                                           int position) {
            RecyclerView.SmoothScroller smoothScroller =
                    new LinearSmoothScroller(recyclerView.getContext()) {
                        @Override
                        public PointF computeScrollVectorForPosition(int targetPosition) {
                            return AgendaLinearLayoutManager.this
                                    .computeScrollVectorForPosition(targetPosition);
                        }

                        @Override
                        protected int getVerticalSnapPreference() {
                            return SNAP_TO_START;
                        }
                    };
            smoothScroller.setTargetPosition(position);
            startSmoothScroll(smoothScroller);
        }
    }
}