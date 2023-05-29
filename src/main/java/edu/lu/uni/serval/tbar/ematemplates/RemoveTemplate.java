package edu.lu.uni.serval.tbar.ematemplates;

import java.io.File;
import java.util.List;
import edu.lu.uni.serval.AST.ASTGenerator;
import edu.lu.uni.serval.AST.ASTGenerator.TokenType;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.utils.Checker;
import edu.lu.uni.serval.tbar.utils.SuspiciousCodeParser;

// extends EMATemplate -> extract quotations / ..
public class RemoveTemplate {
    public static void generatePatches(String errorMessage, String cScn, String redlocus,String filePath, int buggyLine){
        

        SuspiciousCodeParser scp = new SuspiciousCodeParser();
		ITree rootTree = scp.findTargetNodeLine(new File(filePath), buggyLine);
        System.out.println(rootTree.toTreeString());

        List<ITree> children = rootTree.getChildren();
        for (ITree iTree : children) {
            System.out.println(iTree.toTreeString());
            
        }

        //ITree suspCodeTree = this.getSuspiciousCodeTree();
        //ITree rootTree = new ASTGenerator().generateTreeForCodeFragment(cScn, TokenType.EXP_JDT);
        //System.out.println(rootTree.toTreeString());

        
        return;
    }
}
