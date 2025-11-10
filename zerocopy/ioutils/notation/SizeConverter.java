package zerocopy.ioutils.notation;

public class SizeConverter {
    public static Size convert(SizeNotation toNotation, Size sourceSize) {
        int srcLevel = sourceSize.getSizeNotation().getTypeLevel();
        int desLevel = toNotation.getTypeLevel();

        if (srcLevel > desLevel) {
            double convertNeededValue = Math.pow(1024, srcLevel - desLevel);
            return new Size(toNotation, sourceSize.getSize() * convertNeededValue);
        } else if (desLevel > srcLevel) {
            double convertNeededValue = Math.pow(1024, desLevel - srcLevel);
            return new Size(toNotation, sourceSize.getSize() / convertNeededValue);
        } else {
            return sourceSize;
        }
    }

    public static Size toHighestSize(Size sourceSize) {
        // convert to GiB first
        Size GiBSize = convert(SizeNotation.GiB, sourceSize);
        double rawSize = GiBSize.getSize();

        if (rawSize >= 1.00F) {
            return GiBSize;
        } else if (rawSize >= 0.00097F) {
            return convert(SizeNotation.MiB, sourceSize);
        } else if (rawSize >= 0.00000095367431640625F) {
            return convert(SizeNotation.KiB, sourceSize);
        } else {
            // then it can be only bytes, do not convert
            return sourceSize;
        }
    }
}
