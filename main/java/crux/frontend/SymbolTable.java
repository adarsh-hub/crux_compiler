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
        Symbol new_symbol1 = new Symbol("readInt", new FuncType(TypeList.of(), new IntType()));
        Symbol new_symbol2 = new Symbol("readChar", new FuncType(TypeList.of(), new IntType()));
        Symbol new_symbol3 = new Symbol("printBool", new FuncType(TypeList.of(new BoolType()), new VoidType()));
        Symbol new_symbol4 = new Symbol("printInt", new FuncType(TypeList.of(new IntType()), new VoidType()));
        Symbol new_symbol5 = new Symbol("printChar", new FuncType(TypeList.of(new IntType()), new VoidType()));
        Symbol new_symbol6 = new Symbol("println", new FuncType(TypeList.of(), new VoidType()));
        HashMap<String, Symbol> hm1 = new HashMap<>();
        HashMap<String, Symbol> hm2 = new HashMap<>();
        HashMap<String, Symbol> hm3 = new HashMap<>();
        HashMap<String, Symbol> hm4 = new HashMap<>();
        HashMap<String, Symbol> hm5 = new HashMap<>();
        HashMap<String, Symbol> hm6 = new HashMap<>();
        hm1.put("readInt", new_symbol1);
        hm2.put("readChar", new_symbol2);
        hm3.put("printBool", new_symbol3);
        hm4.put("printInt", new_symbol4);
        hm5.put("printChar", new_symbol5);
        hm6.put("println", new_symbol6);
        this.symbolScopes.add(hm1);
        this.symbolScopes.add(hm2);
        this.symbolScopes.add(hm3);
        this.symbolScopes.add(hm4);
        this.symbolScopes.add(hm5);
        this.symbolScopes.add(hm6);

    }

    boolean hasEncounteredError() {
        return encounteredError;
    }

    void enter() {
        HashMap<String, Symbol> hm = new HashMap<>();
        symbolScopes.add(hm);
    }

    void exit() {
        symbolScopes.remove(symbolScopes.size() - 1);
    }

    Symbol add(Position pos, String name) { //pos: line number in crux name: name of the symbol
        HashMap<String, Symbol> hm = new HashMap<>();
        Symbol s = new Symbol(name);
        hm.put(name, s);
        if(!symbolScopes.contains(hm))
        {
            symbolScopes.add(hm);
        }
        else
        {
            symbolScopes.remove(hm);
            symbolScopes.add(hm);
        }
        return s;
    }

    Symbol add(Position pos, String name, Type type) {
        HashMap<String, Symbol> hm = new HashMap<>();
        Symbol s = new Symbol(name, type);
        hm.put(name, s);
        symbolScopes.add(hm);
        if(!symbolScopes.contains(hm))
        {
            symbolScopes.add(hm);
        }//Report an error
        else
        {
            var rem_s = find(name);
            HashMap<String, Symbol> hm2 = new HashMap<>();
            hm2.put(name, rem_s);
            symbolScopes.remove(hm2);
            symbolScopes.add(hm);
        }
        return s;
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

    private Symbol find(String name) { //check the most recent scope
        for(Map<String, Symbol> node : symbolScopes){
            if(node.containsKey(name)){
                return node.get(name);
            }
        }
        return null;
    }
}