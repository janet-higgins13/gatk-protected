import org.broadinstitute.sting.queue.extensions.gatk._
import org.broadinstitute.sting.queue.extensions.gatk.TaggedFile._
import org.broadinstitute.sting.queue.{QException, QScript}

class Phase1IndelProjectConsensus extends QScript {
  qscript =>

  @Input(doc="path to GATK jar", shortName="gatk", required=true)
  var gatkJar: File = _

  @Input(doc="output path", shortName="outputDir", required=true)
  var outputDir: String = _

  @Input(doc="queue", shortName="queue", required=true)
  var jobQueue: String = _

  @Input(doc="the chromosome to process", shortName="onlyOneChr", required=false)
  var onlyOneChr: Boolean = false

  @Input(doc="the chromosome to process", shortName="chrToProcess", required=false)
  var chrToProcess: Int = 20

  @Input(doc="the chromosome to process", shortName="indelsOnly", required=false)
  var indelsOnly: Boolean = false

  @Input(doc="path to tmp space for storing intermediate bam files", shortName="outputTmpDir", required=true)
  var outputTmpDir: String = "/broad/shptmp/delangel"

  @Input(doc="Generate bam files", shortName="generateBAMs", required=false)
  var generateBAMs: Boolean = false
  @Input(doc="Generate bam files", shortName="createTargets", required=false)
  var createTargets: Boolean = false

  @Input(doc="indel alleles", shortName="indelAlleles", required=false)
  var indelAlleles: String = "/humgen/1kg/processing/production_wgs_phase1/consensus/ALL.indels.combined.chr20.vcf"

  @Input(doc="nt", shortName="nt", required=false)
  var nt: Int = 1


  private val reference: File = new File("/humgen/1kg/reference/human_g1k_v37.fasta")
  private val dbSNP: File = new File("/humgen/gsa-hpprojects/GATK/data/dbsnp_132_b37.leftAligned.vcf")
  private val dindelCalls: String = "/humgen/gsa-hpprojects/GATK/data/Comparisons/Unvalidated/AFR+EUR+ASN+1KG.dindel_august_release_merged_pilot1.20110126.sites.vcf"
  val chromosomeLength = List(249250621,243199373,198022430,191154276,180915260,171115067,159138663,146364022,141213431,135534747,135006516,133851895,115169878,107349540,102531392,90354753,81195210,78077248,59128983,63025520,48129895,51304566,155270560)
  //  val chromosomeLength = List(249250621,243199373,198022430,191154276,180915260,171115067,159138663,146364022,141213431,135534747,135006516,133851895,115169878,107349540,102531392,90354753,81195210,78077248,59128983,3000000,48129895,51304566,155270560)
  val populations = List("ASW","CEU","CHB","CHS","CLM","FIN","GBR","IBS","JPT","LWK","MXL","PUR","TSI","YRI")
  private val snpAlleles: String = "/humgen/1kg/processing/production_wgs_phase1/consensus/ALL.phase1.wgs.union.pass.sites.vcf"

  private val subJobsPerJob:Int = 1


  trait CommandLineGATKArgs extends CommandLineGATK {
    this.jarFile = qscript.gatkJar
    this.reference_sequence = qscript.reference
    this.memoryLimit = Some(2)
    this.jobQueue = qscript.jobQueue

  }

  class AnalysisPanel(val baseName: String, val pops: List[String], val jobNumber: Int, val subJobNumber: Int, val chr: String) {
    val rawVCFindels = new File(qscript.outputDir + "/calls/chr" + chr + "/" + baseName + "/" + baseName + ".phase1.chr" + chr + "." + subJobNumber + ".raw.indels.vcf")

    val callIndels = new UnifiedGenotyper with CommandLineGATKArgs
    callIndels.out = rawVCFindels
    callIndels.dcov = 50
    callIndels.stand_call_conf = 4.0
    callIndels.stand_emit_conf = 4.0
    callIndels.baq = org.broadinstitute.sting.utils.baq.BAQ.CalculationMode.OFF
    callIndels.jobName = qscript.outputTmpDir + "/calls/chr" + chr + "/" +baseName + ".phase1.chr" + chr + "." + subJobNumber + ".raw.indels"
    callIndels.glm = org.broadinstitute.sting.gatk.walkers.genotyper.GenotypeLikelihoodsCalculationModel.Model.INDEL
    callIndels.genotyping_mode = org.broadinstitute.sting.gatk.walkers.genotyper.GenotypeLikelihoodsCalculationModel.GENOTYPING_MODE.GENOTYPE_GIVEN_ALLELES
    callIndels.out_mode = org.broadinstitute.sting.gatk.walkers.genotyper.UnifiedGenotyperEngine.OUTPUT_MODE.EMIT_ALL_SITES
    callIndels.alleles = qscript.indelAlleles
    callIndels.dbsnp = qscript.dbSNP
    //callIndels.A ++= List("TechnologyComposition")
    callIndels.sites_only = false

    //    callIndels.BTI = "alleles"
    callIndels.ignoreSNPAlleles = true
    callIndels.nt=Some(qscript.nt)
  }

  class Chromosome(val inputChr: Int) {
    var chr: String = inputChr.toString
    if(inputChr == 23) { chr = "X" }

    val indelCombine = new CombineVariants with CommandLineGATKArgs
    val indelChrVCF = new File(qscript.outputDir + "/calls/" + "combined.phase1.chr" + chr + ".raw.indels.vcf")
    indelCombine.out = indelChrVCF
    indelCombine.intervalsString :+= chr
    indelCombine.jobName = qscript.outputDir + "/calls/" + "combined.phase1.chr" + chr + ".raw.indels"
    indelCombine.memoryLimit = Some(4)
  }

  def script = {

//    var chrList =  List(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23)
    var chrList =  List(1,2)
    if (qscript.onlyOneChr) {
      chrList = List(qscript.chrToProcess)
    }
    for(chr <- chrList) {
      val chrObject = new Chromosome(chr)
      var basesPerSubJob: Int = 3000000/qscript.subJobsPerJob

      val lastBase: Int = qscript.chromosomeLength(chr - 1)
      var start: Int = 1
      var stop: Int = start - 1 + basesPerSubJob
      if( stop > lastBase ) { stop = lastBase }
      var subJobNumber: Int = 1
      while( subJobNumber < (lastBase.toFloat / basesPerSubJob.toFloat) + 1.0) {
        if( chr != 23 ) {
          callThisChunk("%d:%d-%d".format(chr, start, stop), subJobNumber, chr, chrObject)

        }
        else {
          callThisChunk("X:%d-%d".format(start, stop), subJobNumber, chr, chrObject)
        }
        start += basesPerSubJob
        stop += basesPerSubJob
        if( stop > lastBase ) { stop = lastBase }
        subJobNumber += 1
      }

      add(chrObject.indelCombine)

    }
  }

   def callThisChunk(interval: String, subJobNumber: Int, inputChr: Int, chrObject: Chromosome) = {

    var chr: String = inputChr.toString
    if(inputChr == 23) { chr = "X" }



    var jobNumber = ( ( subJobNumber -1)  / qscript.subJobsPerJob  ) + 1

    val AFRadmix = new AnalysisPanel("AFR.admix", List("LWK","YRI","ASW","CLM","PUR"), jobNumber, subJobNumber, chr)
    val AMRadmix = new AnalysisPanel("AMR.admix", List("MXL","CLM","PUR","ASW"), jobNumber, subJobNumber, chr)
    val EURadmix = new AnalysisPanel("EUR.admix", List("CEU","FIN","GBR","TSI","IBS","MXL","CLM","PUR","ASW"), jobNumber, subJobNumber, chr)
    val ASNadmix = new AnalysisPanel("ASN.admix", List("CHB","CHS","JPT","MXL","CLM","PUR"), jobNumber, subJobNumber, chr)
    val AFR = new AnalysisPanel("AFR", List("LWK","YRI","ASW"), jobNumber, subJobNumber, chr)
    val AMR = new AnalysisPanel("AMR", List("MXL","CLM","PUR"), jobNumber, subJobNumber, chr)
    val EUR = new AnalysisPanel("EUR", List("CEU","FIN","GBR","TSI","IBS"), jobNumber, subJobNumber, chr)
    val ASN = new AnalysisPanel("ASN", List("CHB","CHS","JPT"), jobNumber, subJobNumber, chr)
    val ALL = new AnalysisPanel("ALL", List("LWK","YRI","ASW","MXL","CLM","PUR","CEU","FIN","GBR","TSI","IBS","CHB","CHS","JPT"), jobNumber, subJobNumber, chr)

    //val analysisPanels = List(AFR, ASN, AMR, EUR, AFRadmix, ASNadmix, AMRadmix, EURadmix) //ALL
    val analysisPanels = List(AFR, ASN, AMR, EUR) //ALL

    val indelCombine = new CombineVariants with CommandLineGATKArgs
    val combinedIndelChunk = new File(qscript.outputDir + "/calls/chr" + chr + "/" + "combined.phase1.chr" + chr + "." + subJobNumber + ".raw.indels.vcf")

    indelCombine.out = combinedIndelChunk
    indelCombine.jobName = qscript.outputTmpDir + "/calls/chr" + chr + "/" + "combined.phase1.chr" + chr + "." + subJobNumber + ".raw.indels"
    indelCombine.intervalsString :+= interval
    indelCombine.mergeInfoWithMaxAC = true
    //indelCombine.priority = "AFR.admix,AMR.admix,EUR.admix,ASN.admix,AFR,AMR,EUR,ASN" //ALL,
    indelCombine.sites_only = false

    indelCombine.priority = "AFR,AMR,EUR,ASN" //ALL,


    for( population <- qscript.populations ) {
      val baseTmpName: String = qscript.outputTmpDir + "/calls/chr" + chr + "/" + population + ".phase1.chr" + chr + "." + jobNumber.toString + "."
      //     val cleanedBam = new File(baseTmpName + "cleaned.bam")
      var cleanedBam = new File("/humgen/1kg/phase1_cleaned_bams/bams/chr" + chr + "/" + population + ".phase1.chr"+chr + "." +jobNumber.toString+".cleaned.bam")
      if (chr.equals("1") || chr.equals("2")) {
        cleanedBam = new File("/broad/shptmp/temporary_1000G_bams/chr" + chr + "/" + population + ".phase1.chr"+chr + "." +jobNumber.toString+".cleaned.bam")
      }
      for( a <- analysisPanels ) {
        for( p <- a.pops) {
          if( p == population ) {
            a.callIndels.input_file :+= cleanedBam
          }
        }
      }
    }

    for( a <- analysisPanels ) {
      // use BTI with indels
      a.callIndels.intervalsString :+= interval

      if(a.baseName == "ALL") {
        a.callIndels.memoryLimit = 4
      }

      add(a.callIndels)

      indelCombine.variant :+= TaggedFile(a.callIndels.out, a.baseName)
    }

    add(indelCombine)

    chrObject.indelCombine.variant :+= TaggedFile( indelCombine.out,"ALL" + subJobNumber.toString)
  }
}