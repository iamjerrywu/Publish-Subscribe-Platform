package Debug;

import java.io.FileNotFoundException;
import java.io.PrintStream;

public class Debug {
    public Debug() {}
    public void redirectPrint(String fileName ) {
        try {
            PrintStream fileOut = new PrintStream(fileName);
            System.setOut(fileOut);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }
}
