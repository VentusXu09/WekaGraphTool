import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

import java.io.File;

public class Main {
    /**

     * @param args

     */

    public static void main(String[] args) {

        // TODO Auto-generated method stub

        Instances ins = null;

        Classifier cfs = null;

        try{
            /*
             * 1.读入训练、测试样本
             * 在此我们将训练样本和测试样本作为同一个样本
             */

            File file= new File("");

            ArffLoader loader = new ArffLoader();

            loader.setFile(file);

            ins = loader.getDataSet();



            //在使用样本之前一定要首先设置instances的classIndex，否则在使用instances对象是会抛出异常

            ins.setClassIndex(ins.numAttributes()-1);



            /*
             * 2.初始化分类器
             * 具体使用哪一种特定的分类器可以选择，请将特定分类器的class名称放入forName函数
             * 这样就构建了一个简单的分类器
             */

            cfs = (Classifier)Class.forName("weka.classifiers.bayes.NaiveBayes").newInstance();



            /*
             * 3.使用训练样本训练分类器
             */

            cfs.buildClassifier(ins);


            /*
             * 4.使用测试样本测试分类器的学习效果
             * 在这里我们使用的训练样本和测试样本是同一个，在实际的工作中需要读入一个特定的测试样本
             */

            Instance testInst;

            /*
             * Evaluation: Class for evaluating machine learning models
             * 即它是用于检测分类模型的类
             */

            Evaluation testingEvaluation = new Evaluation(ins);

            int length = ins.numInstances();

            for (int i =0; i < length; i++) {

                testInst = ins.instance(i);

                //通过这个方法来用每个测试样本测试分类器的效果

                testingEvaluation.evaluateModelOnceAndRecordPrediction(

                        cfs, testInst);

            }



            /*
             * 5.打印分类结果
             * 在这里我们打印了分类器的正确率
             * 其它的一些信息我们可以通过Evaluation对象的其它方法得到
             */
            System.out.println( "分类器的正确率：" + (1- testingEvaluation.errorRate()));

        }catch(Exception e){

            e.printStackTrace();

        }

    }
}
