package com.demo.fingerfollowupeffect;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

/**
 * Created by chenwei on 4/5/15.
 */
public class CustomizeViewGroup extends ViewGroup {
    private static final String TAG = "CustomizeViewGroup";

    private static final int CHILD_PADDING = 10;
    private static final int LINE_CHILD_COUNT =7;

    private boolean mFollowingStart;
    private static final int MSG_FOLLOWING = 1;
    private static final int GATHER_INTERVAL = 16;

    private ArrayList<View> mChildren = new ArrayList<View>();

    public CustomizeViewGroup(Context context) {
        super(context);
        setChildrenDrawingOrderEnabled(true);
    }

    public CustomizeViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        setChildrenDrawingOrderEnabled(true);
    }

    public CustomizeViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setChildrenDrawingOrderEnabled(true);
    }

    public CustomizeViewGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setChildrenDrawingOrderEnabled(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int w = getMeasuredWidth();
        int h = getMeasuredHeight();
        int c = getChildCount();
        for (int i=0; i<c; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                int cw = (w - (LINE_CHILD_COUNT - 1) * CHILD_PADDING) / LINE_CHILD_COUNT;
                int ch = cw;
                int wSpec = MeasureSpec.makeMeasureSpec(cw, MeasureSpec.EXACTLY);
                int hSpec = MeasureSpec.makeMeasureSpec(ch, MeasureSpec.EXACTLY);
                child.measure(wSpec, hSpec);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Log.d(TAG, "onLayout called.");
        int c = getChildCount();
        int w = getMeasuredWidth();

        int left = getPaddingLeft();
        int top = getPaddingTop();
        int right = 0;
        int bottom = 0;

        int maxHeight = Integer.MIN_VALUE;

        for (int i=0; i<c; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                int cw = child.getMeasuredWidth();
                int ch = child.getMeasuredHeight();
                if (maxHeight < ch) {
                    maxHeight = ch;
                }

                if (left + cw + getPaddingRight() > w) {
                    left = getPaddingLeft();
                    top += maxHeight + CHILD_PADDING;
                }
                right = left + cw;
                bottom = top + ch;
                child.layout(left, top, right, bottom);

                left += cw + CHILD_PADDING;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        float x = event.getX();
        float y = event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mFollowingStart = initChildArray();
                if (!mFollowingStart) {
                    return false;
                }
                // move the first child
                moveFirstChild(x, y);
                Message msg = mHandler.obtainMessage(MSG_FOLLOWING);
                mHandler.sendMessageDelayed(msg, GATHER_INTERVAL);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mFollowingStart) {
                    // move the first child
                    moveFirstChild(x, y);
                }
                break;
            case MotionEvent.ACTION_UP:
                // restore children's position
                restoreChildPosition();
                mFollowingStart = false;
                break;
        }
        return true;
    }

    private boolean initChildArray() {
        mChildren.clear();
        int c = getChildCount();
        for (int i=0; i<c; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                mChildren.add(child);
            }
        }
        return mChildren.size() > 0;
    }

    private void moveFirstChild(float x, float y) {
        if (!mFollowingStart) {
            return;
        }

        View first = mChildren.get(0);
        int cw = first.getMeasuredWidth();
        int ch = first.getMeasuredHeight();
        moveChild(first, (x - cw / 2), (y - ch / 2));
    }

    private void moveChild(View child, float x, float y) {
        child.setTranslationX(x);
        child.setTranslationY(y);
    }

    private void restoreChildPosition() {
        if (mFollowingStart) {
            for (View child : mChildren) {
                child.setTranslationX(0);
                child.setTranslationY(0);
            }
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (!mFollowingStart) {
                return;
            }

            // gather children
            for (int i=(mChildren.size() - 1); i>0; i--) {
                View next = mChildren.get(i);
                int nextL = next.getLeft();
                int nextT = next.getTop();
                View prev = mChildren.get(i - 1);
                int prevL = prev.getLeft();
                int prevT = prev.getTop();
                float prevTransX = prev.getTranslationX();
                float prevTransY = prev.getTranslationY();
                // need to subtract the relative distance
                moveChild(next, prevTransX - (nextL - prevL), prevTransY - (nextT - prevT));
            }

            // next pass
            msg = mHandler.obtainMessage(MSG_FOLLOWING);
            sendMessageDelayed(msg, GATHER_INTERVAL);
        }
    };

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        // let first child drawn on the top
        return (childCount - 1 - i);
    }
}
