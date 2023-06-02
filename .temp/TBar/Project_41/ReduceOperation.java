package org.obolibrary.robot;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reason over an ontology and remove redundant SubClassOf axioms
 *
 * <p>Every axiom <code>A = SubClassOf(C D)</code> is tested (C or D are permitted to be anonymous,
 * e.g. SomeValuesFrom)
 *
 * <p>If there already exists an axiom <code>SubClassOf(C Z)</code>, where Z is entailed to be a
 * proper SubClassOf of D (direct or indirect), then A is redundant, and removed.
 *
 * <h2>Implementation</h2>
 *
 * <p>Because an OWL reasoner will only return named (non-anonymous) superclasses, we add a
 * pre-processing step, where for each class C appearing in either LHS or RHS of a SubClassOf
 * expression, if C is anonymous, we create a named class C' and add a temporary axioms <code>
 * EquivalentClasses(C' C)</code>, which is later removed as a post-processing step. When performing
 * reasoner tests, we can then substitute C for C'
 *
 * <h2>GENERAL CLASS INCLUSION AXIOMS</h2>
 *
 * <p>We make a special additional case of redunancy, as in the following example: <code>
 * 1. (hand and part-of some human) SubClassOf part-of some forelimb
 * 2. hand SubClassOf part-of some forelimb
 * </code> Here we treat axiom 1 as redundant, but this is not detected by the algorithm above,
 * because there is no explicit SubClassOf axiom between the GCI LHS and 'human'. We therefore
 * extend the test above and first find all superclasses of anonymous LHSs, and then test for these
 *
 * @author <a href="mailto:cjmungall@lbl.gov">Chris Mungall</a>
 */
public class ReduceOperation {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(ReduceOperation.class);

  /**
   * Return a map from option name to default option value, for all the available reasoner options.
   *
   * @return a map with default values for all available options
   */
  public static Map<String, String> getDefaultOptions() {
    Map<String, String> options = new HashMap<>();
    options.put("preserve-annotated-axioms", "false");
    options.put("named-classes-only", "false");
    return options;
  }

  /**
   * Remove redundant SubClassOf axioms.
   *
   * @param ontology The ontology to reduce.
   * @param reasonerFactory The reasoner factory to use.
   * @throws OWLOntologyCreationException on ontology problem
   */
  public static void reduce(OWLOntology ontology, OWLReasonerFactory reasonerFactory)
      throws OWLOntologyCreationException {
    reduce(ontology, reasonerFactory, getDefaultOptions());
  }

  /**
   * Remove redundant SubClassOf axioms.
   *
   * @param ontology The ontology to reduce.
   * @param reasonerFactory The reasoner factory to use.
   * @param options A map of options for the operation.
   * @throws OWLOntologyCreationException on ontology problem
   */
  public static void reduce(
      OWLOntology ontology, OWLReasonerFactory reasonerFactory, Map<String, String> options)
      throws OWLOntologyCreationException {
    boolean preserveAnnotatedAxioms =
        OptionsHelper.optionIsTrue(options, "preserve-annotated-axioms");
    boolean namedClassesOnly = OptionsHelper.optionIsTrue(options, "named-classes-only");
    if (namedClassesOnly) {
      reduceNamedOnly(ontology, reasonerFactory, preserveAnnotatedAxioms);
    } else {
      reduceAllClassExpressions(ontology, reasonerFactory, preserveAnnotatedAxioms);
    }
  }

  /**
   * Remove redundant SubClassOf axioms.
   *
   * @param ontology The ontology to reduce.
   * @param reasonerFactory The reasoner factory to use.
   * @param preserveAnnotatedAxioms Whether to not remove redundant, but annotated, axioms.
   * @throws OWLOntologyCreationException on ontology problem
   */
  private static void reduceAllClassExpressions(
      OWLOntology ontology, OWLReasonerFactory reasonerFactory, boolean preserveAnnotatedAxioms)
      throws OWLOntologyCreationException {

    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    OWLDataFactory dataFactory = manager.getOWLDataFactory();

    // we treat an axiom as redundant if its is redundant within the
    // subClassOf graph, including OP characteristic axioms (e.g. transitivity)
    OWLOntology subOntology = manager.createOntology();
    for (OWLAxiom a : ontology.getAxioms(Imports.INCLUDED)) {
      if (a instanceof OWLSubClassOfAxiom || a instanceof OWLObjectPropertyCharacteristicAxiom) {
        manager.addAxiom(subOntology, a);
      }
    }

    Map<OWLClass, Set<OWLClass>> assertedSubClassMap = new HashMap<>();
    Set<OWLSubClassOfAxiom> assertedSubClassAxioms = ontology.getAxioms(AxiomType.SUBCLASS_OF);
    // TODO - exprs is updated but never used
    Set<OWLClassExpression> exprs = new HashSet<>();
    Map<OWLClassExpression, OWLClass> exprToNamedClassMap = new HashMap<>();

    for (OWLSubClassOfAxiom ax : assertedSubClassAxioms) {
      OWLClass subClass = mapClass(dataFactory, exprToNamedClassMap, ax.getSubClass());
      OWLClass superClass = mapClass(dataFactory, exprToNamedClassMap, ax.getSuperClass());

      if (!assertedSubClassMap.containsKey(subClass)) {
        assertedSubClassMap.put(subClass, new HashSet<>());
      }

      assertedSubClassMap.get(subClass).add(superClass);

      // DEP
      if (ax.getSubClass().isAnonymous()) {
        exprs.add(ax.getSubClass());
      }
      if (ax.getSuperClass().isAnonymous()) {
        exprs.add(ax.getSuperClass());
      }
    }

    for (OWLClassExpression x : exprToNamedClassMap.keySet()) {
      OWLAxiom ax = dataFactory.getOWLEquivalentClassesAxiom(exprToNamedClassMap.get(x), x);
      manager.addAxiom(subOntology, ax);
    }

    // TO DO: DRY - move to ReasonerOperation module
    OWLReasoner reasoner = reasonerFactory.createReasoner(subOntology);
    if (!reasoner.isConsistent()) {
      logger.info("Ontology is not consistent!");
      return;
    }

    Node<OWLClass> unsatisfiableClasses = reasoner.getUnsatisfiableClasses();
    if (unsatisfiableClasses.getSize() > 1) {
      logger.info(
          "There are {} unsatisfiable classes in the ontology.", unsatisfiableClasses.getSize());
      for (OWLClass cls : unsatisfiableClasses) {
        if (!cls.isOWLNothing()) {
          logger.info("    unsatisfiable: " + cls.getIRI());
        }
      }
    }

    // Constructing an inverse map of exprToNamedClassMap, which will be generated and used for the
    // purpose of debugging only.
    Multimap<OWLClass, OWLClassExpression> revExprToNamedClassMap = HashMultimap.create();
    if (logger.isDebugEnabled()) {
      for (Map.Entry<OWLClassExpression, OWLClass> entry : exprToNamedClassMap.entrySet()) {
        revExprToNamedClassMap.put(entry.getValue(), entry.getKey());
      }
    }

    Set<OWLSubClassOfAxiom> rmAxioms = new HashSet<>();
    for (OWLSubClassOfAxiom ax : assertedSubClassAxioms) {
      if (preserveAnnotatedAxioms) {
        if (ax.getAnnotations().size() > 0) {
          logger.debug("Protecting axiom with annotations: " + ax);
          continue;
        }
      }

      logger.debug("Testing: " + ax);
      OWLClassExpression subClassExpr = ax.getSubClass();
      OWLClassExpression superClassExpr = ax.getSuperClass();
      OWLClass subClass = exprToNamedClassMap.get(subClassExpr);
      OWLClass superClass = exprToNamedClassMap.get(superClassExpr);
      boolean isRedundant = false;

      for (OWLClass assertedSuper : assertedSubClassMap.get(subClass)) {
        if (reasoner.getSuperClasses(assertedSuper, false).containsEntity(superClass)) {
          isRedundant = true;
          // (Previous) DUMB CODE FOR DEBUGGING
          /*
          OWLClassExpression assertedSuperX = assertedSuper;
          for (OWLClassExpression x : exprToNamedClassMap.keySet()) {
            if (exprToNamedClassMap.get(x).equals(assertedSuper)) {
              assertedSuperX = x;
            }
          }
          */

          // Optimized codes that will run only if the debugging mode is enabled
          if (logger.isDebugEnabled()) {
            Collection<OWLClassExpression> classExprs = revExprToNamedClassMap.get(assertedSuper);
            for (OWLClassExpression assertedSuperX : classExprs) {
              logger.debug(
                  "Redundant: "
                      + ax
                      + ", because "
                      + assertedSuper
                      + "("
                      + assertedSuperX
                      + ") "
                      + " subClassOf "
                      + superClass
                      + " ("
                      + superClassExpr
                      + ")");
            }
          }

          break;
        }
      }

      // Special case for GCIs
      if (subClassExpr.isAnonymous()) {
        logger.debug("GCI:" + subClassExpr);
        for (OWLClass intermediateParent :
            reasoner.getSuperClasses(subClass, false).getFlattened()) {
          if (assertedSubClassMap.containsKey(intermediateParent)) {
            logger.debug("GCI intermediate parent:" + intermediateParent);
            if (reasoner.getSuperClasses(intermediateParent, false).containsEntity(superClass)) {
              isRedundant = true;
              break;
            }
            //                   for (OWLClass assertedSuper
            //                           : subClassMap.get(intermediateParent)) {
            //                       logger.info("  DOES: " + assertedSuper
            //                           + "  CONTAIN:"+superClass);
            //                       logger.info("   SUPES="
            //                           + reasoner.getSuperClasses(assertedSuper, false));
            //                       if (reasoner.getSuperClasses(assertedSuper, false)
            //                             .containsEntity(superClass)) {
            //                           isRedundant = true;
            //                           break;
            //                       }
            //                   }
          }
        }
      }

      if (isRedundant) {
        logger.info("REMOVING REDUNDANT: " + ax);
        rmAxioms.add(ax);
      }
    }

    // remove redundant axiom
    for (OWLAxiom ax : rmAxioms) {
      manager.removeAxiom(ontology, ax);
    }
    reasoner.dispose();
  }

  /**
   * Map a class expression to an equivalent named class; creates temp class plus axiom if not
   * already present.
   *
   * @param dataFactory A datafactory for creating the mapped class expression
   * @param rxmap A map from class expressions to classes
   * @param x The OWLClassExpression to map
   * @return the mapped OWLClass
   */
  private static OWLClass mapClass(
      OWLDataFactory dataFactory, Map<OWLClassExpression, OWLClass> rxmap, OWLClassExpression x) {
    if (!rxmap.containsKey(x)) {
      if (x.isAnonymous()) {
        UUID uuid = UUID.randomUUID();
        OWLClass c = dataFactory.getOWLClass(IRI.create("urn:uuid" + uuid.toString()));
        logger.debug(c + " ==> " + x);
        rxmap.put(x, c);
      } else {
        rxmap.put(x, (OWLClass) x);
      }
    }
    return rxmap.get(x);
  }

  /**
   * Remove redundant SubClassOf axioms, only considering named classes. When only considering named
   * classes, a somewhat more efficient algorithm can be used.
   *
   * @param ontology The ontology to reduce.
   * @param reasonerFactory The reasoner factory to use.
   * @param preserveAnnotatedAxioms Whether to not remove redundant, but annotated, axioms.
   */
  private static void reduceNamedOnly(
      OWLOntology ontology, OWLReasonerFactory reasonerFactory, boolean preserveAnnotatedAxioms) {
    // Map<superclass, Map<subclass, axioms>>
    Map<OWLClass, Map<OWLClass, Set<OWLSubClassOfAxiom>>> assertions = new HashMap<>();
    Set<OWLSubClassOfAxiom> assertedSubClassAxioms = ontology.getAxioms(AxiomType.SUBCLASS_OF);
    for (OWLSubClassOfAxiom ax : assertedSubClassAxioms) {
      if (!ax.getSubClass().isAnonymous() && !ax.getSuperClass().isAnonymous()) {
        OWLClass subclass = ax.getSubClass().asOWLClass();
        OWLClass superclass = ax.getSuperClass().asOWLClass();
        if (!assertions.containsKey(superclass)) {
          assertions.put(superclass, new HashMap<>());
        }
        Map<OWLClass, Set<OWLSubClassOfAxiom>> subMap = assertions.get(superclass);
        if (!subMap.containsKey(subclass)) {
          subMap.put(subclass, new HashSet<>());
        }
        Set<OWLSubClassOfAxiom> axioms = subMap.get(subclass);
        axioms.add(ax);
      }
    }
    OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
    if (!reasoner.isConsistent()) {
      logger.info("Ontology is not consistent!");
      return;
    }
    Node<OWLClass> unsatisfiableClasses = reasoner.getUnsatisfiableClasses();
    if (unsatisfiableClasses.getSize() > 1) {
      logger.info(
          "There are {} unsatisfiable classes in the ontology.", unsatisfiableClasses.getSize());
      for (OWLClass cls : unsatisfiableClasses) {
        if (!cls.isOWLNothing()) {
          logger.info("    unsatisfiable: " + cls.getIRI());
        }
      }
    }
    Set<OWLSubClassOfAxiom> nonredundant = new HashSet<>();
    Set<Node<OWLClass>> alreadySeen = new HashSet<>();
    findNonRedundant(reasoner.getTopClassNode(), reasoner, assertions, nonredundant, alreadySeen);
    OWLOntologyManager manager = ontology.getOWLOntologyManager();
    for (OWLSubClassOfAxiom ax : assertedSubClassAxioms) {
      if (!ax.getSubClass().isAnonymous() && !ax.getSuperClass().isAnonymous()) {
        if (preserveAnnotatedAxioms) {
          if (ax.getAnnotations().size() > 0) {
            logger.debug("Protecting axiom with annotations: " + ax);
            continue;
          }
        }
        if (!nonredundant.contains(ax)) {
          manager.removeAxiom(ontology, ax);
        }
      }
    }
    reasoner.dispose();
  }

  private static void findNonRedundant(
      Node<OWLClass> node,
      OWLReasoner reasoner,
      Map<OWLClass, Map<OWLClass, Set<OWLSubClassOfAxiom>>> assertions,
      Set<OWLSubClassOfAxiom> nonredundant,
      Set<Node<OWLClass>> alreadySeen) {
    if (!alreadySeen.contains(node)) {
      NodeSet<OWLClass> subclasses = reasoner.getSubClasses(node.getRepresentativeElement(), true);
      for (OWLClass superclass : node.getEntities()) {
        for (OWLClass subclass : subclasses.getFlattened()) {
          if (assertions.containsKey(superclass)) {
            Map<OWLClass, Set<OWLSubClassOfAxiom>> subclassAxiomsBySubclass =
                assertions.get(superclass);
            if (subclassAxiomsBySubclass.containsKey(subclass)) {
              nonredundant.addAll(subclassAxiomsBySubclass.get(subclass));
            }
          }
        }
      }
      alreadySeen.add(node);
      for (Node<OWLClass> subclassNode : subclasses.getNodes()) {
        findNonRedundant(subclassNode, reasoner, assertions, nonredundant, alreadySeen);
      }
    }
  }
}
