package edu.lu.uni.serval.tbar.utils;

import java.util.ArrayList;
import edu.lu.uni.serval.tbar.dataprepare.SuspStmt;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;

public class DBUtils {
    private static String url = "url";
	private static String user = "sonar";
	private static String password = "password";
    private static String dbSonarqube = "sonarqube";
    private static String dbResults = "validation_results";
    private static String tableResults = "validation_result";


    public static String getFisrtAnalysisID(String projectID) {
		String query = "select ca.analysis_uuid " +
                        "from public.ce_activity ca " +
                        "where ca.main_component_uuid = " +
                        "(select c.uuid from public.components c where c.kee = \'" + projectID + "\')" +
                        "order by ca.executed_at desc " +
                        "limit 1;";
		System.out.println(query);
		String result = "";
		try (
			Connection con = DriverManager.getConnection(url + dbSonarqube, user, password);
			Statement st = con.createStatement();
			ResultSet rs = st.executeQuery(query);) {
			while (rs.next()) {
                result = rs.getString(1);
			}
			con.close();
        } catch (Exception ex) {
			System.out.println(ex);
        }
        
		return result;
	}

    public static String createNewTable() {
        String query = "CREATE TABLE public." + tableResults +" ( " +
                        "patchid varchar NOT NULL, " +
                        "issueid varchar NULL, " +
                        "diff text NULL, " +
                        "fix_pattern varchar NULL, " +
                        "new_issue varchar NULL, " +
                        "closed_issue varchar NULL, " +
                        "olds_issue varchar NULL, " +
                        "is_fixed bool NULL);";
        System.out.println(query);
        String result = "";
        try (
			Connection con = DriverManager.getConnection(url + dbResults, user, password);
			Statement st = con.createStatement();
			ResultSet rs = st.executeQuery(query);) {
			while (rs.next()) {
                result = rs.getString(1);
			}
			con.close();
        } catch (Exception ex) {
			System.out.println(ex);
        }
        
		return result;
    }

    public static int DeleteColumns(String projectID) {

        /*
         *     query = "select i.kee as issueID, i.status, i.rule_uuid, " + 
                                "concat(c.long_name, '@', i.line) as suspCode " +
                            "from public.components c, public.issues i " +
                            "where i.project_uuid = (select p.uuid from public.projects p where p.name = \'" + projectID +"\') " +
                            "and c.uuid = i.component_uuid ;";
         */

        String query = "delete from public.issues i "+
        "where i.project_uuid = (select p.uuid from public.projects p where p.name = \'" + projectID +"\') ";
                        
        System.out.println(query);
		String result = "";

        int affectedrows = 0;

        try (Connection con = DriverManager.getConnection(url + dbSonarqube, user, password);
                PreparedStatement pstmt = con.prepareStatement(query)) {

           // pstmt.setInt(1, id);

            affectedrows = pstmt.executeUpdate();

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return affectedrows;
        /* 
		try (
			Connection con = DriverManager.getConnection(url + dbSonarqube, user, password);
			Statement st = con.createStatement();
			st.executeUpdate(query);) {
            //ResultSet rs = st.executeQuery(query);
                //while (rs.next()) {
            //   result = rs.getString(1);
			//}
			con.close();
        } catch (Exception ex) {
			System.out.println(ex);
        }
        */
		//return result;
    }


    public static String insertValidationResult(String patchID, String issueID, String gitDiff, String fixPattern, String newsStr, String oldsStr, String closedStr, boolean isFixed) {
        String query = "insert into public." + tableResults + 
                       " values (" +
                       "\'" + patchID + "\'," +
                       "\'" + issueID + "\'," +
                       "\'" + gitDiff + "\'," +
                       "\'" + fixPattern + "\'," +
                       "\'" + newsStr + "\'," +
                       "\'" + oldsStr + "\'," +
                       "\'" + closedStr + "\'," +
                       "\'" + isFixed + "\'" +
                       ");";
        System.out.println(query);
        String result = "";
        try {
			Connection con = DriverManager.getConnection(url + dbResults, user, password);
			Statement st = con.createStatement();
			ResultSet rs = st.executeQuery(query);
			while (rs.next()) {
                result = rs.getString(1);
			}
			con.close();
        } catch (Exception ex) {
			System.out.println(ex);
        }
        
		return result;
    }

    public static ArrayList<SuspStmt> getSuspStmt(String projectID, int option) {
        String query = "";
        if (option == 1) { 
            query = "select i.kee as issueID, i.status, i.rule_uuid, " + 
                                "concat(c.long_name, '@', i.line) as suspCode " +
                            "from public.components c, public.issues i " +
                            "where i.project_uuid = (select p.uuid from public.projects p where p.name = \'" + projectID +"\') " +
                            "and c.uuid = i.component_uuid ;";
        } else if (option == 0) {
            query = "select i.kee as issueID, i.status, i.rule_uuid, " + 
                                "concat(c.long_name, '@', i.line) as suspCode " +
                            "from public.components c, public.issues i " +
                            "where i.project_uuid = (select p.uuid from public.projects p where p.name = \'" + projectID +"\') " +
                            "and c.uuid = i.component_uuid and (i.issue_type = 2 or i.rule_uuid in ('AX0iZchbtb71eKZI1qxx','AX0iZcgutb71eKZI1qsI','AX0iZchXtb71eKZI1qwT'));";
        }
		System.out.println(query);
		ArrayList<SuspStmt> results = new ArrayList<>();
		try (
			Connection con = DriverManager.getConnection(url + dbSonarqube, user, password);
			Statement st = con.createStatement();
			ResultSet rs = st.executeQuery(query);) {
            
			while (rs.next()) {
                String issueID = rs.getString(1);
                String status = rs.getString(2);
                String ruleID = rs.getString(3);
                String suspCode = rs.getString(4);
				SuspStmt tempSuspStmt = new SuspStmt(issueID,status,ruleID,suspCode);
				results.add(tempSuspStmt);
                
			}
			con.close();
        } catch (Exception ex) {
			System.out.println(ex);
        }
		return results;
	}
}