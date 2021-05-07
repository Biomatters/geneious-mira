package com.biomatters.plugins.mira;

import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.documents.sequence.*;
import com.biomatters.geneious.publicapi.plugin.*;
import com.biomatters.geneious.publicapi.utilities.*;
import jebl.util.ProgressListener;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Copyright (C) 2014, Biomatters Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
public class MiraAssembler extends Assembler {
    @Override
    public String getUniqueId() {
        return "MiraAssembler";
    }

    @Override
    public String getName() {
        return "MIRA";
    }

    @Override
    public boolean canRunOnGeneiousServer() {
        return true;
    }

    @Override
    public boolean canRunOnLocalGeneious() {
        return true;
    }

    @Override
    public Options getOptions(OperationLocationOptions locationOptions, AssemblerInput.Properties inputProperties) throws DocumentOperationException {
        return new MiraOptions(inputProperties, locationOptions);
    }

    @Override
    public ReferenceSequenceSupport getReferenceSequenceSupport() {
        return ReferenceSequenceSupport.NoReferenceSequence;
    }

    @Override
    public ContigOutputSupport getContigOutputSupport() {
        return ContigOutputSupport.ContigsAndConsensus;
    }

    @Override
    public boolean providesUnusedReads() {
        return false;
    }

    // We don't use the large contigs output anyway, so just ignore this error message:
    @SuppressWarnings("SpellCheckingInspection")
    private static final String largeContigsErrorWeCanIgnore = "Could not find executable 'miraconvert' for extracting large contigs?\n" +
            "Large contigs could not be extracted, sorry.\n" +
            "\n" +
            "DON'T PANIC! In the directory\n" +
            "    'geneious_assembly/geneious_d_results'\n" +
            "you will find a script called   'extractLargeContigs.sh'\n" +
            "which shows you how to extract large contigs from your assembly. Eitherread" + // DO NOT FIX THIS TYPO - MIRA has this typo and we want to find this error message in the MIRA output.
            " it to understand how it's done or ... just run it :-)";

    @Override
    public void assemble(Options _options, AssemblerInput assemblyInput, ProgressListener progressListener, Callback callback) throws DocumentOperationException {
        MiraInputFileManager.removePreviousMiraTempDataKeptAfterItFinished(null);
        MiraOptions options = (MiraOptions) _options;
        AssemblerInput.Reads reads = assemblyInput.getReads();
        MiraInputFileManager inputManager = new MiraInputFileManager("geneious",reads, options.getAllowedDataTypes(), progressListener, (int) assemblyInput.getNumberOfReadSequences());
        try {
            String exeName = SystemUtilities.isWindows()?
                    "windows/bin/mira.exe": // This needs to have 'bin' in its path because something in mira or cygwin looks for other files in the relative location (../bin/)
                    SystemUtilities.isMac()?
                            "mac/bin/mira":  // The mac build of mira looks for a shared library in directory ../lib/ relative to the executable, so we use mac/bin/ and mac/lib/
                            "linux/mira";
            File miraExecutable = FileUtilities.getResourceForClass(getClass(), exeName);

            StringBuilder manifest = new StringBuilder();

            manifest.append("project = geneious\n");
            manifest.append(options.getJobCommand()).append("\n");

            manifest.append("parameters =").append(options.getParameters()).append("\n");
            Map<String,String> environmentVariables = new HashMap<String, String>();
            if (SystemUtilities.isLinux()) {
                environmentVariables.put("LC_ALL", "C"); // C is the catch-all value for locale settings, required to prevent an assertion fault
            }
            manifest.append("\n");
            for (MiraInputFileManager.Library library : inputManager.getLibraries()) {
                manifest.append(library.getManifestData());
            }

            List<String> commands = new ArrayList<String>();
            commands.add(miraExecutable.getPath());
            File manifestFile = new File(inputManager.getDirectory(),"manifest.txt");
            try {
                FileUtilities.writeTextToFile(manifestFile, manifest.toString());
            } catch (IOException e) {
                throw new DocumentOperationException(e);
            }
            commands.add(manifestFile.getName());
            GeneralUtilities.println("MIRA: in directory: " + inputManager.getDirectory());
            GeneralUtilities.println("MIRA: run command: " + StringUtilities.join(" ", commands));
            String commandReport = "From temporary directory: " + inputManager.getDirectory() + "\n" +
                    "Ran command: " + StringUtilities.join(" ", commands) + "\n" +
                    "The temporary directory will be deleted either next time MIRA is run or Geneious is restarted.";
            ExecutionForOperation.OutputListenerThatNotifiesProgressListener outputListener;
            String errorToIgnore = "sh: uname: command not found\n";
            try {
                progressListener.setMessage("Starting MIRA now...");
                outputListener = ExecutionForOperation.execute("MIRA", commands, environmentVariables, inputManager.getDirectory(), progressListener);
            } catch (DocumentOperationException e) {
                File stackDumpFile = new File(inputManager.getDirectory(),miraExecutable.getName()+".stackdump");
                String message = e.getMessage();
                message = message.replace(errorToIgnore,"");
                message = message.replace(largeContigsErrorWeCanIgnore,"");
                if (message.contains("High average coverage detected")) {
                    throw new DocumentOperationException("MIRA: High average coverage detected.\n\n"+MiraOptions.highCoverageDescriptionFromMiraManual+
                            "\n\n"+"If you really want to go ahead with this high level of coverage, turn off the\nadvanced option called '"+MiraOptions.FAIL_IF_COVERAGE_LEVEL_EXCEEDS+"...'"+
                            "\n\n\n"+message+"\n\n"+commandReport);
                }
                else {
                    message = commandReport+"\n\n"+message;
                    if (stackDumpFile.exists()) {
                        message = "MIRA crashed. This is a bug in MIRA, not Geneious. To report this bug, use the MIRA bug tracking system at http://sourceforge.net/p/mira-assembler/tickets/\n\n" +
                                "If it is small enough you may want to include a copy of the input data from "+inputManager.getDirectory()+"\n\n\n"+message;
                    }
                    else {
                        message = "MIRA failed:\n\n"+message;
                    }
                    throw new DocumentOperationException(message);
                }
            }
            outputListener.addProgressMessage("Assembly complete. Importing results...");
            String assemblyReport =
                    "<pre>MIRA output:\n" + outputListener.getStandardError().replace(errorToIgnore,"") + "\n" + outputListener.getStandardOut().replace(largeContigsErrorWeCanIgnore,"") + "\n" +
                            commandReport + "</pre>";
            int maxReportLength = 100*1000;
            if (assemblyReport.length()>maxReportLength) {
                String extraMessage = String.format("\n\n\n... ****** MIRA output (%,d bytes) is too long, details omitted here ****** ...\n\n\n", assemblyReport.length());
                int halfSizeOfFragmentToKeep = maxReportLength/2 - extraMessage.length()/2;
                assemblyReport = assemblyReport.substring(0,halfSizeOfFragmentToKeep)+ extraMessage +  assemblyReport.substring(assemblyReport.length()-halfSizeOfFragmentToKeep,assemblyReport.length());
            }
            callback.addAssemblyReportText(assemblyReport);
            File contigsName = new File(inputManager.getDirectory(),"geneious_assembly/geneious_d_results/geneious_out.caf");
            File consensusName = new File(inputManager.getDirectory(),"geneious_assembly/geneious_d_results/geneious_out.unpadded.fasta");
            if (!consensusName.exists()) {
                throw new DocumentOperationException("MIRA didn't produce any reports.\n"+assemblyReport);
            }
            List<NucleotideSequenceDocument> consensusSequences = getConsensusSequencesFromResultsFile(consensusName);
            if (assemblyInput.isGenerateContigs()) {
                List<AnnotatedPluginDocument> results;
                try {
                    results = PluginUtilities.importDocuments(contigsName, ProgressListener.EMPTY);
                    if (results.size()!=consensusSequences.size())
                        throw new IllegalStateException("results.size="+results.size()+" cons="+consensusSequences.size());
                    for (int i = 0; i < results.size(); i++) {
                        AnnotatedPluginDocument result = results.get(i);
                        SequenceAlignmentDocument contig = (SequenceAlignmentDocument) result.getDocument();
                        if (options.isRemoveTrimAnnotations())
                            stripTrimAnnotations(contig);
                        callback.addContigDocument(contig, consensusSequences.get(i), consensusSequences.size() == 1, ProgressListener.EMPTY);
                    }
                } catch (IOException e) {
                    throw new DocumentOperationException(e);
                } catch (DocumentImportException e) {
                    throw new DocumentOperationException(e);
                }

            }
            else {
                for (NucleotideSequenceDocument consensusSequence : consensusSequences) {
                    callback.addContigDocument(null, consensusSequence, consensusSequences.size()==1,ProgressListener.EMPTY);
                }
            }
        } finally {
            inputManager.cleanUp();
        }
    }

    private static void stripTrimAnnotations(SequenceAlignmentDocument contig) {
        List<SequenceDocument> sequences = contig.getSequences();
        for (int i = 0; i < sequences.size(); i++) {
            SequenceDocument sequenceDocument = sequences.get(i);
            NucleotideGraph graph = sequenceDocument instanceof NucleotideGraph? (NucleotideGraph) sequenceDocument :null;
            if (sequenceDocument instanceof ImmutableSequence) {
                ImmutableSequence immutableSequence = (ImmutableSequence) sequenceDocument;
                if (immutableSequence.getLeadingTrimLength() > 0 && immutableSequence.getTrailingTrimLength() > 0) {
                    immutableSequence = immutableSequence.withTrimLengths(0, 0);
                }
                contig.updateSequence(i, immutableSequence, Collections.<SequenceAnnotation>emptyList(), graph, false);
            } else {
                List<SequenceAnnotation> trimAnnotations = SequenceUtilities.getAnnotationsOfType(sequenceDocument, SequenceAnnotation.TYPE_TRIMMED, false);
                if (!trimAnnotations.isEmpty()) {
                    List<SequenceAnnotation> newAnnotations = new ArrayList<SequenceAnnotation>(sequenceDocument.getSequenceAnnotations());
                    newAnnotations.removeAll(trimAnnotations);
                    contig.updateSequence(i, sequenceDocument.getCharSequence(), newAnnotations, graph, false);
                }
            }
        }
    }

    private List<NucleotideSequenceDocument> getConsensusSequencesFromResultsFile(File resultFile) throws DocumentOperationException {
        try {
            List<SequenceDocument> sequenceDocuments = ImportUtilities.importFastaSequences(resultFile, SequenceDocument.Alphabet.NUCLEOTIDE, ProgressListener.EMPTY);
            List<NucleotideSequenceDocument> nucleotideSequenceDocuments = new ArrayList<NucleotideSequenceDocument>();
            for (SequenceDocument sequenceDocument : sequenceDocuments) {
                nucleotideSequenceDocuments.add((NucleotideSequenceDocument)sequenceDocument);
            }
            if (nucleotideSequenceDocuments.isEmpty())
                throw new DocumentOperationException("MIRA produced no results");
            return nucleotideSequenceDocuments;
        } catch (IOException e) {
            throw new DocumentOperationException(e);
        } catch (DocumentImportException e) {
            throw new DocumentOperationException(e);
        }
    }



}
