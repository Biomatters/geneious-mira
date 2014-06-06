package com.biomatters.plugins.mira;

import com.biomatters.geneious.publicapi.components.Dialogs;
import com.biomatters.geneious.publicapi.plugin.AssemblerInput;
import com.biomatters.geneious.publicapi.plugin.OperationLocationOptions;
import com.biomatters.geneious.publicapi.plugin.Options;
import com.biomatters.geneious.publicapi.utilities.StandardIcons;
import com.biomatters.geneious.publicapi.utilities.SystemUtilities;
import com.google.common.base.Splitter;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
public class MiraOptions extends Options {
    private static final OptionValue draft = new OptionValue("draft","Draft","A quick and dirty assembly for first insights");
    private static final OptionValue accurate = new OptionValue("accurate","Accurate","An assembly that should be able to tackle even most nasty cases");

    private static final OptionValue genome = new OptionValue("genome","Genome / contiguous sequence","For assembling data into a larger contiguous sequence");
    private static final OptionValue est = new OptionValue("est","EST / mRNA", "For assembling small fragments like in EST or mRNA libraries");

    private static final DateTypeOptionValue data_454 = new DateTypeOptionValue(MiraInputFileManager.MiraDataType.Type454);
    private static final DateTypeOptionValue data_ionTorrent = new DateTypeOptionValue(MiraInputFileManager.MiraDataType.IonTorrent);
    private static final DateTypeOptionValue data_solexa = new DateTypeOptionValue(MiraInputFileManager.MiraDataType.Illumina);
    private static final DateTypeOptionValue data_sanger = new DateTypeOptionValue(MiraInputFileManager.MiraDataType.Sanger);
    private static final DateTypeOptionValue data_pacBio = new DateTypeOptionValue(MiraInputFileManager.MiraDataType.PacBioHq);
    private static final List<DateTypeOptionValue> dataTypes = Arrays.asList(data_solexa,data_ionTorrent, data_454,data_sanger, data_pacBio);
    public static final String FAIL_IF_COVERAGE_LEVEL_EXCEEDS = "Fail if coverage level exceeds";
    private final StringOption parameters;
    private final BooleanOption failWithHighCoverage;
    private final IntegerOption failWithCoverageOver;

    public static final String highCoverageDescriptionFromMiraManual = wrapLongLines("In genome de-novo assemblies, MIRA will perform checks early in the assembly process whether the average coverage to be expected exceeds a given value.\n" +
            "\n" +
            "With todays' sequencing technologies (especially Illumina, but also Ion Torrent and 454), many people simply take everything they get and throw it into an assembly. Which, in the case of Illumina and Ion, can mean they try to assemble their organism with a coverage of 100x, 200x and more (I've seen trials with more than 1000x).\n" +
            "\n" +
            "This is not good. Not. At. All! For two reasons (well, three to be precise).\n" +
            "\n" +
            "The first reason is that, usually, one does not sequence a single cell but a population of cells. If this population is not clonal (i.e., it contains subpopulations with genomic differences with each other), assemblers will be able to pick up these differences in the DNA once a certain sequence count is reached and they will try reconstruct a genome containing all clonal variations, treating these variations as potential repeats with slightly different sequences. Which, of course, will be wrong and I am pretty sure you do not want that.\n" +
            "\n" +
            "The second and way more important reason is that none of the current sequencing technologies is completely error free. Even more problematic, they contain both random and non-random sequencing errors. Especially the latter can become a big hurdle if these non-random errors are so prevalent that they suddenly appear to be valid sequence to an assembler. This in turn leads to false repeat detection, hence possibly contig breaks or even wrong consensus sequence. You don't want that, do you?\n" +
            "\n" +
            "The last reason is that overlap based assemblers (like MIRA is) need exponentially more time and memory when the coverage increases. So keeping the coverage comparatively low helps you there.",
            80);
    private final AssemblerInput.Properties inputProperties;

    private static String wrapLongLines(String text, int maxLineLength) {
        StringBuilder result = new StringBuilder();
        for (String line : Splitter.on("\n").split(text)) {
            if (line.length()>maxLineLength) {
                StringBuilder newLine = new StringBuilder();
                for (String word : Splitter.on(" ").split(line)) {
                    if (newLine.length()>0 && newLine.length()+word.length()>maxLineLength) {
                        result.append(newLine).append("\n");
                        newLine.setLength(0);
                    }
                    if (newLine.length()>0) {
                        newLine.append(" ");
                    }
                    newLine.append(word);
                }
                if (newLine.length()>0) {
                    result.append(newLine).append("\n");
                    newLine.setLength(0);
                }
            }
            else {
                result.append(line).append("\n");
            }
        }
        return result.toString();
    }

    private final BooleanOption removeTrimAnnotationsFromResults;

    private static class DateTypeOptionValue extends OptionValue {
        private DateTypeOptionValue(MiraInputFileManager.MiraDataType dataType) {
            super(dataType.getFileName(), dataType.getLabel());
        }


    }

    private static MiraInputFileManager.MiraDataType getDataType(String name) {
        for (MiraInputFileManager.MiraDataType dataType : MiraInputFileManager.MiraDataType.values()) {
            if (dataType.getFileName().equals(name))
                return dataType;
        }
        throw new IllegalArgumentException("unknown name "+name);
    }

    private List<BooleanOption> dataTypeOptions = new ArrayList<BooleanOption>();
    private final RadioOption<OptionValue> level;
    private final RadioOption<OptionValue> sequenceType;
    private OperationLocationOptions operationLocationOptions;

    public MiraOptions(AssemblerInput.Properties inputProperties, OperationLocationOptions _operationLocationOptions) {
        operationLocationOptions = _operationLocationOptions;
        if (is32BitWithLocalLocationOptions()) {
            addLabelWithIcon("<html><b>MIRA requires a 64 bit operating system</b></html>", StandardIcons.warning.getIcons());
        }
        this.inputProperties = inputProperties;
        beginAlignHorizontally(null,false);
        sequenceType = addRadioOption("sequenceType", "Data Type:", Arrays.asList(genome, est), genome,Alignment.HORIZONTAL_ALIGN);
        endAlignHorizontally();
        beginAlignHorizontally(null,false);
        level = addRadioOption("level", "Quality Level:", Arrays.asList(draft, accurate), accurate,Alignment.HORIZONTAL_ALIGN);
        addCustomComponent((JComponent)Box.createHorizontalStrut(65));
        ((LabelOption)addLabel("<html><i><a href=\"http://mira-assembler.sourceforge.net/\">MIRA</a> 4.0</i></html>")).setSelectable(true);
        endAlignHorizontally();
        beginAlignHorizontally("Potential Technology Types:",false);
        for (OptionValue dataType : dataTypes) {
            BooleanOption e = addBooleanOption(dataType.getName(), dataType.getLabel(), true);
            e.setAdvanced(true);
            e.setDescription("<html>Geneious can usually automatically assign the correct data type to each input data set. Uncheck technology<br>types definitely not used as input to assist Geneious in assigning the correct type to each data set.</html>");
            dataTypeOptions.add(e);
        }
        endAlignHorizontally();
        beginAlignHorizontally(null, false);
        failWithHighCoverage = addBooleanOption("failWithHighCoverage", FAIL_IF_COVERAGE_LEVEL_EXCEEDS, true);
        String highCoverageHelp = "<html>"+highCoverageDescriptionFromMiraManual.replace("\n","<br>")+"</html>";
        failWithHighCoverage.setDescription(highCoverageHelp);
        failWithCoverageOver = addIntegerOption("failWithCoverageOver", "", 80, 1, 9999999);
        failWithCoverageOver.setDescription(highCoverageHelp);
        failWithHighCoverage.addDependent(failWithCoverageOver, true);
        failWithHighCoverage.setAdvanced(true);
        failWithCoverageOver.setAdvanced(true);
        addHelpButtonOption("High Coverage",highCoverageHelp).setAdvanced(true);
        endAlignHorizontally();

        removeTrimAnnotationsFromResults = addBooleanOption("removeTrimAnnotationsFromResults", "Remove trim annotations from results", false);
        removeTrimAnnotationsFromResults.setAdvanced(true);
        removeTrimAnnotationsFromResults.setDescription("<html>" +
                "For optimal results, MIRA trims sequences and identifies regions in the contigs as trimmed. Geneious<br>" +
                "will ignore these regions trimmed when calling consensus sequences which may not be ideal. This option<br>" +
                "will remove the trim annotations from the results prior to calling consensus sequences.</html>");

        beginAlignHorizontally(null,false);
        parameters = addStringOption("parameters", "Additional Parameters:", "");

        parameters.setDescription("<html>Specify any MIRA additional parameters for the 'parameters' option documented in the <a href=\"http://mira-assembler.sourceforge.net/docs/DefinitiveGuideToMIRA.html#sect_ref_manifest_parameters\">MIRA manual</a></html>");
        parameters.setFillHorizontalSpace(true);
        parameters.setAdvanced(true);
        addHelpButtonOption("Additional Parameters",parameters.getDescription()).setAdvanced(true);
        endAlignHorizontally();

    }

    private boolean is32BitWithLocalLocationOptions() {
        return !SystemUtilities.is64BitOS() && (operationLocationOptions==null || operationLocationOptions.getSelectedLocation() == null);
    }

    @Override
    public String verifyOptionsAreValid() {
        if (is32BitWithLocalLocationOptions()) {
            String osName = SystemUtilities.isWindows()?"Windows":SystemUtilities.isMac()?"MacOS":"Linux";
            return "Unfortunately you are using 32 bit "+osName+". You'll need to upgrade to 64 bit "+osName+" to run MIRA.";
        }

        for (BooleanOption dataTypeOption : dataTypeOptions) {
            if (dataTypeOption.getValue())
                return null;
        }
        return "You ust select at least one technology type.";
    }

    @Override
    public boolean areValuesGoodEnoughToContinue() {
        if (inputProperties.getNumberOfSequences()>10*1000*1000) {
            String message = String.format("There are %,d reads. Provided you have enough memory, MIRA is expected to work on data sets of up to " +
                    "20 million short reads or 50 million for EST / RNASeq. Are you sure you want to continue?",inputProperties.getNumberOfSequences());
            if (Dialogs.showDialogWithDontShowAgain(new Dialogs.DialogOptions(Dialogs.CONTINUE_CANCEL, "Potentially too many reads", null, Dialogs.DialogIcon.WARNING),message,"miraOptionsWarning","Don't show again")==Dialogs.CANCEL)
                return false;
        }
        return super.areValuesGoodEnoughToContinue();
    }

    public List<MiraInputFileManager.MiraDataType> getAllowedDataTypes() {
        List<MiraInputFileManager.MiraDataType> results = new ArrayList<MiraInputFileManager.MiraDataType>();
        for (BooleanOption dataTypeOption : dataTypeOptions) {
            if (dataTypeOption.getValue())
                results.add(getDataType(dataTypeOption.getName()));
        }
        return results;
    }

    public String getJobCommand() {
        return "job = denovo,"+sequenceType.getValue().getName()+","+level.getValue().getName();
    }

    public String getParameters() {
        StringBuilder parameters = new StringBuilder();
        parameters.append(" -NAG_AND_WARN:check_maxreadnamelength=no");// Stop it failing on long read names which are fine in Geneious
        if (failWithHighCoverage.getValue()) {
            parameters.append(" -NAG_AND_WARN:average_coverage_value=").append(failWithCoverageOver.getValue());
        }
        else {
            parameters.append(" -NAG_AND_WARN:check_average_coverage=warn");
        }
        String moreParameters = this.parameters.getValue();
        if (!moreParameters.isEmpty()) {
            parameters.append(" ");
            parameters.append(moreParameters);
        }

        return parameters.toString();
    }

    public boolean isRemoveTrimAnnotations() {
        return removeTrimAnnotationsFromResults.getValue();
    }

    public void setFailWithHighCoverage(boolean fail) {
        failWithHighCoverage.setValue(fail);
    }
}
