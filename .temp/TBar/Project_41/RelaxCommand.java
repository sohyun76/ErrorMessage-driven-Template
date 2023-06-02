package org.obolibrary.robot;

import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles inputs and outputs for the {@link RelaxOperation}.
 *
 * @author <a href="mailto:cjmungall@lbl.gov">Chris Mungall</a>
 */
public class RelaxCommand implements Command {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(RelaxCommand.class);

  /** Store the command-line options for the command. */
  private Options options;

  /** Initialize the command. */
  public RelaxCommand() {
    Options o = CommandLineHelper.getCommonOptions();
    o.addOption("i", "input", true, "relax ontology from a file");
    o.addOption("I", "input-iri", true, "relax ontology from an IRI");
    o.addOption("o", "output", true, "save relaxed ontology to a file");
    options = o;
  }

  /**
   * Name of the command.
   *
   * @return name
   */
  public String getName() {
    return "relax";
  }

  /**
   * Brief description of the command.
   *
   * @return description
   */
  public String getDescription() {
    return "relax ontology";
  }

  /**
   * Command-line usage for the command.
   *
   * @return usage
   */
  public String getUsage() {
    return "robot relax --input <file> " + "[options] " + "--output <file>";
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
   * Handle the command-line and file operations for the relaxOperation.
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
   * Given an input state and command line arguments, run a reasoner, and add axioms to the input
   * ontology, returning a state with the updated ontology.
   *
   * @param state the state from the previous command, or null
   * @param args the command-line arguments
   * @return the state with inferred axioms added to the ontology
   * @throws Exception on any problem
   */
  public CommandState execute(CommandState state, String[] args) throws Exception {
    CommandLine line = CommandLineHelper.getCommandLine(getUsage(), getOptions(), args);
    if (line == null) {
      return null;
    }

    if (state == null) {
      state = new CommandState();
    }

    IOHelper ioHelper = CommandLineHelper.getIOHelper(line);
    state = CommandLineHelper.updateInputOntology(ioHelper, state, line);
    OWLOntology ontology = state.getOntology();

    // Override default reasoner options with command-line options
    Map<String, String> relaxOptions = RelaxOperation.getDefaultOptions();
    for (String option : relaxOptions.keySet()) {
      if (line.hasOption(option)) {
        relaxOptions.put(option, line.getOptionValue(option));
      }
    }

    RelaxOperation.relax(ontology, relaxOptions);

    CommandLineHelper.maybeSaveOutput(line, ontology);

    return state;
  }
}
