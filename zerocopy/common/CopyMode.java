package zerocopy.common;

public enum CopyMode {
    COPY(1),
    ZEROCOPY(2),
    COPY_MULTITHREAD(3),
    ZEROCOPY_MULTITHREAD(4);

    private int mode;
    
    CopyMode(int mode) {
        this.mode = mode;
    }

    public static CopyMode fromMode(int mode) {
        for (CopyMode _mode : CopyMode.values()) {
            if (_mode.getMode() == mode) {
                return _mode;
            }
        }
        return CopyMode.COPY;
    }

    public int getMode() {
        return mode;
    }
    
    public String toString() {
        // no need to break due to returning
        switch (this) {
        case COPY:
            return "COPY";
        case ZEROCOPY:
            return "ZEROCOPY";
        case COPY_MULTITHREAD:
            return "COPY_MULTITHREAD";
        case ZEROCOPY_MULTITHREAD:
            return "ZEROCOPY_MULTITHREAD";
        }
        return "";
    }
}
