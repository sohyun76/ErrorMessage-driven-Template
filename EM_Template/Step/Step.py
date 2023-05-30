import re
import sys
from sys import prefix
from Verbs.Remove2 import Remove
from Verbs.Change import Change
from Verbs.Make import Make
from Verbs.Use import Use
from Verbs.Cast import Cast
import pandas as pd
#from Verbs.Add import Add

def step1(cInput):
    cVerb = cInput[0]
    cErrorMessage  = cInput[1]
    cScn = cInput[2].strip()
    cQuotationExpression= cInput[3]
    cRedLine = cInput[4]
    cRedStart = cInput[5].strip()
    cRedEnd = cInput[6].strip()

    FixedStr = ""
    if cVerb == 'Remove':
        FixedStr = Remove(cErrorMessage,cScn,cQuotationExpression,cRedLine,cRedStart,cRedEnd)
    elif cVerb == 'Replace' or cVerb =='Convert' or cVerb == 'Change' or cVerb == 'Correct':
        FixedStr = Change(cErrorMessage,cScn,cQuotationExpression,cRedLine,cRedStart,cRedEnd)
    elif cVerb == 'Make' or cVerb == 'Reorder':
        FixedStr = Make(cErrorMessage,cScn,cQuotationExpression,cRedLine,cRedStart,cRedEnd)
    elif cVerb == 'Remove' and cErrorMessage.contains('Prefer'):
        FixedStr = Remove(cErrorMessage,cScn,cQuotationExpression,cRedLine,cRedStart,cRedEnd)
    elif cVerb == 'Use' and cVerb == 'Update':
        FixedStr = Use(cErrorMessage,cScn,cQuotationExpression,cRedLine,cRedStart,cRedEnd)
    elif cVerb == 'Cast':
        FixedStr = Cast(cErrorMessage,cScn,cQuotationExpression,cRedLine,cRedStart,cRedEnd)
    else:
        FixedStr = "NOTHING"
    return FixedStr

def step2(cInput):
    FixedStr = "THIS IS STEP 2."
    return FixedStr