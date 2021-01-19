package crux.frontend.types;

public final class ArrayType extends Type {
    private final Type base;
    private final long extent;

    public ArrayType(long extent, Type base) {
        this.extent = extent;
        this.base = base;
    }

    public Type getBase() {
        return base;
    }

    public long getExtent() { return extent; }

    @Override
    public String toString() {
        return String.format("array[%d,%s]", extent, base);
    }
}
