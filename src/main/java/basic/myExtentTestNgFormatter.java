package basic;

import basic.sendWeChatMsg;
import basic.urlData;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.ResourceCDN;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.model.TestAttribute;
import com.aventstack.extentreports.reporter.ExtentHtmlReporter;
import com.aventstack.extentreports.reporter.configuration.ChartLocation;
import com.aventstack.extentreports.reporter.configuration.Theme;
import org.testng.*;
import org.testng.xml.XmlSuite;
import java.io.File;
import java.io.IOException;
import java.util.*;


/**
 * 此处创建TestNg的report的监听器：1、创建html测试报告；2、发送汇总消息到企业微信
 * 不用调用该函数，只需在testng.xml中设置监听器即可
 * 0625gst
 * */


public class myExtentTestNgFormatter implements IReporter {
    //生成的路径以及文件名
    private static final String OUTPUT_FOLDER = "test-output/";
    private static final String FILE_NAME = "test_report.html";
    private ExtentReports extent;

    //获取测试统计数据，便于向企业微信发送信息(在buildTestNodes方法中赋值，在sendMessage方法中调用)
    int pass=0;
    int fail=0;
    int skip=0;
    //设置发送企业微信的语言,一般用来说明测试项
    String testItems="产品自动化";


    @Override
    //创建测试报告
    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
        //1、测试报告名称/样式等数据
        init();
        boolean createSuiteNode = false;
        if(suites.size()>1){
            createSuiteNode=true;
        }
        //2、获取测试结果详情
        for (ISuite suite : suites) {
            Map<String, ISuiteResult> result = suite.getResults();
            //如果suite里面没有任何用例，直接跳过，不在报告里生成
            if(result.size()==0){
                continue;
            }
            //统计suite下的成功、失败、跳过的总用例数
            int suiteFailSize=0;
            int suitePassSize=0;
            int suiteSkipSize=0;
            ExtentTest suiteTest=null;
            //存在多个suite的情况下，在报告中将同一个一个suite的测试结果归为一类，创建一级节点。
            if(createSuiteNode){
                suiteTest = extent.createTest(suite.getName()).assignCategory(suite.getName());
            }
            boolean createSuiteResultNode = false;
            if(result.size()>1){
                createSuiteResultNode=true;
            }
            for (ISuiteResult r : result.values()) {
                ExtentTest resultNode;
                ITestContext context = r.getTestContext();
                if(createSuiteResultNode){
                    //没有创建suite的情况下，将在SuiteResult的创建为一级节点，否则创建为suite的一个子节点。
                    if( null == suiteTest){
                        resultNode = extent.createTest(r.getTestContext().getName());
                    }else{
                        resultNode = suiteTest.createNode(r.getTestContext().getName());
                    }
                }else{
                    resultNode = suiteTest;
                }
                if(resultNode != null){
                    resultNode.getModel().setName(suite.getName()+" : "+r.getTestContext().getName());
                    if(resultNode.getModel().hasCategory()){
                        resultNode.assignCategory(r.getTestContext().getName());
                    }else{
                        resultNode.assignCategory(suite.getName(),r.getTestContext().getName());
                    }
                    resultNode.getModel().setStartTime(r.getTestContext().getStartDate());
                    resultNode.getModel().setEndTime(r.getTestContext().getEndDate());
                    //统计SuiteResult下的数据
                    int passSize = r.getTestContext().getPassedTests().size();
                    int failSize = r.getTestContext().getFailedTests().size();
                    int skipSize = r.getTestContext().getSkippedTests().size();
                    suitePassSize += passSize;
                    suiteFailSize += failSize;
                    suiteSkipSize += skipSize;
                    if(failSize>0){
                        resultNode.getModel().setStatus(Status.FAIL);
                    }
                    resultNode.getModel().setDescription(String.format("Pass: %s ; Fail: %s ; Skip: %s ;",passSize,failSize,skipSize));
                }
                buildTestNodes(resultNode,context.getFailedTests(), Status.FAIL);
                buildTestNodes(resultNode,context.getSkippedTests(), Status.SKIP);
                buildTestNodes(resultNode,context.getPassedTests(), Status.PASS);
            }
            if(suiteTest!= null){
                suiteTest.getModel().setDescription(String.format("Pass: %s ; Fail: %s ; Skip: %s ;",suitePassSize,suiteFailSize,suiteSkipSize));
                if(suiteFailSize>0){
                    suiteTest.getModel().setStatus(Status.FAIL);
                }
            }

        }

        //3、创建测试报告（测试完成,准备将测试结果生成测试报告,执行此方法）
        extent.flush();     //测试完成,准备将测试结果生成测试报告,执行此方法
        //4、发送测试消息
        sendMessage(testItems,pass,fail,skip);
    }





    /**
     * 设置测试报告的名称、样式等信息（被generateReport调用）
     *
     * */
    private void init() {
        //文件夹不存在的话进行创建
        File reportDir= new File(OUTPUT_FOLDER);
        if(!reportDir.exists()&& !reportDir .isDirectory()){
            reportDir.mkdir();
        }
        ExtentHtmlReporter htmlReporter = new ExtentHtmlReporter(OUTPUT_FOLDER + FILE_NAME);
        // 设置静态文件的DNS
        //怎么样解决cdn.rawgit.com访问不了的情况
        htmlReporter.config().setResourceCDN(ResourceCDN.EXTENTREPORTS);
        htmlReporter.config().setDocumentTitle("产品自动化测试报告");
        htmlReporter.config().setReportName("产品自动化测试报告");
        htmlReporter.config().setChartVisibilityOnOpen(true);
        htmlReporter.config().setTestViewChartLocation(ChartLocation.TOP);
        htmlReporter.config().setTheme(Theme.STANDARD);
        htmlReporter.config().setCSS(".node.level-1  ul{ display:none;} .node.level-1.active ul{display:block;}");
        extent = new ExtentReports();
        extent.attachReporter(htmlReporter);
        extent.setReportUsesManualConfiguration(true);
    }




    /**
    * 被generateReport调用
    *
    *
    * */
    private void buildTestNodes(ExtentTest extenttest, IResultMap tests, Status status) {
        //存在父节点时，获取父节点的标签
        String[] categories=new String[0];
        if(extenttest != null ){
            List<TestAttribute> categoryList = extenttest.getModel().getCategoryContext().getAll();
            categories = new String[categoryList.size()];
            for(int index=0;index<categoryList.size();index++){
                categories[index] = categoryList.get(index).getName();
            }
        }
        ExtentTest test;
        if (tests.size() > 0) {
            //调整用例排序，按时间排序
            Set<ITestResult> treeSet = new TreeSet<ITestResult>(new Comparator<ITestResult>() {
                @Override
                public int compare(ITestResult o1, ITestResult o2) {
                    return o1.getStartMillis()<o2.getStartMillis()?-1:1;
                }
            });
            treeSet.addAll(tests.getAllResults());
            for (ITestResult result : treeSet) {
                String name="";
                //把@test中的用例名称写到测试报告里（如果有参数，则使用参数的toString组合代替报告中的name）
                Object[] parameters = result.getParameters();
                if(name.length()>0){
                    if(name.length()>50){
                        name= name.substring(0,49)+"...";
                    }
                }else{
                    name = result.getMethod().getMethodName()+"---"+result.getMethod().getDescription();
                }
                if(extenttest==null){
                    test = extent.createTest(name);
                }else{
                    //作为子节点进行创建时，设置同父节点的标签一致，便于报告检索。
                    test = extenttest.createNode(name).assignCategory(categories);
                }
                for (String group : result.getMethod().getGroups())
                    test.assignCategory(group);
                List<String> outputList = Reporter.getOutput(result);
                for(String output:outputList){
                    //将用例的log输出报告中
                    test.debug(output);
                }

                //0625：统计测试结果
                if (result.getThrowable() != null) {
                    test.log(status, result.getThrowable());
                    fail+=1;
                } else {
                    test.log(status, "Test " + status.toString().toLowerCase() + "ed");
                    pass+=1;
                }










                test.getModel().setStartTime(getTime(result.getStartMillis()));
                test.getModel().setEndTime(getTime(result.getEndMillis()));
            }
        }
    }



    /**
     * 向企业微信发送测试结果数据
     * parm: int pass/fail/skip
     * 0625 gst
     *
     *
     *
     * */
    private  void sendMessage(String name,int pass,int fail,int skip){
        String result_string=name+"测试结束，本次共计测试"+(pass+fail+skip)+"条case，其中pass："+pass+"条，fail："+fail+"条，skip："+skip+"条，详情请查看测试报告。";
        sendWeChatMsg sw = new sendWeChatMsg();
        try {
            //0、拼接测试结果字符串
            //1、根据企业ID、应用token获取密钥（Sercret）获取token
            String token = sw.getToken("wwa1fb212aeaaf8744","MZeQAGn8_MpOobHo3V2bmrDG3z6KrCQ9uhQJRsEmYtY");
            //2、创建数组保存所有收信人的企业微信账号
            LinkedList<String> msglist = new LinkedList();
//            msglist.add("LinCheng");   //林诚
//            msglist.add("ZhangLongXin");   //张龙欣
//            msglist.add("QiuTu");   //刘通
            msglist.add("3571bedef240ea1ad687067ca39cf85d");  //郭舒婷

            //2、创建微信发送请求post数据（接收人的账号、信息类型、应用ID、内容类型、内容）
            for (int i=0;i<msglist.size();i++){
                String postdata = sw.createPostData(msglist.get(i), "text", 1000010, "content",result_string); //10000010是钱多多应用的agentid，每个应用的ID都不一样
                //3、创建微信发送请求post实体
                String resp = sw.post("utf-8", sendWeChatMsg.CONTENT_TYPE,(new urlData()).getSend_message_url(), postdata, token);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    /**
     * 获取时间
     *
     * */
    private Date getTime(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return calendar.getTime();
    }
}