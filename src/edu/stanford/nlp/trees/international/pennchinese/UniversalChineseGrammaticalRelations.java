// Stanford Dependencies - Code for producing and using Stanford dependencies.
// Copyright © 2005-2014 The Board of Trustees of
// The Leland Stanford Junior University. All Rights Reserved.
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// For more information, bug reports, fixes, contact:
//    Christopher Manning
//    Dept of Computer Science, Gates 1A
//    Stanford CA 94305-9010
//    USA
//    parser-support@lists.stanford.edu
//    http://nlp.stanford.edu/software/stanford-dependencies.shtml

package edu.stanford.nlp.trees.international.pennchinese;

import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.HeadFinder;
import edu.stanford.nlp.trees.tregex.TregexPatternCompiler;
import edu.stanford.nlp.international.Language;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static edu.stanford.nlp.trees.GrammaticalRelation.DEPENDENT;

/**
 * ChineseGrammaticalRelations is a
 * set of {@link GrammaticalRelation} objects for the Chinese language.
 * Examples are from CTB_001.fid
 *
 * TODO(pliang): need to take some of these relations and move them into a
 * Universal Stanford Dependencies class (e.g., dep, arg, mod).
 * Currently, we have an external data structure that stores information about
 * whether a relation is universal or not, but that should probably be moved
 * into GrammaticalRelation.
 *
 * TODO(pliang): add an option to produce trees which use only the USD
 * relations rather than the more specialized Chinese ones.
 *
 * @author Galen Andrew
 * @author Pi-Chuan Chang
 * @author Huihsin Tseng
 * @author Marie-Catherine de Marneffe
 * @author Percy Liang
 * @see edu.stanford.nlp.trees.GrammaticalStructure
 * @see GrammaticalRelation
 * @see UniversalChineseGrammaticalStructure
 */
public class UniversalChineseGrammaticalRelations {

  /** This class is just a holder for static classes
   *  that act a bit like an enum.
   */
  private UniversalChineseGrammaticalRelations() {}

  // By setting the HeadFinder to null, we find out right away at
  // runtime if we have incorrectly set the HeadFinder for the
  // dependency tregexes
  private static final TregexPatternCompiler tregexCompiler = new TregexPatternCompiler((HeadFinder) null);


  private static final String COMMA_PATTERN = "/^,|，$/";

  /** Return an unmodifiable list of grammatical relations.
   *  Note: the list can still be modified by others, so you
   *  should still get a lock with {@code valuesLock()} before
   *  iterating over this list.
   *
   *  @return A list of grammatical relations
   */
  public static List<GrammaticalRelation> values() {
    return Collections.unmodifiableList(values);
  }

  private static final ReadWriteLock valuesLock = new ReentrantReadWriteLock();

  public static Lock valuesLock() {
    return valuesLock.readLock();
  }


  public static GrammaticalRelation valueOf(String s) {
    return GrammaticalRelation.valueOf(s, values(), valuesLock());
  }

  ////////////////////////////////////////////////////////////
  // ARGUMENT relations
  ////////////////////////////////////////////////////////////

  /**
   * The "argument" (arg) grammatical relation (abstract).
   * Arguments are required by their heads.
   */
  public static final GrammaticalRelation ARGUMENT =
    new GrammaticalRelation(Language.UniversalChinese, "arg", "argument", DEPENDENT);

  /**
   * The "subject" (subj) grammatical relation (abstract).
   */
  public static final GrammaticalRelation SUBJECT =
    new GrammaticalRelation(Language.UniversalChinese, "subj", "subject", ARGUMENT);

  /**
   * The "nominal subject" (nsubj) grammatical relation.  A nominal subject is
   * a subject which is an noun phrase.
   * <p>
   * <code>
   * <pre>
   * Input:
   *   (ROOT
   *     (IP
   *       (NP
   *         (NP (NR 上海) (NR 浦东))
   *         (NP (NN 开发)
   *           (CC 与)
   *           (NN 法制) (NN 建设)))
   *       (VP (VV 同步))))
   * Output:
   *   nsubj(同步, 建设)
   *
   * </pre>
   * </code>
   */
  public static final GrammaticalRelation NOMINAL_SUBJECT =
    new GrammaticalRelation(Language.UniversalChinese, "nsubj", "nominal subject",
        SUBJECT, "IP|NP", tregexCompiler,
            "IP <( ( NP|QP=target!< NT ) $+ ( /^VP|VCD|IP/  !< VE !<VC !<SB !<LB  ))",
            // Handle the case where the subject and object is separated by a comma
            "IP <( ( NP|QP=target!< NT ) $+ (PU (<: " + COMMA_PATTERN + " $+ ( /^VP|VCD|IP/  !< VE !<VC !<SB !<LB  ))))",
            // There are a number of cases of NP-SBJ not under IP, and we should try to get some of them as this
            // pattern does. There are others under CP, especially CP-CND
            "NP !$+ VP < ( (  NP|DP|QP=target !< NT ) $+ ( /^VP|VCD/ !<VE !< VC !<SB !<LB))",
            "IP < (/^NP/=target $+ (VP < VC))" // Go over copula
    );

  /**
   * The "nominal passive subject" (nsubjpass) grammatical relation.
   * The noun is the subject of a passive sentence.
   * The passive marker in Chinese is "被".
   * <p>
   * <code>
   * <pre>
   * Input:
   *   (IP
   *     (NP (NN 镍))
   *     (VP (SB 被)
   *       (VP (VV 称作)
   *         (NP (PU “)
   *           (DNP
   *             (NP
   *               (ADJP (JJ 现代))
   *               (NP (NN 工业)))
   *             (DEG 的))
   *           (NP (NN 维生素))
   *           (PU ”)))))
   * Output:
   *   nsubjpass(称作-3, 镍-1)
   * </pre>
   * </code>
   */
  public static final GrammaticalRelation NOMINAL_PASSIVE_SUBJECT =
    new GrammaticalRelation(Language.UniversalChinese,
      "nsubjpass", "nominal passive subject",
      NOMINAL_SUBJECT, "IP", tregexCompiler,
            "IP < (NP=target $+ (VP|IP < SB|LB))");

  /**
   * The "clausal subject" grammatical relation.  A clausal subject is
   * a subject which is a clause.
   * <p /> Examples:
   * <code>
   * <pre>
   * </pre>
   * </code>
   * <p />
   * Note: This one might not exist in Chinese, or very rare.
   * cdm 2016: There are a few CP-SBJ in the CTB like this one:
   * 我 估计 [CP-SBJ 他 欺负 别人 的 ] 多
   * but it doesn't seem like there would be any way to detect them without using -SBJ
   */
  /*public static final GrammaticalRelation CLAUSAL_SUBJECT =
    new GrammaticalRelation(Language.UniversalChinese,
      "csubj", "clausal subject",
      SUBJECT, "IP", tregexCompiler,
      new String[]{
        // This following rule is too general and collide with 'ccomp'.
        // Delete it for now.
        // TODO: come up with a new rule. Does this exist in Chinese?
        //"IP < (IP=target $+ ( VP !< VC))",
      });

  /**
   * The "complement" (comp) grammatical relation.
   */
  public static final GrammaticalRelation COMPLEMENT =
    new GrammaticalRelation(Language.UniversalChinese, "comp", "complement", ARGUMENT);

  /**
   * The "object" (obj) grammatical relation.
   */
  public static final GrammaticalRelation OBJECT =
    new GrammaticalRelation(Language.UniversalChinese, "obj", "object", COMPLEMENT);

  /**
   * The "direct object" (dobj) grammatical relation.
   * <p>
   * <code>
   * <pre>
   * Input:
   *   (IP
   *     (NP (NR 上海) (NR 浦东))
   *     (VP
   *       (VCD (VV 颁布) (VV 实行))
   *            (AS 了)
   *            (QP (CD 七十一)
   *                (CLP (M 件)))
   *            (NP (NN 法规性) (NN 文件))))
   *
   *   In recent years Shanghai 's Pudong has promulgated and implemented
   *   some regulatory documents.
   * Output:
   *   dobj(颁布, 文件)
   * </pre>
   * </code>
   */
  public static final GrammaticalRelation DIRECT_OBJECT =
    new GrammaticalRelation(Language.UniversalChinese,
      "dobj", "direct object",
      OBJECT, "CP|VP", tregexCompiler,
            "VP < ( /^V*/ $+ NP $+ NP|DP=target ) !< VC ",
            " VP < ( /^V*/ $+ NP|DP=target ! $+ NP|DP) !< VC ",
            "CP < (IP $++ NP=target ) !<< VC");

  /**
   * The "indirect object" (iobj) grammatical relation.
   */
  public static final GrammaticalRelation INDIRECT_OBJECT =
    new GrammaticalRelation(Language.UniversalChinese,
      "iobj", "indirect object",
      OBJECT, "VP", tregexCompiler,
            " CP !> VP < ( VV $+ ( NP|DP|QP|CLP=target . NP|DP ) )");

  /**
   * The "range" grammatical relation (Chinese only).  The indirect
   * object of a VP is the quantifier phrase which is the (dative) object
   * of the verb.<p>
   * <p>
   * <code>
   * <pre>
   * Input:
   *   (VP (VV 成交)
   *       (NP (NN 药品))
   *       (QP (CD 一亿多)
   *           (CLP (M 元))))
   * Output:
   *   range(成交, 元)
   * </pre>
   * </code>
   */
  // todo [cdm 2016]: Need to get rid of this somehow....
  public static final GrammaticalRelation RANGE =
    new GrammaticalRelation(Language.UniversalChinese,
      "range", "range",
      INDIRECT_OBJECT, "VP", tregexCompiler,
            "VP < ( NP|DP|QP $+ NP|DP|QP=target)",
            "VP < ( VV $+ QP=target )");

  /**
   * The "clausal complement" (ccomp) grammatical relation.
   * <p>
   * <code>
   * <pre>
   * Input:
   *   (IP
   *       (VP
   *         (VP
   *           (ADVP (AD 一))
   *           (VP (VV 出现)))
   *         (VP
   *           (ADVP (AD 就))
   *           (VP (SB 被)
   *             (VP (VV 纳入)
   *               (NP (NN 法制) (NN 轨道)))))))))))
   * Output:
   *   ccomp(出现, 纳入)
   * </pre>
   * </code>
   */
  public static final GrammaticalRelation CLAUSAL_COMPLEMENT =
    new GrammaticalRelation(Language.UniversalChinese,
      "ccomp", "clausal complement",
      COMPLEMENT, "VP|ADJP|IP", tregexCompiler,
            "  VP  < VV|VC|VRD|VCD  !< NP|QP|LCP  < IP|VP|VRD|VCD=target > IP|VP ");
        //        "  VP|IP <  ( VV|VC|VRD|VCD !$+  NP|QP|LCP ) > (IP   < IP|VP|VRD|VCD=target)   "
       //          "VP < (S=target < (VP !<, TO|VBG) !$-- NP)",


  /**
   * The "xclausal complement" (xcomp) grammatical relation.
   */
  // pichuan: this is difficult to recognize in Chinese.
  // remove the rules since it (always) collides with ccomp
  /*public static final GrammaticalRelation XCLAUSAL_COMPLEMENT =
    new GrammaticalRelation(Language.UniversalChinese,
      "xcomp", "xclausal complement",
      COMPLEMENT, "VP|ADJP", tregexCompiler,
      new String[]{
        // TODO: these rules seem to always collide with ccomp.
        // Is this really desirable behavior?
        //"VP !> (/^VP/ < /^VC$/ ) < (IP=target < (VP < P))",
        //"ADJP < (IP=target <, (VP <, P))",
        //"VP < (IP=target < (NP $+ NP|ADJP))",
        //"VP < (/^VC/ $+ (VP=target < VC < NP))"
      });

  ////////////////////////////////////////////////////////////
  // MODIFIER relations
  ////////////////////////////////////////////////////////////

  /**
   * The "modifier" (mod) grammatical relation (abstract).
   */
  public static final GrammaticalRelation MODIFIER =
    new GrammaticalRelation(Language.UniversalChinese, "mod", "modifier", DEPENDENT);

  /**
   * The "number modifier" (nummod) grammatical relation.
   * <p>
   * <pre>
   * <code>
   * Input:
   *   (NP
   *         (NP (NN 拆迁) (NN 工作))
   *         (QP (CD 若干))
   *         (NP (NN 规定)))
   * Output:
   *   nummod(规定-48, 若干-47)
   * </code>
   * </pre>
   */
  public static final GrammaticalRelation NUMERIC_MODIFIER =
    new GrammaticalRelation(Language.UniversalChinese, "nummod", "numeric modifier",
                            MODIFIER,
                            "QP|NP", tregexCompiler,
            "QP < CD=target",
            "NP < ( QP=target !<< CLP )");

  /**
   * The "ordinal modifier" (ordmod) grammatical relation.
   */
  public static final GrammaticalRelation ORDINAL_MODIFIER =
    new GrammaticalRelation(Language.UniversalChinese, "nummod:ordmod", "ordinal numeric modifier",
                            NUMERIC_MODIFIER,
                            "NP|QP", tregexCompiler,
            "NP < QP=target < ( OD !$+ CLP )",
            "QP < (OD=target $+ CLP)");

  /**
   * The "appositional modifier" (appos) grammatical relation (abstract).
   */
  public static final GrammaticalRelation APPOSITIONAL_MODIFIER =
    new GrammaticalRelation(Language.UniversalChinese, "appos", "appositional modifier", MODIFIER);

  /**
   * The "parenthetical modifier" (prnmod) grammatical relation (Chinese-specific).
   */
  public static final GrammaticalRelation PARENTHETICAL_MODIFIER =
    new GrammaticalRelation(Language.UniversalChinese, "prnmod", "parenthetical modifier",
                            MODIFIER, "NP", tregexCompiler,
            "NP < PRN=target ");

  /**
   * The "noun modifier" grammatical relation (abstract).
   */
  public static final GrammaticalRelation NOUN_MODIFIER =
    new GrammaticalRelation(Language.UniversalChinese, "nmod", "noun modifier", MODIFIER);

  /**
   * The "associative modifier" (assmod) grammatical relation (Chinese-specific).
   * See "case" for example.
   */
  public static final GrammaticalRelation ASSOCIATIVE_MODIFIER =
    new GrammaticalRelation(Language.UniversalChinese,
      "assmod", "associative modifier (examples: 上海市/Shanghai[modifier] 的 规定/law[head])",
      NOUN_MODIFIER, "NP|QP", tregexCompiler,
            "NP|QP < ( DNP =target $++ NP|QP ) ");

  /**
   * The "temporal modifier" grammatical relation.
   * (IP
   *           (VP
   *             (NP (NT 以前))
   *             (ADVP (AD 不))
   *             (ADVP (AD 曾))
   *             (VP (VV 遇到) (AS 过))))
   *(VP
   *     (LCP
   *       (NP (NT 近年))
   *       (LC 来))
   *     (VP
   *       (VCD (VV 颁布) (VV 实行))
   * {@code tmod } (遇到, 以前)
   */
  public static final GrammaticalRelation TEMPORAL_MODIFIER =
    new GrammaticalRelation(Language.UniversalChinese,
      "tmod", "temporal modifier",
      NOUN_MODIFIER, "VP|IP", tregexCompiler,
            "VP|IP < (NP=target < NT !.. /^VC$/ $++  VP)");

  /* This rule actually matches nothing.
     There's another tmod rule. This is removed for now.
     (pichuan) Sun Mar  8 18:22:40 2009
  */
  /*
  public static final GrammaticalRelation TEMPORAL_MODIFIER =
    new GrammaticalRelation(Language.UniversalChinese,
      "tmod", "temporal modifier",
      MODIFIER, "VP|IP|ADJP", tregexCompiler,
      new String[]{
        " VC|VE ! >> VP|ADJP < NP=target < NT",
        "VC|VE !>>IP <( NP=target < NT $++ VP !< VC|VE )"
      });
  */

  /**
   * The "relative clause modifier" (relcl) grammatical relation.
   * <p>
   * <pre>
   * <code>
   * Input:
   *   (NP-PRD (CP (WHNP-3 (-NONE- *OP*))
   *       (CP (IP (NP-SBJ (-NONE- *pro*))
   *         (VP (NP-TMP (NT 以前))
   *             (ADVP (AD 不))
   *             (ADVP (AD 曾))
   *             (VP (VV 遇到)
   *           (AS 过)
   *           (NP-OBJ (-NONE- *T*-3)))))
   *           (DEC 的)))
   *         (NP (NP (ADJP (JJ 新))
   *           (NP (NN 情况)))
   *       (PU 、)
   *       (NP (ADJP (JJ 新))
   *           (NP (NN 问题)))))
   *   The new problem that has not been encountered.
   * Output:
   *   relcl(问题, 遇到)
   * </code>
   * </pre>
   */
  public static final GrammaticalRelation RELATIVE_CLAUSE_MODIFIER =
    new GrammaticalRelation(Language.UniversalChinese, "relcl", "relative clause modifier",
                            MODIFIER, "NP", tregexCompiler,
            "NP  $++ (CP=target ) > NP ",
            "NP  < ( CP=target $++ NP )");

  /**
   * The "non-finite clause" grammatical relation.
   * This used to be verb modifier (vmod).
   */
  public static final GrammaticalRelation NONFINITE_CLAUSE_MODIFIER =
    new GrammaticalRelation(Language.UniversalChinese,
      "nfincl", "non-finite clause modifier (examples: stores[head] based[modifier] in Boston",
      MODIFIER, "NP", tregexCompiler,
            "NP < IP=target ");

  /**
   * The "adjective modifier" (amod) grammatical relation.
   * <p>
   * <pre>
   * <code>
   * Input:
   *   (NP
   *     (ADJP (JJ 新))
   *     (NP (NN 情况)))
   * Output:
   *   amod(情况-34, 新-33)
   * </code>
   * </pre>
   */
  public static final GrammaticalRelation ADJECTIVAL_MODIFIER =
    new GrammaticalRelation(Language.UniversalChinese,
      "amod", "adjectival modifier",
      MODIFIER, "NP|CLP|QP", tregexCompiler,
            "NP|CLP|QP < (ADJP=target $++ NP|CLP|QP ) ");

  /**
   * The "determiner modifier" (det) grammatical relation.
   * <p>
   * <pre>
   * <code>
   * Input:
   *   (NP (DP (DT 这些))
   *       (NP (NN 经济) (NN 活动)))
   * Output:
   *   det(活动-61, 这些-59)
   * </code>
   * </pre>
   */
  public static final GrammaticalRelation DETERMINER =
    new GrammaticalRelation(Language.UniversalChinese, "det", "determiner",
                            MODIFIER, "^NP|DP", tregexCompiler,
            "/^NP/ < (DP=target $++ NP )"
            //"DP < DT < QP=target"
    );

  /**
   * The "negative modifier" (neg) grammatical relation.
   * <p>
   * <pre>
   * <code>
   * Input:
   *   (VP
   *     (NP (NT 以前))
   *     (ADVP (AD 不))
   *     (ADVP (AD 曾))
   *     (VP (VV 遇到) (AS 过))))
   * Output:
   *   neg(遇到-30, 不-28)
   * </code>
   * </pre>
   */
  public static final GrammaticalRelation NEGATION_MODIFIER =
    new GrammaticalRelation(Language.UniversalChinese,
      "neg", "negation modifier",
      MODIFIER, "VP|ADJP|IP", tregexCompiler,
            "VP|ADJP|IP < (AD|VV=target < /^(\\u4e0d|\\u6CA1|\\u6CA1\\u6709)$/)",
            "VP|ADJP|IP < (ADVP|VV=target < (AD < /^(\\u4e0d|\\u6CA1|\\u6CA1\\u6709)$/))");

  /**
   * The "adverbial modifier" (advmod) grammatical relation.
   * <p>
   * <pre>
   * <code>
   * Input:
   *   (VP
   *     (ADVP (AD 基本))
   *     (VP (VV 做到) (AS 了)
   * Output:
   *   advmod(做到-74, 基本-73)
   * </code>
   * </pre>
   */
  public static final GrammaticalRelation ADVERBIAL_MODIFIER =
    new GrammaticalRelation(Language.UniversalChinese,
      "advmod", "adverbial modifier",
      MODIFIER,
      "VP|ADJP|IP|CP|PP|NP|QP", tregexCompiler,
            "VP|ADJP|IP|CP|PP|NP < (ADVP=target !< (AD < /^(\\u4e0d|\\u6CA1|\\u6CA1\\u6709)$/))",
            "VP|ADJP < AD|CS=target",
            "QP < (ADVP=target $+ QP)",
            "QP < ( QP $+ ADVP=target)");

  /**
   * The "dvp modifier" grammatical relation.
   * <p>
   * <code>
   * <pre>
   * Input:
   *   (VP (DVP
   *         (VP (VA 简单))
   *         (DEV 的))
   *       (VP (VV 采取) ...))
   * Output:
   *   dvpmod(采取-9, 简单-7)
   * </pre>
   * </code>
   */
  public static final GrammaticalRelation DVPM_MODIFIER =
    new GrammaticalRelation(Language.UniversalChinese, "dvpmod", "dvp modifier",
                            ADVERBIAL_MODIFIER, "VP", tregexCompiler,
            " VP < ( DVP=target $+ VP) ");

  ////////////////////////////////////////////////////////////
  // Special clausal dependents
  ////////////////////////////////////////////////////////////

  /**
   * The "auxiliary" (aux) grammatical relation.
   */
  public static final GrammaticalRelation AUX_MODIFIER =
    new GrammaticalRelation(Language.UniversalChinese, "aux", "auxiliary (example: should[modifier] leave[head])",
                            DEPENDENT, "VP", tregexCompiler
    );

  /**
   * The "modal" grammatical relation.
   * (IP
   *           (NP (NN 利益))
   *           (VP (VV 能)
   *             (VP (VV 得到)
   *               (NP (NN 保障)))))))))
   * <code> mmod </code> (得到-64, 能-63)
   */
  public static final GrammaticalRelation MODAL_VERB =
    new GrammaticalRelation(Language.UniversalChinese, "mmod", "modal verb",
                            AUX_MODIFIER, "VP", tregexCompiler,
            "VP < ( VV=target !< /^没有$/ $+ VP|VRD )");

  /**
   * The "aspect marker" grammatical relation.
   * (VP
   *     (ADVP (AD 基本))
   *     (VP (VV 做到) (AS 了)
   * <code> asp </code> (做到,了)
   */
  public static final GrammaticalRelation ASPECT_MARKER =
    new GrammaticalRelation(Language.UniversalChinese, "asp", "aspect",
                            AUX_MODIFIER, "VP", tregexCompiler,
            "VP < ( /^V*/ $+ AS=target)");

  /**
   * The "auxiliary passive" (auxpass) grammatical relation.
   */
  public static final GrammaticalRelation AUX_PASSIVE_MODIFIER =
    new GrammaticalRelation(Language.UniversalChinese, "auxpass", "auxiliary passive",
                            MODIFIER, "VP", tregexCompiler,
            "VP < SB|LB=target");

  /**
   * The "copula" grammatical relation.
   * <p>
   * <pre>
   * <code>
   * Input:
   *   (IP (NP (NR 浦东))
   *       (VP (VC 是)
   *           (NP (NN 工程)))))
   * Output (formerly reverse(attr)):
   *   cop(工程,是)
   * </code>
   * </pre>
   */
  public static final GrammaticalRelation COPULA =
    new GrammaticalRelation(Language.UniversalChinese, "cop", "copula",
                            DEPENDENT, "VP", tregexCompiler,
            " VP < VC=target");

  /**
   * The "marker" (mark) grammatical relation.  A marker is the word
   * introducing a finite clause subordinate to another clause.
   * <p>
   * <pre>
   * <code>
   * Input:
   *   (PP (P 因为)
   *       (IP
   *         (VP
   *           (VP
   *             (ADVP (AD 一))
   *             (VP (VV 开始)))
   *           (VP
   *             (ADVP (AD 就))
   *             (ADVP (AD 比较))
   *             (VP (VA 规范))))))
   * Output (formerly reverse(pccomp)):
   *   mark(开始-20,因为-18)
   *
   * Input:
   *   (LCP (IP (NP-SBJ (-NONE- *pro*))
   *     (VP (VV 积累) (AS 了) (NP-OBJ (NN 经验)))) (LC 以后))
   * Output (formerly reverse(lccomp)):
   *   mark(积累, 以后)
   *
   * Input:
   *   (CP
   *         (IP
   *           (VP
   *             (VP (VV 振兴)
   *               (NP (NR 上海)))
   *             (PU ，)
   *             (VP (VV 建设)
   *               (NP
   *                 (NP (NN 现代化))
   *                 (NP (NN 经济) (PU 、) (NN 贸易) (PU 、) (NN 金融))
   *                 (NP (NN 中心))))))
   *         (DEC 的))
   * Output (formerly cpm):
   *   mark(振兴, 的)
   *
   * Input:
   *   (DVP
   *     (VP (VA 简单))
   *     (DEV 的))
   * Output (formerly dvpm):
   *   mark(简单-7, 的-8)
   * </code>
   * </pre>
   */
  public static final GrammaticalRelation MARK =
    new GrammaticalRelation(Language.UniversalChinese, "mark",
        "marker (examples: that[modifier] expanded[head]; 开发/expand[head] 浦东/Pudong 的[modifier])",
        DEPENDENT, "^PP|^LCP|^CP|^DVP", tregexCompiler,
            "/^PP/ < (P=target $+ VP)",
            "/^LCP/ < (P=target $+ VP)",
            "/^CP/ < (__  $++ DEC=target)",
            "DVP < (__ $+ DEV=target)");

  /**
   * The "punctuation" grammatical relation.  This is used for any piece of
   * punctuation in a clause, if punctuation is being retained in the
   * typed dependencies.
   */
  public static final GrammaticalRelation PUNCTUATION =
    new GrammaticalRelation(Language.UniversalChinese, "punct", "punctuation",
        DEPENDENT, ".*", tregexCompiler,
        "__ < PU=target");

  ////////////////////////////////////////////////////////////
  // Other (compounding, coordination)
  ////////////////////////////////////////////////////////////

  /**
   * The "compound" grammatical relation (abstract).
   */
  public static final GrammaticalRelation COMPOUND =
    new GrammaticalRelation(Language.UniversalChinese, "compound", "compound (examples: phone book, three thousand)", ARGUMENT);

  /**
   * The "noun compound" (nn) grammatical relation.
   * Example:
   * (ROOT
   *   (IP
   *     (NP
   *       (NP (NR 上海) (NR 浦东))
   *       (NP (NN 开发)
   *         (CC 与)
   *         (NN 法制) (NN 建设)))
   *     (VP (VV 同步))))
   * <code> nn </code> (浦东, 上海)
   */
  public static final GrammaticalRelation NOUN_COMPOUND =
    new GrammaticalRelation(Language.UniversalChinese,
      "nn", "noun compound",
      COMPOUND, "^NP", tregexCompiler,
            "NP < (NN|NR|NT=target $+ NN|NR|NT)",
            "NP < (NN|NR|NT $+ FW=target)",
            "NP < (NP=target !$+ PU|CC $++ NP|PRN)");

  /**
   * The "coordinated verb compound" grammatical relation.
   *   (VCD (VV 颁布) (VV 实行))
   * comod(颁布-5, 实行-6)
   */
  public static final GrammaticalRelation VERB_COMPOUND =
    new GrammaticalRelation(Language.UniversalChinese, "comod", "coordinated verb compound",
                            COMPOUND, "VCD", tregexCompiler,
            "VCD < ( VV|VA $+  VV|VA=target)");

  /**
   * The "conjunct" (conj) grammatical relation.
   * <p>
   * <code>
   * <pre>
   * Input:
   *   (ROOT
   *     (IP
   *       (NP
   *         (NP (NR 上海) (NR 浦东))
   *         (NP (NN 开发)
   *           (CC 与)
   *           (NN 法制) (NN 建设)))
   *       (VP (VV 同步))))
   *
   *   The development of Shanghai 's Pudong is in step with the establishment
   *   of its legal system.
   * Output:
   *   conj(建设, 开发) [should be reversed]
   * </pre>
   * </code>
   *
   * TODO(pliang): make first item the head and the subsequent ones modifiers.
   */
  public static final GrammaticalRelation CONJUNCT =
    new GrammaticalRelation(Language.UniversalChinese,
      "conj", "conjunct",
      DEPENDENT, "FRAG|INC|IP|VP|NP|ADJP|PP|ADVP|UCP", tregexCompiler,
            "NP|ADJP|PP|ADVP|UCP < (!PU|CC=target $+ CC)",
            // Split the first rule to the second rule to avoid the duplication:
            // ccomp(前来-12, 投资-13)
            // conj(前来-12, 投资-13)
            //
            //      (IP
            //        (VP
            //          (VP (VV 前来))
            //          (VP
            //            (VCD (VV 投资) (VV 办厂)))
            //          (CC 和)
            //          (VP (VV 洽谈)
            //            (NP (NN 生意))))))
            "VP < (!PU|CC=target !$- VP $+ CC)",
            // TODO: this following line has to be fixed.
            //       I think for now it just doesn't match anything.
            "VP|NP|ADJP|PP|ADVP|UCP < ( __=target $+ PU $+ CC)",
            //"VP|NP|ADJP|PP|ADVP|UCP < ( __=target $+ (PU < 、) )",
            // Consider changing the rule ABOVE to these rules.
            "VP   < ( /^V/=target  $+ ((PU < 、) $+ /^V/))",
            "NP   < ( /^N/=target  $+ ((PU < 、) $+ /^N/))",
            "ADJP < ( JJ|ADJP=target  $+ ((PU < 、) $+ JJ|ADJP))",
            "PP   < ( /^P/=target  $+ ((PU < 、) $+ /^P/))",
            //"ADVP < ( /^AD/=target $+ ((PU < 、) $+ /^AD/))",
            "ADVP < ( /^AD/ $+ ((PU < 、) $+ /^AD/=target))",
            "UCP  < ( !PU|CC=target    $+ (PU < 、) )",
            // This is for the 'conj's separated by commas.
            // For now this creates too much duplicates with 'ccomp'.
            // Need to look at more examples.

            "PP < (PP $+ PP=target )",
            "NP <( NP=target $+ ((PU < 、) $+ NP) )",
            "NP <( NN|NR|NT|PN=target $+ ((PU < ，|、) $+ NN|NR|NT|PN) )",
            "VP < (CC $+ VV=target)",
            // Original version of this did not have the outer layer of
            // the FRAG|INC|IP|VP.  This caused a bug where the basic
            // dependencies could have cycles.
            "FRAG|INC|IP|VP < (VP  < VV|VC|VRD|VCD|VE|VA < NP|QP|LCP  $ IP|VP|VRD|VCD|VE|VC|VA=target)  ",
            "IP|VP < ( IP|VP < NP|QP|LCP $ IP|VP=target )");

  /**
   * The "coordination" grammatical relation.
   * A coordination is the relation between
   * an element and a conjunction.<p>
   * <code>
   * <pre>
   * Input:
   *   (ROOT
   *     (IP
   *       (NP
   *         (NP (NR 上海) (NR 浦东))
   *         (NP (NN 开发)
   *           (CC 与)
   *           (NN 法制) (NN 建设)))
   *       (VP (VV 同步))))
   * Output:
   *   cc(建设, 与) [should be cc(开发, 与)]
   * </pre>
   * </code>
   * TODO(pliang): by convention, the first item in the coordination should be
   * chosen, but currently, it's the head, which happens to be the last.
   */
  public static final GrammaticalRelation COORDINATION =
    new GrammaticalRelation(Language.UniversalChinese,
      "cc", "coordination", DEPENDENT,
      "VP|NP|ADJP|PP|ADVP|UCP|IP|QP", tregexCompiler,
            "VP|NP|ADJP|PP|ADVP|UCP|IP|QP < (CC=target)");

  /**
   * The "case" grammatical relation.
   * This covers prepositions, localizers, and associative markers.
   * <p>
   * <pre>
   * <code>
   * Input:
   *   (PP (P 根据)
   *       (NP
   *         (DNP
   *           (NP
   *             (NP (NN 国家))
   *             (CC 和)
   *             (NP (NR 上海市)))
   *           (DEG 的))
   *         (ADJP (JJ 有关))
   *         (NP (NN 规定))))
   * Output (formerly reverse(pobj)):
   *   case(规定-19, 根据-13)
   *
   * Input:
   *   (LCP
   *       (NP (NT 近年))
   *       (LC 来))
   * Output (formerly reverse(lobj)):
   *   case(近年-3, 来-4)
   *
   * Input:
   *   (NP (DNP
   *         (NP (NP (NR 浦东))
   *         (NP (NN 开发)))
   *         (DEG 的))
   *       (ADJP (JJ 有序))
   *       (NP (NN 进行)))
   * Output (formerly reverse(assm)):
   *   case(开发-31, 的-32)
   * </code>
   * </pre>
   */
  public static final GrammaticalRelation CASE =
    new GrammaticalRelation(Language.UniversalChinese, "case",
        "case marking (examples: Chair[head] 's[modifier], 根据/according[modifier] ... 规定/rule[head]; 近年/this year[head] 来[modifier])",
        DEPENDENT, "^PP|^LCP|^DNP", tregexCompiler,
            "/^PP/ < P=target",
            "/^LCP/ < LC=target",
            "/^DNP/ < DEG=target");

  ////////////////////////////////////////////////////////////
  // Other stuff: pliang: not sure exactly where they should go.
  ////////////////////////////////////////////////////////////

  /**
   * The "prepositional localizer modifier" grammatical relation.
   * (PP (P 在)
   *     (LCP
   *       (NP
   *         (DP (DT 这)
   *             (CLP (M 片)))
   *         (NP (NN 热土)))
   *       (LC 上)))
   * plmod(在-25, 上-29)
   */
  public static final GrammaticalRelation PREPOSITIONAL_LOCALIZER_MODIFIER =
    new GrammaticalRelation(Language.UniversalChinese,
      "plmod", "prepositional localizer modifier",
      MODIFIER, "PP", tregexCompiler,
            "PP < ( P $++ LCP=target )");

  /**
   * The "adjectival complement" grammatical relation.
   * Example:
   */
  // deleted by pichuan: no real matches
  /*
  public static final GrammaticalRelation ADJECTIVAL_COMPLEMENT =
    new GrammaticalRelation(Language.UniversalChinese,
      "acomp", "adjectival complement",
      COMPLEMENT, "VP", tregexCompiler,
      new String[]{
        "VP < (ADJP=target !$-- NP)"
      });
  */

  /**
   * The "localizer complement" grammatical relation.
   * (VP (VV 占)
   *     (LCP
   *       (QP (CD 九成))
   *       (LC 以上)))
   *   (PU ，)
   *   (vp (VV 达)
   *     (QP (CD 四百三十八点八亿)
   *       (CLP (M 美元))))
   * <code> loc </code> (占-11, 以上-13)
   */
  public static final GrammaticalRelation LOCALIZER_COMPLEMENT =
    new GrammaticalRelation(Language.UniversalChinese,
      "loc", "localizer complement",
      COMPLEMENT, "VP|IP", tregexCompiler,
            "VP|IP < LCP=target ");

  /**
   * The "resultative complement" grammatical relation.
   */
  public static final GrammaticalRelation RESULTATIVE_COMPLEMENT =
    new GrammaticalRelation(Language.UniversalChinese,
      "rcomp", "result verb",
      COMPLEMENT, "VRD", tregexCompiler,
            "VRD < ( /V*/ $+ /V*/=target )");

  /**
   * The "ba" grammatical relation.
   */
 public static final GrammaticalRelation BA =
   new GrammaticalRelation(Language.UniversalChinese, "ba", "ba",
                           DEPENDENT, "VP|IP", tregexCompiler,
           "VP|IP < BA=target ");

  /**
   * The "classifier modifier" grammatical relation.
   * <p>
   * <code>
   * <pre>
   * Input:
   *   ((QP (CD 七十一)
   *        (CLP (M 件)))
   *    (NP (NN 法规性) (NN 文件)))
   * Output:
   *   clf(文件-26, 件-24)
   * </pre>
   * </code>
   */
  public static final GrammaticalRelation CLASSIFIER_MODIFIER =
    new GrammaticalRelation(Language.UniversalChinese,
      "clf", "classifier modifier",
      MODIFIER, "^NP|DP|QP", tregexCompiler,
            "NP|QP < ( QP  =target << M $++ NN|NP|QP)",
            "DP < ( DT $+ CLP=target )");

  /**
   * The "possession modifier" grammatical relation.
   */
  // Fri Feb 20 15:40:13 2009 (pichuan)
  // I think this "poss" relation is just WRONG.
  // DEC is a complementizer or a nominalizer,
  // this rule probably originally want to capture "DEG".
  // But it seems like it's covered by "assm" (associative marker).
  /*
  public static final GrammaticalRelation POSSESSION_MODIFIER =
    new GrammaticalRelation(Language.UniversalChinese,
      "poss", "possession modifier",
      MODIFIER, "NP", tregexCompiler,
      new String[]{
        "NP < ( PN=target $+ DEC $+  NP )"
      });
  */

  /**
   * The "possessive marker" grammatical relation.
   */
  // Similar to the comments to "poss",
  // I think this relation is wrong and will not appear.
  /*
  public static final GrammaticalRelation POSSESSIVE_MODIFIER =
    new GrammaticalRelation(Language.UniversalChinese, "possm", "possessive marker",
                            MODIFIER, "NP", tregexCompiler,
                            new String[]{
                              "NP < ( PN $+ DEC=target ) "
                            });
  */

  /**
   * The "prepositional modifier" grammatical relation.
   *(IP
   *  (PP (P 对)
   *   (NP (PN 此)))
   * (PU ，)
   * (NP (NR 浦东))
   * (VP
   *   (VP
   *     (ADVP (AD 不))
   *     (VP (VC 是)
   *       (VP
   *         (DVP
   *           (VP (VA 简单))
   *           (DEV 的))
   *         (VP (VV 采取)
   * <code> prep </code> (采取-9, 对-1)
   */
  public static final GrammaticalRelation PREPOSITIONAL_MODIFIER =
    new GrammaticalRelation(Language.UniversalChinese,
      "prep", "prepositional modifier",
      MARK, "^NP|VP|IP", tregexCompiler,
            "/^NP/ < /^PP/=target",
            "VP < /^PP/=target",
            "IP < /^PP/=target ");

  /**
   * The "participial modifier" (prtmod) grammatical relation.
   */
  public static final GrammaticalRelation PART_VERB =
    new GrammaticalRelation(Language.UniversalChinese,
      "prtmod", "particle verb",
      MODIFIER, "VP|IP", tregexCompiler,
            "VP|IP < ( MSP=target )");

  /**
   * The "etc" grammatical relation.
   *   (NP
   *     (NP (NN 经济) (PU 、) (NN 贸易) (PU 、) (NN 建设) (PU 、) (NN 规划) (PU 、) (NN 科技) (PU 、) (NN 文教) (ETC 等))
   *     (NP (NN 领域)))
   * <code> etc </code> (办法-70, 等-71)
   */
  public static final GrammaticalRelation ETC =
    new GrammaticalRelation(Language.UniversalChinese, "etc", "ETC",
                            MODIFIER, "^NP", tregexCompiler,
            "/^NP/ < (NN|NR . ETC=target)");

  /**
   * The "xsubj" grammatical relation.
   *(IP
   *           (NP (PN 有些))
   *           (VP
   *             (VP
   *               (ADVP (AD 还))
   *               (ADVP (AD 只))
   *               (VP (VC 是)
   *                 (NP
   *                   (ADJP (JJ 暂行))
   *                   (NP (NN 规定)))))
   *             (PU ，)
   *             (VP (VV 有待)
   *               (IP
   *                 (VP
   *                   (PP (P 在)
   *                     (LCP
   *                       (NP (NN 实践))
   *                       (LC 中)))
   *                   (ADVP (AD 逐步))
   *                   (VP (VV 完善))))))))))
   * <code> xsubj </code> (完善-26, 有些-14)
   * TODO(pliang): replace with regular nsubj relation.
   */
  public static final GrammaticalRelation CONTROLLED_SUBJECT =
    new GrammaticalRelation(Language.UniversalChinese,
      "xsubj", "controlled subject",
      DEPENDENT, "VP", tregexCompiler,
            "VP !< NP < VP > (IP !$- NP !< NP !>> (VP < VC ) >+(VP) (VP $-- NP=target))");

  // Universal GrammaticalRelations
  private static final GrammaticalRelation chineseOnly = null;  // Place-holder: put this after a relation to mark it as Chinese-only
  private static final GrammaticalRelation[] rawValues = {
    DEPENDENT,
    ARGUMENT,
      SUBJECT,
        NOMINAL_SUBJECT,
        NOMINAL_PASSIVE_SUBJECT,
        //CLAUSAL_SUBJECT,  // Exists in Chinese?
        //CLAUSAL_PASSIVE_SUBJECT,  // Exists in Chinese?
      COMPLEMENT,
        OBJECT,
          DIRECT_OBJECT,
          INDIRECT_OBJECT,
            RANGE,  // Chinese only
        CLAUSAL_COMPLEMENT,
        //XCLAUSAL_COMPLEMENT,  // Exists in Chinese?
    MODIFIER,
      // Nominal heads, nominal dependents
      NUMERIC_MODIFIER,
        ORDINAL_MODIFIER, chineseOnly,
      APPOSITIONAL_MODIFIER,
        PARENTHETICAL_MODIFIER, chineseOnly,
      NOUN_MODIFIER,
        ASSOCIATIVE_MODIFIER, chineseOnly,
        TEMPORAL_MODIFIER, chineseOnly,
      // Nominal heads, predicate dependents
      RELATIVE_CLAUSE_MODIFIER,
      NONFINITE_CLAUSE_MODIFIER,
      //NOMINALIZED_CLAUSE_MODIFIER,  // Exists in Chinese?
      ADJECTIVAL_MODIFIER,
      DETERMINER,
      NEGATION_MODIFIER,
      // Predicate heads
      //ADVERBIAL_CLAUSE_MODIFIER,  // TODO(pliang): some of the existing advmod should be changed to this (advcl)
      ADVERBIAL_MODIFIER,
        DVPM_MODIFIER, chineseOnly,
    // Special clausal dependents
      //VOCATIVE,
      //DISCOURSE,
      //EXPL,
      AUX_MODIFIER,
        MODAL_VERB, chineseOnly,
        ASPECT_MARKER, chineseOnly,
      AUX_PASSIVE_MODIFIER,
      COPULA,
      MARK,
      PUNCTUATION,
    // Other
      COMPOUND,
        NOUN_COMPOUND, chineseOnly,
        VERB_COMPOUND, chineseOnly,
      CONJUNCT,
      COORDINATION,
      CASE,
    // Don't know what to do about these
      PREPOSITIONAL_LOCALIZER_MODIFIER, chineseOnly,
      LOCALIZER_COMPLEMENT, chineseOnly,
      RESULTATIVE_COMPLEMENT, chineseOnly,
      BA, chineseOnly,
      CLASSIFIER_MODIFIER, chineseOnly,
      PREPOSITIONAL_MODIFIER, chineseOnly,
      PART_VERB, chineseOnly,
      ETC, chineseOnly,
      CONTROLLED_SUBJECT, chineseOnly,
  };

  private static final List<GrammaticalRelation> values = new ArrayList<>();
    // Cache frequently used views of the values list
  private static final List<GrammaticalRelation> synchronizedValues =
    Collections.synchronizedList(values);

  public static final Set<GrammaticalRelation> universalValues = new HashSet<>();

  // Map from GrammaticalRelation short names to their corresponding
  // GrammaticalRelation objects
  public static final Map<String, GrammaticalRelation> shortNameToGRel = new ConcurrentHashMap<>();
  static {
    for (int i = 0; i < rawValues.length; i++) {
      GrammaticalRelation gr = rawValues[i];
      if (gr == chineseOnly) continue;
      synchronizedValues.add(gr);
      if (i + 1 == rawValues.length || rawValues[i + 1] != chineseOnly) {
        universalValues.add(gr);
      }
    }

    valuesLock().lock();
    try {
      for (GrammaticalRelation gr : UniversalChineseGrammaticalRelations.values()) {
        shortNameToGRel.put(gr.getShortName(), gr);
      }
    } finally {
      valuesLock().unlock();
    }
  }

  /**
   * Prints out the Chinese grammatical relations hierarchy.
   *
   * @param args Args are ignored.
   */
  public static void main(String[] args) {
    System.out.println(DEPENDENT.toPrettyString());
  }

}
