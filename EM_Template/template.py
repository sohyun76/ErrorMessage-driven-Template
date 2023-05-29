#from Template.Rename-NNs-to-match-NNs-Quotation
import importlib

def extract_template(Message_Structure):
    folder_name = "Template"  # 모듈이 있는 폴더 이름

    # Template 1
    if Message_Structure == 'Rename-NNs-to-match-NNs-Quotation-.':
        module_name = Message_Structure[:-2]
        module = importlib.import_module(f"{folder_name}.{module_name}")
        module.fixing()

    # Template 2
    if Message_Structure == 'Replace-NNs-of-NNs-by-NNs-.':
        #
        print('no')
    # Template 3 
    if Message_Structure == 'Replace-NNs-in-NNs-with-NNs-Quotation-NNs-.':
        # 
        print('no')
    # Template 4
    if Message_Structure == 'Remove-NNs-to-NNs-Quotation-.':
        # 
        print('no')
    # Template 5
    if Message_Structure == 'Reorder-NNs-to-comply-with-NNs-.':
        #
        print('no')

    # Else
    else:
        return 'nothing' 
    return

if __name__=="__main__":
    name = 'Rename-NNs-to-match-NNs-Quotation-.'
    extract_template(name)