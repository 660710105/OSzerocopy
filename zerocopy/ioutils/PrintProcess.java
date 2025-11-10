package zerocopy.ioutils;

public class PrintProcess implements Runnable {
    private static String process;
    private boolean isStop = false;

    public PrintProcess() {

    }

    public void setProcess(String process) {
        PrintProcess.process = process;
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
