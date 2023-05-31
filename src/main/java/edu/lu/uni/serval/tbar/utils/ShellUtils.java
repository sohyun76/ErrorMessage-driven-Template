package edu.lu.uni.serval.tbar.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import edu.lu.uni.serval.tbar.config.Configuration;
// selab: Add import
import edu.lu.uni.serval.tbar.utils.DBUtils;
// selab: end

public class ShellUtils {
    // selab: start
   
    ///* 
    private static String snqbToken = "token";
    private static String snqbIp = "localhost";
    private static String snqbPort = "port";
    private static String snqbUrl = "http://" + snqbIp + ":" + snqbPort;
    private static String SnScanner = "sonar-scanner";
    //*/

    
    public static String shellCreateProject(String buggyProject) {
        String cmd = "curl -u " + snqbToken + ": -X POST " +
                    snqbUrl + "/api/projects/create" +
                    "?name=" + buggyProject +
                    "&project=" + buggyProject;
        return shellRunCustom(cmd, null); 
    }

    public static String shellDeleteProject(String buggyProject){
        String cmd = "curl -u " + snqbToken + ": -X POST " +
                    snqbUrl + "/api/projects/delete" +
                    "?name=" + buggyProject +
                    "&project=" + buggyProject;
            return shellRunCustom(cmd, null); 
    }
    public static String shellGetRuleID(String issue) {

        String cmd = "curl -u " + snqbToken + ": -X GET " +
        snqbUrl + "/api/issues/search" +
        "?issues=" + issue +
        "&rules";

        return shellRunCustom(cmd, null); 

    }
    public static String shellRunSonarQube(String buggyProjectDir, String buggyProject) {
        String cmd = SnScanner +
                    " -Dsonar.projectKey=" + buggyProject +
                    " -Dsonar.sources=" + buggyProjectDir +
                    " -Dsonar.host.url=" + snqbUrl +
                    " -Dsonar.login=" + snqbToken +
                    " -Dsonar.java.binaries=.";
        return shellRunCustom(cmd, buggyProjectDir);
    }

//    public static String shellRunSonarQube_1(String buggyProjectDir, String projectPath,String buggyProject) {
//        String exclusionStrings = "file:"+buggyProjectDir + "/**/*";
//        System.out.println("SHELL RUN SONARQUBE_1 => "+exclusionStrings);
//        System.out.println("SHELL RUN SONARQUBE_1 => "+projectPath);
//        //log.debug("SHELL RUN SONARQUBE_1 => "+exclusionStrings);
//       String newProject = "Thur_555";
//        String cmd = SnScanner +
//                    " -Dsonar.projectKey=" + newProject +
//                   // " -Dsonar.projectKey=" + buggyProject +
//                    " -Dsonar.inclusions=" + projectPath +
////////                    " -Dsonar.exclusions=" + exclusionStrings +
//                    " -Dsonar.host.url=" + snqbUrl +
//                    " -Dsonar.login=" + snqbToken +
//                   " -Dsonar.java.binaries=.";
//        return shellRunCustom(cmd, buggyProjectDir);
//    }



    public static boolean sleepWhileInProgress(String buggyProject) {
        String cmd = "curl -u " + snqbToken + ": -X GET " +
                    snqbUrl + "/api/ce/activity_status";
        String result = shellRunCustom(cmd, null);
        result = result.substring(1, result.indexOf("}"));
        int inProgressResult = -1;
        int pendingResult = -1;
        for (String str: result.split(",")) {
            if (str.contains("inProgress")) {
                inProgressResult = Integer.parseInt(str.split(":")[1]);
            }
            if (str.contains("pending")) {
                pendingResult = Integer.parseInt(str.split(":")[1]);
            }
        }
        if (inProgressResult == 0 && pendingResult == 0) {
            return false;
        } else {
            try {
				Thread.sleep(500);
			} catch(Exception e) {
				System.out.println("time exception!");
			}
            return sleepWhileInProgress(buggyProject);
        }
    }

    public static String shellSetNewCodePeriod(String buggyProject) {
        String analysisID = DBUtils.getFisrtAnalysisID(buggyProject);
        String cmd = "curl -u " + snqbToken + ": -X POST " +
                    snqbUrl + "/api/new_code_periods/set" +
                    "?type=SPECIFIC_ANALYSIS" +
                    "&project=" + buggyProject +
                    "&value=" + analysisID;
        return shellRunCustom(cmd, null); 
    }

    public static String shellGitReset(String buggyProjectDir) {
        String cmd = "git clean -fd";
        System.out.println(shellRunCustom(cmd, buggyProjectDir));
        
        cmd = "git reset --hard";
        System.out.println(shellRunCustom(cmd, buggyProjectDir));
        return "";
    }

    public static String shellGitDiff(String buggyProjectDir) {
        String cmd = "git diff --no-prefix .";
        return shellRunCustom(cmd, buggyProjectDir);
    }


        //sso : start
        public static String shellFindFile(String buggyProjectDir, String filePath) {
            //String cmd = "find . -name \"*" + filePath + "\"";
            String cmd = "find -name " + filePath + ".java";
            return shellRunCustom(cmd, buggyProjectDir);
        }

        public static String shellRunCmd(String buggyProjectDir, String cmd){
            return shellRunCustom(cmd, buggyProjectDir);
            }
         
        //sso : end

        
    public static String shellRunCustom(String cmd, String cwd) {
        String results = "";
        try {
            Process process = null;
            if (cwd == null) {
                process = Runtime.getRuntime().exec(cmd);
            } else {
                process = Runtime.getRuntime().exec(cmd, null, new File(cwd));
            }
            results = ShellUtils.getShellOut(process, 2);
        } catch (Exception e) {
            System.out.println("Exception!!!\n" + e);
        }
        return results;
    }
    // selab: end

	public static String shellRun(List<String> asList, String buggyProject, int type) throws IOException {
		String fileName;
        String cmd;
        if (System.getProperty("os.name").toLowerCase().startsWith("win")){
            fileName = Configuration.TEMP_FILES_PATH + buggyProject + ".bat";
            cmd = Configuration.TEMP_FILES_PATH + buggyProject + ".bat";
        }
        else {
            fileName = Configuration.TEMP_FILES_PATH + buggyProject + ".sh";
            cmd = "bash " + fileName;
        }
        File batFile = new File(fileName);
        if (!batFile.exists()){
        	if (!batFile.getParentFile().exists()) {
        		batFile.getParentFile().mkdirs();
        	}
            boolean result = batFile.createNewFile();
            if (!result){
                throw new IOException("Cannot Create bat file:" + fileName);
            }
        }
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(batFile);
            for (String arg: asList){
                outputStream.write(arg.getBytes());
            }
        } catch (IOException e){
            if (outputStream != null){
                outputStream.close();
            }
        }
        batFile.deleteOnExit();
        
        Process process= Runtime.getRuntime().exec(cmd);
        String results = ShellUtils.getShellOut(process, type);
        batFile.delete();
        return results;
	}

	private static String getShellOut(Process process, int type) {
		ExecutorService service = Executors.newSingleThreadExecutor();
        Future<String> future = service.submit(new ReadShellProcess(process));
        String returnString = "";
        try {
            if (type == 2)
            	returnString = future.get(Configuration.TEST_SHELL_RUN_TIMEOUT, TimeUnit.SECONDS);
            else 
            	returnString = future.get(Configuration.SHELL_RUN_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e){
            future.cancel(true);
//            e.printStackTrace();
            shutdownProcess(service, process);
            return "";
        } catch (TimeoutException e){
            future.cancel(true);
//            e.printStackTrace();
            shutdownProcess(service, process);
            return "";
        } catch (ExecutionException e){
            future.cancel(true);
//            e.printStackTrace();
            shutdownProcess(service, process);
            return "";
        } finally {
            shutdownProcess(service, process);
        }
        return returnString;
	}

	private static void shutdownProcess(ExecutorService service, Process process) {
		service.shutdownNow();
        try {
			process.getErrorStream().close();
			process.getInputStream().close();
	        process.getOutputStream().close();
		} catch (IOException e) {
			e.printStackTrace();
		}
        process.destroy();
	}
}

class ReadShellProcess implements Callable<String> {
    public Process process;

    public ReadShellProcess(Process p) {
        this.process = p;
    }

    public synchronized String call() {
        StringBuilder sb = new StringBuilder();
        BufferedInputStream in = null;
        BufferedReader br = null;
        try {
            String s;
            in = new BufferedInputStream(process.getInputStream());
            br = new BufferedReader(new InputStreamReader(in));
            while ((s = br.readLine()) != null && s.length()!=0) {
                if (sb.length() < 1000000){
                    if (Thread.interrupted()){
                        return sb.toString();
                    }
                    sb.append(System.getProperty("line.separator"));
                    sb.append(s);
                }
            }
            in = new BufferedInputStream(process.getErrorStream());
            br = new BufferedReader(new InputStreamReader(in));
            while ((s = br.readLine()) != null && s.length()!=0) {
                if (Thread.interrupted()){
                    return sb.toString();
                }
                if (sb.length() < 1000000){
                    sb.append(System.getProperty("line.separator"));
                    sb.append(s);
                }
            }
        } catch (IOException e){
//            e.printStackTrace();
        } finally {
            if (br != null){
                try {
                    br.close();
                } catch (IOException e){
                }
            }
            if (in != null){
                try {
                    in.close();
                } catch (IOException e){
                }
            }
            process.destroy();
        }
        FileHelper.outputToFile("logs/compile_log.log", sb, true);
        return sb.toString();
    }
}