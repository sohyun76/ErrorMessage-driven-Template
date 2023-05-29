#import spacy
import importlib
from collections import defaultdict
from difflib import SequenceMatcher
import pandas as pd
import re
import nltk
from sklearn.feature_extraction.text import CountVectorizer
from sklearn.metrics.pairwise import cosine_similarity

import sys
from sys import prefix
import pandas as pd
#import esprima
import os

#from Template.template import *
from Content.content import Content
from SooNLTK import *

from collections import defaultdict
from difflib import SequenceMatcher

def preprocessing_message(cErrorMessage):

    cErrorMessage = cErrorMessage.encode('utf-8').decode('unicode_escape')
    return cErrorMessage


def main(exampleOutput):
 
    exampleOutput = ' '.join(exampleOutput)
    scnInformation = exampleOutput.split('!=!=!=!=!=!=')
    contents = Content()
    contents.cErrorMessage = preprocessing_message(scnInformation[1].strip())
    contents.cScn = scnInformation[2]
    contents.cRedLine = scnInformation[3].strip()
    contents.cRedStart = scnInformation[4].strip()
    contents.cRedEnd = scnInformation[5].strip()
    contents.cFilePath = scnInformation[6].strip()
    contents.cScnLine = scnInformation[7].strip()
    cQuotationExpression = []


    cDoubleExpression = re.findall(r'"(.*?)"',contents.cErrorMessage)
    for cDouble in cDoubleExpression:
        cLen = len(cDouble)
        cDouble = cDouble[:cLen]
        cQuotationExpression.append(cDouble)

    cSingleExpression = re.findall(r"'(.*?)'",contents.cErrorMessage)
    for cDouble in cSingleExpression:
        cLen = len(cDouble)
        cDouble = cDouble[:cLen]
        cQuotationExpression.append(cDouble)

    contents.cQExpression = cQuotationExpression

    cVerb = contents.cErrorMessage.split(' ')[0]
    cRedCheck = contents.cScn[int(contents.cRedStart):int(contents.cRedEnd)]


    pos_tags = get_nltk_post(contents.cErrorMessage)
    Message_Structure = sso_pos_tag(pos_tags)
    
    module_name = Message_Structure[:-2]
    os.chdir("/mnt/sda1/sohyun/ErrorMessage_driven_Template/EM_Template/")
    #print("pwd --> ", os.getcwd())
    folder_name = "Template"
    file_list = os.listdir(folder_name)
    #print(file_list)
    existed_group = False

    
    for file_name in file_list:

        ratio_result = SequenceMatcher(None, file_name[:-3], module_name).ratio()
        #if file_name[:-3] == module_name:
        module_name_verb = module_name.split('-')[0]
        file_name_verb = file_name.split('-')[0]
        if module_name_verb == file_name_verb and ratio_result > 0.8:
            existed_group = True
            #print("module stasrt")
            #print(file_name[:-3])
            module = importlib.import_module(f"{folder_name}.{file_name[:-3]}")
            #print('THIS IS FILE_NAME IN FILE_LIST')
            print(module.fixing(contents))
            #return module.fixing(contents)
            #break
    if existed_group == False:
        print('NOTHING_1')
        #return 'NOTHING'

if __name__=="__main__":

    exampleOutput = sys.argv
    main(exampleOutput)

