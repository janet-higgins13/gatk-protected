/**
 * Created with IntelliJ IDEA.
 * User: kcibul
 * Date: 9/26/12
 */

package org.broadinstitute.sting.queue.qscripts

import org.broadinstitute.sting.queue.extensions.gatk._

import org.broadinstitute.sting.commandline.Hidden
import org.broadinstitute.sting.queue.extensions.cancer.MuTect
import org.broadinstitute.sting.queue.function.JavaCommandLineFunction
import java.io.File

class CMICancerPipeline extends CmiScript {
  qscript =>


  /****************************************************************************
    * Required executable locations
    ****************************************************************************/
  @Input(doc="The path to the binary of MuTect", fullName="mutect_jar", shortName="mj", required=true)
  var mutectJar: File = _

  @Input(doc="The path to the indel filter script", fullName="indel_filter", shortName="if", required=true)
  var indelFilterPath: File = _

  @Input(doc="The path to the VCF-based indel filter script", fullName="indel_filter_vcf", shortName="ifv", required=true)
  var indelVCFFilterPath: File = _

  @Input(doc="The path to the germline indel filter script", fullName="indel_germline_filter", shortName="igf", required=true)
  var indelGermlineFilterPath: File = _

  @Input(doc="The path to the indel caller refgene file", fullName="indel_refgene", shortName="ir", required=true)
  var indelRefGenePath: File = _

  @Input(doc="The path to the indel caller germline event file", fullName="indel_germline_database", shortName="igdb", required=true)
  var indelGermlineDatabase: File = _

  @Input(doc="The path to the indel caller maflite conversion script", fullName="indel_to_maflite", shortName="itm", required=true)
  var indelToMaflitePath: File = _

  /****************************************************************************
    * Required Parameters
    ****************************************************************************/
  @Argument(required = false, exclusiveOf = "tumorBam,tumorBai,tumorName,normalBam,normalBai,normalName")
  var individual: String = _

  @Input(doc="BAM file of data to be used for tumor", fullName = "tumor_bam", shortName = "tb", required=false, exclusiveOf = "individual")
  var tumorBam: File = _

  @Input(doc="BAI file of data to be used for tumor", fullName = "tumor_bai", shortName = "tbi", required=false, exclusiveOf = "individual")
  var tumorBai: File = _

  @Argument(doc="Tumor sample name", fullName = "tumor_name", shortName = "tn", required=false, exclusiveOf = "individual")
  var tumorName: String = _

  @Argument(doc="Tumor Fraction Contamination Estimate", fullName = "tumor_fraction_contamination", shortName = "tfc")
  var tumorFractionContamination: Float = _

  @Input(doc="BAM file of data to be used for normal", fullName = "normal_bam", shortName = "nb", required=false, exclusiveOf = "individual")
  var normalBam: File = _

  @Input(doc="BAI file of data to be used for normal", fullName = "normal_bai", shortName = "nbi", required=false, exclusiveOf = "individual")
  var normalBai: File = _

  @Argument(doc="Normal sample name", fullName = "normal_name", shortName = "nn", required=false, exclusiveOf = "individual")
  var normalName: String = _

  @Input(doc="Reference fasta file", fullName="reference", shortName="R", required=true)
  var reference: File = _

  @Input(doc="DBSNP or known callset to use (must be in VCF format)", fullName="dbsnp", shortName="D", required=true)
  var dbSNP: Seq[File] = Seq()

  @Input(doc="Intervals to process", fullName="intervals", shortName="L", required=true)
  var intervals: Seq[File] = Seq()

  /****************************************************************************
    * Output Parameters Parameters
    ****************************************************************************/
  @Hidden @Output(fullName="out_vcf", shortName = "ov", doc="output somatic indel and point mutation VCF", required = false)
  var outputVcf: File = _

  @Hidden @Output(fullName="out_vcf_idx", shortName = "ovi", doc="output somatic indel and point mutation VCF idx", required = false)
  var outputVcfIdx: File = _

  @Hidden @Output(fullName="out_wig", shortName= "ow", doc="output somatic coverage WIG", required = false)
  var outputWig: File = _

  /****************************************************************************
    * Optional Input Parameters
    ****************************************************************************/
  @Input(doc="Panel Of Normals or known artifact sites to use (must be in VCF format)", fullName="panel_of_normals", shortName="pon", required=false)
  var pon: Seq[File] = Seq()

  @Input(doc="COSMIC sites to use (must be in VCF format)", fullName="cosmic", shortName="C", required=false)
  var cosmic: Seq[File] = Seq()

  /****************************************************************************
    * Optional Input Parameters with sensible defaults
    ****************************************************************************/
  @Argument(doc="Base filter for SomaticIndelDetector", fullName="indel_base_filter", shortName = "ibf", required=false)
  var indelCallerBaseFilter: String = "T_COV<6||N_COV<4||(T_INDEL_F<=0.3&&T_CONS_CNT<7)||T_INDEL_CF<=0.7"


  /****************************************************************************
    * Hidden Parameters
    ****************************************************************************/
  @Argument(doc="How many ways to scatter/gather", fullName="scatter_gather", shortName="sg", required=false)
  var scatterGather: Int = 0

  @Hidden
  @Argument(doc="Run the pipeline in test mode only", fullName = "test_mode", shortName = "test", required=false)
  var testMode: Boolean = false


  /****************************************************************************
    * Global Variables
    ****************************************************************************/
  val SS_INDEL_TUMOR_FILTER =
      " --max_cons_av_mm 4 " +
      " --max_ref_av_mm 4 " +
      " --max_cons_nqs_av_mm 0.3 " +
      " --min_ref_nqs_av_qual 15 " +
      " --min_cons_nqs_av_qual 15 " +
      " --min_cons_count 0 " +
      " --min_readpos_median 10 " +
      " --min_readpos_mad 3 "

  val SS_INDEL_NORMAL_FILTER =
      " --max_cons_av_mm 1000 " +
      " --max_ref_av_mm 1000 " +
      " --max_cons_nqs_av_mm 1000 " +
      " --min_ref_nqs_av_qual 0 " +
      " --min_cons_nqs_av_qual 0 " +
      " --min_cons_count 0 " +
      " --min_readpos_median 10 " +
      " --min_readpos_mad 3 "


  /****************************************************************************
    * Main script
    ****************************************************************************/

  def script() {
    if (individual != null) {
      for (sample <- getIndividual(individual).sampleMetadatas) {
        if (sample.isTumor) {
          tumorName = sample.name
          tumorBam = sample.getIndexedFile("unreducedTumorBAM", "unreducedTumorBAMIndex")
        } else {
          normalName = sample.name
          normalBam = sample.getIndexedFile("unreducedNormalBAM", "unreducedNormalBAMIndex")
        }
      }
    }

    val outPrefix = tumorName + "-vs-" + normalName

    val mutationVcf = outPrefix + ".somatic.snv.vcf"
    val indelVcf = outPrefix + ".somatic.indel.vcf"
    outputWig = outPrefix + ".somatic.wig.txt"
    outputVcf = outPrefix + ".somatic.vcf"
    outputVcfIdx = outPrefix + ".somatic.vcf.idx"

    add(mutect(tumorName, tumorBam, normalName, normalBam, tumorFractionContamination, mutationVcf, outputWig))

    indels(tumorName, tumorBam, normalName, normalBam, indelVcf)

    add(combine(Seq(mutationVcf, indelVcf), outputVcf))
  }




  /****************************************************************************
    * Classes (GATK Walkers)
    ****************************************************************************/



  // General arguments to non-GATK tools
  trait ExternalCommonArgs extends CommandLineFunction {
    this.memoryLimit = 4
    this.isIntermediate = false
  }

  // General arguments to GATK walkers
  trait CommandLineGATKArgs extends CommandLineGATK with ExternalCommonArgs {
    this.reference_sequence = qscript.reference
  }

  def indels (tumorName : String, tumorBam : File, normalName : String, normalBam : File, indelVcf : String) {
    val rawIndels         = swapExt(indelVcf, ".vcf", ".raw.txt")
    val rawIndelsVCF      = swapExt(indelVcf, ".vcf", ".raw.vcf")

    add(callIndels(tumorBam, normalBam, rawIndelsVCF, rawIndels))

    // legacy
    val nFilteredIndels   = swapExt(indelVcf, ".vcf", ".nfilter.txt")
    val ntFilteredIndels  = swapExt(indelVcf, ".vcf", ".nfilter.tfilter.txt")
    val ntgFilteredIndels = swapExt(indelVcf, ".vcf", ".nfilter.tfilter.gfilter.txt")
    val mafliteIndels     = swapExt(indelVcf, ".vcf", ".filtered.maflite")
    add(filterIndelsNormal(rawIndels, nFilteredIndels))
    add(filterIndelsTumor(nFilteredIndels, ntFilteredIndels))
    add(filterIndelsGermline(ntFilteredIndels, ntgFilteredIndels))
    add(convertIndelCallsToMaflite(tumorName, normalName, ntgFilteredIndels, mafliteIndels))

    // vcf-based
    val nFilteredIndelsVCF   = swapExt(indelVcf, ".vcf", ".nfilter.vcf")
    val ntFilteredIndelsVCF  = swapExt(indelVcf, ".vcf", ".nfilter.tfilter.vcf")
    val finalIndelVCF        = indelVcf

    add(filterIndelsNormalVCF(normalName, rawIndelsVCF, nFilteredIndelsVCF))
    add(filterIndelsTumorVCF(tumorName, nFilteredIndelsVCF, ntFilteredIndelsVCF))
    add(filterIndelsGermline(ntFilteredIndelsVCF, finalIndelVCF))
  }

  case class mutect (tumorName : String, tumorBam : File, normalName : String, normalBam : File, tumorFractionContamination : Float, outVcf : File, outCoverage : File) extends MuTect with CommandLineGATKArgs {
    this.scatterCount = qscript.scatterGather
    this.memoryLimit = 4
    this.jarFile = qscript.mutectJar
    this.intervals = qscript.intervals

    this.dbsnp = qscript.dbSNP
    this.cosmic = qscript.cosmic
    this.normal_panel = qscript.pon

    this.only_passing_calls = true
    this.enable_extended_output = true
    this.downsample_to_coverage = 1000 // TODO: how deep should this be?
    this.fraction_contamination = Some(tumorFractionContamination)

    this.input_file :+= new TaggedFile(tumorBam, "tumor")
    this.input_file :+= new TaggedFile(normalBam, "normal")

    this.out = swapExt(outVcf, ".vcf", ".call_stats.txt")

    this.coverage_file = outCoverage
    this.vcf = outVcf

    this.analysisName = tumorBam.toString + ".mutect"
  }

  case class callIndels (tumorBam : File, normalBam : File, outVcfIndels : File, outTextIndels : File) extends SomaticIndelDetector with CommandLineGATKArgs {
    @Output(doc="output in text format") var rawIndels = outTextIndels
    @Output(doc="output in VCF format") var vcfIndels = outVcfIndels
    this.scatterCount = 1
    this.memoryLimit = 4
    this.intervals = qscript.intervals

    val baseFilter = qscript.indelCallerBaseFilter

    this.input_file :+= new TaggedFile(normalBam, "normal")
    this.input_file :+= new TaggedFile(tumorBam, "tumor")

    this.verbose = rawIndels
    this.out = vcfIndels
    this.refseq = indelRefGenePath
    this.window_size = 400 // why?
    this.maxNumberOfReads = 8000 // why?
    this.filter = Seq(baseFilter)
  }

  case class combine(vcfs : Seq[File], outputVcf : File) extends CombineVariants with CommandLineGATKArgs {
    this.variant = vcfs
    this.out = outputVcf
  }

  /****************************************************************************
    * Classes (non-GATK programs)
    ****************************************************************************/


  case class filterIndelsNormal(originalIndelCalls : File, filteredIndelCalls : File) extends CommandLineFunction with ExternalCommonArgs {
    @Input(doc="indel call file to be filtered") var inCalls = originalIndelCalls
    @Output(doc="filtered indel calls file") var outCalls = filteredIndelCalls
    def commandLine = "perl " + indelFilterPath +
      " --calls " + inCalls +
      " --prefix N_" +
      SS_INDEL_NORMAL_FILTER +
      " --mode ANNOTATE " +
      " --output " + filteredIndelCalls

    this.analysisName = outCalls + ".filterIndelsNormal"
  }

  case class filterIndelsNormalVCF(sample : String, originalIndelCalls : File, filteredIndelCalls : File) extends CommandLineFunction with ExternalCommonArgs {
    @Input(doc="indel call file to be filtered") var inCalls = originalIndelCalls
    @Output(doc="filtered indel calls file") var outCalls = filteredIndelCalls
    def commandLine = "perl " + indelVCFFilterPath +
      " --calls " + inCalls +
      " --prefix N_" +
      SS_INDEL_NORMAL_FILTER +
      " --ref " + qscript.reference +
      " --sample " + sample +
      " --gatk_cp " + JavaCommandLineFunction.currentClasspath.mkString(File.pathSeparator) +
      " --output " + filteredIndelCalls

    this.analysisName = outCalls + ".filterIndelsNormalVCF"
  }

  case class filterIndelsTumor(originalIndelCalls : File, filteredIndelCalls : File) extends CommandLineFunction with ExternalCommonArgs {
    @Input(doc="indel call file to be filtered") var inCalls = originalIndelCalls
    @Output(doc="filtered indel calls file") var outCalls = filteredIndelCalls
    def commandLine = "perl " + indelFilterPath +
      " --calls " + inCalls +
      " --prefix T_" +
      SS_INDEL_TUMOR_FILTER +
      " --mode ANNOTATE " +
      " --output " + filteredIndelCalls

    this.analysisName = outCalls + ".filterIndelsTumor"
  }

  case class filterIndelsTumorVCF(sample : String, originalIndelCalls : File, filteredIndelCalls : File) extends CommandLineFunction with ExternalCommonArgs {
    @Input(doc="indel call file to be filtered") var inCalls = originalIndelCalls
    @Output(doc="filtered indel calls file") var outCalls = filteredIndelCalls
    def commandLine = "perl " + indelVCFFilterPath +
      " --calls " + inCalls +
      " --prefix T_" +
      SS_INDEL_TUMOR_FILTER +
      " --ref " + qscript.reference +
      " --sample " + sample +
      " --gatk_cp " + JavaCommandLineFunction.currentClasspath.mkString(File.pathSeparator) +
      " --output " + filteredIndelCalls

    this.analysisName = outCalls + ".filterIndelsTumorVCF"
  }

  case class filterIndelsGermline(originalIndelCalls : File, filteredIndelCalls : File) extends CommandLineFunction with ExternalCommonArgs {
    @Input(doc="indel call file to be filtered") var inCalls = originalIndelCalls
    @Output(doc="filtered indel calls file") var outCalls = filteredIndelCalls

    def commandLine = "perl " + indelGermlineFilterPath +
      " --calls " + inCalls +
      " --filter " + indelGermlineDatabase +
      " --window 10 " +
      " --mode ANNOTATE " +
      " --pos_column 1" +
      " --output " + filteredIndelCalls

    this.analysisName = outCalls + ".filterIndelsGermline"
  }

  case class convertIndelCallsToMaflite(tumorName : String, normalName : String, originalIndelCalls : File, maflite : File) extends CommandLineFunction with ExternalCommonArgs {
    @Input(doc="indel call file to be filtered") var inCalls = originalIndelCalls
    @Output(doc="filtered indel calls in maflite format") var outCalls = maflite

    def commandLine = "perl " + indelToMaflitePath +
      " --build 37 " +
      " " + tumorName +
      " " + normalName +
      " " + originalIndelCalls +
      " " + maflite +
      " tumor_f,t_ref_count,t_alt_count "

    this.analysisName = outCalls + ".convertIndelsToMaflite"
  }
}
