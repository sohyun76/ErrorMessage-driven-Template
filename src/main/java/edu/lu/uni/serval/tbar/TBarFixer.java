package edu.lu.uni.serval.tbar;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
// selab: add imports
import java.util.Map;

// selab: end

//sso : start
import java.util.Set;
import java.util.Vector;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.lu.uni.serval.AST.ASTGenerator;
import edu.lu.uni.serval.AST.ASTGenerator.TokenType;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.config.Configuration;
import edu.lu.uni.serval.tbar.dataprepare.SuspStmt;
import edu.lu.uni.serval.tbar.ematemplates.RemoveTemplate;
import edu.lu.uni.serval.tbar.fixtemplate.FixTemplate;
import edu.lu.uni.serval.tbar.info.Patch;
import edu.lu.uni.serval.tbar.utils.Checker;
import edu.lu.uni.serval.tbar.utils.DBUtils;
import edu.lu.uni.serval.tbar.utils.FileHelper;
import edu.lu.uni.serval.tbar.utils.ShellUtils;
import edu.lu.uni.serval.tbar.utils.SuspiciousCodeParser;
import edu.lu.uni.serval.tbar.utils.SuspiciousPosition;

import org.eclipse.jdt.core.dom.*;
import java.nio.charset.StandardCharsets;
//import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.StringEscapeUtils;

/**
 * 
 * @author kui.liu
 *
 */
@SuppressWarnings("unused")
public class TBarFixer extends AbstractFixer {
	
	public Granularity granularity = Granularity.FL;
	
	public enum Granularity {
		Line,
		File,
		FL
	}
	
  // selab: set global variable for the generated patches
  private int currentSCNIndex = -1;
  private int currentIssueIndexes = -1;
  public List<Patch> candidatePatches = new ArrayList<>();
  public List<SuspCodeNode> suspiciousCodeNodes = new ArrayList<>();
  public List<String> candidatePatchPatterns = new ArrayList<>();
  public List<Integer> scnIndexes = new ArrayList<>();
  public List<Integer> issueIndexes = new ArrayList<>();
  public List<String> initialIssueIDs = new ArrayList<>();
  public List<String> initialRuleIDs = new ArrayList<>();
// selab: end

//sso : start


public static List<Integer> scn_id = new ArrayList();
public static List<String> scns = new ArrayList();
//public static List<String> candidates = new ArrayList();


public static String Candidate = "";
public static String ContextToAdd = "";
public static String scn_ruleId = "";
public static String scn_errorMessage = "";
public static String scn_redline = "";
public static String scn_component = "";

public static int start_offset = 0;
public static int end_offset = 0;

//sso : end

	private static Logger log = LoggerFactory.getLogger(TBarFixer.class);

	public TBarFixer(String path, String projectName, int bugId, String defects4jPath) {
		super(path, projectName, bugId, defects4jPath);
	}
	
	public TBarFixer(String path, String metric, String projectName, int bugId, String defects4jPath) {
		super(path, metric, projectName, bugId, defects4jPath);
	}

	@Override
	public void fixProcess() {
		// Read paths of the buggy project.
		if (!dp.validPaths) return;
		
		// Read suspicious positions.
		List<SuspiciousPosition> suspiciousCodeList = null;
		if (granularity == Granularity.Line) {
			// It assumes that the line-level bug positions are known.
			// selab: origin
			// suspiciousCodeList = readKnownBugPositionsFromFile();
			// selab: modified code
			suspiciousCodeList = readKnownBugPositionsFromFile(null);
			// selab: end
		} else if (granularity == Granularity.File) {
			// It assumes that the file-level bug positions are known.
			List<String> buggyFileList = readKnownFileLevelBugPositions();
			suspiciousCodeList = readSuspiciousCodeFromFile(buggyFileList);
		} else {
			suspiciousCodeList = readSuspiciousCodeFromFile();
		}
		
		if (suspiciousCodeList == null) return;
		
		List<SuspCodeNode> triedSuspNode = new ArrayList<>();
		log.info("=======TBar: Start to fix suspicious code======");
		for (SuspiciousPosition suspiciousCode : suspiciousCodeList) {
			List<SuspCodeNode> scns = parseSuspiciousCode(suspiciousCode);
			if (scns == null) continue;

			for (SuspCodeNode scn : scns) {
//				log.debug(scn.suspCodeStr);
				System.out.println(scn.suspCodeStr);
				if (triedSuspNode.contains(scn)) continue;
				triedSuspNode.add(scn);
				
				// Parse context information of the suspicious code.

				/* //sso : delete
				List<Integer> contextInfoList = readAllNodeTypes(scn.suspCodeAstNode);
				List<Integer> distinctContextInfo = new ArrayList<>();
				for (Integer contInfo : contextInfoList) {
					if (!distinctContextInfo.contains(contInfo) && !Checker.isBlock(contInfo)) {
						distinctContextInfo.add(contInfo);
					}
				}
				*/
//				List<Integer> distinctContextInfo = contextInfoList.stream().distinct().collect(Collectors.toList());
				
		        // Match fix templates for this suspicious code with its context information.
				//fixWithMatchedFixTemplates(scn, distinctContextInfo);

				// TODO: 0803
				//fixWithMatchedFixTemplates(scn,errorMessage);

				if (!isTestFixPatterns && minErrorTest == 0) break;
				if (this.patchId >= 10000) break;
			}
			if (!isTestFixPatterns && minErrorTest == 0) break;
			if (this.patchId >= 10000) break;
        }
		log.info("=======TBar: Finish off fixing======");
		
		FileHelper.deleteDirectory(Configuration.TEMP_FILES_PATH + this.dataType + "/" + this.buggyProject);
	}

  // selab: start
  public List<SuspiciousPosition> localizeDefects() {

	// FL-1. Create a project on SonarQube
	ShellUtils.shellCreateProject(this.buggyProject);
		
	// FL-2. Run the project with SonarScanner
	ShellUtils.shellRunSonarQube(this.fullBuggyProjectPath, this.buggyProject);

	ShellUtils.sleepWhileInProgress(this.buggyProject);

	// Set configuration for SonarQube
	ShellUtils.shellSetNewCodePeriod(this.buggyProject);

	// FL-3. Get suspicious statements from the results of the project on SonarQube DB
	ArrayList<SuspStmt> suspStmtList = null;
	suspStmtList = DBUtils.getSuspStmt(this.buggyProject, 1);

   

	for (SuspStmt suspStmt: suspStmtList) {
		initialIssueIDs.add(suspStmt.issueID);
		//initialRuleIDs.add(suspStmt.ruleID);
	}
    // FL-4. Load the suspicious statements list
    List<SuspiciousPosition> suspiciousCodeList = 
      readKnownBugPositionsFromFile(suspStmtList);

	//sso :Start
	Map<String, ArrayList<String>> mappingTable = processIssueDiff(suspStmtList);

	for (Map.Entry<String, ArrayList<String>> mT : mappingTable.entrySet()) {
		System.out.println(mT.getKey());
		System.out.println(mT.getValue().size());
	}

    return suspiciousCodeList;
  }


 public List<Patch> generatePatch_singleHunk(List<SuspiciousPosition> suspiciousCodeList) {

	String file_name = "/mnt/sda1/sohyun/ErrorMessage-driven-Template/Results/"+this.buggyProject+".txt";

	File file_final = new File(file_name);
	FileWriter fw_final = null;
	BufferedWriter writer_final = null;

	String ruleType = "";
	String shell_string = "";
	int countBlank = 0;
	
    candidatePatches.clear();
    if (suspiciousCodeList.isEmpty()) return candidatePatches;

    // PG-1. Extract Faulty AST Nodes as buggy context
    List<SuspCodeNode> triedSuspNode = new ArrayList<>();
	log.info("=======TBar: Start to fix suspicious code!!!!!!!!!!!WHAT======");
	for (int scIndex=0; scIndex < suspiciousCodeList.size(); scIndex++) {
		SuspiciousPosition suspiciousCode = suspiciousCodeList.get(scIndex);
		List<SuspCodeNode> scns = parseSuspiciousCode(suspiciousCode);
		String scn_ruleId_1 = "";
		String scn_errorMessage_1 = "";

		if (scns == null) {
		continue;
			}
		
		this.currentIssueIndexes = scIndex;

		for (int scnIndex=0; scnIndex < scns.size(); scnIndex++) {
			SuspCodeNode scn = scns.get(scnIndex);
			if (triedSuspNode.contains(scn)) continue;
			triedSuspNode.add(scn);

			shell_string = ShellUtils.shellGetRuleID(initialIssueIDs.get(scIndex));
			
			if(shell_string.contains("\"type\":\"")){
				String[] ruleType_1 = shell_string.split("\"type\":\"");
				String ruleType_2 = ruleType_1[1];
				String[] ruleType_3 = ruleType_2.split("\",\"scope\"");
				ruleType = ruleType_3[0];
				scn.ruleType = ruleType;
			}
		
			int abc_len = scn.suspCodeStr.split("\n").length;
	
			if(abc_len == 1){ 
			try {
			this.currentSCNIndex = scnIndex;
			if(getLineNum_1(shell_string) == 1){
				String flow_hunk = "";
				//"flows":[],"status":
				boolean isSingleHunk = false;

				if(shell_string.contains("\"flows\":")){
					String[] ruleType_1 = shell_string.split("\"flows\":");
					String ruleType_2 = ruleType_1[1];
					String[] ruleType_3 = ruleType_2.split(",\"status\"");
					flow_hunk = ruleType_3[0];
					Pattern pattern = Pattern.compile("\"startLine\":([0-9]*),\"endLine");
					Matcher matcher = pattern.matcher(flow_hunk);

					List<String> multi_hunk = new ArrayList<>();
					while(matcher.find()){
						multi_hunk.add(matcher.group());
						
					}
					if(multi_hunk.size() ==1 ){
						//"startLine":344,"endLine
						String hunk_line_1 = multi_hunk.get(0).replaceAll("[^\\d]", "");
						int hunk_line = Integer.parseInt(hunk_line_1);
						if(scn.buggyLine == hunk_line){
							isSingleHunk = true;
						} 
						}
					if(flow_hunk.strip().equals("[]")) {isSingleHunk = true;}
				} //if contains "flows"
				
				
				if(isSingleHunk){
					//System.out.println(shell_string);

				if(shell_string.contains("\"startOffset\":")){
					String[] start_a = shell_string.split("\"startOffset\":");
					String start_b = start_a[1];
					String[] start_c = start_b.split(",\"endOffset\":");
					String start_d = start_c[0];
					start_offset = Integer.parseInt(start_d);
					
				}

				if(shell_string.contains("\"endOffset\":")){
					String[] end_a = shell_string.split("\"endOffset\":");
					String end_b = end_a[1];
					String[] end_c = end_b.split("},\"flows");
					String end_d = end_c[0];
					end_offset = Integer.parseInt(end_d);
				}

				if(shell_string.contains("\"message\":\"")){
					String[] end_a = shell_string.split("\"message\":\"");
					String end_b = end_a[1];
					String[] end_c = end_b.split("\",\"effort\"");
					String end_d = end_c[0];
					scn_errorMessage = end_d;
					
				}

				if(shell_string.contains("\"rule\":\"java:S")){
					String[] end_a = shell_string.split("\"rule\":\"java:S");
					String end_b = end_a[1];
					String[] end_c = end_b.split("\",\"severity\"");
					String end_d = end_c[0];
					scn_ruleId = end_d;
				}

				if(shell_string.contains("\"type\":\"")){
					String[] ruleType_1 = shell_string.split("\"type\":\"");
					String ruleType_2 = ruleType_1[1];
					String[] ruleType_3 = ruleType_2.split("\",\"scope\"");
					String ruleType_final = ruleType_3[0];
					scn.ruleType = ruleType_final;
					//ruleType = Integer.parseInt(ruleType_4);
					}	

				String file_scn = ReadCodeLine(scn.targetJavaFile,scn.buggyLine).toString();

					
				int leftBlanksCount = 0;
			
				List<Character> charAt_list = new ArrayList<>();
				for (int i = 0; i < file_scn.length(); i++) {
					charAt_list.add(file_scn.charAt(i));
				}

				int scn_str_pos = 0;

				for(int m=0; m<scn.suspCodeStr.length(); m++){
					if(charAt_list.get(m) != ' '){
						if (charAt_list.get(m) != '\t') {
							scn_str_pos = m;
							break;
						}
					}
				}

				scn.scn_startPos = start_offset;
				scn.scn_endPos = end_offset;
				scn.ruleType = ruleType;
				scn_redline = file_scn.substring(start_offset, end_offset);
				start_offset = start_offset - scn_str_pos;
				end_offset = end_offset - scn_str_pos;

				scn.errorMessage = scn_errorMessage;
				scn.ruleId = scn_ruleId;
				scn.redLine = scn_redline;

				String abcdef = "python   /mnt/sda1/sohyun/ErrorMessage-driven-Template/EM_Template/FindGroup.py !=!=!=!=!=!="+scn.errorMessage+"!=!=!=!=!=!="+file_scn+"!=!=!=!=!=!= "+scn_redline+" !=!=!=!=!=!= "+start_offset+" !=!=!=!=!=!= "+end_offset+" !=!=!=!=!=!= "+scn.targetJavaFile+" !=!=!=!=!=!= "+scn.buggyLine;
				
				Candidate = "";
				ContextToAdd = "";
				int count_to = 0;
				Process p = Runtime.getRuntime().exec(abcdef);
				BufferedReader in = new BufferedReader(new InputStreamReader(
					p.getInputStream()));

				

				String line;  
				while ((line = in.readLine()) != null) {  
					if(count_to == 0){
						Candidate += line;					
						count_to += 1;
					}else{
						ContextToAdd += line;
					}
					
				}  

				Patch patch = new Patch();
				if(Candidate.equals("NOTHING")){
					patch.setFixedCodeStr1("");
				}
				else if(Candidate.equals("NOTHING_1")){
					patch.setFixedCodeStr1(scn.suspCodeStr);
				}
				else{
					patch.setFixedCodeStr1(Candidate);
					patch.setFixedContextStr(ContextToAdd);
				}

				patch.setScnIndex(scnIndex);
				suspiciousCodeNodes.add(scn);
				scnIndexes.add(this.currentIssueIndexes);
				issueIndexes.add(this.currentIssueIndexes);			
				candidatePatches.add(patch);
				
				if(!patch.getFixedContextStr().equals("")){
					//System.out.println(abcdef);
					System.out.println("suspicious code === "+ scn.suspCodeStr);
					System.out.println("Template ==="+patch.getFixedContextStr());
					System.out.println("Patch ==="+patch.getFixedCodeStr1());
				}


				//write text
				fw_final = new FileWriter(file_final, true);
				writer_final = new BufferedWriter(fw_final);
				writer_final.write("\n================\n");
				writer_final.write("\nErrorMessage===\n");
				//String decodedMessage = StringEscapeUtils.unescapeJava(scn.errorMessage);
				writer_final.write(scn.errorMessage);
				writer_final.write("\nScn===\n");
				writer_final.write(file_scn);
				writer_final.write("\nPattern===\n");
				writer_final.write(patch.getFixedContextStr());
				//writer_final.write("\nPatch===\n");
				//writer_final.write(patch.getFixedCodeStr1());

	
				writer_final.flush();	

			}
			}




			} catch (Exception e) {
			e.printStackTrace();
			//TODO: handle exception
			}

		
		} 
	}
		
	if (!isTestFixPatterns && minErrorTest == 0) break;
	if (this.patchId >= 10000) break;
	} //for

	return candidatePatches;

 }

private Map<String, ArrayList<String>> processIssueDiff(ArrayList<SuspStmt> suspStmtList) {
	Map<String, ArrayList<String>> mappingTable = new HashMap<String, ArrayList<String>>();
	ArrayList<String> news = new ArrayList<String>();
	ArrayList<String> olds = new ArrayList<String>();
	ArrayList<String> closed = new ArrayList<String>();

	// Classifying
	for (SuspStmt suspStmt: suspStmtList) {
		// If current issue(suspStmt) not in initial issueIDs
		if (!initialIssueIDs.contains(suspStmt.issueID)) {
			if (suspStmt.status.equals("OPEN")) {
				news.add(suspStmt.issueID);
			} else if (suspStmt.status.equals("CLOSED")) {

			} else {
				System.out.println("Not in initial Issue list AND !(OPEN or CLOSED), " + 
								   "status:" + suspStmt.status + 
								   ", issueID:" + suspStmt.issueID + 
								   ", ruleID:" + suspStmt.ruleID);
			}
		} else if (initialIssueIDs.contains(suspStmt.issueID)) {
			if (suspStmt.status.equals("OPEN")) {
				olds.add(suspStmt.issueID);
			} else if (suspStmt.status.equals("CLOSED")) {
				closed.add(suspStmt.issueID);
			} else {
				System.out.println("Not in initial Issue list AND !(OPEN or CLOSED), " + 
								   "status:" + suspStmt.status + 
								   ", issueID:" + suspStmt.issueID + 
								   ", ruleID:" + suspStmt.ruleID);
			}
		}
	}

	mappingTable.put("news", news);
	mappingTable.put("olds", olds);
	mappingTable.put("closed", closed);

	return mappingTable;
}
  // selab: end

	// selab: origin
	// private List<SuspiciousPosition> readKnownBugPositionsFromFile() {
	// selab: modified code
	private List<SuspiciousPosition> readKnownBugPositionsFromFile(ArrayList<SuspStmt> suspStmtList) {
	// selab: end
		List<SuspiciousPosition> suspiciousCodeList = new ArrayList<>();
		
		// selab: origin
		// String[] posArray = FileHelper.readFile(Configuration.knownBugPositions).split("\n");
		// selab: modified codeg
		ArrayList<String> posArray = new ArrayList<>();
		
		//sso : add
		ArrayList<String> posIssue = new ArrayList<>();
		for (SuspStmt suspStmt: suspStmtList) {
			posArray.add(this.buggyProject + "@" + suspStmt.suspCode);
			posIssue.add(suspStmt.issueID);
		}
		// selab: end
		Boolean isBuggyProject = null;
		//for (String pos : posArray) {
		for(int i =0; i<posArray.size(); i++){
			String pos = posArray.get(i);
			String posId = posIssue.get(i);
			if (isBuggyProject == null || isBuggyProject) {
				if (pos.startsWith(this.buggyProject + "@")) {
					isBuggyProject = true;
					
					String[] elements = pos.split("@");
	            	String[] lineStrArr = elements[2].split(",");
	            	String classPath = elements[1];

					if(classPath.equals("pom.xml")){continue;}

					// sso: modified
					// classPath = classPath.substring(shortSrcPath.length(), classPath.length() - 5);
					classPath = classPath.substring(0, classPath.length() - 5);

					
					//System.out.println("classPath new Version --> "+ classPath);
	            	for (String lineStr : lineStrArr) {
	    				if (lineStr.contains("-")) {
	    					String[] subPos = lineStr.split("-");
	    					for (int line = Integer.valueOf(subPos[0]), endLine = Integer.valueOf(subPos[1]); line <= endLine; line ++) {
	    						SuspiciousPosition sp = new SuspiciousPosition();
	    		            	sp.classPath = classPath;
	    		            	sp.lineNumber = line;
								sp.issueId = posId;
	    		            	suspiciousCodeList.add(sp);
	    					}
	    				} else {
	    					SuspiciousPosition sp = new SuspiciousPosition();
	    	            	sp.classPath = classPath;
	    	            	sp.lineNumber = Integer.valueOf(lineStr);
							sp.issueId = posId;
	    	            	suspiciousCodeList.add(sp);
	    				}
	    			}
				} else if (isBuggyProject!= null && isBuggyProject) isBuggyProject = false;
			} else if (!isBuggyProject) break;
		}
		return suspiciousCodeList;
	} 

	private List<String> readKnownFileLevelBugPositions() {
		List<String> buggyFileList = new ArrayList<>();
		
		String[] posArray = FileHelper.readFile(Configuration.knownBugPositions).split("\n");
		Boolean isBuggyProject = null;
		for (String pos : posArray) {
			if (isBuggyProject == null || isBuggyProject) {
				if (pos.startsWith(this.buggyProject + "@")) {
					isBuggyProject = true;
					
					String[] elements = pos.split("@");
	            	String classPath = elements[1];
	            	String shortSrcPath = dp.srcPath.substring(dp.srcPath.indexOf(this.buggyProject) + this.buggyProject.length() + 1);
	            	classPath = classPath.substring(shortSrcPath.length(), classPath.length() - 5).replace("/", ".");

	            	if (!buggyFileList.contains(classPath)) {
	            		buggyFileList.add(classPath);
	            	}
				} else if (isBuggyProject!= null && isBuggyProject) isBuggyProject = false;
			} else if (!isBuggyProject) break;
		}
		return buggyFileList;
	}
	
	public List<SuspiciousPosition> readSuspiciousCodeFromFile(List<String> buggyFileList) {
		File suspiciousFile = null;
		String suspiciousFilePath = "";
		if (this.suspCodePosFile == null) {
			suspiciousFilePath = Configuration.suspPositionsFilePath;
		} else {
			suspiciousFilePath = this.suspCodePosFile.getPath();
		}
		
		suspiciousFile = new File(suspiciousFilePath + "/" + this.buggyProject + "/" + this.metric + ".txt");
		if (!suspiciousFile.exists()) {
			System.out.println("Cannot find the suspicious code position file." + suspiciousFile.getPath());
			suspiciousFile = new File(suspiciousFilePath + "/" + this.buggyProject + "/" + this.metric.toLowerCase() + ".txt");
		}
		if (!suspiciousFile.exists()) {
			System.out.println("Cannot find the suspicious code position file." + suspiciousFile.getPath());
			suspiciousFile = new File(suspiciousFilePath + "/" + this.buggyProject + "/All.txt");
		}
		if (!suspiciousFile.exists()) return null;
		List<SuspiciousPosition> suspiciousCodeList = new ArrayList<>();
		try {
			FileReader fileReader = new FileReader(suspiciousFile);
            BufferedReader reader = new BufferedReader(fileReader);
            String line = null;
            while ((line = reader.readLine()) != null) {
            	String[] elements = line.split("@");
            	if (!buggyFileList.contains(elements[0])) continue;
            	SuspiciousPosition sp = new SuspiciousPosition();
            	sp.classPath = elements[0];
            	sp.lineNumber = Integer.valueOf(elements[1]);
            	suspiciousCodeList.add(sp);
            }
            reader.close();
            fileReader.close();
        }catch (Exception e){
        	e.printStackTrace();
        	log.debug("Reloading Localization Result...");
            return null;
        }
		if (suspiciousCodeList.isEmpty()) return null;
		return suspiciousCodeList;
	}


	public List<SuspiciousPosition> readSuspiciousCodeFromFile() {
		File suspiciousFile = null;
		String suspiciousFilePath = "";
		if (this.suspCodePosFile == null) {
			suspiciousFilePath = Configuration.suspPositionsFilePath;
		} else {
			suspiciousFilePath = this.suspCodePosFile.getPath();
		}
		suspiciousFile = new File(suspiciousFilePath + "/" + this.buggyProject + "/" + this.metric + ".txt");
		if (!suspiciousFile.exists()) {
			System.out.println("Cannot find the suspicious code position file." + suspiciousFile.getPath());
			suspiciousFile = new File(suspiciousFilePath + "/" + this.buggyProject + "/" + this.metric.toLowerCase() + ".txt");
		}
		if (!suspiciousFile.exists()) {
			System.out.println("Cannot find the suspicious code position file." + suspiciousFile.getPath());
			suspiciousFile = new File(suspiciousFilePath + "/" + this.buggyProject + "/All.txt");
		}
		if (!suspiciousFile.exists()) return null;
		List<SuspiciousPosition> suspiciousCodeList = new ArrayList<>();
		try {
			FileReader fileReader = new FileReader(suspiciousFile);
            BufferedReader reader = new BufferedReader(fileReader);
            String line = null;
            while ((line = reader.readLine()) != null) {
            	String[] elements = line.split("@");
            	SuspiciousPosition sp = new SuspiciousPosition();
            	sp.classPath = elements[0];
            	sp.lineNumber = Integer.valueOf(elements[1]);
            	suspiciousCodeList.add(sp);
            }
            reader.close();
            fileReader.close();
        }catch (Exception e){
        	e.printStackTrace();
        	log.debug("Reloading Localization Result...");
            return null;
        }
		if (suspiciousCodeList.isEmpty()) return null;
		return suspiciousCodeList;
	}

	

	
	protected void generateAndValidatePatches(FixTemplate ft, SuspCodeNode scn) {
		ft.setSuspiciousCodeStr(scn.suspCodeStr);
		ft.setSuspiciousCodeTree(scn.suspCodeAstNode);
		if (scn.javaBackup == null) ft.setSourceCodePath(dp.srcPath);
		else ft.setSourceCodePath(dp.srcPath, scn.javaBackup);

		ft.setDictionary(dic);
		ft.generatePatches();
		List<Patch> patchCandidates = ft.getPatches();
		if (patchCandidates.isEmpty()) return;
    // selab: origin
		// testGeneratedPatches(patchCandidates, scn);
    // selab: modified code
    candidatePatches.addAll(patchCandidates);
    for (Patch patch: patchCandidates) {
		suspiciousCodeNodes.add(scn);
		scnIndexes.add(this.currentSCNIndex);
		issueIndexes.add(this.currentIssueIndexes);
		candidatePatchPatterns.add(ft.getClass().getName());
    }
    // selab: end
	}
	
	public List<Integer> readAllNodeTypes(ITree suspCodeAstNode) {
		List<Integer> nodeTypes = new ArrayList<>();
		nodeTypes.add(suspCodeAstNode.getType());
		List<ITree> children = suspCodeAstNode.getChildren();
		for (ITree child : children) {
			int childType = child.getType();
			if (Checker.isFieldDeclaration(childType) || 
					Checker.isMethodDeclaration(childType) ||
					Checker.isTypeDeclaration(childType) ||
					Checker.isStatement(childType)) break;
			nodeTypes.addAll(readAllNodeTypes(child));
		}
		return nodeTypes;
	}

	public List<Integer> readAllMethodTypes(ITree suspCodeAstNode) {
		List<Integer> nodeTypes = new ArrayList<>();
		nodeTypes.add(suspCodeAstNode.getType());
		List<ITree> children = suspCodeAstNode.getChildren();
		for (ITree child : children) {
			int childType = child.getType();
			if (Checker.isMethodDeclaration(childType))break;
			nodeTypes.addAll(readAllMethodTypes(child));
		}
		return nodeTypes;
	}

	
	//sso : start
	public String ReadCodeLine(File file, int num){
		Path path = file.toPath();
		int line = num -1;
		String line32 = "";
		try(Stream<String> lines = Files.lines(path)){
			line32 = lines.skip(line).findFirst().get();
		}catch (Exception e) {
			//TODO: handle exception
		}
		return line32;
	}

	public static int getLineNum_1(String shell_string){
		int result = 0;
		int start_line = -1;
		int end_line = -2;
		// "startLine":
		if(shell_string.contains("\"startLine\":")){
			String[] ruleType_1 = shell_string.split("\"startLine\":");
			String ruleType_2 = ruleType_1[1];
			String[] ruleType_3 = ruleType_2.split(",\"endLine\"");
			start_line = Integer.parseInt(ruleType_3[0]);
			//ruleType = Integer.parseInt(ruleType_4);
		}

		if(shell_string.contains(",\"endLine\":")){
			String[] ruleType_1 = shell_string.split(",\"endLine\":");
			String ruleType_2 = ruleType_1[1];
			String[] ruleType_3 = ruleType_2.split(",\"startOffset\"");
			end_line = Integer.parseInt(ruleType_3[0]);
			//ruleType = Integer.parseInt(ruleType_4);
		}
		if(start_line == end_line){
			result = 1;
		} else{
			result = -1;
		}

		return result;

	}

	//sso : end

}
