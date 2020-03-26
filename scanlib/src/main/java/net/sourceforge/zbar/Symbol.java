//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package net.sourceforge.zbar;


public class Symbol {
    public static final int NONE = 0;
    public static final int PARTIAL = 1;
    public static final int EAN8 = 8;
    public static final int UPCE = 9;
    public static final int ISBN10 = 10;
    public static final int UPCA = 12;
    public static final int EAN13 = 13;
    public static final int ISBN13 = 14;
    public static final int I25 = 25;
    public static final int DATABAR = 34;
    public static final int DATABAR_EXP = 35;
    public static final int CODABAR = 38;
    public static final int CODE39 = 39;
    public static final int PDF417 = 57;
    public static final int QRCODE = 64;
    public static final int CODE93 = 93;
    public static final int CODE128 = 128;
    private long peer;
    private int type;

    private static native void init();

    Symbol(long var1) {
        this.peer = var1;
    }

    protected void finalize() {
        this.destroy();
    }

    public synchronized void destroy() {
        if (this.peer != 0L) {
            this.destroy(this.peer);
            this.peer = 0L;
        }

    }

    private native void destroy(long var1);

    public int getType() {
        if (this.type == 0) {
            this.type = this.getType(this.peer);
        }

        return this.type;
    }

    private native int getType(long var1);

    public native int getConfigMask();

    public native int getModifierMask();

    public native String getData();

    public native byte[] getDataBytes();

    public native int getQuality();

    public native int getCount();

    public int[] getBounds() {
        int var1 = this.getLocationSize(this.peer);
        if (var1 <= 0) {
            return null;
        } else {
            int[] var2 = new int[4];
            int var3 = 2147483647;
            int var4 = -2147483648;
            int var5 = 2147483647;
            int var6 = -2147483648;

            for(int var7 = 0; var7 < var1; ++var7) {
                int var8 = this.getLocationX(this.peer, var7);
                if (var3 > var8) {
                    var3 = var8;
                }

                if (var4 < var8) {
                    var4 = var8;
                }

                int var9 = this.getLocationY(this.peer, var7);
                if (var5 > var9) {
                    var5 = var9;
                }

                if (var6 < var9) {
                    var6 = var9;
                }
            }

            var2[0] = var3;
            var2[1] = var5;
            var2[2] = var4 - var3;
            var2[3] = var6 - var5;
            return var2;
        }
    }

    private native int getLocationSize(long var1);

    private native int getLocationX(long var1, int var3);

    private native int getLocationY(long var1, int var3);

    public int[] getLocationPoint(int var1) {
        int[] var2 = new int[]{this.getLocationX(this.peer, var1), this.getLocationY(this.peer, var1)};
        return var2;
    }

    public native int getOrientation();

    public SymbolSet getComponents() {
        return new SymbolSet(this.getComponents(this.peer));
    }

    private native long getComponents(long var1);

    native long next();

    static {
        System.loadLibrary("zbar");
        init();
    }
}
