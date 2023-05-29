from collections import defaultdict
from difflib import SequenceMatcher
import pandas as pd
import re
import nltk
from sklearn.feature_extraction.text import CountVectorizer
from sklearn.metrics.pairwise import cosine_similarity

def expand_contractions(text):
    # 축약어 확장을 위한 딕셔너리
    contractions_dict = {
        "ain't": "am not",
        "aren't": "are not",
        "can't": "can not",
        "can't've": "can not have",
        "'cause": "because",
        "could've": "could have",
        "couldn't": "could not",
        "couldn't've": "could not have",
        "didn't": "did not",
        "doesn't": "does not",
        "don't": "do not",
        "hadn't": "had not",
        "hadn't've": "had not have",
        "hasn't": "has not",
        "haven't": "have not",
        "he'd": "he would",
        "he'd've": "he would have",
        "he'll": "he will",
        "he's": "he is",
        "how'd": "how did",
        "how'll": "how will",
        "how's": "how is",
        "i'd": "I would",
        "i'll": "I will",
        "i'm": "I am",
        "i've": "I have",
        "isn't": "is not",
        "it'd": "it would",
        "it'll": "it will",
        "it's": "it is",
        "let's": "let us",
        "ma'am": "madam",
        "mayn't": "may not",
        "might've": "might have",
        "mightn't": "might not",
        "must've": "must have",
        "mustn't": "must not",
        "needn't": "need not",
        "oughtn't": "ought not",
        "shan't": "shall not",
        "sha'n't": "shall not",
        "she'd": "she would",
        "she'll": "she will",
        "she's": "she is",
        "should've": "should have",
        "shouldn't": "should not",
        "that'd": "that would",
        "that's": "that is",
        "there'd": "there had",
        "there's": "there is",
        "they'd": "they would",
        "they'll": "they will",
        "they're": "they are",
        "they've": "they have",
        "wasn't": "was not",
        "we'd": "we would",
        "we'll": "we will",
        "we're": "we are",
        "we've": "we have",
        "weren't": "were not",
        "what'll": "what will",
        "what're": "what are",
        "what's": "what is",
        "what've": "what have",
        "where'd": "where did",
        "where's": "where is",
        "who'll": "who will",
        "who's": "who is",
        "won't": "will not",
        "would've": "would have",
        "wouldn't": "would not",
        "you'd": "you would",
        "you'll": "you will",
        "you're": "you are",
        "you've": "you have"
    }
     # 정규표현식을 이용한 축약어 확장
    contractions_re = re.compile('(%s)' % '|'.join(contractions_dict.keys()))   

     #def replace(match):
     #   return contractions_dict[match.group(0)]
    
    def replace(match):
        return contractions_dict[match.group(0)]

    return contractions_re.sub(replace, text)  



def get_nltk_post(sentence):
    sentences_tagging = []

    verb_tobe_registered = ['Add', 'Call', 'Cast', 'Catch', 'Change', 'Combine', 'Complete', 'Convert', 'Correct', 'Declare', 'Define', 'Enable' ,'End', 'Extract', 'Fix', 'Format', 'Implement', 'Invoke', 'Iterate', 'Lower', 'Make', 'Merge', 'Move', 'Override', 'Prefix', 'Provide', 'Reduce', 'Refactor', 'Remove', 'Rename', 'Reorder', 'Replace', 'Return', 'Simplify', 'Take', 'Update', 'Use', 'Verify']
    #for sentence in data.loc[:,'message']:

    # can't 같은 축약어 풀기
    sentence = expand_contractions(sentence)

    # 큰따옴표 또는 작은따옴표는 PRJ_TAG로 정의하기.
    PRJ_TAG = 'PRJ'
    #sentence = re.sub(r'"(\w+)"', r'\1/' + PRJ_TAG.upper(), sentence) #큰따옴표 
    #sentence = re.sub(r"'(\w+)'", r'\1/' + PRJ_TAG.upper(), sentence) #작은따옴표 

    #pattern = r'(\'|\")(.*?)(\'|\")'
    #replacement = r'\1Quotation\3'
    #result = re.sub(pattern, replacement, sentence)
    
    words_in_quotes = re.findall(r'["\'](.*?)["\']', sentence)
    #print(words_in_quotes)
    
    pattern = r'(\'|\")(.*?)(\'|\")'
    replacement = r'\1Quotation\3'
    sentence = re.sub(pattern, replacement, sentence)

    #print('===')
    #print(sentence)


    # 정규표현식을 사용해서 마침표로 이어진 단어를 하나의 단어로 치환합니다.
    sentence = re.sub(r'\b([A-Za-z]+\.)+([A-Za-z]+)\b', lambda match: match.group(0).replace('.', '_'), sentence)
    
    # 문장에서 등록된 단어들을 동사로 인식하도록 태깅합니다.
    #for verb in verb_tobe_registered:
    #    sentence = sentence.replace(verb, verb.lower(), 1)


    # 동사로 등록된 단어들을 태깅합니다.
    #verb_tobe_registered = [verb.lower() for verb in verb_tobe_registered]

    # 문장에서 첫 단어를 동사로 인식하도록 합니다.
    #irst_word = sentence.split()[0].lower()
    #if first_word not in verb_tobe_registered:
    #    sentence = ' '.join(['VB' if word == first_word else word for word in sentence.split()])
    
    # 문장을 토큰화합니다.
    tokens = nltk.word_tokenize(sentence)

    # 토큰에서 "_"을 다시 "."으로 바꿉니다.
    tokens = [token.replace('_', '.') for token in tokens]

    # POS 태깅을 수행합니다.
    pos_tags = nltk.pos_tag(tokens)
    
    
    #for word, tag in tagger.tag(sent):
    new_tagged = []
    for word,tag in pos_tags:   
        if 'Quotation' in word:
            #imsi_word = words_in_quotes.pop(0)
            #new_tagged.append((imsi_word, 'Quo'))
            #new_tagged.append((word[1:], 'Quo'))
            new_tagged.append(('Quotation', 'Quo'))
        else:
            if word in verb_tobe_registered:
                new_tagged.append((word,'VB'))
            else:
                new_tagged.append((word,tag))
        


    #sentences_tagging.append(pos_tags)
    #sentences_tagging.append(new_tagged)
    return new_tagged

def sso_pos_tag(tags):
    # 필터링할 품사 리스트 (동사, 전치사)
    filter_tags = ["VB", "IN","TO","Quo",".",":"]

    # 품사 태그에서 필터링할 품사를 제외한 태그들은 모두 NNs로 변경
    new_text_list = []
    for word,tag in tags:
        if tag in filter_tags:
            new_text_list.append(word)
        else:
            if word == '"' or word == "'":
                continue
            if len(new_text_list) > 0 and new_text_list[-1] == 'NNs':
                continue
            else:
                new_text_list.append('NNs')
    # 새로운 품사 태그로부터 문장 생성
    #new_text = "-".join([token for token, tag in zip(tokens, new_tags) if tag != ""])
    new_text = "-".join(new_text_list)
    return new_text

def preprocessing_message(cErrorMessage):

    cErrorMessage = cErrorMessage.encode('utf-8').decode('unicode_escape')
    return cErrorMessage
"""
if __name__=="__main__":
    Message = ' Rename this field \\"JTextArea_int\\" to match the regular expression \\u0027^[a-z][a-zA-Z0-9]*$\\u0027. '
    Message = preprocessing_message(Message)
    print(Message)
    pos_tags = get_nltk_post(Message)
    print(pos_tags)
    Message_Structure = sso_pos_tag(pos_tags)
    print('=============')
    print(Message_Structure)
"""

