//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package sourceforge.zbar;


public class Image {
    private long peer;
    private Object data;

    private static native void init();

    public Image() {
        this.peer = this.create();
    }

    public Image(int var1, int var2) {
        this();
        this.setSize(var1, var2);
    }

    public Image(int var1, int var2, String var3) {
        this();
        this.setSize(var1, var2);
        this.setFormat(var3);
    }

    public Image(String var1) {
        this();
        this.setFormat(var1);
    }

    Image(long var1) {
        this.peer = var1;
    }

    private native long create();

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

    public Image convert(String var1) {
        long var2 = this.convert(this.peer, var1);
        return var2 == 0L ? null : new Image(var2);
    }

    private native long convert(long var1, String var3);

    public native String getFormat();

    public native void setFormat(String var1);

    public native int getSequence();

    public native void setSequence(int var1);

    public native int getWidth();

    public native int getHeight();

    public native int[] getSize();

    public native void setSize(int var1, int var2);

    public native void setSize(int[] var1);

    public native int[] getCrop();

    public native void setCrop(int var1, int var2, int var3, int var4);

    public native void setCrop(int[] var1);

    public native byte[] getData();

    public native void setData(byte[] var1);

    public native void setData(int[] var1);

    public SymbolSet getSymbols() {
        return new SymbolSet(this.getSymbols(this.peer));
    }

    private native long getSymbols(long var1);

    static {
        System.loadLibrary("zbar");
        init();
    }
}
