package com.ryan.screenrecoder.glec;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.ryan.screenrecoder.R;
import com.ryan.screenrecoder.util.RawResourceReader;
import com.ryan.screenrecoder.util.ShaderHelper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

/**
 * Created by zx315476228 on 17-3-2.
 */

public class SurfaceRender {
    private EGL10 egl;
    private EGLContext eglContext;
    private EGLDisplay eglDisplay;
    private EGLSurface eglSurface;


    private static short drawOrder[] = { 0, 1, 2, 0, 2, 3};
    private FloatBuffer vertexBuffer;
    private static float squareSize = 1.0f;
    private static float squareCoords[] = {
            -squareSize,  squareSize,   // top left
            -squareSize, -squareSize,   // bottom left
            squareSize, -squareSize,    // bottom right
            squareSize,  squareSize }; // top right

    private FloatBuffer textureBuffer;
    private float textureCoords[] = {
            0.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 1.0f, 0.0f, 1.0f };
    private ShortBuffer drawListBuffer;
    private int[] textures = new int[1];

    private SurfaceTexture videoTexture;
    private Context context;
    public SurfaceRender(Context context) {
        this.context=context;
//        initEGL();
        setupVertexBuffer();
        setupTexture();
        setupGraphics();
    }
//    private void initEGL(){
//        egl = (EGL10) EGLContext.getEGL();
//        //获取显示设备
//        eglDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
//        //version中存放EGL 版本号，int[0]为主版本号，int[1]为子版本号
//        int version[] = new int[2];
//        egl.eglInitialize(eglDisplay, version);
//
//        EGLConfig eglConfig = chooseEglConfig();
//        //创建EGL 的window surface 并且返回它的handles(eslSurface)
//        eglSurface = egl.eglCreateWindowSurface(eglDisplay, eglConfig, surfaceTexture, null);
//
//        eglContext = createContext(egl, eglDisplay, eglConfig);
//
//        //设置当前的渲染环境
//        try {
//            if (eglSurface == null || eglSurface == EGL10.EGL_NO_SURFACE) {
//                throw new RuntimeException("GL error:" + GLUtils.getEGLErrorString(egl.eglGetError()));
//            }
//            if (!egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
//                throw new RuntimeException("GL Make current Error"+ GLUtils.getEGLErrorString(egl.eglGetError()));
//            }
//        }catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
    /***
     *  refer to https://www.khronos.org/registry/egl/sdk/docs/man/
     * @return a EGL frame buffer configurations that match specified attributes
     */
    private EGLConfig chooseEglConfig() {
        int[] configsCount = new int[1];
        EGLConfig[] configs = new EGLConfig[1];
        int[] attributes = getAttributes();
        int confSize = 1;

        if (!egl.eglChooseConfig(eglDisplay, attributes, configs, confSize, configsCount)) {    //获取满足attributes的config个数
            throw new IllegalArgumentException("Failed to choose config:"+ GLUtils.getEGLErrorString(egl.eglGetError()));
        }
        else if (configsCount[0] > 0) {
            return configs[0];
        }

        return null;
    }
    /**
     * 构造绘制需要的特性列表,ARGB,DEPTH...
     */
    private int[] getAttributes()
    {
        return new int[] {
                EGL10.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,  //指定渲染api类别
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 0,
                EGL10.EGL_STENCIL_SIZE, 0,
                EGL10.EGL_NONE      //总是以EGL10.EGL_NONE结尾
        };
    }
    /**
     * 为当前渲染的API创建一个渲染上下文
     * @return a handle to the context
     */
    private EGLContext createContext(EGL10 egl, EGLDisplay eglDisplay, EGLConfig eglConfig) {
        int[] attrs = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL10.EGL_NONE
        };
        return egl.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, attrs);
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
//        checkGlError("Texture generate");

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
//        checkGlError("Texture bind");

        videoTexture = new SurfaceTexture(textures[0]);
    }
    public SurfaceTexture getSurfaceTexture(){
        return videoTexture;
    }
    private int shaderProgram;
    int textureParamHandle;
    int textureCoordinateHandle;
    int positionHandle;
    int textureTranformHandle;
    /**
     * 初始化着色器
     */
    private void setupGraphics() {
        final String vertexShader = RawResourceReader.readTextFileFromRawResource(context, R.raw.vetext_sharder);
        final String fragmentShader = RawResourceReader.readTextFileFromRawResource(context, R.raw.fragment_sharder);

        final int vertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        final int fragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);
        shaderProgram = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                new String[]{"texture","vPosition","vTexCoordinate","textureTransform"});

        GLES20.glUseProgram(shaderProgram);
        textureParamHandle = GLES20.glGetUniformLocation(shaderProgram, "texture");
        textureCoordinateHandle = GLES20.glGetAttribLocation(shaderProgram, "vTexCoordinate");
        positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
        textureTranformHandle = GLES20.glGetUniformLocation(shaderProgram, "textureTransform");
    }
}
