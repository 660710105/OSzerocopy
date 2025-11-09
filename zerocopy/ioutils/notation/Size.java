package zerocopy.ioutils.notation;

public class Size {
    private SizeNotation sizeNotation;
    private double size;

    public Size(SizeNotation sizeNotation, double size) {
        this.sizeNotation = sizeNotation;
        this.size = size;
    }

    public SizeNotation getSizeNotation() {
        return sizeNotation;
    }

    public double getSize() {
        return size;
    }

    @Override
    public String toString() {
        return String.format("%.2f %s", size, sizeNotation.toString());
    }
}
