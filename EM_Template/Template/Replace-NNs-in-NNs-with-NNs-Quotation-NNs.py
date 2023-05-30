import sctokenizer
import re
import sctokenizer
from sctokenizer import JavaTokenizer
from sctokenizer import Source

#def fixing(cMessage, cScn, cQExpression, cRedLine,cRedStart,cRedEnd,cFilePath):
def fixing(contents):
    #print("GO TO THE Replace-NNs-in-NNs-woth-NNs-Quotation-NNs.py")
    cScn = contents.cScn
    cRedStart = int(contents.cRedStart)
    cRedEnd = int(contents.cRedEnd)

    #print('Replace-NNs-in-NNs-with-NNs-Quotation-NNs')
    #print(contents.cRedStart,"===",contents.cRedEnd)
    candidate = cScn[:cRedStart+2] + cScn[cRedEnd:]

    candidate += "\n"
    candidate += "Template 3"
    #candidate += "GO TO THE Remove-NNs-to-NNs-Quotation"
    #candidate += "!=!=!=!=!=!="
    #candidate += "HELLP"
    return candidate

#if __name__=='__main__':
  
#    fixing()