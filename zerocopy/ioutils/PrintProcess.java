package zerocopy.ioutils;

import zerocopy.ioutils.notation.Size;
import zerocopy.ioutils.notation.SizeConverter;
import zerocopy.ioutils.notation.SizeNotation;

public class PrintProcess implements Runnable {
    private static String process;
    private boolean isStop = false;

    public PrintProcess() {

    }

    public void setProcess(long process) {
        PrintProcess.process = SizeConverter
                                .toHighestSize(new Size(SizeNotation.B,(process)))
                                .toString();
    }

    @Override
    public void run() {
        while (!isStop) {
            if(!(process == null))
                System.out.printf("Download %s\n", process);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                //
            }
        }
    }
    public void stop(){
        isStop = true;
    }
}
