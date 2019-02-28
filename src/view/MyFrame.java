package view;

import controller.FrameController;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Ventus Xu on 2019/2/23.
 */

public class MyFrame extends JFrame implements ActionListener {
    private JTextArea ta;
    private JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
    private JButton bOpen, bGenerate, bClear;
    private JScrollPane ps;
    ArrayList<File> filesList=new ArrayList<>();

    public MyFrame() {
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
        bClear =new JButton("清空文件");
        bOpen.addActionListener(this);
        bClear.addActionListener(this);
        bGenerate.addActionListener(this);
        this.add(bOpen);
        this.add(bClear);
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

                filesList.addAll(Arrays.asList(jfc.getSelectedFiles()));
                System.out.println("已选中文件\n");
                filesList.forEach(x -> {
                    String name = x.getName();
                    System.out.println(name);
                    ta.append(name);
                    ta.append("\n");
                });
            }

        } else if (jbt == bGenerate) {//单击生成图片按钮
            FrameController frameController = new FrameController(filesList);
            frameController.readArffDataAndGenerate();
        }
    }
}

