package com.wolcano.musicplayer.music.widgets;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.wolcano.musicplayer.music.R;
import com.wolcano.musicplayer.music.utils.Utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;


public  class RotateFabBehav extends CoordinatorLayout.Behavior<FloatingActionButton> {
    private static final boolean AUTO_HIDE_DEFAULT = true;

    private Rect mTmpRect;
    private FloatingActionButton.OnVisibilityChangedListener mInternalAutoHideListener;
    private boolean mAutoHideEnabled;
    private static boolean isAnimate;
    public RotateFabBehav() {
        super();
        mAutoHideEnabled = AUTO_HIDE_DEFAULT;
    }

    public RotateFabBehav(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs,
                com.google.android.material.R.styleable.FloatingActionButton_Behavior_Layout);
        mAutoHideEnabled = a.getBoolean(
                com.google.android.material.R.styleable.FloatingActionButton_Behavior_Layout_behavior_autoHide,
                AUTO_HIDE_DEFAULT);
        a.recycle();
    }


    @Override
    public void onAttachedToLayoutParams(@NonNull CoordinatorLayout.LayoutParams lp) {
        if (lp.dodgeInsetEdges == Gravity.NO_GRAVITY) {
            // If the developer hasn't set dodgeInsetEdges, lets set it to BOTTOM so that
            // we dodge any Snackbars
            lp.dodgeInsetEdges = Gravity.BOTTOM;
        }
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, FloatingActionButton child,
                                          View dependency) {

        if (dependency instanceof AppBarLayout) {
            // If we're depending on an AppBarLayout we will show/hide it automatically
            // if the FAB is anchored to the AppBarLayout
            updateFabVisibilityForAppBarLayout(parent, (AppBarLayout) dependency, child);
        }
        return false;
    }

    private boolean shouldUpdateVisibility(View dependency, FloatingActionButton child) {

        final CoordinatorLayout.LayoutParams lp =
                (CoordinatorLayout.LayoutParams) child.getLayoutParams();
        if (!mAutoHideEnabled) {

            return false;
        }

        return true;
    }

    private boolean updateFabVisibilityForAppBarLayout(CoordinatorLayout parent,
                                                       AppBarLayout appBarLayout, FloatingActionButton child) {
        if (!shouldUpdateVisibility(appBarLayout, child)) {

            return false;
        }

        if (mTmpRect == null) {

            mTmpRect = new Rect();
        }

        // First, let's get the visible rect of the dependency
        final Rect rect = mTmpRect;
        getDescendantRect(parent, appBarLayout, rect);

        int height = 0;
        try {
            Method method = AppBarLayout.class.getDeclaredMethod("getMinimumHeightForVisibleOverlappingContent", (Class<?>[]) new Class[0]);
            method.setAccessible(true);
            height = (int) method.invoke(appBarLayout, new Object[0]);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        if (rect.bottom <= height) {
            // If the anchor's bottom is below the seam, we'll animate our FAB out
            if (!isAnimate&&child.getVisibility()== View.VISIBLE){
                hide(child);
            }
        } else {

            // Else, we'll animate our FAB back in
            if(!isAnimate&&child.getVisibility()== View.GONE){
                show(child,0,false);
            }
        }
        return true;
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, FloatingActionButton child,
                                 int layoutDirection) {
        // First, let's make sure that the visibility of the FAB is consistent
        final List<View> dependencies = parent.getDependencies(child);

        for (int i = 0, count = dependencies.size(); i < count; i++) {

            final View dependency = dependencies.get(i);
            if (dependency instanceof AppBarLayout) {

                if (updateFabVisibilityForAppBarLayout(

                        parent, (AppBarLayout) dependency, child)) {

                    break;
                }
            }
        }
        // Now let the CoordinatorLayout lay out the FAB
        parent.onLayoutChild(child, layoutDirection);
        // Now offset it if needed
        offsetIfNeeded(parent, child);
        return true;
    }

    @Override
    public boolean getInsetDodgeRect(@NonNull CoordinatorLayout parent,
                                     @NonNull FloatingActionButton child, @NonNull Rect rect) {
        // Since we offset so that any internal shadow padding isn't shown, we need to make
        // sure that the shadow isn't used for any dodge inset calculations

        Rect shadowPadding = new Rect(0, 0, 0, 0);
        try {
            Field mShadowPadding = FloatingActionButton.class.getDeclaredField("mShadowPadding");
            mShadowPadding.setAccessible(true);
            shadowPadding=(Rect)mShadowPadding.get(child);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        rect.set(child.getLeft() + shadowPadding.left,
                child.getTop() + shadowPadding.top,
                child.getRight() - shadowPadding.right,
                child.getBottom() - shadowPadding.bottom);
        return true;
    }

    /**
     * Pre-Lollipop we use padding so that the shadow has enough space to be drawn. This method
     * offsets our layout position so that we're positioned correctly if we're on one of
     * our parent's edges.
     */
    private void offsetIfNeeded(CoordinatorLayout parent, FloatingActionButton fab) {
        Rect padding = new Rect(0, 0, 0, 0);
        try {
            Field mShadowPadding = FloatingActionButton.class.getDeclaredField("mShadowPadding");
            mShadowPadding.setAccessible(true);
            padding=(Rect)mShadowPadding.get(fab);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        if (padding != null && padding.centerX() > 0 && padding.centerY() > 0) {
            final CoordinatorLayout.LayoutParams lp =
                    (CoordinatorLayout.LayoutParams) fab.getLayoutParams();

            int offsetTB = 0, offsetLR = 0;

            if (fab.getRight() >= parent.getWidth() - lp.rightMargin) {
                // If we're on the right edge, shift it the right
                offsetLR = padding.right;
            } else if (fab.getLeft() <= lp.leftMargin) {
                // If we're on the left edge, shift it the left
                offsetLR = -padding.left;
            }
            if (fab.getBottom() >= parent.getHeight() - lp.bottomMargin) {
                // If we're on the bottom edge, shift it down
                offsetTB = padding.bottom;
            } else if (fab.getTop() <= lp.topMargin) {
                // If we're on the top edge, shift it up
                offsetTB = -padding.top;
            }

            if (offsetTB != 0) {
                ViewCompat.offsetTopAndBottom(fab, offsetTB);
            }
            if (offsetLR != 0) {
                ViewCompat.offsetLeftAndRight(fab, offsetLR);
            }
        }
    }

    private void getDescendantRect(ViewGroup parent, View descendant, Rect out) {
        out.set(0, 0, descendant.getWidth(), descendant.getHeight());
        ViewGroupUtilsHoneycomb.offsetDescendantRect(parent, descendant, out);
    }

    public static void show(final FloatingActionButton floatingActionButton, int color, boolean fromUser) {
        if(fromUser){
            floatingActionButton.setBackgroundTintList(ColorStateList.valueOf(color));
            if(!Utils.isColorLight(color)){
                floatingActionButton.setColorFilter(Color.WHITE);

            } else {
                floatingActionButton.setColorFilter(Color.BLACK);

            }
        }
        floatingActionButton.postDelayed(new Runnable() {
            @Override
            public void run() {
                AnimatorSet fabAnimation = (AnimatorSet) AnimatorInflater.loadAnimator(floatingActionButton.getContext(), R.animator.float_button_in);
                fabAnimation.setTarget(floatingActionButton);
                fabAnimation.setInterpolator(new FastOutSlowInInterpolator());
                fabAnimation.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        isAnimate = true;
                        floatingActionButton.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        isAnimate = false;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        hide(floatingActionButton);
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                fabAnimation.start();
            }
        }, 150L);
    }

    private static void hide(final FloatingActionButton floatingActionButton) {
        AnimatorSet fabAnimation = (AnimatorSet) AnimatorInflater.loadAnimator(floatingActionButton.getContext(), R.animator.float_button_out);
        fabAnimation.setTarget(floatingActionButton);
        fabAnimation.setInterpolator(new FastOutSlowInInterpolator());
        fabAnimation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                isAnimate = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                floatingActionButton.setVisibility(View.GONE);
                isAnimate = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                show(floatingActionButton,0,false);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        fabAnimation.start();
    }

    static class ViewGroupUtilsHoneycomb {
        private static final ThreadLocal<Matrix> sMatrix = new ThreadLocal<>();
        private static final ThreadLocal<RectF> sRectF = new ThreadLocal<>();

        private static void offsetDescendantRect(ViewGroup group, View child, Rect rect) {
            Matrix m = sMatrix.get();
            if (m == null) {
                m = new Matrix();
                sMatrix.set(m);
            } else {
                m.reset();
            }

            offsetDescendantMatrix(group, child, m);

            RectF rectF = sRectF.get();
            if (rectF == null) {
                rectF = new RectF();
                sRectF.set(rectF);
            }
            rectF.set(rect);
            m.mapRect(rectF);
            rect.set((int) (rectF.left + 0.5f), (int) (rectF.top + 0.5f),
                    (int) (rectF.right + 0.5f), (int) (rectF.bottom + 0.5f));
        }

        static void offsetDescendantMatrix(ViewParent target, View view, Matrix m) {
            final ViewParent parent = view.getParent();
            if (parent instanceof View && parent != target) {
                final View vp = (View) parent;
                offsetDescendantMatrix(target, vp, m);
                m.preTranslate(-vp.getScrollX(), -vp.getScrollY());
            }

            m.preTranslate(view.getLeft(), view.getTop());

            if (!view.getMatrix().isIdentity()) {
                m.preConcat(view.getMatrix());
            }
        }
    }

}
