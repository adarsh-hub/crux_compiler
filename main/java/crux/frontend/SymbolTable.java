package crux.frontend;

import crux.frontend.ast.Position;
import crux.frontend.types.*;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class SymbolTable {
    private final PrintStream err;
    private final List<Map<String, Symbol>> symbolScopes = new ArrayList<>();

    private boolean encounteredError = false;

    SymbolTable(PrintStream err) {
        this.err = err;
        // TODO
    }

    boolean hasEncounteredError() {
        return encounteredError;
    }

    void enter() {
        // TODO
    }

    void exit() {
        // TODO
    }

    Symbol add(Position pos, String name) {
        // TODO
        return null;
    }

    Symbol add(Position pos, String name, Type type) {
        // TODO
        return null;
    }

    Symbol lookup(Position pos, String name) {
        var symbol = find(name);
        if (symbol == null) {
            err.printf("ResolveSymbolError%s[Could not find %s.]%n", pos, name);
            encounteredError = true;
            return new Symbol(name, "ResolveSymbolError");
        } else {
            return symbol;
        }
    }

    private Symbol find(String name) {
        // TODO
        return null;
    }
}
