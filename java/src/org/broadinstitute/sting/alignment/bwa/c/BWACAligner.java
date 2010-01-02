package org.broadinstitute.sting.alignment.bwa.c;

import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMFileHeader;
import org.broadinstitute.sting.utils.StingException;
import org.broadinstitute.sting.alignment.Alignment;
import org.broadinstitute.sting.alignment.bwa.BWAConfiguration;
import org.broadinstitute.sting.alignment.bwa.BWTFiles;
import org.broadinstitute.sting.alignment.bwa.BWAAligner;

import java.util.*;

/**
 * An aligner using the BWA/C implementation.
 *
 * @author mhanna
 * @version 0.1
 */
public class BWACAligner extends BWAAligner {
    static {
        System.loadLibrary("bwa");
    }

    /**
     * A pointer to the C++ object representing the BWA engine.
     */
    private long thunkPointer = 0;

    public BWACAligner(BWTFiles bwtFiles, BWAConfiguration configuration) {
        super(bwtFiles,configuration);
        if(thunkPointer != 0)
            throw new StingException("BWA/C attempting to reinitialize.");

        if(!bwtFiles.annFile.exists()) throw new StingException("ANN file is missing; please rerun 'bwa aln' to regenerate it.");
        if(!bwtFiles.ambFile.exists()) throw new StingException("AMB file is missing; please rerun 'bwa aln' to regenerate it.");
        if(!bwtFiles.pacFile.exists()) throw new StingException("PAC file is missing; please rerun 'bwa aln' to regenerate it.");
        if(!bwtFiles.forwardBWTFile.exists()) throw new StingException("Forward BWT file is missing; please rerun 'bwa aln' to regenerate it.");
        if(!bwtFiles.forwardSAFile.exists()) throw new StingException("Forward SA file is missing; please rerun 'bwa aln' to regenerate it.");
        if(!bwtFiles.reverseBWTFile.exists()) throw new StingException("Reverse BWT file is missing; please rerun 'bwa aln' to regenerate it.");
        if(!bwtFiles.reverseSAFile.exists()) throw new StingException("Reverse SA file is missing; please rerun 'bwa aln' to regenerate it.");

        thunkPointer = create(bwtFiles,configuration);
    }

    /**
     * Create an aligner object using an array of bytes as a reference.
     * @param referenceSequence Reference sequence to encode ad-hoc.
     * @param configuration Configuration for the given aligner.
     */
    public BWACAligner(byte[] referenceSequence, BWAConfiguration configuration) {
        this(BWTFiles.createFromReferenceSequence(referenceSequence),configuration);    
    }

    /**
     * Update the configuration passed to the BWA aligner.
     * @param configuration New configuration to set.
     */
    @Override
    public void updateConfiguration(BWAConfiguration configuration) {
        if(thunkPointer != 0)
            throw new StingException("BWA/C: attempting to update configuration of uninitialized aligner.");
        updateConfiguration(thunkPointer,configuration);
    }

    /**
     * Close this instance of the BWA pointer and delete its resources.
     */
    @Override
    public void close() {
        if(thunkPointer == 0)
            throw new StingException("BWA/C close attempted, but BWA/C is not properly initialized.");
        destroy(thunkPointer);
        super.close();
    }

    /**
     * Allow the aligner to choose one alignment randomly from the pile of best alignments.
     * @param bases Bases to align.
     * @return An align
     */
    @Override
    public Alignment getBestAlignment(final byte[] bases) {
        if(thunkPointer == 0)
            throw new StingException("BWA/C getBestAlignment attempted, but BWA/C is not properly initialized.");
        return getBestAlignment(thunkPointer,bases);
    }

    /**
     * Get the best aligned read, chosen randomly from the pile of best alignments.
     * @param read Read to align.
     * @param newHeader New header to apply to this SAM file.  Can be null, but if so, read header must be valid.
     * @return Read with injected alignment data.
     */
    @Override
    public SAMRecord align(final SAMRecord read, final SAMFileHeader newHeader) {
        return Alignment.convertToRead(getBestAlignment(read.getReadBases()),read,newHeader);   
    }

    /**
     * Get a iterator of alignments, batched by mapping quality.
     * @param bases List of bases.
     * @return Iterator to alignments.
     */
    @Override
    public Iterable<Alignment[]> getAllAlignments(final byte[] bases) {
        final BWAPath[] paths = getPaths(bases);
        return new Iterable<Alignment[]>() {
            public Iterator<Alignment[]> iterator() {
                return new Iterator<Alignment[]>() {
                    /**
                     * The last position accessed.
                     */
                    private int position = 0;

                    /**
                     * Whether all alignments have been seen based on the current position.
                     * @return True if any more alignments are pending.  False otherwise.
                     */
                    public boolean hasNext() { return position < paths.length; }

                    /**
                     * Return the next cross-section of alignments, based on mapping quality.
                     * @return Array of the next set of alignments of a given mapping quality.
                     */
                    public Alignment[] next() {
                        if(position >= paths.length)
                            throw new UnsupportedOperationException("Out of alignments to return.");
                        int score = paths[position].score;
                        int startingPosition = position;
                        while(position < paths.length && paths[position].score == score) position++;
                        return convertPathsToAlignments(bases,Arrays.copyOfRange(paths,startingPosition,position));
                    }

                    /**
                     * Unsupported.
                     */
                    public void remove() { throw new UnsupportedOperationException("Cannot remove from an alignment iterator"); }
                };
            }
        };
    }

    /**
     * Get a iterator of aligned reads, batched by mapping quality.
     * @param read Read to align.
     * @param newHeader Optional new header to use when aligning the read.  If present, it must be null.
     * @return Iterator to alignments.
     */
    @Override
    public Iterable<SAMRecord[]> alignAll(final SAMRecord read, final SAMFileHeader newHeader) {
        final Iterable<Alignment[]> alignments = getAllAlignments(read.getReadBases());
        return new Iterable<SAMRecord[]>() {
            public Iterator<SAMRecord[]> iterator() {
                final Iterator<Alignment[]> alignmentIterator = alignments.iterator();
                return new Iterator<SAMRecord[]>() {
                    /**
                     * Whether all alignments have been seen based on the current position.
                     * @return True if any more alignments are pending.  False otherwise.
                     */
                    public boolean hasNext() { return alignmentIterator.hasNext(); }

                    /**
                     * Return the next cross-section of alignments, based on mapping quality.
                     * @return Array of the next set of alignments of a given mapping quality.
                     */
                    public SAMRecord[] next() {
                        Alignment[] alignmentsOfQuality = alignmentIterator.next();
                        SAMRecord[] reads = new SAMRecord[alignmentsOfQuality.length];
                        for(int i = 0; i < alignmentsOfQuality.length; i++) {
                            reads[i] = Alignment.convertToRead(alignmentsOfQuality[i],read,newHeader);
                        }
                        return reads;
                    }

                    /**
                     * Unsupported.
                     */
                    public void remove() { throw new UnsupportedOperationException("Cannot remove from an alignment iterator"); }
                };
            }
        };
    }

    /**
     * Get the paths associated with the given base string.
     * @param bases List of bases.
     * @return A set of paths through the BWA.
     */
    public BWAPath[] getPaths(byte[] bases) {
        if(thunkPointer == 0)
            throw new StingException("BWA/C getPaths attempted, but BWA/C is not properly initialized.");
        return getPaths(thunkPointer,bases);
    }

    /**
     * Create a pointer to the BWA/C thunk.
     * @param files BWT source files.
     * @param configuration Configuration of the aligner.
     * @return Pointer to the BWA/C thunk.
     */
    protected native long create(BWTFiles files, BWAConfiguration configuration);

    /**
     * Update the configuration passed to the BWA aligner.  For internal use only.
     * @param thunkPointer pointer to BWA object.
     * @param configuration New configuration to set.
     */
    protected native void updateConfiguration(long thunkPointer, BWAConfiguration configuration);

    /**
     * Destroy the BWA/C thunk.
     * @param thunkPointer Pointer to the allocated thunk.
     */
    protected native void destroy(long thunkPointer);

    /**
     * Do the extra steps involved in converting a local alignment to a global alignment.
     * @param bases ASCII representation of byte array.
     * @param paths Paths through the current BWT.
     * @return A list of alignments.
     */
    protected Alignment[] convertPathsToAlignments(byte[] bases, BWAPath[] paths) {
        if(thunkPointer == 0)
            throw new StingException("BWA/C convertPathsToAlignments attempted, but BWA/C is not properly initialized.");
        return convertPathsToAlignments(thunkPointer,bases,paths);
    }

    /**
     * Caller to the path generation functionality within BWA/C.  Call this method's getPaths() wrapper (above) instead.
     * @param thunkPointer pointer to the C++ object managing BWA/C.
     * @param bases ASCII representation of byte array.
     * @return A list of paths through the specified BWT.
     */
    protected native BWAPath[] getPaths(long thunkPointer, byte[] bases);

    /**
     * Do the extra steps involved in converting a local alignment to a global alignment.
     * Call this method's convertPathsToAlignments() wrapper (above) instead.
     * @param thunkPointer pointer to the C++ object managing BWA/C.
     * @param bases ASCII representation of byte array.
     * @param paths Paths through the current BWT.
     * @return A list of alignments.
     */
    protected native Alignment[] convertPathsToAlignments(long thunkPointer, byte[] bases, BWAPath[] paths);

    /**
     * Gets the best alignment from BWA/C, randomly selected from all best-aligned reads.
     * @param thunkPointer Pointer to BWA thunk.
     * @param bases bases to align.
     * @return The best alignment from BWA/C.
     */
    protected native Alignment getBestAlignment(long thunkPointer, byte[] bases);
}
