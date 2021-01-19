package crux.frontend.types;

public final class BoolType extends Type {
    @Override
    public boolean equivalent(Type that) {
        return that.getClass() == BoolType.class;
    }

    @Override
    public String toString() {
        return "bool";
    }
}    
