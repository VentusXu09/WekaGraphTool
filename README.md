# WekaGraphTool

A simple Java tool to generate ROC, PRC and Cost Curve with .arff file created by Weka.

# Input Format
Please make sure the input .arff file contains all following attributes, or the tool cannot create target pictures.

@attribute 'True Positives' numeric   
@attribute 'False Negatives' numeric   
@attribute 'False Positives' numeric   
@attribute 'True Negatives' numeric   
@attribute 'False Positive Rate' numeric   
@attribute 'True Positive Rate' numeric   
@attribute Precision numeric   
@attribute Recall numeric   
@attribute Threshold numeric