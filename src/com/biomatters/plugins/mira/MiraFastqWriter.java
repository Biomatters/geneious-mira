package com.biomatters.plugins.mira;

import com.biomatters.geneious.publicapi.documents.sequence.NucleotideGraphSequenceDocument;
import com.biomatters.geneious.publicapi.documents.sequence.NucleotideSequenceDocument;
import com.biomatters.geneious.publicapi.implementations.SequenceExtractionUtilities;
import jebl.util.ProgressListener;

import java.io.IOException;
import java.io.PrintWriter;

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
 *
 *
 * A copy of FastqWriter from Geneious, but bundled with MIRA so that we don't have to make the latest version of MIRA require Geneious 8.0 since
 * rna conversion support was only added to FastqWriter for Geneious 8.0
 */
public class MiraFastqWriter {
    public static void writeFastqSequence(PrintWriter out, NucleotideSequenceDocument sequence, ProgressListener progressListener, boolean stripReversedSuffix) throws IOException {
        writeFastqSequence(out, sequence, progressListener, stripReversedSuffix, false);
    }

    public static void writeFastqSequence(PrintWriter out, NucleotideSequenceDocument sequence, ProgressListener progressListener, boolean stripReversedSuffix, boolean convertRnaToDna) throws IOException {
        if (progressListener.isCanceled())
            throw new IOException("Canceled");
        if (!(sequence instanceof NucleotideGraphSequenceDocument) || !((NucleotideGraphSequenceDocument)sequence).hasSequenceQualities()) {
            throw new IOException("The Geneious MIRA plugin wrapper doesn't support sequences without quality. The MIRA author has this to say on this topic:\n\n" +
                    "Assembling sequences without quality values is like ... like ... like driving a car downhill a sinuous mountain road with no rails at " +
                    "200 km/h without brakes, airbags and no steering wheel. With a ravine on one side and a rock face on the other. Did I mention the missing " +
                    "seat-belts? You might get down safely, but experience tells the result will rather be a bloody mess.\n" +
                    "\n" +
                    "Well, assembling without quality values is a bit like above, but bloodier. And the worst: you (or the people using the results of such " +
                    "an assembly) will notice the gore only until it is way too late and money has been sunk in follow-up experiments based on wrong data.\n" +
                    "\n" +
                    "All MIRA routines internally are geared toward quality values guiding decisions. No one should ever assembly anything without quality " +
                    "values. Never. Ever. Even if quality values are sometimes inaccurate, they do help.\n\n" +
                    "Now, there are very rare occasions where getting quality values is not possible. If you absolutely cannot get them, " +
                    "and I mean only in this case, use the following switch:--noqualities[=SEQUENCINGTECHNOLOGY]\n\n" +
                    "Note: This Geneious MIRA wrapper plugin won't export to fasta, so you'll need to run MIRA from the command line if you want to use that setting," +
                    " but for reasons mentioned above, you probably shouldn't bother."+
                    "\n");
        }
        NucleotideGraphSequenceDocument graphSequence = (NucleotideGraphSequenceDocument) sequence;
        String name = graphSequence.getName();
        if (stripReversedSuffix && name.endsWith(SequenceExtractionUtilities.REVERSED_NAME_SUFFIX)) {
            name = name.substring(0,name.length()-SequenceExtractionUtilities.REVERSED_NAME_SUFFIX.length());
        }
        name = name.replace(' ','_');
        out.println("@"+name);
        CharSequence charSequence = sequence.getCharSequence();
        if (convertRnaToDna) {
            charSequence = charSequence.toString().replace('U','T').replace('u','t');
        }
        out.println(charSequence);
        out.println("+"+name);
        StringBuilder qualityString = new StringBuilder();
        final int length = graphSequence.getSequenceLength();
        for (int i = 0; i < length; i++) {
            int quality = graphSequence.getSequenceQuality(i);
            // this doesn't get written right for non-ascii chars
            // I can't find a max value for actual fastq format, but mayhem starts going down at 7F which is the first control char after 33
            char qualityChar = (char) Math.min(126, 33+quality);
            qualityString.append(qualityChar);
            assert qualityString.length() == i + 1;
            out.print(qualityChar);
        }
        out.println();
    }
}
