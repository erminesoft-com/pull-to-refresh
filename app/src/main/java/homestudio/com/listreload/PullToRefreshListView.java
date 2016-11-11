package homestudio.com.listreload;

import android.widget.ListView;
import android.content.Context;
import android.util.AttributeSet;
import android.view.*;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.*;
import android.view.animation.Animation.AnimationListener;
import android.widget.*;

public class PullToRefreshListView extends ListView {

    private static final float PULL_RESISTANCE                 = 1.7f;
    private static final int   BOUNCE_ANIMATION_DURATION       = 700;
    private static final int   BOUNCE_ANIMATION_DELAY          = 100;
    private static final float BOUNCE_OVERSHOOT_TENSION        = 1.4f;

    private enum State{
        PULL_TO_REFRESH,
        RELEASE_TO_REFRESH,
        REFRESHING
    }

    public interface OnRefreshListener{
        void onRefresh();
    }

    private static int measuredHeaderHeight;

    private boolean scrollbarEnabled;
    private boolean bounceBackHeader;
    private boolean lockScrollWhileRefreshing;

    private float                   previousY;
    private int                     headerPadding;
    private boolean                 hasResetHeader;
    private long                    lastUpdated = -1;
    private State                   state;
    private LinearLayout            headerContainer;
    private RelativeLayout          header;
    private OnItemClickListener     onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;
    private OnRefreshListener       onRefreshListener;

    private ImageView imageViewPoint1;
    private ImageView imageViewPoint2;
    private ImageView imageViewPoint3;

    private Animation firstStepAnimation1;
    private Animation firstStepAnimation2;

    private float mScrollStartY;
    private final int IDLE_DISTANCE = 5;

    public PullToRefreshListView(Context context){
        super(context);
        init(context);
    }

    public PullToRefreshListView(Context context, AttributeSet attrs){
        super(context, attrs);
        init(context);
    }

    public PullToRefreshListView(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
        init(context);
    }

    @Override
    public void setOnItemClickListener(OnItemClickListener onItemClickListener){
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener){
        this.onItemLongClickListener = onItemLongClickListener;
    }

    public void setOnRefreshListener(OnRefreshListener onRefreshListener){
        this.onRefreshListener = onRefreshListener;
    }

    public void onRefreshComplete(){
        state = State.PULL_TO_REFRESH;
        resetHeader();
        lastUpdated = System.currentTimeMillis();
    }

    private void init(Context context){
        setVerticalFadingEdgeEnabled(false);

        headerContainer = (LinearLayout) LayoutInflater.from(getContext()).inflate(R.layout.ptr_header, null);
        header = (RelativeLayout) headerContainer.findViewById(R.id.ptr_id_header);

        imageViewPoint1 = (ImageView) header.findViewById(R.id.point);
        imageViewPoint2 = (ImageView) header.findViewById(R.id.point2);
        imageViewPoint3 = (ImageView) header.findViewById(R.id.point3);

        firstStepAnimation1 = AnimationUtils.loadAnimation(context, R.anim.right_step_anim);
        firstStepAnimation1.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                imageViewPoint1.setVisibility(View.VISIBLE);
                imageViewPoint2.setVisibility(View.VISIBLE);
                imageViewPoint3.setVisibility(View.VISIBLE);

                firstStepAnimation1.setRepeatCount(10);
                firstStepAnimation2.setRepeatCount(10);

                imageViewPoint1.startAnimation(firstStepAnimation1);
                imageViewPoint2.startAnimation(firstStepAnimation2);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        firstStepAnimation2 = AnimationUtils.loadAnimation(context, R.anim.left_step_anim);

        addHeaderView(headerContainer);
        setState(State.PULL_TO_REFRESH);
        scrollbarEnabled = isVerticalScrollBarEnabled();

        ViewTreeObserver vto = header.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new PTROnGlobalLayoutListener());

        super.setOnItemClickListener(new PTROnItemClickListener());
        super.setOnItemLongClickListener(new PTROnItemLongClickListener());
    }

    private void setHeaderPadding(int padding){
        headerPadding = padding;

        MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) header.getLayoutParams();
        mlp.setMargins(0, Math.round(padding), 0, 0);
        header.setLayoutParams(mlp);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        if(lockScrollWhileRefreshing && (state == State.REFRESHING || getAnimation() != null && !getAnimation().hasEnded())){
            return true;
        }

        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                if(getFirstVisiblePosition() == 0){
                    previousY = event.getY();
                }
                else {
                    previousY = -1;
                }

                mScrollStartY = event.getY();

                break;

            case MotionEvent.ACTION_UP:
                if(previousY != -1 && (state == State.RELEASE_TO_REFRESH || getFirstVisiblePosition() == 0)){
                    switch(state){
                        case RELEASE_TO_REFRESH:
                            setState(State.REFRESHING);
                            bounceBackHeader();

                            break;

                        case PULL_TO_REFRESH:
                            resetHeader();
                            break;
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if(previousY != -1 && getFirstVisiblePosition() == 0 && Math.abs(mScrollStartY-event.getY()) > IDLE_DISTANCE){
                    float y = event.getY();
                    float diff = y - previousY;
                    if(diff > 0) diff /= PULL_RESISTANCE;
                    previousY = y;

                    int newHeaderPadding = Math.max(Math.round(headerPadding + diff), -header.getHeight());

                    if(newHeaderPadding != headerPadding && state != State.REFRESHING){
                        setHeaderPadding(newHeaderPadding);

                        if(state == State.PULL_TO_REFRESH && headerPadding > 0){
                            setState(State.RELEASE_TO_REFRESH);

                            imageViewPoint1.clearAnimation();
                            imageViewPoint2.clearAnimation();
                            imageViewPoint1.startAnimation(firstStepAnimation1);
                            imageViewPoint2.startAnimation(firstStepAnimation2);
                        }
                    }
                }

                break;
        }
        return super.onTouchEvent(event);
    }

    private void bounceBackHeader(){
        int yTranslate = state == State.REFRESHING ? header.getHeight() - headerContainer.getHeight() : -headerContainer.getHeight() - headerContainer.getTop() + getPaddingTop();

        TranslateAnimation bounceAnimation = new TranslateAnimation(TranslateAnimation.ABSOLUTE, 0, TranslateAnimation.ABSOLUTE, 0, TranslateAnimation.ABSOLUTE, 0, TranslateAnimation.ABSOLUTE, yTranslate);

        bounceAnimation.setDuration(BOUNCE_ANIMATION_DURATION);
        bounceAnimation.setFillEnabled(true);
        bounceAnimation.setFillAfter(false);
        bounceAnimation.setFillBefore(true);
        bounceAnimation.setInterpolator(new OvershootInterpolator(BOUNCE_OVERSHOOT_TENSION));
        bounceAnimation.setAnimationListener(new HeaderAnimationListener(yTranslate));

        startAnimation(bounceAnimation);
    }

    private void resetHeader(){
        if(getFirstVisiblePosition() > 0){
            setHeaderPadding(-header.getHeight());
            setState(State.PULL_TO_REFRESH);
            return;
        }

        if(getAnimation() != null && !getAnimation().hasEnded()){
            bounceBackHeader = true;
        } else {
            bounceBackHeader();
        }
    }

    private void setUiRefreshing(){

    }

    private void setState(State state){
        this.state = state;
        switch(state){
            case PULL_TO_REFRESH:

                break;

            case RELEASE_TO_REFRESH:

                break;

            case REFRESHING:
                setUiRefreshing();

                lastUpdated = System.currentTimeMillis();
                if(onRefreshListener == null){
                    setState(State.PULL_TO_REFRESH);
                } else {
                    onRefreshListener.onRefresh();
                }

                break;
        }
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt){
        super.onScrollChanged(l, t, oldl, oldt);

        if(!hasResetHeader){
            if(measuredHeaderHeight > 0 && state != State.REFRESHING){
                setHeaderPadding(-measuredHeaderHeight);
            }

            hasResetHeader = true;
        }
    }

    private class HeaderAnimationListener implements AnimationListener{

        private int height, translation;
        private State stateAtAnimationStart;

        public HeaderAnimationListener(int translation){
            this.translation = translation;
        }

        @Override
        public void onAnimationStart(Animation animation){
            stateAtAnimationStart = state;

            android.view.ViewGroup.LayoutParams lp = getLayoutParams();
            height = lp.height;
            lp.height = getHeight() - translation;
            setLayoutParams(lp);

            if(scrollbarEnabled){
                setVerticalScrollBarEnabled(false);
            }
        }

        @Override
        public void onAnimationEnd(Animation animation){
            setHeaderPadding(stateAtAnimationStart == State.REFRESHING ? 0 : -measuredHeaderHeight - headerContainer.getTop());
            setSelection(0);

            android.view.ViewGroup.LayoutParams lp = getLayoutParams();
            lp.height = height;
            setLayoutParams(lp);

            if(bounceBackHeader){
                bounceBackHeader = false;

                postDelayed(new Runnable(){

                    @Override
                    public void run(){
                        resetHeader();
                    }
                }, BOUNCE_ANIMATION_DELAY);
            }else if(stateAtAnimationStart != State.REFRESHING){
                setState(State.PULL_TO_REFRESH);
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation){}
    }

    private class PTROnGlobalLayoutListener implements OnGlobalLayoutListener{

        @Override
        public void onGlobalLayout(){
            int initialHeaderHeight = header.getHeight();

            if(initialHeaderHeight > 0){
                measuredHeaderHeight = initialHeaderHeight;

                if(measuredHeaderHeight > 0 && state != State.REFRESHING){
                    setHeaderPadding(-measuredHeaderHeight);
                    requestLayout();
                }
            }

            getViewTreeObserver().removeGlobalOnLayoutListener(this);
        }
    }

    private class PTROnItemClickListener implements OnItemClickListener{

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id){
            hasResetHeader = false;

            if(onItemClickListener != null && state == State.PULL_TO_REFRESH){
                onItemClickListener.onItemClick(adapterView, view, position - getHeaderViewsCount(), id);
            }
        }
    }

    private class PTROnItemLongClickListener implements OnItemLongClickListener{

        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id){
            hasResetHeader = false;

            if(onItemLongClickListener != null && state == State.PULL_TO_REFRESH){
                return onItemLongClickListener.onItemLongClick(adapterView, view, position - getHeaderViewsCount(), id);
            }

            return false;
        }
    }
}
