import model.CostCurve;
import model.WekaConstants;
import org.jfree.chart.*;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.core.Attribute;
import weka.core.Instances;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


public class Main {
    public static void main(String[] args) {
        Myframe frame = new Myframe();
    }

}

class Myframe extends JFrame implements ActionListener {
    private JTextArea ta;
    private JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
    private JButton bOpen, bGenerate;
    private JScrollPane ps;
    private File[] selectedFiles;

    public Myframe() {
        jfc.setMultiSelectionEnabled(true);//支持多选
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);//只支持文件
        jfc.setDialogTitle("选取一至多个文件(按住control)");
        jfc.setAcceptAllFileFilterUsed(false);
        //限制文件只能选择arff格式
        FileNameExtensionFilter filter = new FileNameExtensionFilter("arff", "arff");
        jfc.addChoosableFileFilter(filter);
        ta = new JTextArea(10, 35);
        ta.setEditable(false);
        ps = new JScrollPane(ta);
        bOpen = new JButton("选择文件");
        bGenerate = new JButton("生成图片");
        bOpen.addActionListener(this);
        bGenerate.addActionListener(this);
        this.add(bOpen);
        this.add(bGenerate);
        this.add(ps);
        this.setTitle("weka图片生成存储器");
        this.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 10));
        this.setSize(500, 500);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);


    }

    @Override
    public void actionPerformed(ActionEvent e) {

        JButton jbt = (JButton) e.getSource();
        //1.点击bOpen要做的事为
        if (jbt == bOpen) {
            //打开文件选择器对话框
            int status = jfc.showOpenDialog(this);
            //没有选打开按钮结果提示
            if (status != JFileChooser.APPROVE_OPTION) {
                //
            } else {

                selectedFiles = jfc.getSelectedFiles();
                System.out.println("已选中文件\n");
                Arrays.asList(selectedFiles).forEach(x -> {
                    String name = x.getName();
                    System.out.println(name);
                    ta.append(name);
                    ta.append("\n");
                });
            }

        } else if (jbt == bGenerate) {//单击生成图片按钮
            readArffDataAndGenerate();
        }
    }

    public void readArffDataAndGenerate() {
        DefaultXYDataset ROCxydataSet = new DefaultXYDataset();
        DefaultXYDataset PRCxydataSet = new DefaultXYDataset();
        DefaultXYDataset CCxyDataSet = new DefaultXYDataset();
        int index = 0;

        for (File x : selectedFiles) {
            String filePath = x.getAbsolutePath();
            try {
                Instances data = new Instances(
                        new BufferedReader(
                                new FileReader(filePath)));
                data.setClassIndex(data.numAttributes() - 1);
                System.out.println("\nNew Data is coming");
                int tpIndex = 0;
                int fpIndex = 0;
                int precisionIndex=0;
                int recallIndex=0;
                for (int i = 0; i < data.numAttributes() - 1; i++) {
                    Attribute att = data.attribute(i);
                    String name = att.name();
                    switch (name) {
                        case WekaConstants.TRUE_POSITIVES: {
                            tpIndex = i;
                            break;
                        }
                        case WekaConstants.FALSE_POSITIVES: {
                            fpIndex = i;
                            break;
                        }
                        case WekaConstants.PRECISION: {
                            precisionIndex = i;
                            break;
                        }
                        case WekaConstants.RECALL: {
                            recallIndex = i;
                            break;
                        }
                        default:
                            break;
                    }

                }
                ArrayList<Double> tpList = new ArrayList<Double>();
                ArrayList<Double> fpList = new ArrayList<Double>();
                ArrayList<Double> preList = new ArrayList<Double>();
                ArrayList<Double> reList = new ArrayList<Double>();
                for (int i = 0; i < data.numInstances(); i++) {//循环输出第i个样本
                    tpList.add(data.instance(i).value(tpIndex));
                    System.out.println("tpindex : " + data.instance(i).value(tpIndex));
                    fpList.add(data.instance(i).value(fpIndex));
                    System.out.println("fpindex : " + data.instance(i).value(fpIndex));
                    preList.add(data.instance(i).value(precisionIndex));
                    reList.add(data.instance(i).value(recallIndex));

                }

                //AUC for ROC
                Double auc = ThresholdCurve.getROCArea(data);
                System.out.println("AOC under ROC : " + auc.toString());

                //AUC for PRC
                Double aucPRC = ThresholdCurve.getPRCArea(data);
                System.out.println("AOC under PRC :" + aucPRC.toString());


                Instances ccResult = CostCurve.getCurve(data);
                CCxyDataSet = createCostCurve(ccResult, index);


                //根绝实际需求加载数据集到xydatasets中
                ROCxydataSet = getData(ROCxydataSet, tpList, fpList, index);
                PRCxydataSet = getData(PRCxydataSet, preList,reList, index);
            } catch (Exception e) {
                e.printStackTrace();
            }
            index++;
        }
        generatePic(ROCxydataSet,"ROC","False Positive Rate","True Positive Rate");
        generatePic(PRCxydataSet,"PRC","Recall","Precision");
        generatePic(CCxyDataSet, WekaConstants.COST_CURVE, WekaConstants.PROBABILITY_COSY_FUNCTION, WekaConstants.NORMALIZED_EXPECTED_COST);
    }

    public DefaultXYDataset getData(DefaultXYDataset dataSet, List tpList, List fpList, int index) {
        int size = tpList.size();
        double[][] datas = new double[2][size];
        for (int i = 0; i < size; i++) {
            datas[0][i] = (double) fpList.get(i);
            datas[1][i] = (double) tpList.get(i);
        }
        dataSet.addSeries(index, datas);
        return dataSet;
    }

    public void saveChart(JFreeChart chart, String outputPath, int weight, int height) {
        FileOutputStream out = null;
        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String nowStr = dateFormat.format(now);
        outputPath = outputPath + File.separator + nowStr + ".png";
        try {

            File outFile = new File(outputPath);

            if (!outFile.getParentFile().exists()) {

                outFile.getParentFile().mkdirs();

            }

            out = new FileOutputStream(outputPath);
            // 保存为PNG

            ChartUtilities.writeChartAsPNG(out, chart, weight, height);

            // 保存为JPEG

            // ChartUtilities.writeChartAsJPEG(out, chart, weight, height);

            out.flush();

        } catch (FileNotFoundException e) {

            e.printStackTrace();

        } catch (IOException e) {

            e.printStackTrace();

        } finally {

            if (out != null) {

                try {

                    out.close();
                } catch (IOException e) {
                    // do nothing
                }
            }
        }
    }

    public void generatePic(DefaultXYDataset xyDataset,String name,String xName,String yName) {

        //创建主题样式

        StandardChartTheme mChartTheme = new StandardChartTheme("CN");

        //设置标题字体

        mChartTheme.setExtraLargeFont(new Font("黑体", Font.BOLD, 20));

        //设置轴向字体

        mChartTheme.setLargeFont(new Font("宋体", Font.CENTER_BASELINE, 15));

        //设置图例字体

        mChartTheme.setRegularFont(new Font("宋体", Font.CENTER_BASELINE, 15));

        //应用主题样式

        ChartFactory.setChartTheme(mChartTheme);

        JFreeChart chart = ChartFactory.createXYLineChart(name, xName, yName, xyDataset, PlotOrientation.VERTICAL, true, false, false);

        ChartPanel panel = new ChartPanel(chart, true);

        chart.setBackgroundPaint(Color.white);

        chart.setBorderPaint(Color.GREEN);

        chart.setBorderStroke(new BasicStroke(1.5f));

        XYPlot xyplot = (XYPlot) chart.getPlot();


        xyplot.setBackgroundPaint(new Color(255, 253, 246));

        ValueAxis vaaxis = xyplot.getDomainAxis();

        vaaxis.setAxisLineStroke(new BasicStroke(1.5f));


        ValueAxis va = xyplot.getDomainAxis(0);

        va.setAxisLineStroke(new BasicStroke(1.5f));


        va.setAxisLineStroke(new BasicStroke(1.5f));        // 坐标轴粗细

        va.setAxisLinePaint(new Color(215, 215, 215));    // 坐标轴颜色

        xyplot.setOutlineStroke(new BasicStroke(1.5f));   // 边框粗细

        va.setLabelPaint(new Color(10, 10, 10));          // 坐标轴标题颜色

        va.setTickLabelPaint(new Color(102, 102, 102));   // 坐标轴标尺值颜色

        ValueAxis axis = xyplot.getRangeAxis();

        axis.setAxisLineStroke(new BasicStroke(1.5f));


        XYLineAndShapeRenderer xylineandshaperenderer = (XYLineAndShapeRenderer) xyplot

                .getRenderer();

        xylineandshaperenderer.setSeriesOutlinePaint(0, Color.WHITE);

        xylineandshaperenderer.setUseOutlinePaint(true);

        NumberAxis numberaxis = (NumberAxis) xyplot.getDomainAxis();

        numberaxis.setAutoRangeIncludesZero(false);

        numberaxis.setTickMarkInsideLength(2.0F);

        numberaxis.setTickMarkOutsideLength(0.0F);

        numberaxis.setAxisLineStroke(new BasicStroke(1.5f));

        JFrame frame = new JFrame();
        frame.setLayout(new BorderLayout());
        frame.add(panel, "Center");
        JButton saveButton = new JButton("save");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
                jfc.setMultiSelectionEnabled(false);//不支持多选
                jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);//只支持文件夹
                int re = jfc.showOpenDialog(frame);
                if (re == JFileChooser.APPROVE_OPTION) {
                    saveChart(chart, jfc.getSelectedFile().getAbsolutePath(), 600, 600);
                }
            }
        });
        frame.add(saveButton, "South");
        frame.pack();

        frame.setVisible(true);

    }


    public static int[] convertIntegers(List<Integer> integers)
    {
        int[] ret = new int[integers.size()];
        for (int i=0; i < ret.length; i++)
        {
            ret[i] = integers.get(i).intValue();
        }
        return ret;
    }

    public DefaultXYDataset createCostCurve(Instances ccInst, int index) {
        DefaultXYDataset result = new DefaultXYDataset();
        if (ccInst.size() != 0) {
            int necIndex = 0;
            int psfIndex = 0;
//            int ThresholdIndex = 0;
            for (int i = 0; i < ccInst.numAttributes() - 1; i++) {
                Attribute att = ccInst.attribute(i);
                String name = att.name();
                switch (name) {
                    case WekaConstants.NORMALIZED_EXPECTED_COST:{
                        necIndex = i;
                        break;
                    }
                    case WekaConstants.PROBABILITY_COSY_FUNCTION:{
                        psfIndex = i;
                        break;
                    }
                    default: break;
                }
            }

            List<Double> necList = new ArrayList<>();
            List<Double> psfList = new ArrayList<>();

            for (int i = 0; i < ccInst.numInstances(); i++) {
                necList.add(ccInst.instance(i).value(necIndex));
                psfList.add(ccInst.instance(i).value(necIndex));
            }

            result = getData(result, necList, psfList, index);
        }
        return result;
    }
}
