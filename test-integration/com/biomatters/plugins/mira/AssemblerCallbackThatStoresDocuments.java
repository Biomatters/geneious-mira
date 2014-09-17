package com.biomatters.plugins.mira;

import com.biomatters.geneious.publicapi.plugin.Assembler;
import com.biomatters.geneious.publicapi.documents.sequence.NucleotideSequenceDocument;
import com.biomatters.geneious.publicapi.documents.sequence.SequenceAlignmentDocument;
import com.biomatters.geneious.publicapi.plugin.AssemblerInput;
import com.biomatters.geneious.publicapi.plugin.DocumentOperationException;
import jebl.util.ProgressListener;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Matt Kearse
 * @version $Id$
 */
public class AssemblerCallbackThatStoresDocuments extends Assembler.Callback {
    private List<SequenceAlignmentDocument> contigs = new ArrayList<SequenceAlignmentDocument>();
    private List<NucleotideSequenceDocument> consensusSequences = new ArrayList<NucleotideSequenceDocument>();
    private List<AssemblerInput.Read> unusedReads = new ArrayList<AssemblerInput.Read>();
    private List<AssemblerInput.Read> usedReads = new ArrayList<AssemblerInput.Read>();
    @Override
    public void addContigDocument(SequenceAlignmentDocument contig, NucleotideSequenceDocument contigConsensus, boolean isThisTheOnlyContigGeneratedByDeNovoAssembly, ProgressListener progressListener) throws DocumentOperationException {
        contigs.add(contig);
        consensusSequences.add(contigConsensus);
    }

    @Override
    public void addUnusedRead(AssemblerInput.Read read, ProgressListener progressListener) throws DocumentOperationException {
        unusedReads.add(read);
    }

    @Override
    public void addUsedRead(AssemblerInput.Read read, ProgressListener progressListener) throws DocumentOperationException {
        usedReads.add(read);
    }

    public List<SequenceAlignmentDocument> getContigs() {
        return contigs;
    }

    public List<NucleotideSequenceDocument> getConsensusSequences() {
        return consensusSequences;
    }

    public List<AssemblerInput.Read> getUnusedReads() {
        return unusedReads;
    }

    public List<AssemblerInput.Read> getUsedReads() {
        return usedReads;
    }
}
