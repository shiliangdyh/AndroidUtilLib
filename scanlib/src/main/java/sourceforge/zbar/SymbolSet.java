//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package sourceforge.zbar;

import java.util.AbstractCollection;
import java.util.Iterator;

public class SymbolSet extends AbstractCollection<Symbol> {
    private long peer;

    private static native void init();

    SymbolSet(long var1) {
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

    public Iterator<Symbol> iterator() {
        long var1 = this.firstSymbol(this.peer);
        return var1 == 0L ? new SymbolIterator((Symbol)null) : new SymbolIterator(new Symbol(var1));
    }

    public native int size();

    private native long firstSymbol(long var1);

    static {
        System.loadLibrary("zbar");
        init();
    }
}
