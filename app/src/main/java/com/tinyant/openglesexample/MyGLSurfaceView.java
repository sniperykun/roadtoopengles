package com.tinyant.openglesexample;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

public class MyGLSurfaceView extends GLSurfaceView {
    private MyGLRenderer _targetRenderer;
    private CameraSurfaceRenderer _cameraRenderer;

    public MyGLSurfaceView(Context context) {
        super(context);

        // create And OPENGL Es 2.0 context
        setEGLContextClientVersion(2);
        // _targetRenderer = new MyGLRenderer();
        _cameraRenderer = new CameraSurfaceRenderer(this);

        // set the renderder for drawing on the GLSurfaceView
        setRenderer(_cameraRenderer);

        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;

    private float previousX;
    private float previousY;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.


        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float dx = x - previousX;
                float dy = y - previousY;

                // reverse direction of rotation above the mid-line
                if (y > getHeight() / 2) {
                    dx = dx * -1;
                }

                // reverse direction of rotation to left of the mid-line
                if (x < getWidth() / 2) {
                    dy = dy * -1;
                }

                // _targetRenderer.setAngle(_targetRenderer.getAngle() + ((dx + dy) * TOUCH_SCALE_FACTOR));
                requestRender();
                break;
        }
        previousY = y;
        previousX = x;
        return true;
    }
}
