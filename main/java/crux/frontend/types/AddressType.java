package crux.frontend.types;

import java.util.stream.Stream;

public final class AddressType extends Type {
    private final Type base;

    public AddressType(Type base) {
        this.base = base;
    }

    public Type getBaseType() { return base; }

    @Override
    public boolean equivalent(Type that) {
        if (that.getClass() != AddressType.class)
            return false;

        var aType = (AddressType) that;
        return base.equivalent(aType.base);
    }

    @Override
    public String toString() {
        return "Address(" + base + ")";
    }
}
