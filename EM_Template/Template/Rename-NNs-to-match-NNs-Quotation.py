import sctokenizer
import re
import sctokenizer
from sctokenizer import JavaTokenizer
from sctokenizer import Source

#def fixing(cMessage, cScn, cQExpression, cRedLine,cRedStart,cRedEnd,cFilePath):
def fixing(contents):
    #print('GO TO THE Rename-NNs-to-match-NNs-Quotation')
    cScn = contents.cScn
    cQExpression = contents.cQExpression
    cRedLine = contents.cRedLine
    cRedStart = int(contents.cRedStart)
    cRedEnd = int(contents.cRedEnd)
    cFilePath = contents.cFilePath

    scn_type = ''
        #scn_regex = ''
    scn_str = ''

    if len(cQExpression) == 1:
        scn_split = list(cRedLine)
        scn_str = cRedLine
        regex_expression = cQExpression[0]
    else:
        scn_split = list(cQExpression[0])
        scn_str = cQExpression[0]
        regex_expression = cQExpression[1]
        
    #print('scn_split -> ',scn_split)

    # regex 

    # case 1 : ^[a-z][a-zA-Z0-9]*$
    if regex_expression == '^[a-z][a-zA-Z0-9]*$':
        match = re.match("^[a-z][a-zA-Z0-9]*$", scn_str)
        if not match:
            if '_' in scn_str:
                scn_str =  ''.join([w.capitalize() for w in scn_str.split('_')]).replace(scn_str[0], scn_str[0].lower(), 1)
                scn_str = scn_str.replace(scn_str[0], scn_str[0].lower())
            else:
                scn_str = scn_str.replace(scn_str[0], scn_str[0].lower())
        

    # case 2 : ^[A-Z][0-9]?$
    if regex_expression == '^[A-Z][0-9]?$':
        pattern = r"^[A-Z][0-9]?$"
        # Find the part of the string that matches the pattern
        match = re.search(pattern, scn_str)

        # If there isn;t a match, extract the first letter of the match
        if not match:
            if re.match("[0-9]",scn_str[1]) is not None:
                scn_str = scn_str[:2]
            else:
                scn_str = scn_str[0]
        else:
            scn_str = scn_str



    # case 3 : ^[A-Z][A-Z0-9](_[A-Z0-9]+)$
    if regex_expression == '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$':
        scn_str = ''.join(['_' + i if i.isupper() else i.upper() for i in scn_str])

    

    ## CHECK OUTPUT
    #print(scn_str)


    #"""
    # check
    #check_regex = 'r\n'+regex_expression+'\n'
    #cde = re.match(check_regex,scn_regex)
    cde = re.match(regex_expression,scn_str)

    #print("cde -> ",cde)

    """
    # else 에서 필요한 변수들
    cnt = 0
    renamed_tokens_column = []
    renamed_tokens_line = []



    # 생성한 변수가 올바르지 않다.
    if cde is None:
        nothing = regex_expression + '==='+scn_str+'=== nothing'
        #print('not match')
        # TODO : break 주석 풀기.
        return nothing
        #break



    else:
        # 해당 변수를 갖고 있는 다른 라인에서도 변수명 변경해주기.



        # scn tokenize
        tokens = sctokenizer.tokenize_str(cScn)
        for token in tokens:
            if str(token.token_value) == cRedLine:
                scn_type = token.token_type 

        
        

        # Tokenize cFilePath
        src = Source.from_file(cFilePath, lang='java')
        tokens = src.tokenize()
        for token in tokens:
            if token.token_value == cRedLine:
                #print(token.token_value) #print(token.token_type)
                if token.token_type == scn_type:
                    cnt += 1 
                    token.token_value = cRedLine
                    renamed_tokens_column.append(token.column)
                    renamed_tokens_line.append(token.line)


    with open(cFilePath,'r+') as f:
        lines = []
        get_all = f.readlines()
        for i,line in enumerate(get_all,1):
            if i in renamed_tokens_line:
                index_ = renamed_tokens_line.index(i)
                check_index = renamed_tokens_column[index_]-1
                #new_line = line[:check_index] + scn_regex + line[check_index+len(cRedLine):]
                new_line = line[:check_index] + scn_str + line[check_index+len(cRedLine):]
                lines = lines + [new_line]
            else:
                lines = lines + [line]
        f.seek(0)
        f.writelines(lines)
        f.truncate()
    
    """
    #print(scn_str)
    #print('==NEXT==')
    candidate = cScn[:cRedStart+1] + scn_str + cScn[cRedEnd+1:]

    candidate += "\n"
    candidate += "Template 1"
    #candidate += "Rename!=!=!=!=!=!="
    #candidate += cRedLine
    #candidate += "!=!=!=!=!=!="
    #candidate += scn_str

    return candidate
