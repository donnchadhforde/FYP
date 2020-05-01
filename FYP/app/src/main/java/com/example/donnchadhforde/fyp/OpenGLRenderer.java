package com.example.donnchadhforde.fyp;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class OpenGLRenderer implements GLSurfaceView.Renderer {

    static Quaternion quat = new Quaternion(0.0f, 1.0f, 0.0f, 0.0f);

    private Cube mCube;
    private float mCubeRotation;

    // vPMatrix is an abbreviation for "Model View Projection Matrix"
    private final float[] vPMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private float[] rotationMatrix = new float[16];

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mCube = new Cube();
        GLES20.glClearColor(0.5f, 0.8f, 1.0f, 1.0f);
        GLES20.glClearDepthf(1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        float[] scratch = new float[16];
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(viewMatrix, 0, 0, 0, -5, 0f, 0f, 0f, 0.0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        // Create a rotation transformation for the triangle
//        long time = SystemClock.uptimeMillis() % 4000L;
//        float angle = 0.090f * ((int) time);
//        Matrix.setRotateM(rotationMatrix, 0, angle, -1.0f, 1.0f, -1.0f);
        float[] quaternion = new float[4];
        quaternion[0] = quat.qw;
        quaternion[1] = quat.qx;
        quaternion[2] = quat.qy;
        quaternion[3] = quat.qz;

        rotateM(rotationMatrix, 0, quaternion);

        // Set Rotation Matrix with the quaternion variable
        Matrix.setRotateM(rotationMatrix, 0,
        (float) (2.0f * Math.acos(quat.qw) * 180.0f / Math.PI),
        quat.qx, quat.qy, quat.qz);
        //rotationMatrix = quat.toMatrix();

        // Combine the rotation matrix with the projection and camera view
        // Note that the vPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
        Matrix.multiplyMM(scratch, 0, vPMatrix, 0, rotationMatrix, 0);

        // Draw shape
        mCube.draw(scratch);

    }

    public static int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    public static void rotateM(float[] m, int mOffset, float[] quaternion) {
        float[] sTemp = new float[32];
        toMatrix(sTemp, quaternion);
        Matrix.multiplyMM(sTemp, 16, m, mOffset, sTemp, 0);
        System.arraycopy(sTemp, 16, m, mOffset, 16);
    }

    public static void toMatrix(float[] matrix, float[] quaternion) {
        if (quaternion.length != 4) {
            throw new IllegalArgumentException("Wrong length of quaternion");
        }
        if (matrix.length < 16) {
            throw new IllegalArgumentException("Not enough space to write the result");
        }
        matrix[3] = 0.0f;
        matrix[7] = 0.0f;
        matrix[11] = 0.0f;
        matrix[12] = 0.0f;
        matrix[13] = 0.0f;
        matrix[14] = 0.0f;
        matrix[15] = 1.0f;

        matrix[0] = (1.0f - (2.0f * ((quaternion[1] * quaternion[1]) + (quaternion[2] * quaternion[2]))));
        matrix[1] = (2.0f * ((quaternion[0] * quaternion[1]) - (quaternion[2] * quaternion[3])));
        matrix[2] = (2.0f * ((quaternion[0] * quaternion[2]) + (quaternion[1] * quaternion[3])));

        matrix[4] = (2.0f * ((quaternion[0] * quaternion[1]) + (quaternion[2] * quaternion[3])));
        matrix[5] = (1.0f - (2.0f * ((quaternion[0] * quaternion[0]) + (quaternion[2] * quaternion[2]))));
        matrix[6] = (2.0f * ((quaternion[1] * quaternion[2]) - (quaternion[0] * quaternion[3])));

        matrix[8] = (2.0f * ((quaternion[0] * quaternion[2]) - (quaternion[1] * quaternion[3])));
        matrix[9] = (2.0f * ((quaternion[1] * quaternion[2]) + (quaternion[0] * quaternion[3])));
        matrix[10] = (1.0f - (2.0f * ((quaternion[0] * quaternion[0]) + (quaternion[1] * quaternion[1]))));
    }
}
