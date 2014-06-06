package com.biomatters.plugins.mira;

import com.biomatters.geneious.publicapi.documents.sequence.ImmutableSequence;
import com.biomatters.geneious.publicapi.documents.sequence.NucleotideSequenceDocument;
import com.biomatters.geneious.publicapi.implementations.SequenceExtractionUtilities;
import com.biomatters.geneious.publicapi.implementations.sequence.DefaultSequenceDocument;
import com.biomatters.geneious.publicapi.plugin.AssemblerInput;
import com.biomatters.geneious.publicapi.plugin.DocumentOperationException;
import com.biomatters.geneious.publicapi.utilities.FileUtilities;
import com.biomatters.geneious.publicapi.utilities.GeneralUtilities;
import com.biomatters.geneious.publicapi.utilities.SequenceUtilities;
import jebl.util.ProgressListener;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// todo put something like this in the Assembler API to make it easier for assemblers to be implemented.
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
public class MiraInputFileManager {
    private String fileNamePrefix;
    private List<MiraDataType> allowedTypes;

    public class Library {
        private File file;
        private PrintWriter out;
        private MiraDataType dataType;
        private final int id;
        private int insertSize;

        public Library(MiraDataType dataType, int insertSize, int id) throws IOException {
            this.dataType = dataType;
            this.id = id;
            String name= fileNamePrefix+"_in_"+id+".fastq";
            file = new File(directory, name);
            out = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            this.insertSize = insertSize;
        }

        private void close() {
            if (out!=null)
                out.close();
            out = null;
        }

        @Override
        public String toString() {
            return "Library{" +
                    "insertSize=" + dataType +
                    " file="+file.getName()+
                    '}';
        }

        public File getFile() {
            return file;
        }

        public MiraDataType getDataType() {
            return dataType;
        }

        public String getManifestData() {
            StringBuilder manifest = new StringBuilder();
            manifest.append("readgroup = group").append(id).append("\n");
            manifest.append("data = ").append(getFile().getName()).append("\n");
            manifest.append("technology = ").append(getDataType().getFileName()).append("\n");
            if (insertSize>0) {
                int minInsertSize = insertSize - insertSize/4;
                int maxInsertSize = insertSize + insertSize/4;
                manifest.append("template_size  = ").append(minInsertSize).append(" ").append(maxInsertSize).append("\n");
                manifest.append("segment_placement = ---> <---\n");
                manifest.append("segment_naming = solexa\n"); // We always use the /1 /2 naming scheme for now
            }
            manifest.append("\n");
            return manifest.toString();
        }

    }

    private List<Library> libraries = new ArrayList<Library>();
    private File directory;

    public File getDirectory() {
        return directory;
    }

    public enum MiraDataType {
        Illumina("solexa","Illumina (Solexa)"), Type454("454","454"),IonTorrent("iontor","Ion Torrent"), Sanger("sanger","Sanger"), PacBioHq("pcbiohq","PacBio");

        private final String fileName;
        private final String label;

        private MiraDataType(String fileName, String label) {
            this.fileName = fileName;
            this.label = label;
        }

        public String getFileName() {
            return fileName;
        }

        public String getLabel() {
            return label;
        }
    }


    public MiraInputFileManager(String fileNamePrefix, AssemblerInput.Reads reads,List<MiraDataType> allowedTypes, ProgressListener progressListener, int numberOfReadSequences) throws DocumentOperationException  {
        this.fileNamePrefix = fileNamePrefix;
        this.allowedTypes = allowedTypes;
        try {
            directory = FileUtilities.createTempDir(false);
        } catch (IOException e) {
            throw new DocumentOperationException(e);
        }

        addReads(reads, progressListener, numberOfReadSequences);
    }

    public List<Library> getLibraries() {
        return libraries;
    }

    private static File tempDirectoryToKeep = null;

    /**
     * A bit of a hack to keep the mira data around until either Geneious is restarted or another mira job is run. This is so
     * we can report the mira data and command run in the assembly report so the user can grab any extra data they want
     * and to make it easier to work out why it might have failed.
     */
    public static synchronized void removePreviousMiraTempDataKeptAfterItFinished(File newTempDirectoryToKeep) {
        if (tempDirectoryToKeep!=null) {
            GeneralUtilities.println("Deleting old temporary MIRA directory "+tempDirectoryToKeep+" now");
            FileUtilities.deleteDirectory(tempDirectoryToKeep, ProgressListener.EMPTY);
        }
        tempDirectoryToKeep = newTempDirectoryToKeep;
        if (newTempDirectoryToKeep!=null) {
            GeneralUtilities.println("Keeping temporary MIRA directory "+newTempDirectoryToKeep+" in place until either another MIRA job is run or Geneious is restarted");
        }
    }

    public void cleanUp() {
        for (Library library : libraries) {
            library.close();
        }
        removePreviousMiraTempDataKeptAfterItFinished(directory);
    }

    private MiraDataType getMiraDataType(AssemblerInput.DataType probableDataType) {
        List<MiraDataType> prefs = Arrays.asList(MiraDataType.Illumina, MiraDataType.Sanger, MiraDataType.IonTorrent, MiraDataType.Type454, MiraDataType.PacBioHq);
        if (probableDataType == AssemblerInput.DataType.IonTorrent)
            prefs = Arrays.asList(MiraDataType.IonTorrent, MiraDataType.Type454, MiraDataType.Sanger, MiraDataType.PacBioHq, MiraDataType.Illumina);
        else if (probableDataType == AssemblerInput.DataType.Type454)
            prefs = Arrays.asList(MiraDataType.Type454, MiraDataType.IonTorrent, MiraDataType.Sanger, MiraDataType.PacBioHq, MiraDataType.Illumina);
        else if (probableDataType == AssemblerInput.DataType.Sanger)
            prefs = Arrays.asList(MiraDataType.Sanger, MiraDataType.IonTorrent, MiraDataType.Type454, MiraDataType.PacBioHq, MiraDataType.Illumina);
        else if (probableDataType == AssemblerInput.DataType.PacBio)
            prefs = Arrays.asList(MiraDataType.PacBioHq, MiraDataType.Type454, MiraDataType.IonTorrent, MiraDataType.Sanger, MiraDataType.Illumina);
        for (MiraDataType pref : prefs) {
            if (allowedTypes.contains(pref))
                return pref;
        }
        throw new IllegalStateException();
    }


    private void addReads(AssemblerInput.Reads reads, ProgressListener progressListener, int numberOfReadSequences) throws DocumentOperationException {
        try {
            int readsDone = 0;
            while(reads.hasNext()) {
                AssemblerInput.Read readPair = reads.getNextReadPair();
                NucleotideSequenceDocument read = readPair.getReadNormalized();
                NucleotideSequenceDocument mate = readPair.getMateNormalizedReversed();
                if (mate!=null) { // So that the names of a forward and reversed pair now match for pairing purposes
                    read = stripReversedSuffix(read);
                    mate = stripReversedSuffix(mate);
                }
                read = removeInvalidNameCharacters(read);
                mate = removeInvalidNameCharacters(mate);
                readsDone++;
                if (readsDone%1000==0 && progressListener.setMessage(String.format("Preparing reads: Done %,d of %,d",readsDone,numberOfReadSequences)))
                    throw new DocumentOperationException.Canceled();
                if (mate!=null) {
                    readsDone++;
                    if (readsDone%1000==0 && progressListener.setMessage(String.format("Preparing reads: Done %,d of %,d",readsDone,numberOfReadSequences)))
                        throw new DocumentOperationException.Canceled();
                    if (read.getName().endsWith("/2") && mate.getName().endsWith("/1")) {
                        // Normalize so that /1 is the first read and /2 is the second one
                        NucleotideSequenceDocument temp = read;
                        read = mate;
                        mate = temp;
                    }
                    String name1 = read.getName();
                    String name2 = mate.getName();
                    if (!name1.endsWith("/1") || !name2.endsWith("/2")) {
                        // We need to normalise the names so that they end with /1 and /2:
                        int prefixLength = 0;
                        String newNamePrefix = name1+"_"+name2;
                        if (name1.length()==name2.length()) {
                            while (prefixLength<name1.length() && name1.charAt(prefixLength)==name2.charAt(prefixLength)) {
                                prefixLength++;
                            }
                            if (prefixLength>0 && prefixLength>=name1.length()-1) {
                                newNamePrefix = name1.substring(0,prefixLength);
                            }
                        }
                        if (!newNamePrefix.endsWith("/"))
                            newNamePrefix+="/";

                        read = createCopyWithName(read, newNamePrefix+"1");
                        mate = createCopyWithName(mate, newNamePrefix+"2");
                    }
                }
                int insertSize = readPair.getExpectedMateDistanceNormalized();
                if (mate==null)
                    insertSize = 0;
                else if (insertSize<1)
                    insertSize = 1;
                MiraDataType miraDataType = getMiraDataType(readPair.getProbableDataType());
                Library library = getLibrary(miraDataType, insertSize);
                MiraFastqWriter.writeFastqSequence(library.out, read, ProgressListener.EMPTY, true, true);
                if (mate!=null) {
                    MiraFastqWriter.writeFastqSequence(library.out, mate, ProgressListener.EMPTY, true, true);
                }
            }
            for (Library library : libraries) {
                library.close();
            }
        } catch (IOException e) {
            cleanUp();
            throw new DocumentOperationException(e);
        }
    }

    private static NucleotideSequenceDocument stripReversedSuffix(NucleotideSequenceDocument read) {
        if (read.getName().endsWith(SequenceExtractionUtilities.REVERSED_NAME_SUFFIX)) {
            read = createCopyWithName(read,read.getName().substring(0,read.getName().length()-SequenceExtractionUtilities.REVERSED_NAME_SUFFIX.length()));
        }
        return read;
    }

    private static NucleotideSequenceDocument removeInvalidNameCharacters(NucleotideSequenceDocument read) {
        if (read==null)
            return read;
        String name = read.getName();
        String newName = name;
        newName = newName.replace("(","_");
        newName = newName.replace(")","_");
        if (!newName.equals(name)) {
            return createCopyWithName(read, newName);
        }
        else {
            return read;
        }
    }

    private static NucleotideSequenceDocument createCopyWithName(NucleotideSequenceDocument read, String newName) {
        if (read instanceof ImmutableSequence) {
            ImmutableSequence immutableSequence = (ImmutableSequence) read;
            return (NucleotideSequenceDocument) immutableSequence.createCopyWithNewName(newName);
        }
        else {
            DefaultSequenceDocument newSequence = SequenceUtilities.createSequenceCopyEditable(read);
            newSequence.setName(newName);
            return (NucleotideSequenceDocument) newSequence;
        }
    }


    private Library getLibrary(MiraDataType dataType, int insertSize) throws IOException {
        for (int i = 0; i < libraries.size(); i++) {
            Library library = libraries.get(i);
            if (library.dataType == dataType && library.insertSize ==insertSize) {
                if (i>0) {
                    libraries.set(i,libraries.get(0));
                    libraries.set(0,library);
                }
                return library;
            }
        }
        Library result = new Library(dataType, insertSize, libraries.size());
        libraries.add(0,result);
        return result;
    }
}
