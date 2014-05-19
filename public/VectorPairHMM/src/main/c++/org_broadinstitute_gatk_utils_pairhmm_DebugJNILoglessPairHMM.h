/*Copyright (c) 2012 The Broad Institute

*Permission is hereby granted, free of charge, to any person
*obtaining a copy of this software and associated documentation
*files (the "Software"), to deal in the Software without
*restriction, including without limitation the rights to use,
*copy, modify, merge, publish, distribute, sublicense, and/or sell
*copies of the Software, and to permit persons to whom the
*Software is furnished to do so, subject to the following
*conditions:

*The above copyright notice and this permission notice shall be
*included in all copies or substantial portions of the Software.

*THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
*EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
*OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
*NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
*HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
*WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
*FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
*THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/


/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_broadinstitute_gatk_utils_pairhmm_DebugJNILoglessPairHMM */

#ifndef _Included_org_broadinstitute_gatk_utils_pairhmm_DebugJNILoglessPairHMM
#define _Included_org_broadinstitute_gatk_utils_pairhmm_DebugJNILoglessPairHMM
#ifdef __cplusplus
extern "C" {
#endif
#undef org_broadinstitute_gatk_utils_pairhmm_DebugJNILoglessPairHMM_TRISTATE_CORRECTION
#define org_broadinstitute_gatk_utils_pairhmm_DebugJNILoglessPairHMM_TRISTATE_CORRECTION 3.0
#undef org_broadinstitute_gatk_utils_pairhmm_DebugJNILoglessPairHMM_dumpSandboxOnly
#define org_broadinstitute_gatk_utils_pairhmm_DebugJNILoglessPairHMM_dumpSandboxOnly 0L
#undef org_broadinstitute_gatk_utils_pairhmm_DebugJNILoglessPairHMM_debug
#define org_broadinstitute_gatk_utils_pairhmm_DebugJNILoglessPairHMM_debug 0L
#undef org_broadinstitute_gatk_utils_pairhmm_DebugJNILoglessPairHMM_verify
#define org_broadinstitute_gatk_utils_pairhmm_DebugJNILoglessPairHMM_verify 1L
#undef org_broadinstitute_gatk_utils_pairhmm_DebugJNILoglessPairHMM_debug0_1
#define org_broadinstitute_gatk_utils_pairhmm_DebugJNILoglessPairHMM_debug0_1 0L
#undef org_broadinstitute_gatk_utils_pairhmm_DebugJNILoglessPairHMM_debug1
#define org_broadinstitute_gatk_utils_pairhmm_DebugJNILoglessPairHMM_debug1 0L
#undef org_broadinstitute_gatk_utils_pairhmm_DebugJNILoglessPairHMM_debug2
#define org_broadinstitute_gatk_utils_pairhmm_DebugJNILoglessPairHMM_debug2 0L
#undef org_broadinstitute_gatk_utils_pairhmm_DebugJNILoglessPairHMM_debug3
#define org_broadinstitute_gatk_utils_pairhmm_DebugJNILoglessPairHMM_debug3 0L
/*
 * Class:     org_broadinstitute_gatk_utils_pairhmm_DebugJNILoglessPairHMM
 * Method:    jniInitialize
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_org_broadinstitute_gatk_utils_pairhmm_DebugJNILoglessPairHMM_jniInitialize
  (JNIEnv *, jobject, jint, jint);

/*
 * Class:     org_broadinstitute_gatk_utils_pairhmm_DebugJNILoglessPairHMM
 * Method:    jniInitializeProbabilities
 * Signature: ([[D[B[B[B)V
 */
JNIEXPORT void JNICALL Java_org_broadinstitute_gatk_utils_pairhmm_DebugJNILoglessPairHMM_jniInitializeProbabilities
  (JNIEnv *, jclass, jobjectArray, jbyteArray, jbyteArray, jbyteArray);

/*
 * Class:     org_broadinstitute_gatk_utils_pairhmm_DebugJNILoglessPairHMM
 * Method:    jniInitializePriorsAndUpdateCells
 * Signature: (ZII[B[B[BI)D
 */
JNIEXPORT jdouble JNICALL Java_org_broadinstitute_gatk_utils_pairhmm_DebugJNILoglessPairHMM_jniInitializePriorsAndUpdateCells
  (JNIEnv *, jobject, jboolean, jint, jint, jbyteArray, jbyteArray, jbyteArray, jint);

/*
 * Class:     org_broadinstitute_gatk_utils_pairhmm_DebugJNILoglessPairHMM
 * Method:    jniSubComputeReadLikelihoodGivenHaplotypeLog10
 * Signature: (II[B[B[B[B[B[BI)D
 */
JNIEXPORT jdouble JNICALL Java_org_broadinstitute_gatk_utils_pairhmm_DebugJNILoglessPairHMM_jniSubComputeReadLikelihoodGivenHaplotypeLog10
  (JNIEnv *, jobject, jint, jint, jbyteArray, jbyteArray, jbyteArray, jbyteArray, jbyteArray, jbyteArray, jint);

#ifdef __cplusplus
}
#endif
#endif
