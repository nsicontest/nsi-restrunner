package it.nextworks.nsicontest.subprocess;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
 
public class RuntimeExec {
    static public class ExecutionError extends RuntimeException {

        public ExecutionError(String message) {
            super(message);
        }
    
    };
    
    static public StreamWrapper getStreamWrapper(PrintStream os, InputStream is, String prefix, StringBuffer buffer)
    {
                return new StreamWrapper(os, is, prefix, buffer);
    }
    
    public static class StreamWrapper extends Thread 
    {
        InputStream is = null;
        
        String prefix = null;          
        PrintStream os = null;
        StringBuffer buffer = null;

        StreamWrapper(PrintStream os, InputStream is, String prefix, StringBuffer buffer) 
        {
            this.os = os;
            this.is = is;
            this.prefix = prefix;
            this.buffer = buffer;
        }

        public void run() 
        {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line = null;
                while ( (line = br.readLine()) != null) {
                    os.println("["+prefix+"] "+line);
                    buffer.append(line);//.append("\n");
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();  
            }
        }
    }
}
  
