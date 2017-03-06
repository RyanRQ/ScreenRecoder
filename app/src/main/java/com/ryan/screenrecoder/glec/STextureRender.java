package com.ryan.screenrecoder.glec;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.ryan.screenrecoder.R;
import com.ryan.screenrecoder.util.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Created by guoheng on 2016/8/31.
 */
public  class STextureRender {
    private static final int FLOAT_SIZE_BYTES = 4;
    private static final String TAG = "STextureRendering";

    private static final float TRANSFORM_RECTANGLE_COORDS[] = {
            -0.914337f, -0.949318f,1.0f,
            0.494437f, -0.683502f,1.0f,
            -0.895833f, 0.62963f,1.0f,
            0.76524f, 0.689287f,1.0f

    };


    private static final float TRANSFORM_RECTANGLE_TEX_COORDS[] = {
            0f, 0.822368f, 0.822368f,1.0f,
            0.710227f, 0.710227f, 0.710227f,1.0f,
            0f, 0f, 1f,1.0f,
            0.838926f, 0f, 0.838926f,1.0f

    };

    private static final float FULL_RECTANGLE_COORDS[] = {
            -1.0f, -1.0f,1.0f,   // 0 bottom left
            1.0f, -1.0f,1.0f,   // 1 bottom right
            -1.0f,  1.0f,1.0f,   // 2 top left
            1.0f,  1.0f,1.0f   // 3 top right
    };

    private static final float FULL_RECTANGLE_TEX_COORDS[] = {
            0.0f, 1.0f, 1f,1.0f,    // 0 bottom left
            1.0f, 1.0f,1f,1.0f,     // 1 bottom right
            0.0f, 0.0f, 1f,1.0f,    // 2 top left
            1.0f, 0.0f ,1f,1.0f     // 3 top right
    };

    private static final FloatBuffer FULL_RECTANGLE_BUF =
            GlUtil.createFloatBuffer(FULL_RECTANGLE_COORDS);
    private static final FloatBuffer FULL_RECTANGLE_TEX_BUF =
            GlUtil.createFloatBuffer(FULL_RECTANGLE_TEX_COORDS);



    private static final FloatBuffer TRANSFORM_RECTANGLE_BUF =
            GlUtil.createFloatBuffer(TRANSFORM_RECTANGLE_COORDS);
    private static final FloatBuffer TRANSFORM_RECTANGLE_TEX_BUF =
            GlUtil.createFloatBuffer(TRANSFORM_RECTANGLE_TEX_COORDS);



    private FloatBuffer mTriangleVertices;

//    private static final String VERTEX_SHADER =
//            "uniform mat4 uMVPMatrix;\n" +
//                    "uniform mat4 uSTMatrix;\n" +
//                    "attribute vec4 aPosition;\n" +
//                    "attribute vec4 aTextureCoord;\n" +
//                    "varying vec4 vTextureCoord;\n" +
//                    "void main() {\n" +
//                    "    gl_Position = uMVPMatrix * aPosition;\n" +
//                    "    vTextureCoord = uSTMatrix * aTextureCoord;\n" +
//                    "}\n";
//
//    private static final String FRAGMENT_SHADER =
//            "#extension GL_OES_EGL_image_external : require\n" +
//                    "precision mediump float;\n" +      // highp here doesn't seem to matter
//                    "varying vec4 vTextureCoord;\n" +
//                    "uniform samplerExternalOES sTexture;\n" +
//                    "void main() {\n" +
//                    "    gl_FragColor = texture2D(sTexture, vTextureCoord.xy/vTextureCoord.z);" +
//                    "}\n";

    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 aPosition;" +
                    "void main() {" +
                    "    gl_Position = uMVPMatrix * aPosition;" +
                    "}";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;" +
                    "uniform vec4 uColor;" +
                    "void main() {" +
                    "    gl_FragColor = uColor;" +
                    "}";


    private float[] mMVPMatrix = new float[16];
    private float[] mSTMatrix = new float[16];

    private int mProgram;
    private int mTextureID = -12345;
    int textureParamHandle;
    int textureCoordinateHandle;
    int positionHandle;
    int textureTranformHandle;
    private static short drawOrder[] = {0, 1, 2, 0, 2, 3};
    private static float squareSize = 1.0f;
    private static float squareCoords[] = {
            -squareSize, squareSize,   // top left
            -squareSize, -squareSize,   // bottom left
            squareSize, -squareSize,    // bottom right
            squareSize, squareSize}; // top right
    private FloatBuffer textureBuffer;
    private float textureCoords[] = {
            0.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 1.0f, 0.0f, 1.0f};
    private int[] textures = new int[1];
    private float[] videoTextureTransform = new float[16];
    private FloatBuffer vertexBuffer;
    private ShortBuffer drawListBuffer;
    private Context context;
    public STextureRender(Context context) {
        this.context=context;
//        Matrix.setIdentityM(mSTMatrix, 0);
    }

    public int getTextureId() {
        return mTextureID;
    }

    /**
     * Draws the external texture in SurfaceTexture onto the current EGL surface.
     */
    public void drawFrame(SurfaceTexture st, boolean invert) {
        st.getTransformMatrix(videoTextureTransform);
//        if (invert) {
//            mSTMatrix[5] = -mSTMatrix[5];
//            mSTMatrix[13] = 1.0f - mSTMatrix[13];
//        }
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        //画背景色
//        GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glViewport(0, 0, 1080, 1920);
        GLES20.glEnableVertexAttribArray(positionHandle);
        //指定positionHandle的数据值可以在什么地方访问。 vertexBuffer在内部（NDK）是个指针，指向数组的第一组值的内存
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glBindTexture(GLES20.GL_TEXTURE0, textures[0]);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        //指定一个当前的textureParamHandle对象为一个全局的uniform 变量
        GLES20.glUniform1i(textureParamHandle, 0);

        GLES20.glEnableVertexAttribArray(textureCoordinateHandle);
        GLES20.glVertexAttribPointer(textureCoordinateHandle, 4, GLES20.GL_FLOAT, false, 0, textureBuffer);

        GLES20.glUniformMatrix4fv(textureTranformHandle, 1, false, videoTextureTransform, 0);
        //GLES20.GL_TRIANGLES（以无数小三角行的模式）去绘制出这个纹理图像
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(textureCoordinateHandle);


//
//        checkGlError("onDrawFrame start");
//        st.getTransformMatrix(mSTMatrix);
//        if (invert) {
//            mSTMatrix[5] = -mSTMatrix[5];
//            mSTMatrix[13] = 1.0f - mSTMatrix[13];
//        }
//
//        // (optional) clear to green so we can see if we're failing to set pixels
//        //画背景色
////        GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
////        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
//
//        GLES20.glUseProgram(mProgram);
//        checkGlError("glUseProgram");
//
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);
//
//
//
//        // Enable the "aPosition" vertex attribute.
//        GLES20.glEnableVertexAttribArray(positionHandle);
//        GlUtil.checkGlError("glEnableVertexAttribArray");
//
//        // Connect vertexBuffer to "aPosition".
//        GLES20.glVertexAttribPointer(positionHandle, 3,
//                GLES20.GL_FLOAT, false, 3*FLOAT_SIZE_BYTES, TRANSFORM_RECTANGLE_BUF);
//        GlUtil.checkGlError("glVertexAttribPointer");
//
//        // Enable the "aTextureCoord" vertex attribute.
//        GLES20.glEnableVertexAttribArray(positionHandle);
//        GlUtil.checkGlError("glEnableVertexAttribArray");
//
//        // Connect texBuffer to "aTextureCoord".
//        GLES20.glVertexAttribPointer(textureParamHandle, 4,
//                GLES20.GL_FLOAT, false, 4*FLOAT_SIZE_BYTES, TRANSFORM_RECTANGLE_TEX_BUF);
//        GlUtil.checkGlError("glVertexAttribPointer");
//
//        Matrix.setIdentityM(mMVPMatrix, 0);
//        GLES20.glUniformMatrix4fv(textureCoordinateHandle, 1, false, mMVPMatrix, 0);
//        GLES20.glUniformMatrix4fv(textureTranformHandle, 1, false, mSTMatrix, 0);
//
//
//        // Draw the rect.
////        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
////        GlUtil.checkGlError("glDrawArrays");
////
////
////        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
////        checkGlError("glDrawArrays");
//
//
//        // Done -- disable vertex array, texture, and program.
//        GLES20.glDisableVertexAttribArray(positionHandle);
//        GLES20.glDisableVertexAttribArray(textureParamHandle);
//        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
//        GLES20.glUseProgram(0);

    }


    /**
     * Initializes GL state.  Call this after the EGL surface has been created and made current.
     */
    public void surfaceCreated() {
        //todo 着色器初始化
//         final String vertexShader = RawResourceReader.readTextFileFromRawResource(context, R.raw.vetext_sharder);
//        final String fragmentShader = RawResourceReader.readTextFileFromRawResource(context, R.raw.fragment_sharder);

        final int vertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        final int fragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        mProgram = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                new String[]{"texture", "vPosition", "vTexCoordinate", "textureTransform"});
//        mProgram = com.ryan.screenrecoder.coder.GlUtil.createProgram(VERTEX_SHADER,FRAGMENT_SHADER);
        GLES20.glUseProgram(mProgram);
        textureParamHandle = GLES20.glGetUniformLocation(mProgram, "texture");
        checkGlError("glBindTexture mTextureID");
        textureCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "vTexCoordinate");
        positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        textureTranformHandle = GLES20.glGetUniformLocation(mProgram, "textureTransform");
        checkGlError("glTexParameter");
        setupVertexBuffer();
        setupTexture();
//        int[] textures = new int[1];
//        GLES20.glGenTextures(1, textures, 0);
//
//        mTextureID = textures[0];
//        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);
//
//        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
//                GLES20.GL_NEAREST);
//        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
//                GLES20.GL_LINEAR);
//        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
//                GLES20.GL_CLAMP_TO_EDGE);
//        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
//                GLES20.GL_CLAMP_TO_EDGE);
//        checkGlError("glTexParameter");
    }
    private void setupVertexBuffer() {
        // Draw list buffer
        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        // Initialize the texture holder
        ByteBuffer bb = ByteBuffer.allocateDirect(squareCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());

        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);
    }
    private void setupTexture() {
        ByteBuffer texturebb = ByteBuffer.allocateDirect(textureCoords.length * 4);
        texturebb.order(ByteOrder.nativeOrder());

        textureBuffer = texturebb.asFloatBuffer();
        textureBuffer.put(textureCoords);
        textureBuffer.position(0);

        // Generate the actual texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glGenTextures(1, textures, 0);
        checkGlError("Texture generate");

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        checkGlError("Texture bind");
        mTextureID=textures[0];
//        videoTexture = new SurfaceTexture();
//        videoTexture.setOnFrameAvailableListener(this);
    }
    public void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }
}
