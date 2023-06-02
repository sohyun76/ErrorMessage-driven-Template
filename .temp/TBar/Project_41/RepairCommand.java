package org.obolibrary.robot;

import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles inputs and outputs for the {@link RepairOperation}.
 *
 * @author cjm
 */
public class RepairCommand implements Command {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(RepairCommand.class);

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialze the command. */
  public RepairCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("i", "input", true, "load ontology from a file");
    o.addOption("I", "input-iri", true, "load ontology from an IRI");
    o.addOption("o", "output", true, "save ontology to a file");
    o.addOption("O", "output-iri", true, "set OntologyIRI for output");
    o.addOption(
        "m",
        "merge-axiom-annotations",
        true,
        "if true, merge axiom annotations on duplicate axioms");
    options = o;
    o.addOption("a", "annotation-property", true, "an annotation property to migrate");
    o.addOption(
        "A",
        "annotation-properties-file",
        true,
        "load annotation properties to migrate from a file");
  }

  /**
   * Name of the command.
   *
   * @return name
   */
  public String getName() {
    return "repair";
  }

  /**
   * Brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "repair terms from an ontology";
  }

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  public String getUsage() {
    return "robot repair --input <file> " + "--output <file> " + "--output-iri <iri>";
  }

  /**
   * Command-line options for the command.
   *
   * @return options
   */
  public Options getOptions() {
    return options;
  }

  /**
   * Handle the command-line and file operations for the RepairOperation.
   *
   * @param args strings to use as arguments
   */
  public void main(String[] args) {
    try {
      execute(null, args);
    } catch (Exception e) {
      CommandLineHelper.handleException(e);
    }
  }

  /**
   * Given an input state and command line arguments, repair a new ontology and return an new state.
   * The input ontology is not changed.
   *
   * @param state the state from the previous command, or null
   * @param args the command-line arguments
   * @return a new state with the repaired ontology
   * @throws Exception on any problem
   */
  public CommandState execute(CommandState state, String[] args) throws Exception {
    OWLOntology outputOntology;

    CommandLine line = CommandLineHelper.getCommandLine(getUsage(), getOptions(), args);
    if (line == null) {
      return null;
    }

    IOHelper ioHelper = CommandLineHelper.getIOHelper(line);
    state = CommandLineHelper.updateInputOntology(ioHelper, state, line);
    OWLOntology inputOntology = state.getOntology();

    IRI outputIRI = CommandLineHelper.getOutputIRI(line);
    if (outputIRI == null) {
      outputIRI = inputOntology.getOntologyID().getOntologyIRI().orNull();
    }

    boolean mergeAxiomAnnotations =
        CommandLineHelper.getBooleanValue(line, "merge-axiom-annotations", false);

    OWLDataFactory factory = inputOntology.getOWLOntologyManager().getOWLDataFactory();
    Set<OWLAnnotationProperty> properties =
        CommandLineHelper.getTerms(
                ioHelper, line, "annotation-property", "annotation-properties-file")
            .stream()
            .map(factory::getOWLAnnotationProperty)
            .collect(Collectors.toSet());

    RepairOperation.repair(inputOntology, ioHelper, mergeAxiomAnnotations, properties);
    outputOntology = inputOntology;
    if (outputIRI != null) {
      outputOntology.getOWLOntologyManager().setOntologyDocumentIRI(outputOntology, outputIRI);
    }

    CommandLineHelper.maybeSaveOutput(line, outputOntology);

    state.setOntology(outputOntology);
    return state;
  }
}
