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
 * EUSD购买流程一：用户购买EUSD下单后，匹配到承兑商生成订单；用户取消订单，交易失败。
 * 前提：1、普通用户登录，
 *          A、【我的】【收付款设置】里面有添加收付款账户（银行卡、微信、支付宝）
 *          B、EUSD账户有余额
 *      2、确保能匹配到承兑商：
 *          A、平台有承兑商；
 *          B、承兑商【承兑】【收款服务】有开启；
 *          C、【收款设置】里有收款账号；
 *          D、承兑商EUSD资金足够
 *          //E、承兑商【承兑】【今日累积收款金额】有设置数值；
 *       3、承兑商、用户的支付密码都是123456
 * 测试步骤：
 *      1、用户下单，判断是否下单成功（根据是否返回订单ID来判断）
 *      2、下单成功后，用户【我的】【我的订单】页面第一列显示该订单，且订单状态为“未付款”
 *      3、下单成功后，承兑商【承兑】【承兑资产】资金冻结X
 *      4、下单成功后，承兑商【承兑】【收款订单】页面第一列显示该订单，订单状态为“未付款”
 *      5、下单成功后，承兑商【承兑】【今日累积收款】金额变化
 *
 *      6、用户取消订单，查看订单是否取消
 *      7、取消订单后，用户【我的】【我的订单】页面第一列显示该订单，且订单状态为”已取消”
 *      8、取消订单后，承兑商【承兑】【承兑资产】资金解冻X
 *      9、取消订单后，承兑商【承兑】【收款订单】页面第一列显示该订单，订单状态为“已取消”
 *      10、取消订单后，承兑商【承兑】【今日累积收款】金额变化
 *
 * */

public class test_eusd_buy_0 {
    public static Log log = LogFactory.getLog(test_eusd_buy_0.class);

    @BeforeMethod
    public void before_methd() throws InterruptedException {
        log.info("当前线程是："+Thread.currentThread().getId());
        Thread.sleep(5*1000);
        log.info("test_eusd_buy_0,测试用例之间等待5秒");
    }

    @BeforeClass
    public void before_class(ITestContext context) throws IOException {
        log.info("当前线程是："+Thread.currentThread().getId());
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
        log.info("test_eusd_buy_0用例获得的手机账号是：");
        log.info("user_mobile："+user_mobile);
        log.info("eu_user_mibile："+eu_user_mibile);

        //3、存储测试过程中用到的每个账号token、EUSD的各种余额    available/frozen     available/trade
        //普通用户登录
        User user = new User();
        String user_token = user.login_and_return_token(user_mobile);
        BigDecimal user_eusd_before_available =user.get_EUSD_available();
        BigDecimal user_eusd_before_frozen =user.get_EUSD_frozen();
        context.setAttribute("user_token",user_token);
        context.setAttribute("user_eusd_before_available",user_eusd_before_available);
        context.setAttribute("user_eusd_before_frozen",user_eusd_before_frozen);

        //承兑商登录
        User eu_user = new User();
        String eu_token = user.login_and_return_token(eu_user_mibile);
        BigDecimal eu_eusd_before_available =eu_user.get_EUSD_available();
        BigDecimal eu_eusd_before_trade =eu_user.v1_exchange_info_return_trade();
        BigDecimal eu_eusd_before_buy_rmb_today = eu_user.v1_exchange_info_return_buy_rmb_day();        //【承兑】【今日累积收款】

        context.setAttribute("eu_token",eu_token);
        context.setAttribute("eu_eusd_before_available",eu_eusd_before_available);
        context.setAttribute("eu_eusd_before_trade",eu_eusd_before_trade);      //forzen   变成trade
        context.setAttribute("eu_eusd_before_buy_rmb_today",eu_eusd_before_buy_rmb_today);
        log.info("content,setAttribute,eu_token是："+eu_token);
        log.info("content,setAttribute,eu_eusd_before_available："+eu_eusd_before_available);
        log.info("content,setAttribute,eu_eusd_before_trade："+eu_eusd_before_trade);
        log.info("content,setAttribute,eu_eusd_before_buy_rmb_today："+eu_eusd_before_buy_rmb_today);

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
        log.info("当前线程是："+Thread.currentThread().getId());
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





    @Test(description="用户下单，判断是否下单成功（根据是否返回订单ID来判断）" )
    //一、用户下单，判断是否下单成功（根据是否返回订单ID来判断）
    public void eusd_buy_0_1(ITestContext context) throws IOException {
        log.info("当前线程是："+Thread.currentThread().getId());
        log.info("eusd_buy_0_1，根据订单ID判断是否下单成功");
        //1、用户登录
        String user_token = (String) context.getAttribute("user_token");
        User user = new User(user_token);
        //2、下单并获取订单id
        BigInteger id =user.execute_buy_return_id();
        log.info("eusd_buy_0_1，查看下单返回订单的id是："+id);
        if (id!=null){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }

        //查看下单匹配的承兑商与制定的承兑商是否是同一个
        log.info("eusd_buy_0_1，此处需要查看匹配的承兑商，是否是开启的那个承兑商");
        //预计匹配的承兑商
        String eu_user_mibile = (String) context.getAttribute("eu_user_mibile");
        //实际匹配的承兑商
        String get_eu_user_mobile= user.according_to_orders_id_return_eu_mobile();
        log.info("eusd_buy_0_1，预计匹配的承兑商是："+eu_user_mibile);
        log.info("eusd_buy_0_1，实际匹配的承兑商是："+get_eu_user_mobile);


        //2、保存数据供后续请求使用
        //订单唯一ID
        context.setAttribute("id",id);
        //EUSD_buy_quantity是下单的金额
        context.setAttribute("EUSD_buy_quantity",user.get_EUSD_buy_quantity());
        log.info("eusd_buy_0_1，测试结束");
    }


    @Test(dependsOnMethods = { "eusd_buy_0_1" },description = "下单成功后，用户【我的】【我的订单】页面第一列显示该订单，且订单状态为“未付款”")
    //二、下单成功后，用户【我的】【我的订单】页面第一列显示该订单，且订单状态为“未付款”
    public void eusd_buy_0_2(ITestContext context) throws IOException {
        log.info("当前线程是："+Thread.currentThread().getId());
        log.info("eusd_buy_0_2：下单成功后，用户【我的】【我的订单】页面第一列显示该订单，且订单状态为“未付款”");
        String user_token = (String) context.getAttribute("user_token");
        User user = new User(user_token);
        int status = user.get_first_orders_status_by_id();        //查看普通用户，订单列表第一个订单，返回订单的状态，订单状态1为未付款
        log.info("eusd_buy_0_2:查看订单状态（1为正确）："+status);
        Assert.assertEquals(1,status);
        log.info("eusd_buy_0_2,测试结束");
    }





    @Test(dependsOnMethods ="eusd_buy_0_2" ,description ="下单成功后，承兑商【承兑】【承兑资产】资金冻结X")
    //三、下单成功后，承兑商【承兑】【承兑资产】资金冻结X
    public void eusd_buy_0_3(ITestContext context)throws IOException {
        log.info("当前线程是："+Thread.currentThread().getId());
        log.info("eusd_buy_0_3:下单成功后，承兑商【承兑】【承兑资产】资金冻结X");
        //1、获取承兑商测试前EUSD冻结余额
        BigDecimal eu_eusd_before_trade = (BigDecimal) context.getAttribute("eu_eusd_before_trade");

        //2、获取承兑商token，登录查看下单后承兑商【承兑】EUSD冻结余额
        String eu_token= (String) context.getAttribute("eu_token");
        User eu_user = new User(eu_token);
        BigDecimal eu_eusd_after_frozen = eu_user.v1_exchange_info_return_trade();         //查看承兑商【承兑】页面的EUSD的冻结数值

        //3、获取用户下单的数量
        BigDecimal EUSD_buy_quantity = (BigDecimal) context.getAttribute("EUSD_buy_quantity");

        //4、判断:用户下单后承兑商冻结余额 + 下单数量 = 下单前余额
        log.info("eusd_buy_0_3,用户下单后，承兑商冻结余额 + 下单数量 = 下单前余额,参与计算的数值是：");
        log.info("eusd_buy_0_3,eu_eusd_before_trade是："+eu_eusd_before_trade);
        log.info("eusd_buy_0_3,EUSD_buy_quantity是："+EUSD_buy_quantity);
        log.info("eusd_buy_0_3,eu_eusd_after_frozen是："+eu_eusd_after_frozen);
        if(eu_eusd_before_trade.add(EUSD_buy_quantity).compareTo(eu_eusd_after_frozen)==0){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        context.setAttribute("eu_eusd_after_frozen",eu_eusd_after_frozen);
        log.info("eusd_buy_0_3，测试结束");
    }






    @Test(dependsOnMethods = "eusd_buy_0_3",description = "下单成功后，承兑商【承兑】【收款订单】页面第一列显示该订单，订单状态为“未付款”")
    //四、下单成功后，承兑商【承兑】【收款订单】页面第一列显示该订单，订单状态为“未付款”
    public void eusd_buy_0_4(ITestContext context) throws IOException {
        log.info("当前线程是："+Thread.currentThread().getId());
        log.info("eusd_buy_0_4，下单成功后，承兑商【承兑】【收款订单】页面第一列显示该订单，订单状态为“未付款”");
        String  eu_token = (String) context.getAttribute("eu_token");
        User eu_user = new User(eu_token);
        int status = eu_user.get_eu_orders_buy_status_by_id();
        log.info("eusd_buy_0_4,订单状态是(1为正确）："+status);
        Assert.assertEquals(1,status);
        log.info("eusd_buy_0_4，测试结束");
    }



    @Test(dependsOnMethods = { "eusd_buy_0_4" },description = "用户取消订单，查看是否取消成功")
    //五 用户取消订单，查看是否取消成功
    public void eusd_buy_0_5(ITestContext context) throws IOException, InterruptedException {
        log.info("当前线程是："+Thread.currentThread().getId());
        log.info("eusd_buy_0_5,用户取消订单，查看是否取消成功");
        Thread.currentThread().sleep(5*1000) ;    //等待5秒，取消订单不能操作太快
        BigInteger id_for_cancel = (BigInteger) context.getAttribute("id");
        String user_token = (String) context.getAttribute("user_token");
        User user = new User(user_token);   //user构造方法，传入token，获取user对象
        int return_code =user.by_cancel(id_for_cancel);
        log.info("eusd_buy_0_5：普通用户取消订单，调用取消方法，查看返回(200说明订单取消成功）："+return_code);
        Assert.assertEquals(200,return_code);
        log.info("eusd_buy_0_5,测试结束");
    }


    @Test(dependsOnMethods = { "eusd_buy_0_5" },description ="用户取消后，查看【我的】【我的订单】页面第一个订单状态是否为已取消" )
    //六、用户取消后，查看【我的】【我的订单】页面第一个订单状态是否为已取消
    public void eusd_buy_0_6(ITestContext context) throws IOException {
        log.info("当前线程是："+Thread.currentThread().getId());
        log.info("eusd_buy_0_6：订单取消后，查看普通用户的订单是否取消，订单状态是否为已取消");
        String user_token = (String) context.getAttribute("user_token");
        User user = new User(user_token);
        int status = user.get_first_orders_status_by_id();
        log.info("eusd_buy_0_6：订单取消后，查看普通用户的订单是否取消，订单状态（4为正确）是："+status);
        Assert.assertEquals(4,status);
        log.info("eusd_buy_0_6，测试结束");
    }


    @Test(dependsOnMethods = { "eusd_buy_0_6" },description = "用户取消后，查看承兑商，【承兑】【承兑资产】资金是否解冻")
    //七、用户取消后，查看承兑商，【承兑】【承兑资产】资金是否解冻；
    public void eusd_buy_0_7(ITestContext context) throws IOException {
        log.info("当前线程是："+Thread.currentThread().getId());
        log.info("eusd_buy_0_7,用户取消后，查看承兑商，【承兑】【承兑资产】资金是否解冻；");

        //1、获取承兑商冻结后的余额（用户下单匹配，承兑商余额冻结）  after
        BigDecimal eu_eusd_after_frozen= (BigDecimal) context.getAttribute("eu_eusd_after_frozen");

        //2、获取用户下单数值
        BigDecimal EUSD_buy_quantity = (BigDecimal) context.getAttribute("EUSD_buy_quantity");

        //3、获取承兑商现在的冻结余额(用户执行取消，此时会取消冻结，所以数值会比之前的小）
        String eu_token = (String) context.getAttribute("eu_token");
        User eu_user = new User(eu_token);
        BigDecimal eu_eusd_final_frozen = eu_user.v1_exchange_info_return_trade();

        log.info("eusd_buy_0_7,冻结后的余额  +  用户下单金额（取消后被解冻金额）  = 现冻结余额，参与计算的数值是：");
        log.info("eusd_buy_0_7,eu_eusd_final_frozen是："+eu_eusd_final_frozen);
        log.info("eusd_buy_0_7,EUSD_buy_quantity："+EUSD_buy_quantity);
        log.info("eusd_buy_0_7,eu_eusd_after_frozen："+eu_eusd_after_frozen);

        //4、判断   冻结后的余额  +  用户下单金额（取消后被解冻金额）  = 现冻结余额
        if(eu_eusd_final_frozen.add(EUSD_buy_quantity).compareTo(eu_eusd_after_frozen)==0){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }

    }


    @Test(dependsOnMethods = { "eusd_buy_0_7" },description = "用户取消后，查看承兑商，【承兑】【收款订单】页面，第一个订单的状态是否为已取消")
    //八、用户取消后，查看承兑商，【承兑】【收款订单】页面，第一个订单的状态是否为已取消；
    public void eusd_buy_0_8(ITestContext context) throws IOException {
        log.info("当前线程是："+Thread.currentThread().getId());
        log.info("eusd_buy_0_8，下单成功后，承兑商【承兑】【收款订单】页面第一列显示该订单，订单状态为“已取消”");
        String  eu_token = (String) context.getAttribute("eu_token");
        User eu_user = new User(eu_token);
        int status = eu_user.get_eu_orders_buy_status_by_id();
        log.info("eusd_buy_0_8,订单状态是(4为正确）："+status);
        Assert.assertEquals(4,status);
        log.info("eusd_buy_0_8，测试结束");

    }
}