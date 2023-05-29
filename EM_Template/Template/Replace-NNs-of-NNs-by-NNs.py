import sctokenizer
import re
import sctokenizer
from sctokenizer import JavaTokenizer
from sctokenizer import Source
import javalang
import sys
sys.path.append('/mnt/sda1/sohyun/REPEM')

#from Content.content import Content




           

#def fixing(cMessage, cScn, cQExpression, cRedLine,cRedStart,cRedEnd,cFilePath):
def fixing(contents):

    cFilePath = contents.cFilePath
    cScnLine = contents.cScnLine
    cScn = contents.cScn
    cRedEnd = int(contents.cRedEnd)
    cRedLine = contents.cRedLine
    #print("cRedENd -> "+ cScn[cRedEnd:] )

    imsi_candidate = ''

    if cScn[cRedEnd+1:].startswith('.print'):
        if 'println' in cScn:
            imsi_candidate = cScn.replace(cRedLine+'.println','logger.log')
            #print(imsi_candidate)
        
        else:
            imsi_candidate = cScn.replace(cRedLine+'.print','logger.log')
            #print(imsi_candidate)
 
    
    #class_name_scn = find_containing_class(cFilePath, cScnLine)

    #candidate = ""

    else:
        imsi_candidate = ''



    #print('GO TO THE Replace-NNs-of-NNs-by-NNs')
    
    #AddToImport = "import java.util.logging.Logger;"
    #AddToDeclaration = "private static final Logger logger = Logger.getLogger("+class_name_scn+".class.getName());"



    candidate = imsi_candidate
    candidate += "\n"
    candidate += "Template 2"
    #candidate += "Replace!=!=!=!=!=!="
    #candidate += AddToImport
    #candidate += "!=!=!=!=!=!="
    #candidate += AddToDeclaration
    return candidate
    ## import문 Rename.py 참고해서 추가하기..


    # IF System.err OR System.out --> multi-line 제외.


   




"""
if __name__=='__main__':

    #my_content = Content()
    cMessage = 'Replace this use of System.out or System.err by a logger.'
    cScn = 'System.out.println(path);'
    cQExpression = []
    cRedLine =  'System.out'
    cRedStart = 0
    cRedEnd = 10
    cScnLine = 12
    cFilePath = '/mnt/sda1/sohyun/TBar_TSQ_555_Check/D4J/projects/Example_1/src/App.java'
    #print(cScn[cRedStart:cRedEnd])
    #print(my_content)
    #print(my_content.cMessage)
    print(fixing(cMessage, cScn, cQExpression, cRedLine,cRedStart,cRedEnd,cFilePath))
"""
