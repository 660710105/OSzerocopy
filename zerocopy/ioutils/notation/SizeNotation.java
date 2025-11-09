package zerocopy.ioutils.notation;

public enum SizeNotation {
    B(1),
    KiB(2),
    MiB(3),
    GiB(4);

    private int notationType;

    private SizeNotation(int notationtype) {
        this.notationType = notationtype;
    }

    public int getTypeLevel() {
        return notationType;
    }

    public String toString() {
        String notation = "";
        
        switch (this) {
        case SizeNotation.B:
            notation = "B";
            break;
        case SizeNotation.KiB:
            notation = "KiB";
            break;
        case SizeNotation.MiB:
            notation = "MiB";
            break;
        case SizeNotation.GiB:
            notation = "GiB";
            break;
        }

        return notation;
    }
}
