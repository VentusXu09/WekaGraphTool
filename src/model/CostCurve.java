package model;

/**
 * Created by Ventus Xu on 2019/2/27.
 */

import weka.core.*;
import weka.core.pmml.Array;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for generate CostCurve According to {
 *  False Positive Rate
 *  True Positive Rate
 *  Threshold
 * } in .arff files
 *
 * This class fails to support handle raw data, pls make sure the .arff file
 * has been successfully evaluated
 */
public class CostCurve implements RevisionHandler {
    private static final String TAG = "CostCurve";

    public CostCurve() {

    }

    public String getRevision() {
        return RevisionUtils.extract("$Revision: 10169 $");
    }

    public static Instances getCurve(Instances threshInst) {
        if (threshInst.size() != 0 ) {
            Instances insts = makeHeader();
            int fpind = threshInst.attribute(WekaConstants.FALSE_POSITIVE_RATE).index();
            int tpind = threshInst.attribute(WekaConstants.TRUE_POSITIVE_RATE).index();
            int threshind = threshInst.attribute(WekaConstants.THRESHOLD).index();

            for(int i = 0; i < threshInst.numInstances(); ++i) {
                double fpval = threshInst.instance(i).value(fpind);
                double tpval = threshInst.instance(i).value(tpind);
                double thresh = threshInst.instance(i).value(threshind);
                double[] vals = new double[]{0.0D, fpval, thresh};
                insts.add(new DenseInstance(1.0D, vals));
                vals = new double[]{1.0D, 1.0D - tpval, thresh};
                insts.add(new DenseInstance(1.0D, vals));
            }

            return insts;
        } else {
            return null;
        }
    }

    private static Instances makeHeader() {
        ArrayList<Attribute> fv = new ArrayList();
        fv.add(new Attribute(WekaConstants.PROBABILITY_COSY_FUNCTION));
        fv.add(new Attribute(WekaConstants.NORMALIZED_EXPECTED_COST));
        fv.add(new Attribute(WekaConstants.THRESHOLD));
        return new Instances(WekaConstants.COST_CURVE, fv, 100);
    }

    public static class Point {
        double x;
        double y;
        double threshold;

        public Point(double x, double y, double threshold) {
            this.x = x;
            this.y = y;
            this.threshold = threshold;
        }

        public Point(double[] point) {
            this.x = point[0];
            this.y = point[1];
            this.threshold = point[2];
        }

        public static ArrayList<Point> getCCPoints(Instances ccResult) {
            ArrayList points = new ArrayList();
            for (Instance instance : ccResult) {
                if (instance instanceof DenseInstance) {
                    DenseInstance ccInstance = (DenseInstance) instance;
                    ArrayList list = new ArrayList();
                    points.add(new Point(ccInstance.toDoubleArray()));
                }
            }
            return  points;
        }

        public String toString() {
            return String.format("%.1f", x) + "\t" + String.format("%.5f", y) + "\t" + String.format("%.5f", threshold) + "\n";
        }

        public static String printLineCordinate(Point point1, Point point2) {
            return "(" + String.format("%.1f", point1.x) + ", " + String.format("%.5f", point1.y) + ")\t" +
                    "(" + String.format("%.1f", point2.x) + ", " + String.format("%.5f", point2.y) + ")\n" ;
        }

        public static String[] printLineCordinates(int num, Point point1, Point point2) {
            return new String[]{"" + num,
                    String.format("%.1f", point1.x), String.format("%.5f", point1.y),
                    String.format("%.1f", point2.x), String.format("%.5f", point2.y)};
        }
    }
}
