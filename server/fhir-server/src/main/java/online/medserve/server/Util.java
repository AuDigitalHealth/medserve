package online.medserve.server;

public class Util {
    public static int getCount(Integer theCount) {
        if (theCount == null || theCount == 0) {
            theCount = 10;
        } else if (theCount > 1000) {
            theCount = 1000;
        }
        return theCount;
    }
}
