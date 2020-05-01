package com.example.donnchadhforde.fyp;

public class Quaternion {

    public float qw, qx, qy, qz;

    public Quaternion(float qw, float qx, float qy, float qz) {
        this.qw = qw;
        this.qx = qx;
        this.qy = qy;
        this.qz = qz;
    }

    public float[] toMatrix () {

        float[] rotation = new float[16];
        final float xx = qx * qx;
        final float xy = qx * qy;
        final float xz = qx * qz;
        final float xw = qx * qw;
        final float yy = qy * qy;
        final float yz = qy * qz;
        final float yw = qy * qw;
        final float zz = qz * qz;
        final float zw = qz * qw;
        final float ww = qw * qw;

        rotation[0] = 2*ww - 1+2*xx;
        rotation[1] = 2 * (xy + zw);
        rotation[2] = 2 * (xz + yw);
        rotation[3] = 0;
        rotation[4] = 2 * (xy - zw);
        rotation[5] = 2*ww - 1 + 2*yy;
        rotation[6] = 2 * (yz + xw);
        rotation[7] = 0;
        rotation[8] = 2 * (xz + yw);
        rotation[9] = 2 * (yz - xw);
        rotation[10] = 2 * ww - 1 + 2 * zz;
        rotation[11] = 0;
        rotation[12] = 0;
        rotation[13] = 0;
        rotation[14] = 0;
        rotation[15] = 1;

        return rotation;


        // Set matrix from quaternion
//        matrix[Matrix4.M00] = 1 - 2 * (yy + zz);
//        matrix[Matrix4.M01] = 2 * (xy - zw);
//        matrix[Matrix4.M02] = 2 * (xz + yw);
//        matrix[Matrix4.M03] = 0;
//        matrix[Matrix4.M10] = 2 * (xy + zw);
//        matrix[Matrix4.M11] = 1 - 2 * (xx + zz);
//        matrix[Matrix4.M12] = 2 * (yz - xw);
//        matrix[Matrix4.M13] = 0;
//        matrix[Matrix4.M20] = 2 * (xz - yw);
//        matrix[Matrix4.M21] = 2 * (yz + xw);
//        matrix[Matrix4.M22] = 1 - 2 * (xx + yy);
//        matrix[Matrix4.M23] = 0;
//        matrix[Matrix4.M30] = 0;
//        matrix[Matrix4.M31] = 0;
//        matrix[Matrix4.M32] = 0;
//        matrix[Matrix4.M33] = 1;
    }

}
