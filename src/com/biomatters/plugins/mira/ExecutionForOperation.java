package com.biomatters.plugins.mira;

import com.biomatters.geneious.publicapi.plugin.DocumentOperationException;
import com.biomatters.geneious.publicapi.utilities.Execution;
import com.biomatters.geneious.publicapi.utilities.GeneralUtilities;
import jebl.util.ProgressListener;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ExecutionForOperation {
    public static OutputListenerThatNotifiesProgressListener execute(String humanReadableNameForProgram, List<String> commands, Map<String,String> environmentVariables, File workingDirectory, ProgressListener progressListener) throws DocumentOperationException {
        String[] commandArray = commands.toArray(new String[commands.size()]);
        OutputListenerThatNotifiesProgressListener outputListener = new OutputListenerThatNotifiesProgressListener(progressListener, humanReadableNameForProgram+": ");
        Execution execution = new Execution(commandArray,progressListener, outputListener,"",false);
        execution.setWorkingDirectory(workingDirectory.getPath());
        try {
            int result = execution.execute(environmentVariables);
            if (result!=0) {
                String message = humanReadableNameForProgram + " failed with error code "+result+"\n";
                String standardError = outputListener.getStandardError();
                String standardOut = outputListener.getStandardOut();
                message+=standardError;
                message+=standardOut;
                if (message.length()>10000) {
                    message = message.substring(0,5000)+"\n...\n"+message.substring(message.length()-4990,message.length());
                }
                GeneralUtilities.println("** stderr:%s",standardError);
                GeneralUtilities.println("** stdout:%s",standardOut);
                throw new DocumentOperationException(message);
            }
        } catch (InterruptedException e) {
            throw new DocumentOperationException("Process Killed!", e);
        } catch (IOException e) {
            throw new DocumentOperationException(humanReadableNameForProgram
                    +" failed:" + e.getMessage(), e);
        }
        return outputListener;
    }

    public static class OutputListenerThatNotifiesProgressListener extends Execution.OutputListener {
        private List<String> currentMessages = new LinkedList<String>();
        private final ProgressListener progressListener;
        private final String progressPrefix;
        private StringBuilder standardError = new StringBuilder();
        private StringBuilder standardOut1 = new StringBuilder();
        private StringBuilder standardOut2 = new StringBuilder();
        private StringBuilder standardOut3 = new StringBuilder();
        private boolean discardedTooMuchDataFromStandardOut = false;
        private int maxStandardOutBufferLength = 1000*1000;

        public OutputListenerThatNotifiesProgressListener(ProgressListener progressListener, String progressPrefix) {
            this.progressListener = progressListener;
            this.progressPrefix = progressPrefix;
        }

        public String getStandardError() {
            return standardError.toString();
        }

        public String getStandardOut() {
            return standardOut1.toString()+(discardedTooMuchDataFromStandardOut?"\n...\n\n":"")+standardOut2.toString()+standardOut3.toString();
        }

        public void setMaxStandardOutBufferLength(int maxStandardOutBufferLength) {
            if (standardOut1.length()>0)
                throw new IllegalStateException("setMaxStandardOutBufferLength must be called before writing to standard out");
            this.maxStandardOutBufferLength = maxStandardOutBufferLength;
        }

        public void addProgressMessage(String message) {
            currentMessages.add(progressPrefix+message);
            if (currentMessages.size()>5)
                currentMessages.remove(0);
            StringBuilder completeMessage = new StringBuilder();
            for (int i = 0; i < currentMessages.size(); i++) {
                String currentMessage = currentMessages.get(i);
                completeMessage.append("\n");
                String fontColor = null;
                if (i<currentMessages.size()-1) {
                    char ch = (char) ('9' - i*2);
                    String c = ""+ ch;
                    fontColor = "<font color=#"+c+c+c+c+c+c+">";
                    completeMessage.append(fontColor);
                }
                completeMessage.append(currentMessage);
                if (fontColor!=null)
                    completeMessage.append("</font>");
            }
            progressListener.setMessage(completeMessage.toString());
        }

        @Override
        public void stdoutWritten(String output) {
            addProgressMessage(output);
            // standardOut1 contains the first 1MB. standardOut2 + standardOut3 contain the last 1 to 2 MB.
            if (standardOut1.length()> maxStandardOutBufferLength) {
                if (standardOut2.length()> maxStandardOutBufferLength) {
                    if (standardOut3.length() > maxStandardOutBufferLength) {
                        discardedTooMuchDataFromStandardOut = true;
                        standardOut2 = standardOut3;
                        standardOut3 = new StringBuilder();
                    }
                    standardOut3.append(output).append("\n");
                }
                else {
                    standardOut2.append(output).append("\n");
                }
            }
            else {
                standardOut1.append(output).append("\n");
            }
        }

        @Override
        public void stderrWritten(String output) {
            addProgressMessage(output);
            standardError.append(output).append("\n");
        }
    }
}
