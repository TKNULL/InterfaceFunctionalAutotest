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
 * USDT转账流程一：承兑商用户转账给承兑商用户
 * 前提：1、承兑商用户登录，USDT账户有余额；
 * 测试步骤：
 *      1、承兑商用户A登录，点击【转账】，输入承兑商用户B手机号码、金额，执行转账            ==》点击转账，在有余额的情况下，报可用token不足0614
 *      2、转账成功后，承兑商用户A可用余额有减少，【资产详情】页面有订单展示
 *      3、承兑商用户B可用余额有增加，【资产详情】页面有订单展示
 * */

public class test_usdt_transfer_accounts_0 {

    public static Log log = LogFactory.getLog(test_usdt_transfer_accounts_0.class);

    @BeforeMethod
    public void before_methd() throws InterruptedException {
        Thread.sleep(5*1000);
        log.info("test_usdt_transfer_accounts_0,等待5秒");
    }

    @BeforeClass
    public void before_class(ITestContext context) throws IOException {
        log.info("before_class开始");
        //1、关闭所有承兑商[承兑]页面功能
        boolean return_result=User.test_otcclear();
        log.info("关闭所有承兑商，结果是："+return_result);

        //1、获取本条用例的测试账号
        LinkedList<LinkedList<String>> user_mobiles_roles = before_fun.get_mobiles("eu_user","available",2);        //[[user, 15108708247], [user, 13007100247]]

        //获取列表中,第1个eu_user手机号
        String eu_mobile1 = user_mobiles_roles.get(0).get(1);
        context.setAttribute("eu_mobile1",eu_mobile1);

        //获取列表中,第2个eu_user手机号
        String eu_mobile2 = user_mobiles_roles.get(1).get(1);
        context.setAttribute("eu_mobile2",eu_mobile2);

        log.info("test_usdt_transfer_accounts_0用例获得的手机账号是：");
        log.info("eu_mobile1："+eu_mobile1);
        log.info("eu_mobile2："+eu_mobile2);

        //3、存储账号token、USDT的余额
        User eu_user1 = new User();        //承兑商账号1
        User eu_user2 = new User();        //承兑商账号2
        String eu_token_1 = eu_user1.login_and_return_token(eu_mobile1);
        String eu_token_2 = eu_user2.login_and_return_token(eu_mobile2);
        context.setAttribute("eu_token_1",eu_token_1);
        context.setAttribute("eu_token_2",eu_token_2);

        BigDecimal eu_user1_usdt_available = eu_user1.v1_usdt_return_available();
        BigDecimal eu_user1_usdt_frozen = eu_user1.v1_usdt_return_mortgaged();
        BigDecimal eu_user2_usdt_available = eu_user2.v1_usdt_return_available();
        BigDecimal eu_user2_usdt_frozen = eu_user2.v1_usdt_return_mortgaged();

        context.setAttribute("eu_user1_usdt_available",eu_user1_usdt_available);
        context.setAttribute("eu_user1_usdt_frozen",eu_user1_usdt_frozen);
        context.setAttribute("eu_user2_usdt_available",eu_user2_usdt_available);
        context.setAttribute("eu_user2_usdt_frozen",eu_user2_usdt_frozen);

        log.info("before_class结束");
    }


    @AfterClass
    public void afterclass(ITestContext context) {
        log.info("after_class开始");
        //1、连接数据库，已经使用的手机号，is_used标注为true
        String eu_mobile1 = (String) context.getAttribute("eu_mobile1");
        String eu_mobile2 = (String) context.getAttribute("eu_mobile2");
        LinkedList<String> used_mobiles = new LinkedList<String>();
        used_mobiles.add(eu_mobile1);
        used_mobiles.add(eu_mobile2);
        before_fun.insert_is_used(used_mobiles);
        log.info("after_class结束");
    }






    @Test(description = "承兑商A登录，点击【转账】，输入承兑商B手机号码、金额，执行转账")
    //一 承兑商A登录，点击【转账】，输入承兑商B手机号码、金额，执行转账
    public void usdt_transfer_accounts_0_1(ITestContext context) throws IOException {
        log.info("usdt_transfer_accounts_0_1,承兑商用户登录，点击【转账】，输入其他普通用户手机号码、金额，执行转账");
        //用户登录,获取二次支付密码,然后执行转账
        String eu_token1 = (String) context.getAttribute("eu_token_1");
        User user1  = new User(eu_token1);
        //获取被转账的手机号,执行转账
        String transfer_mobile = (String) context.getAttribute("transfer_mobile");
        int result_code = user1.eos_transfer(transfer_mobile);       //默认转账101
        Assert.assertEquals(200,result_code);
        log.info("usdt_transfer_accounts_0_1,测试完成");

    }

    @Test(dependsOnMethods = { "usdt_transfer_accounts_0_1" },description = "转账成功后，承兑商A可用余额有减少")
    //二 转账成功后，承兑商A可用余额有减少
    public void usdt_transfer_accounts_0_2(ITestContext context) throws IOException {
        log.info("usdt_transfer_accounts_0_2, 转账成功后，承兑商A可用余额有减少");
        String eu_token_1 = (String) context.getAttribute("eu_token_1");
        User user1 = new User(eu_token_1);
        //获得测试前的可用余额
        BigDecimal user1_usdt_available = (BigDecimal) context.getAttribute("user1_usdt_available");
        //获得转账金额
        BigDecimal USDT_transfer_amount = user1.get_USDT_and_EUSD_transfer_amount();
        context.setAttribute("USDT_transfer_amount",USDT_transfer_amount);
        //获得测试后的可用余额
        BigDecimal user1_after_usdt_available = user1.v1_usdt_return_available();
        log.info("usdt_transfer_accounts_0_2,测试前的可用余额-转账金额=测试后的可用余额则测试通过,参与计算的数值是:");
        log.info("user1_usdt_available:"+user1_usdt_available);
        log.info("USDT_transfer_amount:"+USDT_transfer_amount);
        log.info("user1_after_usdt_available:"+user1_after_usdt_available);

        //测试前的可用余额-转账金额=测试后的可用余额
        if(user1_usdt_available.subtract(USDT_transfer_amount).compareTo(user1_after_usdt_available)==0){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        log.info("usdt_transfer_accounts_0_2,测试结束");
    }

    @Test(dependsOnMethods = { "usdt_transfer_accounts_0_2" },description = "转账成功后，【资产详情】页面有订单展示"       )
    //三 转账成功后，【资产详情】页面有订单展示
    public void usdt_transfer_accounts_0_3(ITestContext context) throws IOException {
        log.info("usdt_transfer_accounts_0_3, 转账成功后，普通【资产详情】页面有订单展示");
        String eu_token_1 = (String) context.getAttribute("eu_token_1");
        User user1 = new User(eu_token_1);
        //根据转账金额,查询订单
        BigDecimal USDT_transfer_amount = (BigDecimal) context.getAttribute("USDT_transfer_amount");
        int []arr = user1.v1_usdt_records(USDT_transfer_amount);
        log.info("usdt_transfer_accounts_0_3,转账成功后:【资产】【USDT】【资产详情】页面订单状态(2为正确)是："+arr[0]);
        log.info("usdt_transfer_accounts_0_3,转账成功后：【资产】【USDT】【资产详情】页面订单类型是(??为正确)是："+arr[1]);
        if((arr[0]==2)&(arr[1]==3)){           //0527,转账成功(服务器返回200),但是没有订单生成,金额没有变化        //0626,arr[1]==？？？
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        log.info("usdt_transfer_accounts_0_3,测试结束");
    }



    @Test(dependsOnMethods = { "usdt_transfer_accounts_0_3" },description = "转账成功后，承兑商B可用余额有增加"  )
    //四 转账成功后，承兑商B可用余额有增加
    public void usdt_transfer_accounts_0_4(ITestContext context) throws IOException {
        log.info("usdt_transfer_accounts_0_4,转账成功后，被转账账户可用余额有增加");
        String eu_token_2 = (String) context.getAttribute("eu_token_2");
        User user2 = new User(eu_token_2);
        //获取被转账用户,转帐前可用余额
        BigDecimal user2_usdt_available = (BigDecimal) context.getAttribute("user2_usdt_available");
        //获取转账余额
        BigDecimal USDT_transfer_amount = (BigDecimal) context.getAttribute("USDT_transfer_amount");
        //获取被转账用户,转帐后可用余额
        BigDecimal user2_after_usdt_available = user2.v1_usdt_return_available();
        log.info("usdt_transfer_accounts_0_4,转帐前可用余额 +转账余额 = 转帐后可用余额,则测试通过,参数计算的数值是:");
        log.info("user2_usdt_available:"+user2_usdt_available);
        log.info("USDT_transfer_amount:"+USDT_transfer_amount);
        log.info("user2_after_usdt_available:"+user2_after_usdt_available);
        //转帐前可用余额 +转账余额 = 转帐后可用余额
        if(user2_usdt_available.add(USDT_transfer_amount).compareTo(user2_after_usdt_available)==0){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        log.info("usdt_transfer_accounts_0_4,测试结束");
    }


    @Test(dependsOnMethods = { "usdt_transfer_accounts_0_4" },description = "转账成功后，承兑商B【资产详情】页面有订单展示" )
    //五 转账成功后，承兑商B【资产详情】页面有订单展示
    public void usdt_transfer_accounts_0_5(ITestContext context) throws IOException {
        log.info("usdt_transfer_accounts_0_5, 转账成功后，承兑商B【资产详情】页面有订单展示");
        String eu_token_2 = (String) context.getAttribute("eu_token_2");
        User user2 = new User(eu_token_2);
        //根据转账金额,查询订单
        BigDecimal USDT_transfer_amount = (BigDecimal) context.getAttribute("USDT_transfer_amount");
        int []arr = user2.v1_usdt_records(USDT_transfer_amount);
        log.info("usdt_transfer_accounts_0_5,转账成功后:【资产】【USDT】【资产详情】页面订单状态(2为正确)是："+arr[0]);
        log.info("usdt_transfer_accounts_0_5,转账成功后：【资产】【USDT】【资产详情】页面订单类型是(??为正确)是："+arr[1]);
        if((arr[0]==2)&(arr[1]==3)){           //0527,转账成功(服务器返回200),但是没有订单生成,金额没有变化             //0626,arr[1]==？？？
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        log.info("usdt_transfer_accounts_0_5,测试结束");
    }
}
