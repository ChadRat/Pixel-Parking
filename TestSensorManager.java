public class TestSensorManager {
    public static void manualRemap(float[] inR, float[] outR) {
        // New X = Original X
        outR[0] = inR[0];
        outR[3] = inR[3];
        outR[6] = inR[6];
        // New Y = -Original Z
        outR[1] = -inR[2];
        outR[4] = -inR[5];
        outR[7] = -inR[8];
        // New Z = Original Y
        outR[2] = inR[1];
        outR[5] = inR[4];
        outR[8] = inR[7];
    }
    public static float[] getOrientation(float[] R, float values[]) {
        values[0] = (float) Math.atan2(R[1], R[4]);
        return values;
    }
    public static float determinant(float[] m) {
        return m[0]*(m[4]*m[8] - m[5]*m[7])
             - m[1]*(m[3]*m[8] - m[5]*m[6])
             + m[2]*(m[3]*m[7] - m[4]*m[6]);
    }
    public static void main(String[] args) {
        // Camera pointing East, Portrait mode
        float[] R_east = {
             0,  0, -1,
            -1,  0,  0,
             0,  1,  0
        };
        float[] R_remap = new float[9];
        manualRemap(R_east, R_remap);
        System.out.println("Determinant: " + determinant(R_remap));
        float[] values = new float[3];
        getOrientation(R_remap, values);
        System.out.println("Azimuth: " + Math.toDegrees(values[0]));
        
        // Camera pointing North, Portrait mode
        float[] R_north = {
            1,  0,  0,
            0,  0, -1,
            0,  1,  0
        };
        manualRemap(R_north, R_remap);
        getOrientation(R_remap, values);
        System.out.println("Azimuth North: " + Math.toDegrees(values[0]));
    }
}
