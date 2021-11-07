package com.tinyant.openglesexample;

import android.app.Activity;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.security.auth.login.LoginException;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;
import android.telephony.ClosedSubscriberGroupInfo;
import android.util.Log;
import android.opengl.Matrix;
import android.graphics.SurfaceTexture;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class CameraSurfaceRenderer implements GLSurfaceView.Renderer {
    private final FloatBuffer vertexBuffer, mTexVertexBuffer;
    private final ShortBuffer mVertexIndexBuffer;

    private int mProgram;
    private int textureId;

    private String TAG = "CameraSurfaceRenderer";

    private final String vertexShaderCode =
            "attribute vec4 vPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "uniform mat4 uTextureMatrix;\n" +
                    "varying highp vec2 yuvTexCoords;\n" +
                    "void main() {\n" +
                    "gl_Position  = vPosition;\n" +
                    "gl_PointSize = 10.0;" +
                    "   yuvTexCoords = (uTextureMatrix * aTextureCoord).xy;\n" +
                    "}\n";

    private final String fragmentShaderCode =
                    "#extension GL_OES_EGL_image_external : require\n" +
                    "uniform samplerExternalOES yuvTexSampler;\n" +
                    "varying highp vec2 yuvTexCoords;\n" +
                    "void main() { \n" +
                    "gl_FragColor = texture2D(yuvTexSampler, yuvTexCoords); "
                        //     "gl_FragColor = vec4(0.5, 1.0,0.0,1.0);"
                            + "}\n";

    private float[] transformMatrix = new float[16];

    private float[] POSITION_VERTEX = new float[]{
            0f, 0f, 0f,
            1f, 1f, 0f,
            -1f, 1f, 0f,
            -1f, -1f, 0f,
            1f, -1f, 0f
    };

    private static final float[] TEX_VERTEX = {
            0.5f, 0.5f,
            1f, 1f,
            0f, 1f,
            0f, 0.0f,
            1f, 0.0f
    };

    private static final short[] VERTEX_INDEX = {
            0, 1, 2,
            0, 2, 3,
            0, 3, 4,
            0, 4, 1
    };


    private GLSurfaceView mGLSurfaceView;
    private int mCameraId;
    private Camera mCamera;
    private SurfaceTexture mSurfaceTexture;

    private int uTextureMatrixLocation;
    private int uTextureSamplerLocation;
    private int positionHandle;
    private int colorHandle;
    private int texCoordHandle;

    public CameraSurfaceRenderer(GLSurfaceView surfaceView) {
        this.mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        mGLSurfaceView = surfaceView;

        mCamera = Camera.open(mCameraId);
        setCameraDisplayOrientation(mCameraId, mCamera);

        vertexBuffer = ByteBuffer.allocateDirect(POSITION_VERTEX.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        vertexBuffer.put(POSITION_VERTEX);
        vertexBuffer.position(0);

        mTexVertexBuffer = ByteBuffer.allocateDirect(TEX_VERTEX.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(TEX_VERTEX);
        mTexVertexBuffer.position(0);

        mVertexIndexBuffer = ByteBuffer.allocateDirect(VERTEX_INDEX.length * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
                .put(VERTEX_INDEX);
        mVertexIndexBuffer.position(0);
    }

    private void setCameraDisplayOrientation(int cameraId, Camera camera) {
        Log.i(TAG, "setCameraDisplayOrientation: now");
        Activity targetActivity = (Activity) mGLSurfaceView.getContext();
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = targetActivity.getWindowManager().getDefaultDisplay()
                .getRotation();
        Log.i(TAG, "setCameraDisplayOrientation: cameraid:" + cameraId + ",info:" + info.toString());
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);

        int vertexShader = MyGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = MyGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        int[] linked = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0);
        if(linked[0] != GLES20.GL_TRUE)
        {
            Log.e(TAG, "Error linking Program");
            Log.e(TAG, "Error:" + GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            return;
        }
        mProgram = program;
        positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        texCoordHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
        uTextureMatrixLocation = GLES20.glGetUniformLocation(mProgram, "uTextureMatrix");
        uTextureSamplerLocation = GLES20.glGetUniformLocation(mProgram, "yuvTexSampler");

        Log.i(TAG, "onSurfaceCreated: pos:" + positionHandle + ",texCoord:" + texCoordHandle + ",matrix:" + uTextureMatrixLocation + ",sampler:" + uTextureSamplerLocation);
        textureId = loadTexture();
        Log.i(TAG, "textureid:" + textureId);
        loadSurfaceTexture(textureId);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);
//
        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(transformMatrix);
//
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glUniform1i(uTextureSamplerLocation, 0);
        
        GLES20.glUniformMatrix4fv(uTextureMatrixLocation, 1, false, transformMatrix, 0);
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(texCoordHandle);
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mTexVertexBuffer);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, VERTEX_INDEX.length, GLES20.GL_UNSIGNED_SHORT, mVertexIndexBuffer);
    }

    public int loadTexture() {
        int[] tex = new int[1];
        GLES20.glGenTextures(1, tex, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        return tex[0];
    }

    public boolean loadSurfaceTexture(int textureId) {
        mSurfaceTexture = new SurfaceTexture(textureId);
        mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                // Log.i(TAG, "onFrameAvailable: surface texture callback...");
                mGLSurfaceView.requestRender();
            }
        });

        try {
            Log.i(TAG, "loadSurfaceTexture: set preview texture...");
            mCamera.setPreviewTexture(mSurfaceTexture);
        } catch (IOException e) {
            Log.e(TAG, "loadSurfaceTexture: exception:" + e.getMessage());
            e.printStackTrace();
            return false;
        }
        mCamera.startPreview();
        return true;
    }
}
