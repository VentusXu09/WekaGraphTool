package controller;

import model.CostCurve;
import model.WekaConstants;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Created by Ventus Xu on 2019/2/23.
 */
public class FrameController  {
    private List<File> fileList = new ArrayList<>();
    private int lineShape = 1;//default
    ArrayList ccPoints = new ArrayList<CostCurve.Point>();

    public FrameController(ArrayList<File> selectedFiles) {
        this.fileList = selectedFiles;
    }

    public void readArffDataAndGenerate() {
        DefaultXYDataset ROCxydataSet = new DefaultXYDataset();
        DefaultXYDataset PRCxydataSet = new DefaultXYDataset();
        DefaultXYDataset CCxyDataSet = new DefaultXYDataset();
        int index = 0;

        for (File x : fileList) {
            String filePath = x.getAbsolutePath();
            try {
                Instances data = new Instances(
                        new BufferedReader(
                                new FileReader(filePath)));
                data.setClassIndex(data.numAttributes() - 1);

                //ROC

                //AUC for ROC
                Double auc = ThresholdCurve.getROCArea(data);
                ROCxydataSet = createCurve(ROCxydataSet, data, index, WekaConstants.FALSE_POSITIVES, WekaConstants.TRUE_POSITIVES, auc);

                //PRC

                //AUC for PRC
                Double aucPRC = ThresholdCurve.getPRCArea(data);
                PRCxydataSet = createCurve(PRCxydataSet, data, index, WekaConstants.RECALL, WekaConstants.PRECISION, aucPRC);

                //Cost Curve
                //All end points are saved in ccResult
                Instances ccResult = CostCurve.getCurve(data);
                ccPoints = CostCurve.Point.getCCPoints(ccResult);
                CCxyDataSet = createCurve(CCxyDataSet, ccResult, index, WekaConstants.PROBABILITY_COSY_FUNCTION, WekaConstants.NORMALIZED_EXPECTED_COST, -1);


            } catch (Exception e) {
                e.printStackTrace();
            }
            index++;
        }
        generatePic(ROCxydataSet, WekaConstants.ROC, WekaConstants.FALSE_POSITIVES,WekaConstants.TRUE_POSITIVES);
        generatePic(PRCxydataSet, WekaConstants.PRC,WekaConstants.PRECISION,WekaConstants.RECALL);
        generatePic(CCxyDataSet, WekaConstants.COST_CURVE, WekaConstants.PROBABILITY_COSY_FUNCTION, WekaConstants.NORMALIZED_EXPECTED_COST);
    }

    public DefaultXYDataset getData(DefaultXYDataset dataSet, java.util.List xList, java.util.List yList, int index, double auc) {
        if (xList.size() > 0) {
            int size = xList.size();
            double[][] datas = new double[2][size];
            for (int i = 0; i < size; i++) {
                datas[0][i] = (double) xList.get(i);
                datas[1][i] = (double) yList.get(i);

            }
            if(-1 == auc)
                dataSet.addSeries(fileList.get(index).getName(), datas);
            else
            {
                String info="[" + fileList.get(index).getName() + " auc = " + auc + "]";
                dataSet.addSeries(info, datas);
            }
        }
        return dataSet;
    }

    public void saveChart(JFreeChart chart, String outputPath, int weight, int height) {
        FileOutputStream out = null;
        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String nowStr = dateFormat.format(now);
        String picOutputPath = outputPath + File.separator + nowStr + ".png";
        try {

            File outFile = new File(picOutputPath);

            if (!outFile.getParentFile().exists()) {

                outFile.getParentFile().mkdirs();

            }

            out = new FileOutputStream(picOutputPath);
            // 保存为PNG

            ChartUtilities.writeChartAsPNG(out, chart, weight, height);

            // 保存为JPEG

            // ChartUtilities.writeChartAsJPEG(out, chart, weight, height);

            out.flush();

            //Blank workbook
            XSSFWorkbook workbook = new XSSFWorkbook();

            //Create a blank sheet
            XSSFSheet sheet = workbook.createSheet("Employee Data");

            Map<Integer, Object[]> data = new TreeMap<Integer, Object[]>();
            data.put(1, new Object[]{"Num", "Point1.x", "Point1.y", "Point2.x", "Point2.y"});


            if (WekaConstants.COST_CURVE.equalsIgnoreCase(chart.getTitle().getText())){
                String pointOutputPath = outputPath + File.separator + "CostCurvePoints.xlsx";
                Iterator<CostCurve.Point> iterable = ccPoints.iterator();
                int i = 2;
                while (iterable.hasNext()) {
                    data.put(i++, CostCurve.Point.printLineCordinates(i-2, iterable.next(), iterable.next()));
                }

                Set<Integer> keyset = data.keySet();
                int rownum = 0;
                for (Integer key : keyset)
                {
                    Row row = sheet.createRow(rownum++);
                    Object [] objArr = data.get(key);
                    int cellnum = 0;
                    for (Object obj : objArr)
                    {
                        Cell cell = row.createCell(cellnum++);
                        if(obj instanceof String)
                            cell.setCellValue((String)obj);
                        else if(obj instanceof Integer)
                            cell.setCellValue((Integer)obj);
                    }
                }

                try
                {
                    //Write the workbook in file system
                    FileOutputStream excelOut = new FileOutputStream(new File(pointOutputPath));
                    if (null == excelOut) return;
                    workbook.write(excelOut);
                    excelOut.close();
                    System.out.println(pointOutputPath + " has been written successfully on disk.");
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }

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


        xyplot.setBackgroundPaint(new Color(255, 255, 255));

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

        for(int i=0;i<xyDataset.getSeriesCount();i++) {
            xylineandshaperenderer.setSeriesStroke(i, new BasicStroke(lineShape));//调整线条粗细
        }

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

    public DefaultXYDataset createCurve(DefaultXYDataset result, Instances inst, int index, String xName, String yName, double auc) {
        if (inst.size() != 0) {
            int xIndex = 0;
            int yIndex = 0;
            for (int i = 0; i < inst.numAttributes() - 1; i++) {
                Attribute att = inst.attribute(i);
                String name = att.name();

                if (name.equalsIgnoreCase(xName)){
                    xIndex = i;
                } else if (name.equalsIgnoreCase(yName)){
                    yIndex = i;
                }
            }

            java.util.List<Double> necList = new ArrayList<>();
            List<Double> psfList = new ArrayList<>();

            for (int i = 0; i < inst.numInstances(); i++) {
                necList.add(inst.instance(i).value(xIndex));
                psfList.add(inst.instance(i).value(yIndex));
            }

            result = getData(result, necList, psfList, index, auc);
        }
        return result;
    }

    public int getLineShape() {
        return lineShape;
    }

    public void setLineShape(int lineShape) {
        this.lineShape = lineShape;
    }

    public List<File> getFileList() {
        return fileList;
    }

    public void setFileList(List<File> fileList) {
        this.fileList = fileList;
    }
}
