package crux.frontend.ast;

public final class Position {
    public final int line;

    public Position(int line) {
        this.line = line;
    }

    @Override
    public String toString() {
        return String.format("(%d)", line);
    }
}
