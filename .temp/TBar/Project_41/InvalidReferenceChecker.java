package org.obolibrary.robot.checks;

import java.util.HashSet;
import java.util.Set;
import org.obolibrary.robot.checks.InvalidReferenceViolation.Category;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;

/**
 * Checks to determine if axioms contain invalid references: to dangling classes or deprecated
 * classes.
 *
 * <p>This is a structural check. To determine the *logical* validity of an axiom reference, robot
 * uses OWL reasoning.
 *
 * <p>See: https://github.com/ontodev/robot/issues/1
 *
 * @author cjm
 */
public class InvalidReferenceChecker {

  /**
   * Checks if entity is dangling.
   *
   * @param ontology the OWLOntology to check
   * @param entity the OWLEntity to check
   * @return true if owlClass has no logical or non-logical axioms
   */
  public static boolean isDangling(OWLOntology ontology, OWLEntity entity) {
    if (entity instanceof OWLClass) {
      OWLClass owlClass = (OWLClass) entity;
      // Ignore OWLThing and OWLNothing
      // Not dangling if there are axioms about the class
      if (owlClass.isOWLThing()
          || owlClass.isOWLNothing()
          || ontology.getAxioms(owlClass, Imports.INCLUDED).size() > 0) return false;
      return ontology.getAnnotationAssertionAxioms(owlClass.getIRI()).size() <= 0;
    }
    if (entity instanceof OWLObjectProperty) {
      OWLObjectProperty owlProperty = (OWLObjectProperty) entity;
      // Ignore top and bottom properties
      // Not dangling if there are axioms about the property
      if (owlProperty.isOWLBottomDataProperty()
          || owlProperty.isOWLTopDataProperty()
          || owlProperty.isOWLBottomObjectProperty()
          || owlProperty.isOWLTopObjectProperty()
          || ontology.getAxioms(owlProperty, Imports.INCLUDED).size() > 0) return false;
      return ontology.getAnnotationAssertionAxioms(owlProperty.getIRI()).size() <= 0;
    }
    return false;
  }

  /**
   * Checks if entity is deprecated.
   *
   * @param ontology the OWLOntology to check
   * @param entity the OWLEntity to check
   * @return true if entity is deprecated
   */
  public static boolean isDeprecated(OWLOntology ontology, OWLEntity entity) {
    for (OWLOntology o : ontology.getImportsClosure()) {
      for (OWLAnnotationAssertionAxiom a : o.getAnnotationAssertionAxioms(entity.getIRI())) {
        if (a.isDeprecatedIRIAssertion()) {
          OWLLiteral value = a.getValue().asLiteral().orNull();
          if (value != null && value.parseBoolean()) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Finds axioms that reference a deprecated or dangling entity.
   *
   * <p>Declaration axioms that reference a deprecated class do not count.
   *
   * <p>Note that this does not count the value field of an annotation assertion, since these
   * reference IRIs and not entities
   *
   * @param ontology the OWLOntology to check
   * @param axioms set of OWLAxioms to check
   * @param ignoreDangling if true, ignore dangling entities
   * @return all violations
   */
  public static Set<InvalidReferenceViolation> getInvalidReferenceViolations(
      OWLOntology ontology, Set<OWLAxiom> axioms, boolean ignoreDangling) {
    Set<InvalidReferenceViolation> violations = new HashSet<>();
    for (OWLAxiom axiom : axioms) {
      for (OWLEntity e : expandedSignature(axiom, ontology)) {
        if (!ignoreDangling && isDangling(ontology, e)) {
          violations.add(InvalidReferenceViolation.create(axiom, e, Category.DANGLING));
        }
        if (isDeprecated(ontology, e)) {
          if (!(axiom instanceof OWLDeclarationAxiom)) {
            violations.add(InvalidReferenceViolation.create(axiom, e, Category.DEPRECATED));
          }
        }
      }
    }
    return violations;
  }

  /**
   * @param ontology the OWLOntology to check
   * @param ignoreDangling boolean to ignore dangling classes
   * @return all violations in ontology
   */
  public static Set<InvalidReferenceViolation> getInvalidReferenceViolations(
      OWLOntology ontology, boolean ignoreDangling) {
    return getInvalidReferenceViolations(
        ontology, ontology.getAxioms(Imports.INCLUDED), ignoreDangling);
  }

  /**
   * Checks an import module. If the base ontology refers to deprecated entities in the import
   * module this constitutes a violation
   *
   * @param importModule the OWLOntology import to check
   * @param baseOntology the base OWLOntology to check
   * @return all deprecation-reference violations in baseOntology with respect to import module
   */
  public static Set<InvalidReferenceViolation> checkImportModule(
      OWLOntology importModule, OWLOntology baseOntology) {
    return getInvalidReferenceViolations(
        importModule, baseOntology.getAxioms(Imports.INCLUDED), true);
  }

  /**
   * Get the entity signature for an axiom. This expands the standard axiom signature to include
   * entities found in the ontology which have IRIs used as the subject for an annotation assertion
   * axiom.
   *
   * @param axiom the axiom for which to get the entity signature
   * @param ontology ontology to search for entities with IRIs used in annotation assertions
   * @return set of entities representing the signature of the axiom
   */
  private static Set<OWLEntity> expandedSignature(OWLAxiom axiom, OWLOntology ontology) {
    if (axiom instanceof OWLAnnotationAssertionAxiom) {
      Set<OWLEntity> signature = axiom.getSignature();
      OWLAnnotationAssertionAxiom annotationAxiom = (OWLAnnotationAssertionAxiom) axiom;
      if (annotationAxiom.getSubject().isIRI()) {
        IRI subjectIRI = (IRI) annotationAxiom.getSubject();
        signature.addAll(ontology.getEntitiesInSignature(subjectIRI));
      }
      return signature;
    } else {
      return axiom.getSignature();
    }
  }
}
