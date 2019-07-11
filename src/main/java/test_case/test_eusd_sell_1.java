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
import java.math.BigInteger;
import java.util.LinkedList;


/**
 * EUSD出售流程一：普通用户,出售下单后，匹配到承兑商生成订单；承兑商15分钟内无操作，系统自动取消订单，交易失败。
 * 前提：1、普通用户登录，
 *          A、【我的】【收付款设置】里面有添加收付款账户（银行卡、微信、支付宝）
 *          B、EUSD账户有余额
 *      2、确保能匹配到承兑商：
 *          A、平台有承兑商；
 *          B、承兑商【承兑】【收款服务】有开启；
 *          C、【收款设置】里有收款账号；
 *          D、承兑商EUSD资金足够
 *          E、承兑商【承兑】【今日累积收款金额】有设置数值；
 *       3、承兑商、用户的支付密码都是123456
 * 测试步骤：
 *      1、用户下单，查看是否下单成功；下单成功后，用户【资产】【EUSD】模块资产可用余额-X，冻结余额+X（记录数据，便于后续参与计算）
 *      2、下单成功后，用户【我的】【我的订单】页面第一列显示该订单，且订单状态为“未付款”
 *      3、下单成功后，承兑商【承兑】【支付订单】页面第一列显示该订单，订单状态为“未付款”
 *
 *      4、承兑商15分钟内无操作,自动取消订单  ==》订单状态变化~~~~~~~~~~~~~~~~~
 *      5、取消订单后，用户【资产】【EUSD】模块资产可用余额+X，冻结余额-X
 *      6、取消订单后，用户【我的】【我的订单】页面第一列显示该订单，且订单状态为“已取消”
 *      7、取消订单后，承兑商【承兑】【支付订单】页面第一列显示该订单，订单状态为“已取消”
 *
 * */

public class test_eusd_sell_1 {
    public static Log log = LogFactory.getLog(test_eusd_sell_1.class);

    @BeforeMethod
    public void before_methd() throws InterruptedException {
        Thread.sleep(5*1000);
        log.info("test_eusd_buy_0,等待5秒");
    }

    @BeforeClass
    public void before_class(ITestContext context) throws IOException {
        log.info("before_class开始");
        //1、关闭所有承兑商[承兑]页面功能
        boolean return_result=User.test_otcclear();
        log.info("关闭所有承兑商，结果是："+return_result);

        //2、获取本条用例的测试账号
        LinkedList<LinkedList<String>> user_mobiles_roles = before_fun.get_mobiles("user","available",1);
        LinkedList<LinkedList<String>> eu_user_mobiles_roles = before_fun.get_mobiles("eu_user","available",1);

        //获取列表中,第一个user手机号
        String user_mobile = user_mobiles_roles.get(0).get(1);
        context.setAttribute("user_mobile",user_mobile);

        //获取列表中,第一个eu_user手机号
        String eu_user_mibile = eu_user_mobiles_roles.get(0).get(1);
        context.setAttribute("eu_user_mibile",eu_user_mibile);
        log.info("test_eusd_sell_1用例获得的手机账号是：");
        log.info("user_mobile："+user_mobile);
        log.info("eu_user_mibile："+eu_user_mibile);

        //3、存储账号token、EUSD的各种余额    available/frozen     available/trade
        //普通用户登录
        User user = new User();
        String user_token = user.login_and_return_token(user_mobile);
        BigDecimal user_eusd_before_available =user.get_EUSD_available();       //[资产]页面
        BigDecimal user_eusd_before_frozen =user.get_EUSD_frozen();
        context.setAttribute("user_token",user_token);
        context.setAttribute("user_eusd_before_available",user_eusd_before_available);
        context.setAttribute("user_eusd_before_frozen",user_eusd_before_frozen);



        //承兑商登录
        User eu_user = new User();
        String eu_token = user.login_and_return_token(eu_user_mibile);
        BigDecimal eu_eusd_before_available =eu_user.v1_exchange_info_return_available();      //[承兑]页面
        BigDecimal eu_eusd_before_trade =eu_user.v1_exchange_info_return_trade();
        BigDecimal eu_eusd_before_sell_rmb_today = eu_user.v1_exchange_info_return_sell_rmb_day();        //【承兑】【今日累积收款】

        context.setAttribute("eu_token",eu_token);
        context.setAttribute("eu_eusd_before_available",eu_eusd_before_available);
        context.setAttribute("eu_eusd_before_trade",eu_eusd_before_trade);
        context.setAttribute("eu_eusd_before_sell_rmb_today",eu_eusd_before_sell_rmb_today);
        log.info("测试前先获取数据,获取的数据是:");
        log.info("user_eusd_before_available:"+user_eusd_before_available);
        log.info("user_eusd_before_frozen:"+user_eusd_before_frozen);
        log.info("eu_eusd_before_available:"+eu_eusd_before_available);
        log.info("eu_eusd_before_trade:"+eu_eusd_before_trade);
        log.info("eu_eusd_before_sell_rmb_today:"+eu_eusd_before_sell_rmb_today);


        //4、开启指定的承兑商
        boolean buy_start_code = eu_user.va_buy_start(true,eu_token);
        boolean sell_start_code = eu_user.va_sell_start(true,eu_token);
        log.info("开启指定的承兑商，结果是:");
        log.info("buy_start_code:"+buy_start_code);
        log.info("sell_start_code:"+sell_start_code);

        log.info("before_class结束");
    }




    @AfterClass
    public void afterclass(ITestContext context) throws IOException {
        log.info("after_class开始");
        //1、关闭所有承兑商承兑功能
        boolean return_result=User.test_otcclear();
        log.info("关闭所有承兑商承兑功能,结果是："+return_result);
        //2、连接数据库，已经使用的手机号，is_used标注为true
        String user_mobile = (String) context.getAttribute("user_mobile");
        String eu_user_mibile = (String) context.getAttribute("eu_user_mibile");
        LinkedList<String> used_mobiles = new LinkedList<String>();
        used_mobiles.add(user_mobile);
        used_mobiles.add(eu_user_mibile);
        before_fun.insert_is_used(used_mobiles);
        log.info("after_class结束");
    }




    @Test(description ="用户下单，查看是否下单成功(根据是否返回订单ID来判断)")
    //一、用户下单，查看是否下单成功(根据是否返回订单ID来判断)
    public void eusd_sell_1_1(ITestContext context) throws IOException {
        log.info("eusd_sell_1_1,用户下单，查看是否下单成功");
        //1、用户下单，判断是否生成订单
        String user_token = (String) context.getAttribute("user_token");
        User user = new User(user_token);
        BigInteger id =user.execute_sell_return_id();
        if (id!=null){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        //2、返回数据
        //订单唯一ID
        context.setAttribute("id",id);
        //下单金额
        context.setAttribute("EUSD_sell_quantity",user.get_EUSD_sell_quantity());       //每次下单101
    }



    @Test(dependsOnMethods = { "eusd_sell_1_1" }  ,description ="下单成功后，用户[资产][EUSD]可用余额-x,冻结金额+x"  )
    //二、下单成功后，用户[资产][EUSD]可用余额-x,冻结金额+x
    public void eusd_sell_1_2(ITestContext context) throws IOException, InterruptedException {
        log.info("eusd_sell_0_2:下单成功后，用户[资产][EUSD]可用余额-x,冻结金额+x");
        Thread.sleep(3 * 60 * 1000);
        boolean result_1;
        boolean result_2;
        //1.用户登录
        String user_token = (String) context.getAttribute("user_token");
        User user = new User(user_token);

        //2.获取数据进行比较
        BigDecimal user_after_available = user.get_EUSD_available();     //现有可用余额
        BigDecimal user_after_frozen = user.get_EUSD_frozen();           //现有冻结余额

        BigDecimal user_eusd_before_available = (BigDecimal) context.getAttribute("user_eusd_before_available");     //原有可用余额
        BigDecimal user_eusd_before_frozen = (BigDecimal) context.getAttribute("user_eusd_before_frozen");           //原有冻结余额

        BigDecimal EUSD_sell_quantity = (BigDecimal) context.getAttribute("EUSD_sell_quantity");     //下单金额

        log.info("eusd_sell_1_2,用户原可用余额-出售金额=现可用余额,则测试通过,参与计算的数值是:");
        log.info("user_eusd_before_available:" + user_eusd_before_available);
        log.info("EUSD_sell_quantity:" + EUSD_sell_quantity);
        log.info("user_after_available:" + user_after_available);
        if (user_eusd_before_available.subtract(EUSD_sell_quantity).compareTo(user_after_available) == 0) {
            result_1 = true;
        } else {
            result_1 = false;
        }
        log.info("result_1是:" + result_1);

        log.info("eusd_sell_1_2,用户原冻结余额+出售金额=现冻结余额,则测试通过,参与计算的数值是:");
        log.info("user_after_frozen:" + user_after_frozen);
        log.info("EUSD_sell_quantity:" + EUSD_sell_quantity);
        log.info("user_eusd_before_frozen:" + user_eusd_before_frozen);
        if (user_eusd_before_frozen.add(EUSD_sell_quantity).compareTo(user_after_frozen) == 0) {
            result_2 = true;
        } else {
            result_2 = false;
        }
        log.info("result_2是:" + result_2);

        //3.进行比较
        if (result_1 & result_2) {
            Assert.assertTrue(true);
        } else {
            Assert.assertTrue(false);
        }

        log.info("eusd_sell_1_2,测试结束");
    }



    @Test(dependsOnMethods = { "eusd_sell_1_2" }      ,description ="下单成功后，用户【我的】【我的订单】页面第一列显示该订单，且订单状态为“未付款”"      )
    //三、下单成功后，用户【我的】【我的订单】页面第一列显示该订单，且订单状态为“未付款”
    public void eusd_sell_1_3(ITestContext context) throws IOException {
        //1.用户登录
        log.info("eusd_sell_1_3：下单成功后，用户【我的】【我的订单】页面第一列显示该订单，且订单状态为“未付款”");
        String user_token = (String) context.getAttribute("user_token");
        User user = new User(user_token);
        //2.查看订单状态
        int status = user.get_first_orders_status_by_id();
        log.info("eusd_sell_1_3:查看订单状态(1为正确)："+status);
        Assert.assertEquals(1,status);
        log.info("eusd_sell_1_3,测试结束");

    }


    @Test(dependsOnMethods = { "eusd_sell_1_3" } ,description ="下单成功后，承兑商【承兑】【支付订单】页面第一列显示该订单，订单状态为“未付款”"     )
    //四、下单成功后，承兑商【承兑】【支付订单】页面第一列显示该订单，订单状态为“未付款”
    public void eusd_sell_1_4(ITestContext context) throws IOException {
        log.info("eusd_sell_1_4:下单成功后，承兑商【承兑】【支付订单】页面第一列显示该订单，订单状态为“未付款”");
        //1.承兑商登录
        String eu_token = (String) context.getAttribute("eu_token");
        User eu_user = new User(eu_token);
        //2.获取订单状态
        int status = eu_user.get_eu_orders_buy_status_by_id_side_2();
        log.info("eusd_sell_1_4,订单状态是(1为正确)："+status);
        Assert.assertEquals(1,status);
        log.info("eusd_sell_1_4，测试结束");
    }


    @Test(dependsOnMethods = { "eusd_sell_1_4" } ,description ="承兑商15分钟内无操作,自动取消订单")
    //五、承兑商15分钟内无操作,自动取消订单
    public void eusd_sell_1_5(ITestContext context) throws  InterruptedException {
        log.info("eusd_sell_1_5:承兑商15分钟内无操作,自动取消订单");
        Thread.sleep(15*60*1000);
        log.info("eusd_sell_1_5,测试结束");
    }




    @Test(dependsOnMethods = { "eusd_sell_1_5" },description ="自动取消订单后，用户【我的】【我的订单】页面第一列显示该订单，且订单状态为“已取消”")
    //六、自动取消订单后，用户【我的】【我的订单】页面第一列显示该订单，且订单状态为“已取消”
    public void eusd_sell_1_6(ITestContext context) throws IOException {
        log.info("eusd_sell_1_6:自动取消订单后，用户【我的】【我的订单】页面第一列显示该订单，且订单状态为“已过期自动取消”");
        String user_token = (String) context.getAttribute("user_token");
        User user = new User(user_token);
        int status = user.get_first_orders_status_by_id();
        log.info("eusd_sell_1_6:查看订单状态(6为正确)："+status);
        Assert.assertEquals(6,status);
        log.info("eusd_sell_1_6,测试结束");
    }



    @Test(dependsOnMethods = { "eusd_sell_1_6" },description ="自动取消订单后，承兑商【承兑】【支付订单】页面第一列显示该订单，订单状态为“已取消”"   )
    //七、自动取消订单后，承兑商【承兑】【支付订单】页面第一列显示该订单，订单状态为“已取消”
    public void eusd_sell_1_7(ITestContext context) throws IOException {
        log.info("eusd_sell_1_7:自动取消订单后，承兑商【承兑】【支付订单】页面第一列显示该订单，订单状态为“已过期取消”");
        String eu_token = (String) context.getAttribute("eu_token");
        User eu_user = new User(eu_token);
        int status = eu_user.get_eu_orders_buy_status_by_id_side_2();
        log.info("eusd_sell_1_7,订单状态是(6为正确)："+status);
        Assert.assertEquals(6,status);
        log.info("eusd_sell_1_7，测试结束");

    }




    @Test(dependsOnMethods = { "eusd_sell_1_7" },description ="自动取消订单后，用户【资产】【EUSD】模块资产可用余额+X，冻结余额-X" )
    //八、自动取消订单后，用户【资产】【EUSD】模块资产可用余额+X，冻结余额-X
    public void eusd_sell_1_8(ITestContext context) throws IOException, InterruptedException {
        log.info("eusd_sell_1_8:自动取消订单后，用户【资产】【EUSD】模块资产可用余额+X，冻结余额-X");
        Thread.sleep(3*60*1000);
        boolean result_1;
        boolean result_2;
        //1.用户登录
        String user_token = (String) context.getAttribute("user_token");
        User user = new User(user_token);

        //2.获取数据进行比较
        BigDecimal user_after_available= user.get_EUSD_available();     //现有可用余额
        BigDecimal user_after_frozen= user.get_EUSD_frozen();           //现有冻结余额

        BigDecimal user_eusd_before_available= (BigDecimal) context.getAttribute("user_eusd_before_available");     //原有可用余额
        BigDecimal user_eusd_before_frozen= (BigDecimal) context.getAttribute("user_eusd_before_frozen");           //原有冻结余额

        //BigDecimal EUSD_sell_quantity = (BigDecimal) context.getAttribute("EUSD_sell_quantity");     //下单金额

        log.info("eusd_sell_1_8,用户原可用余额=现可用余额,则测试通过,参与计算的数值是:");
        log.info("user_eusd_before_available:"+user_eusd_before_available);
        log.info("user_after_available:"+user_after_available);
        if(user_eusd_before_available.compareTo(user_after_available)==0){
            result_1=true;
        }else{
            result_1=false;
        }
        log.info("result_1是:"+result_1);

        log.info("eusd_sell_1_8,用户原冻结余额=现冻结余额,则测试通过,参与计算的数值是:");
        log.info("user_after_frozen:"+user_after_frozen);
        log.info("user_eusd_before_frozen:"+user_eusd_before_frozen);
        if(user_eusd_before_frozen.compareTo(user_after_frozen)==0){
            result_2=true;
        }else{
            result_2=false;
        }
        log.info("result_2是:"+result_2);
        //3.进行比较
        if(result_1 & result_2){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        log.info("eusd_sell_1_8,测试结束");
    }

}

