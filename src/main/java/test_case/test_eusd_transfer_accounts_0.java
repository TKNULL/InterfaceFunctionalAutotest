package test_case;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedList;

/**
 * EUSD转账流程一：普通用户转账给普通用户
 * 测试步骤：
 *      1、A用户登录，点击【转账】，输入B用户手机号码、金额，执行转账
 *      2、转账成功后，A用户可用余额有减少，【资产详情】页面有订单展示
 *      3、B用户可用余额有增加，【资产详情】页面有订单展示
 * */

public class test_eusd_transfer_accounts_0 {

    public static Log log = LogFactory.getLog(test_eusd_transfer_accounts_0.class);

    @BeforeMethod
    public void before_methd() throws InterruptedException {
        Thread.sleep(5*1000);
        log.info("before_methd,等待5秒");
    }

    @BeforeClass
    public void before_class(ITestContext context) throws IOException {
        log.info("before_class开始");

        //1、获取本条用例的测试账号
        LinkedList<LinkedList<String>> user_mobiles_roles = before_fun.get_mobiles("user","available",2);        //[[user, 15108708247], [user, 13007100247]]

        //获取列表中,第1个user手机号
        String user_mobile1 = user_mobiles_roles.get(0).get(1);
        context.setAttribute("user_mobile1",user_mobile1);

        //获取列表中,第2个user手机号
        String user_mobile2 = user_mobiles_roles.get(1).get(1);
        context.setAttribute("user_mobile2",user_mobile2);

        log.info("test_eusd_transfer_accounts_0用例获得的手机账号是：");
        log.info("user_mobile1："+user_mobile1);
        log.info("user_mobile2："+user_mobile2);

        //2、存储账号token、EUSD的余额
        User user1 = new User();
        String eu_token_1 = user1.login_and_return_token(user_mobile1);
        log.info("eu_token_1:"+eu_token_1);
        context.setAttribute("eu_token_1",eu_token_1);
        BigDecimal user1_eusd_available = user1.get_EUSD_available();
        context.setAttribute("user1_eusd_available",user1_eusd_available);

        User user2 = new User();
        String eu_token_2 = user2.login_and_return_token(user_mobile2);
        log.info("eu_token_2:"+eu_token_2);
        context.setAttribute("eu_token_2",eu_token_2);
        BigDecimal user2_eusd_available = user2.get_EUSD_available();
        context.setAttribute("user2_eusd_available",user2_eusd_available);

        log.info("before_class结束");
    }




    @AfterClass
    public void afterclass(ITestContext context) throws IOException {
        log.info("after_class开始");
        //1、连接数据库，已经使用的手机号，is_used标注为true
        String user_mobile1 = (String) context.getAttribute("user_mobile1");
        String user_mobile2 = (String) context.getAttribute("user_mobile2");
        LinkedList<String> used_mobiles = new LinkedList<String>();
        used_mobiles.add(user_mobile1);
        used_mobiles.add(user_mobile2);
        before_fun.insert_is_used(used_mobiles);
        log.info("after_class结束");
    }


    @Test(description ="普通用户登录，点击【转账】，输入其他普通用户手机号码、金额，执行转账")
    //一 普通用户登录，点击【转账】，输入其他普通用户手机号码、金额，执行转账
    public void eusd_transfer_accounts_0_1(ITestContext context) throws IOException {
        log.info("eusd_transfer_accounts_0_1,普通用户登录，点击【转账】，输入其他普通用户手机号码、金额，执行转账");
        //用户登录,获取二次支付密码,然后执行转账
        String eu_token_1 = (String) context.getAttribute("eu_token_1");
        User user1  = new User(eu_token_1);
        //获取被转账的手机号,执行转账
        String transfer_mobile = (String) context.getAttribute("user_mobile2");
        log.info("eusd_transfer_accounts_0_1,被转账的手机号是:"+transfer_mobile);
        int result_code = user1.eos_transfer(transfer_mobile);       //默认转账101

        Assert.assertEquals(200,result_code);
        log.info("eusd_transfer_accounts_0_1,测试完成");

    }

    @Test(dependsOnMethods = { "eusd_transfer_accounts_0_1" },description ="转账成功后，转账用户等3分钟,用户可用余额有减少")
    //二 转账成功后，转账用户等3分钟,用户可用余额有减少
    public void eusd_transfer_accounts_0_2(ITestContext context) throws IOException, InterruptedException {
        log.info("eusd_transfer_accounts_0_2, 转账成功后，转账成功后，转账用户等3分钟,用户可用余额有减少");
        Thread.sleep(3*60*1000);
        String eu_token_1 = (String) context.getAttribute("eu_token_1");
        User user1 = new User(eu_token_1);
        //获得测试前的可用余额
        BigDecimal user1_eusd_available = (BigDecimal) context.getAttribute("user1_eusd_available");
        //获得转账金额
        BigDecimal eusd_transfer_amount = user1.get_USDT_and_EUSD_transfer_amount();
        context.setAttribute("eusd_transfer_amount",eusd_transfer_amount);
        //获得测试后的可用余额
        BigDecimal user1_after_eusd_available = user1.get_EUSD_available();
        log.info("eusd_transfer_accounts_0_2,测试前的可用余额-转账金额=测试后的可用余额则测试通过,参与计算的数值是:");
        log.info("user1_eusd_available:"+user1_eusd_available);
        log.info("eusd_transfer_amount:"+eusd_transfer_amount);
        log.info("user1_after_eusd_available:"+user1_after_eusd_available);

        //测试前的可用余额-转账金额=测试后的可用余额
        if(user1_eusd_available.subtract(eusd_transfer_amount).compareTo(user1_after_eusd_available)==0){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        log.info("eusd_transfer_accounts_0_2,测试结束");
    }



    @Test(dependsOnMethods = { "eusd_transfer_accounts_0_2" },description ="转账成功后，【资产详情】页面有订单展示")
    //三 转账成功后，【资产详情】页面有订单展示
    public void eusd_transfer_accounts_0_3(ITestContext context) throws IOException, InterruptedException {
        log.info("eusd_transfer_accounts_0_3, 转账成功后，普通【资产详情】页面有订单展示");
        String eu_token_1 = (String) context.getAttribute("eu_token_1");
        User user1 = new User(eu_token_1);
        //根据转账金额,查询订单
        BigDecimal eusd_transfer_amount = (BigDecimal) context.getAttribute("eusd_transfer_amount");
        Thread.sleep(3*60*1000);        //0527,EUSD转账后,需要等待3分钟才会发起链上转账，一开始是没有转账信息的
        int []arr = user1.v1_eusd_records(eusd_transfer_amount);
        log.info("eusd_transfer_accounts_0_3,转账成功后:【资产】【EUSD】【资产详情】页面订单状态(3为正确)是："+arr[0]);         //目前无分区都是已完成
        log.info("eusd_transfer_accounts_0_3,转账成功后：【资产】【EUSD】【资产详情】页面订单类型是(1为正确)是："+arr[1]);       //转出
        if((arr[0]==3)&(arr[1]==1)){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        log.info("eusd_transfer_accounts_0_3,测试结束");
    }



    @Test(dependsOnMethods = { "eusd_transfer_accounts_0_3" },description ="转账成功后，被转账账户可用余额有增加")
    //四 转账成功后，被转账账户可用余额有增加
    public void eusd_transfer_accounts_0_4(ITestContext context) throws IOException {
        log.info("eusd_transfer_accounts_0_4,转账成功后，被转账账户可用余额有增加");
        String eu_token_2 = (String) context.getAttribute("eu_token_2");
        User user2 = new User(eu_token_2);
        //1.获取被转账用户,转帐前可用余额
        BigDecimal user2_eusd_available = (BigDecimal) context.getAttribute("user2_eusd_available");

        //2.获取转账余额
        BigDecimal eusd_transfer_amount = (BigDecimal) context.getAttribute("eusd_transfer_amount");

        //3.获取被转账用户,转帐后可用余额
        BigDecimal user2_after_eusd_available = user2.get_EUSD_available();
        log.info("eusd_transfer_accounts_0_4,转帐前可用余额 +转账余额 = 转帐后可用余额,则测试通过,参数计算的数值是:");
        log.info("user2_eusd_available:"+user2_eusd_available);
        log.info("eusd_transfer_amount:"+eusd_transfer_amount);
        log.info("user2_after_eusd_available:"+user2_after_eusd_available);

        //4.转帐前可用余额 +转账余额 = 转帐后可用余额
        if(user2_eusd_available.add(eusd_transfer_amount).compareTo(user2_after_eusd_available)==0){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        log.info("eusd_transfer_accounts_0_4,测试结束");
    }





    @Test(dependsOnMethods = { "eusd_transfer_accounts_0_4" },description ="转账成功后，被转账账户【资产详情】页面有订单展示")
    //五 转账成功后，被转账账户【资产详情】页面有订单展示
    public void eusd_transfer_accounts_0_5(ITestContext context) throws IOException, InterruptedException {
        log.info("eusd_transfer_accounts_0_5, 转账成功后，被转账账户【资产详情】页面有订单展示");
        String eu_token_2 = (String) context.getAttribute("eu_token_2");
        User user2 = new User(eu_token_2);
        //1.根据转账金额,查询订单状态
        BigDecimal eusd_transfer_amount = (BigDecimal) context.getAttribute("eusd_transfer_amount");
        int []arr = user2.v1_eusd_records(eusd_transfer_amount);
        log.info("eusd_transfer_accounts_0_5,转账成功后:【资产】【EUSD】【资产详情】页面订单状态(4为正确)是："+arr[0]);
        log.info("eusd_transfer_accounts_0_5,转账成功后：【资产】【EUSD】【资产详情】页面订单类型是(1为正确)是："+arr[1]);
        if((arr[0]==4)&(arr[1]==1)){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        log.info("eusd_transfer_accounts_0_5,测试结束");
    }
}
