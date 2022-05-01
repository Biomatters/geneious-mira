package com.biomatters.plugins.mira;

import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.documents.sequence.SequenceAlignmentDocument;
import com.biomatters.geneious.publicapi.documents.sequence.SequenceDocument;
import com.biomatters.geneious.publicapi.plugin.AssemblerInput;
import com.biomatters.geneious.publicapi.plugin.DocumentOperationException;
import com.biomatters.geneious.publicapi.plugin.TestGeneious;
import com.biomatters.geneious.publicapi.utilities.SequenceUtilities;
import com.biomatters.geneious.publicapi.databaseservice.WritableDatabaseService;
import com.biomatters.geneious.publicapi.databaseservice.DatabaseServiceException;
import com.biomatters.geneious.publicapi.plugin.*;
import jebl.util.ProgressListener;
import junit.framework.TestCase;
import org.junit.Assume;

import java.lang.System;
import java.util.Collections;
import java.io.*;
import java.util.List;
import java.net.URL;

public class MiraAssemblerTest extends TestCase {
    public void testOnSampleSangerData_WithDefaultSettings_Works() throws DocumentOperationException {
        testIt(importTestDataFile("newSamplePairwiseContig.geneious",false),false,null, 2);
    }

    public void testOnSampleIlluminaData_WithDefaultSettings_FailsBecauseOfHighCoverage() throws DocumentOperationException {
        testIt(importTestDataFile("sampleDocsIlluminaReads.geneious",false),false,MiraOptions.highCoverageDescriptionFromMiraManual.substring(0,60),-1);
    }

    public void testOnSampleIlluminaData_WithMaximumCoverageOff_Works() throws DocumentOperationException {
        testIt(importTestDataFile("sampleDocsIlluminaReads.geneious",false),true,null,4600);
    }

    public void testOnSampleIonTorrentData_WithDefaultSettings_Works() throws DocumentOperationException {
        testIt(importTestDataFile("sampleDocsIonTorrentReads.geneious",false),false,null,2400);
    }


    private static final int VERSION_32BIT_DROPPED = 202000;
    public void testWithDataThatCrashesUnder32Bit_Works() throws DocumentOperationException {
        if(Geneious.getMinorApiVersion() < VERSION_32BIT_DROPPED) {
            testIt(importTestDataFile("miraCrashInput.geneious",false),false,null,9900);
        }
    }

    public static AnnotatedPluginDocument importTestDataFile(String name, boolean addToLocalDatabase) {
        TestGeneious.initializePlugins("com.biomatters.plugins.local.LocalDatabasePlugin", "com.biomatters.plugins.abi.ChromatographPlugin");
        try {
            WritableDatabaseService localDatabase = (WritableDatabaseService)PluginUtilities.getGeneiousService("LocalDocuments"); // even if not adding to database, we still need to get it to force initalization of note types so that we can import geneious format files
            AnnotatedPluginDocument doc = PluginUtilities.importDocuments(getTestDataFile(MiraAssemblerTest.class, name), ProgressListener.EMPTY).get(0);
            if (addToLocalDatabase) {
                doc=localDatabase.addDocumentCopy(doc, ProgressListener.EMPTY);
            }
            return doc;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (DocumentImportException e) {
            throw new RuntimeException(e);
        } catch (DatabaseServiceException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param name filename relative to the test data directory
     * @param resourceOf package that file is in
     * @return a file that is a resource in the package of resourceof
     */
    public static File getTestDataFile(Class resourceOf, String name) {
        final URL resource = resourceOf.getResource(name);
        if (resource == null) {
            throw new IllegalArgumentException("No resource " + name + " in " + resourceOf.getName());
        }
        return new File(resource.getFile().replace("%20", " "));
    }

    private void testIt(AnnotatedPluginDocument inputDoc, boolean turnOffMaximumCoverage, String expectedFailureMessageSubstring, int minimumSequencesInResultContig) throws DocumentOperationException {
        MiraAssembler assembler = new MiraAssembler();
        MiraOptions options = (MiraOptions) assembler.getOptions(null, new AssemblerInput.Properties(SequenceUtilities.getNumberOfSequences(inputDoc, SequenceDocument.Alphabet.NUCLEOTIDE)));
        if (turnOffMaximumCoverage) {
            options.setFailWithHighCoverage(false);
        }
        AssemblerInput input = new AssemblerInput(Collections.<AnnotatedPluginDocument>singletonList(inputDoc),Collections.<AssemblerInput.ReferenceSequence>emptyList(),true);
        AssemblerCallbackThatStoresDocuments callback = new AssemblerCallbackThatStoresDocuments();
        try {
            assembler.assemble(options,input, ProgressListener.EMPTY, callback);
            if (expectedFailureMessageSubstring!=null)
                fail("It worked, but it shouldn't have");
        } catch (DocumentOperationException e) {
            if (expectedFailureMessageSubstring!=null) {
                System.out.println("It failed, but that's OK because we expected it to");
                assertTrue(e.getMessage().contains(expectedFailureMessageSubstring));
                return;
            }
            e.printStackTrace();
            System.out.println("******* MIRA failed when we didn't expect it to. It does crash in rare situations (maybe it has a multi-threaded bug), so trying again...");
            assembler.assemble(options,input, ProgressListener.EMPTY, callback);
        }
        assertEquals(1,callback.getConsensusSequences().size());
        SequenceAlignmentDocument contig = getSingleton(callback.getContigs());
        int resultSequenceCount = contig.getSequences().size();
        if (resultSequenceCount<minimumSequencesInResultContig) {
            fail("Expected a contig with at least "+minimumSequencesInResultContig+" sequences but got one with only "+resultSequenceCount);
        }
    }

    /**
     * Assert the list contains 1 element and return it.
     * @param list a List
     * @return the single element
     */
    public static <T> T getSingleton(List<T> list) {
        assertEquals(1, list.size());
        return list.get(0);
    }
}
