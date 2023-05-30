package edu.lu.uni.serval.tbar.main;

// selab: Add imports
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.lu.uni.serval.tbar.TBarFixer;
import edu.lu.uni.serval.tbar.TBarFixer.Granularity;
import edu.lu.uni.serval.tbar.config.Configuration;
import edu.lu.uni.serval.tbar.info.Patch;
import edu.lu.uni.serval.tbar.utils.SuspiciousPosition;
// selab: end
/**
 * Fix bugs with the known bug positions.
 * 
 * @author kui.liu
 *
 */
public class MainPerfectFL {
	
	private static Logger log = LoggerFactory.getLogger(MainPerfectFL.class);
	private static Granularity granularity = Granularity.File;
	
	public static void main(String[] args) {
//		if (args.length != 4) {
//			System.err.println("Arguments: \n"
//					+ "\t<Bug_Data_Path>: the directory of checking out Defects4J bugs. \n"
//					+ "\t<Bug_ID>: bug id of each Defects4J bug, such as Chart_1. \n"
//					+ "\t<defects4j_Home>: the directory of defects4j git repository.\n"
//					+ "\t<isTestFixPatterns>: true - try all fix patterns for each bug, false - perfect fault localization configuration.");
//			System.exit(0);
//		}
		String bugDataPath = args[0];//"/Users/kui.liu/Public/Defects4J_Data/";//
		String bugId = args[1]; //"Closure_4";// 
		// selab: origin
    // String defects4jHome = args[2];//"/Users/kui.liu/Public/GitRepos/defects4j/";//
		// Configuration.failedTestCasesFilePath = args[3];//"/Users/kui.liu/eclipse-fault-localization/FL-VS-APR/data/FailedTestCases/";//
		// Configuration.knownBugPositions = args[4];
		// boolean isTestFixPatterns = false;//Boolean.valueOf(args[3]);//
		// selab: modified code
    String defects4jHome = "";
    Configuration.knownBugPositions = args[2];
    boolean isTestFixPatterns = true;
		// selab: end
		String granularityStr = "Line";
		System.out.println(bugId);
		if ("line".equalsIgnoreCase(granularityStr) || "l".equalsIgnoreCase(granularityStr)) {
			granularity = Granularity.Line;
			if (isTestFixPatterns) Configuration.outputPath += "FixPatterns/";
			else Configuration.outputPath += "PerfectFL/";
//		} else if ("file".equalsIgnoreCase(granularityStr) || "f".equalsIgnoreCase(granularityStr)) {
//			granularity = Granularity.File;
//			Configuration.outputPath += "File/";
//		} else {
//			System.out.println("Last argument must be l, L, line, or Line, f, F, file, or File.");
//			System.exit(0);
		}
		fixBug(bugDataPath, defects4jHome, bugId, isTestFixPatterns);
	}

	public static void fixBug(String bugDataPath, String defects4jHome, String bugIdStr, boolean isTestFixPatterns) {
		String[] elements = bugIdStr.split("_");
		String projectName = elements[0];
		int bugId;
		try {
			bugId = Integer.valueOf(elements[1]);
		} catch (NumberFormatException e) {
			System.err.println("Please input correct buggy project ID, such as \"Chart_1\".");
			return;
		}
		
		TBarFixer fixer = new TBarFixer(bugDataPath, projectName, bugId, defects4jHome); 
		fixer.dataType = "TBar";
		fixer.isTestFixPatterns = isTestFixPatterns;
		//switch (granularity) {
		//case Line:
		//	fixer.granularity = Granularity.Line;
		//	break;
//		case File:
//			fixer.granularity = Granularity.File;
//			break;
		//default:
		//	return;
		//}
		// selab: origin; commenterize origin.
    /*
		if (Integer.MAX_VALUE == fixer.minErrorTest) {
			System.out.println("Failed to defects4j compile bug " + bugIdStr);
			return;
		}
		fixer.metric = Configuration.faultLocalizationMetric;
    fixer.fixProcess();
    */
    // selab: modified code
    // Set variables & contants
//	fixer.metric = Configuration.faultLocalizationMetric;

   
    // * Fault Localization
    // FL-1. Create a project on SonarQube
    // FL-2. Run the project with SonarScanner
    // FL-3. Get suspicious statements from the results of the project on SonarQube DB
    // FL-4. Load the suspicious statements list
    List<SuspiciousPosition> suspiciousCodeList = fixer.localizeDefects();
	System.out.println("fl size~~~:" + suspiciousCodeList.size());


    // * Patch Generation for each suspicious statement
    // PG-1. Extract Faulty AST Nodes as buggy context
    // PG-2. List-up the fix templates
    // PG-3. Concretize the candidate patches using templates
	//  List<Patch> candidatePatches = fixer.generatePatch_1(suspiciousCodeList);

	List<Patch> candidatePatches = fixer.generatePatch_singleHunk(suspiciousCodeList);


    // * Patch Validation for each candidate patch
    // PV-1. Apply the patch
    // PV-2. Run the project with SonarScanner
    // PV-3. Process the validation results
    // PV-4. Save it to the DB

	//TODO:
    // selab: end
		
		int fixedStatus = fixer.fixedStatus;
		switch (fixedStatus) {
		case 0:
			log.info("=======Failed to fix bug " + bugIdStr);
			break;
		case 1:
			log.info("=======Succeeded to fix bug " + bugIdStr);
			break;
		case 2:
			log.info("=======Partial succeeded to fix bug " + bugIdStr);
			break;
		}
	}

}
