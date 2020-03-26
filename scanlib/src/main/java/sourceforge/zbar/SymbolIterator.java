//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package sourceforge.zbar;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class SymbolIterator implements Iterator<Symbol> {
    private Symbol current;

    SymbolIterator(Symbol var1) {
        this.current = var1;
    }

    public boolean hasNext() {
        return this.current != null;
    }

    public Symbol next() {
        if (this.current == null) {
            throw new NoSuchElementException("access past end of SymbolIterator");
        } else {
            Symbol var1 = this.current;
            long var2 = this.current.next();
            if (var2 != 0L) {
                this.current = new Symbol(var2);
            } else {
                this.current = null;
            }

            return var1;
        }
    }

    public void remove() {
        throw new UnsupportedOperationException("SymbolIterator is immutable");
    }
}
