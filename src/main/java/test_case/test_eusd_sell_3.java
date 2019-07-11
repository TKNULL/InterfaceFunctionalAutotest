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
 * EUSD出售流程三：普通用户,出售下单后，匹配到承兑商生成订单；承兑商确认付款；用户15分钟内无操作，订单状态变更为“已逾期”； 用户点击收款，交易完成；
 * 前提：1、普通用户登录，
 *          A、【我的】【收付款设置】里面有添加收付款账户（银行卡、微信、支付宝）
 *          B、EUSD账户有可用余额，且没有冻结余额
 *          C、设置支付密码
 *      2、确保能匹配到承兑商：
 *          A、平台有承兑商；
 *          B、承兑商【承兑】【收款服务】有开启；
 *          C、【收款设置】里有收款账号；
 *          D、承兑商EUSD资金足够
 *          E、承兑商【承兑】【今日累积收款金额】有设置数值；
 *       3、承兑商、用户的支付密码都是123456
 * 测试步骤：
 *      1、用户下单，查看是否下单成功；下单成功后，用户【资产】【EUSD】模块资产可用余额-X，冻结余额+X
 *      2、下单成功后，用户【我的】【我的订单】页面第一列显示该订单，且订单状态为“未付款”
 *      3、下单成功后，承兑商【承兑】【支付订单】页面第一列显示该订单，订单状态为“未付款”
 *
 *
 *      4、承兑商点击【我已付款】，查看是否付款成功（【承兑】【支付订单】【订单详情】页面点击“我已付款”）
 *      5、承兑商付款成功后，承兑商【承兑】【支付订单】页面第一列显示该订单，订单状态为“已付款”
 *      6、承兑商付款成功后，用户【我的】【我的订单】页面第一列显示该订单，订单状态为“已付款”
 *
 *      7、用户15分钟内无操作,系统自动完成订单       ====》订单状态更改~~~~~~~~~~~~~~
 *      8、用户收款成功后，用户【资产】【EUSD】冻结余额-X（用户下单的时候，可用余额先转到冻结余额，承兑商付款的时候，冻结余额减少，转到承兑商账户）
 *      9、用户收款成功后，【我的】【我的订单】页面第一列显示该订单，订单状态为"已确认”
 *      10、用户收款成功后，承兑商【承兑】【承兑资产】EUSD数值可用余额不变，冻结余额+X
 *      11、用户收款成功后，承兑商【承兑】【承兑资产】：4分钟后，冻结余额——X，可用余额+x
 *      12、用户收款成功后，承兑商【承兑】【支付订单】页面第一列显示该订单，订单状态为"已确认“
 *      13、用户收款成功后，承兑商【承兑】【支付】今日累积购买金额+X
 *
 *
 * */

public class test_eusd_sell_3 {
    public static Log log = LogFactory.getLog(test_eusd_sell_3.class);
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
        log.info("test_eusd_sell_3用例获得的手机账号是：");
        log.info("user_mobile："+user_mobile);
        log.info("eu_user_mibile："+eu_user_mibile);

        //3、存储账号token、EUSD的各种余额    available/frozen     available/trade
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




    @Test( description ="用户下单，查看是否下单成功(根据是否返回订单ID来判断)")
    //一、用户下单，查看是否下单成功(根据是否返回订单ID来判断)
    public void eusd_sell_3_1(ITestContext context) throws IOException {
        log.info("eusd_sell_3_1,用户下单，查看是否下单成功");
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



    @Test(dependsOnMethods = { "eusd_sell_3_1" }   ,description ="下单成功后，用户[资产][EUSD]可用余额-x,冻结金额+x"  )
    //二、下单成功后，用户[资产][EUSD]可用余额-x,冻结金额+x
    public void eusd_sell_3_2(ITestContext context) throws IOException, InterruptedException {
        log.info("eusd_sell_3_2:下单成功后，用户[资产][EUSD]可用余额-x,冻结金额+x");
        Thread.sleep(3 * 60 * 1000);
        boolean result_1;
        boolean result_2;
        //1.用户登录
        String user_token = (String) context.getAttribute("user_token");
        User user = new User(user_token);

        //2.获取数据进行比较
        BigDecimal user_after_available = user.get_EUSD_available();     //现有可用余额
        BigDecimal user_after_frozen = user.get_EUSD_frozen();           //现有冻结余额
        context.setAttribute("user_after_frozen",user_after_frozen);

        BigDecimal user_eusd_before_available = (BigDecimal) context.getAttribute("user_eusd_before_available");     //原有可用余额
        BigDecimal user_eusd_before_frozen = (BigDecimal) context.getAttribute("user_eusd_before_frozen");           //原有冻结余额

        BigDecimal EUSD_sell_quantity = (BigDecimal) context.getAttribute("EUSD_sell_quantity");     //下单金额

        log.info("eusd_sell_3_2,用户原可用余额-出售金额=现可用余额,则测试通过,参与计算的数值是:");
        log.info("user_eusd_before_available:" + user_eusd_before_available);
        log.info("EUSD_sell_quantity:" + EUSD_sell_quantity);
        log.info("user_after_available:" + user_after_available);
        if (user_eusd_before_available.subtract(EUSD_sell_quantity).compareTo(user_after_available) == 0) {
            result_1 = true;
        } else {
            result_1 = false;
        }
        log.info("result_1是:" + result_1);
        log.info("eusd_sell_3_2,用户原冻结余额+出售金额=现冻结余额,则测试通过,参与计算的数值是:");
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
        log.info("eusd_sell_3_2,测试结束");
    }



    @Test(dependsOnMethods = { "eusd_sell_3_2" },description ="下单成功后，用户【我的】【我的订单】页面第一列显示该订单，且订单状态为“未付款”" )
    //三、下单成功后，用户【我的】【我的订单】页面第一列显示该订单，且订单状态为“未付款”
    public void eusd_sell_3_3(ITestContext context) throws IOException {
        //1.用户登录
        log.info("eusd_sell_3_3：下单成功后，用户【我的】【我的订单】页面第一列显示该订单，且订单状态为“未付款”");
        String user_token = (String) context.getAttribute("user_token");
        User user = new User(user_token);
        //2.查看订单状态
        int status = user.get_first_orders_status_by_id();
        log.info("eusd_sell_3_3:查看订单状态(1为正确)："+status);
        Assert.assertEquals(1,status);
        log.info("eusd_sell_3_3,测试结束");

    }


    @Test(dependsOnMethods = { "eusd_sell_3_3" },description ="下单成功后，承兑商【承兑】【支付订单】页面第一列显示该订单，订单状态为“未付款”" )
    //四、下单成功后，承兑商【承兑】【支付订单】页面第一列显示该订单，订单状态为“未付款”
    public void eusd_sell_3_4(ITestContext context) throws IOException {
        log.info("eusd_sell_3_4:下单成功后，承兑商【承兑】【支付订单】页面第一列显示该订单，订单状态为“未付款”");
        //1.承兑商登录
        String eu_token = (String) context.getAttribute("eu_token");
        User eu_user = new User(eu_token);
        //2.获取订单状态
        int status = eu_user.get_eu_orders_buy_status_by_id_side_2();
        log.info("eusd_sell_3_4,订单状态是(1为正确)："+status);
        Assert.assertEquals(1,status);
        log.info("eusd_sell_3_4，测试结束");
    }






    @Test(dependsOnMethods = { "eusd_sell_3_4" },description ="承兑商点击【我已付款】，查看是否付款成功" )
    //五、承兑商点击【我已付款】，查看是否付款成功
    public void eusd_sell_3_5(ITestContext context) throws IOException {
        log.info("eusd_sell_3_5,承兑商点击【我已付款】，查看是否付款成功");
        String eu_token = (String) context.getAttribute("eu_token");
        BigInteger id = (BigInteger) context.getAttribute("id");
        User  eu_user = new User(eu_token);
        int code=  eu_user.sell_id_pay(id);
        log.info("eusd_sell_3_5,返回数据（200表示成功）是："+code);
        Assert.assertEquals(200,code);
        log.info("eusd_sell_3_5,测试完成");
    }



    @Test(dependsOnMethods = { "eusd_sell_3_5" },description ="承兑商付款成功后，承兑商【承兑】【支付订单】页面第一列显示该订单，订单状态为“已付款”" )
    //六、承兑商付款成功后，承兑商【承兑】【支付订单】页面第一列显示该订单，订单状态为“已付款”
    public void eusd_sell_3_6(ITestContext context) throws IOException {
        log.info("eusd_sell_3_6:下单成功后，承兑商【承兑】【支付订单】页面第一列显示该订单，订单状态为“已付款”");
        String eu_token = (String) context.getAttribute("eu_token");
        User eu_user = new User(eu_token);
        //获取订单状态
        int status = eu_user.get_eu_orders_buy_status_by_id_side_2();
        log.info("eusd_sell_3_6,订单状态是(2为正确)："+status);
        Assert.assertEquals(2,status);
        log.info("eusd_sell_3_6，测试结束");
    }



    @Test(dependsOnMethods = { "eusd_sell_3_6" },description ="承兑商付款成功后，用户【我的】【我的订单】页面第一列显示该订单，订单状态为”已付款“" )
    //七、承兑商付款成功后，用户【我的】【我的订单】页面第一列显示该订单，订单状态为"已付款"
    public void eusd_sell_3_7(ITestContext context) throws IOException {
        log.info("eusd_sell_3_7:承兑商付款成功后，用户【我的】【我的订单】页面第一列显示该订单，订单状态为”已付款“");
        String user_token = (String) context.getAttribute("user_token");
        User eu_user = new User(user_token);
        int status = eu_user.get_first_orders_status_by_id();
        log.info("eusd_sell_3_7,点击确认支付后，查看订单状态(2为正确)："+status);
        Assert.assertEquals(2,status);
        log.info("eusd_sell_3_7，测试结束");
    }


    @Test(dependsOnMethods = { "eusd_sell_3_7" },description ="用户5分钟内无操作,订单状态更改为”已逾期“，用户点击确认收款，查看是否成功" )
    //八、用户6分钟内无操作,系统自动完成订单
    public void eusd_sell_3_8(ITestContext context) throws InterruptedException, IOException {
        log.info("eusd_sell_3_8:用户5分钟内无操作,订单状态更改为”已逾期“，用户点击确认收款，查看是否成功");
        Thread.sleep(6*60*1000);
        //用户点击确认
        log.info("eusd_sell_3_3:用户5分钟内无操作,订单状态更改为”已逾期“，用户点击确认收款，查看是否成功");
        String user_token = (String) context.getAttribute("user_token");
        BigInteger id = (BigInteger) context.getAttribute("id");
        User user = new User(user_token);
        int code = user.v1_sell_id_confirm(id);
        log.info("eusd_sell_3_8,返回的状态码（200表示正确）是："+code);
        Assert.assertEquals(200,code);
        log.info("eusd_sell_3_8,测试结束");
    }




    @Test(dependsOnMethods = { "eusd_sell_3_8" },description ="用户点击收款成功后，【我的】【我的订单】页面第一列显示该订单，订单状态为”已确认“" )
    //九、系统自动完成订单（用户收款成功后），【我的】【我的订单】页面第一列显示该订单，订单状态为"已确认"
    public void eusd_sell_3_9(ITestContext context) throws IOException {
        log.info("eusd_sell_3_9：用户点击收款成功后，【我的】【我的订单】页面第一列显示该订单，订单状态为“已确认”");
        String user_token= (String) context.getAttribute("user_token");
        User  user = new User(user_token);
        int status = user.get_first_orders_status_by_id();
        log.info("eusd_sell_3_9:查看订单状态(3为正确)："+status);
        Assert.assertEquals(3,status);
        log.info("eusd_sell_3_9,测试结束");
    }



    @Test(dependsOnMethods = { "eusd_sell_3_9" },description ="用户点击收款成功后，承兑商【承兑】【支付订单】页面第一列显示该订单，订单状态为”已确认“")
    //十、系统自动完成订单（用户收款成功后），，承兑商【承兑】【支付订单】页面第一列显示该订单，订单状态为”已确认“
    public void eusd_sell_3_10(ITestContext context) throws IOException {
        log.info("eusd_sell_3_10:用户收款成功后，承兑商【承兑】【支付订单】页面第一列显示该订单，订单状态为”已确认“");
        String eu_token = (String) context.getAttribute("eu_token");
        User eu_user = new User(eu_token);
        //获取订单状态
        int status = eu_user.get_eu_orders_buy_status_by_id_side_2();
        log.info("eusd_sell_3_10,订单状态是(3为正确)："+status);
        Assert.assertEquals(3,status);
        log.info("eusd_sell_3_10，测试结束");

    }






    @Test(dependsOnMethods = { "eusd_sell_3_10" },description ="用户点击收款成功后，用户【资产】【EUSD】可用余额-X")
    //十一、系统自动完成订单（用户收款成功后），用户【资产】【EUSD】可用余额-X
    //用户收款成功后，用户【资产】【EUSD】冻结余额-X（用户下单的时候，可用余额先转到冻结余额，承兑商付款的时候，冻结余额减少，转到承兑商账户）
    public void eusd_sell_3_11(ITestContext context) throws IOException {
        log.info("eusd_sell_3_11:用户点击收款成功后，用户【资产】【EUSD】冻结余额-X（用户下单的时候，可用余额先转到冻结余额，承兑商付款的时候，冻结余额减少，转到承兑商账户）");
        String user_token = (String) context.getAttribute("user_token");
        User user = new User(user_token);

        //前冻结余额(用户下单后的冻结余额) - 下单出售金额 = 现冻结余额
        BigDecimal user_after_frozen= (BigDecimal) context.getAttribute("user_after_frozen");
        BigDecimal EUSD_sell_quantity = (BigDecimal) context.getAttribute("EUSD_sell_quantity");
        BigDecimal user_final_frozen = user.get_EUSD_frozen();

        log.info("eusd_sell_3_11,前冻结余额 - 下单出售金额 = 现冻结余额,则测试通过 参与计算的金额是：");
        log.info("user_after_frozen="+user_after_frozen);
        log.info("EUSD_sell_quantity="+EUSD_sell_quantity);
        log.info("user_final_frozen="+user_final_frozen);
        log.info("user_after_frozen-EUSD_sell_quantity=user_final_frozen,表示测试成功");
        if(user_after_frozen.subtract(EUSD_sell_quantity).compareTo(user_final_frozen)==0){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        log.info("eusd_sell_3_11,测试结束");
    }







    @Test(dependsOnMethods = { "eusd_sell_3_11" },description ="用户点击收款成功后，承兑商【承兑】【承兑资产】EUSD数值可用余额不变，冻结余额+X  ")
    //十二、系统自动完成订单（用户收款成功后），承兑商【承兑】【承兑资产】EUSD数值可用余额不变，冻结余额+X
    public void eusd_sell_3_12(ITestContext context) throws IOException, InterruptedException {
        log.info("eusd_sell_3_12:用户点击收款成功后，承兑商【承兑】【承兑资产】EUSD数值可用余额不变，冻结余额+X，");
        Thread.sleep(60*1000);      //0521调试发现，用户点击【我已收款】后，承兑商冻结余额并不会立即增加，所以等待1分钟。
        String eu_token = (String) context.getAttribute("eu_token");
        User eu_user = new User(eu_token);
        //获取承兑商现有冻结余额，此处保存最新的冻结余额，便于4分钟解冻后作比较
        BigDecimal final_euuser_after_trade=eu_user.v1_exchange_info_return_trade();
        context.setAttribute("final_euuser_after_trade",final_euuser_after_trade);
        //获取承兑商原有冻结余额、用户下单金额
        BigDecimal eu_eusd_before_trade = (BigDecimal) context.getAttribute("eu_eusd_before_trade");     //承兑商，【承兑页面】冻结余额
        BigDecimal EUSD_sell_quantity = (BigDecimal) context.getAttribute("EUSD_sell_quantity");
        log.info("eusd_sell_3_12,原有冻结金额 + 下单金额 = 现有冻结金额,测试通过,参与计算的数值是：");
        log.info("eusd_sell_3_12，eu_eusd_before_trade=："+eu_eusd_before_trade);   // null
        log.info("eusd_sell_3_12，EUSD_sell_quantity=："+EUSD_sell_quantity);
        log.info("eusd_sell_3_12，final_euuser_after_trade=："+final_euuser_after_trade);
        log.info(eu_eusd_before_trade.add(EUSD_sell_quantity));      //96004
        log.info("获取的值是"+eu_eusd_before_trade.add(EUSD_sell_quantity).compareTo(final_euuser_after_trade));

        Assert.assertEquals((eu_eusd_before_trade.add(EUSD_sell_quantity)),final_euuser_after_trade);
        log.info("eusd_sell_3_12,测试结束");
    }


    @Test(dependsOnMethods = { "eusd_sell_3_12" },description ="用户点击收款成功后，承兑商【承兑】【承兑资产】EUSD数值可用余额不变，冻结余额+X：4分钟后，冻结余额—X，可用余额+x ")
    //十三、系统自动完成订单（用户收款成功后），承兑商【承兑】【承兑资产】EUSD数值可用余额不变，冻结余额+X：4分钟后，冻结余额—X，可用余额+x
    public void eusd_sell_3_13(ITestContext context) throws IOException, InterruptedException {
        log.info("eusd_sell_3_13:用户点击收款成功后，承兑商【承兑】【承兑资产】EUSD数值可用余额不变，冻结余额+X：4分钟后，冻结余额—X，可用余额+x");
        boolean result1;
        boolean result2;
        Thread.sleep(4*60*1000);
        //承兑商登陆
        String eu_token = (String) context.getAttribute("eu_token");
        User eu_user = new User(eu_token);
        //获取下单金额
        BigDecimal EUSD_sell_quantity = (BigDecimal) context.getAttribute("EUSD_sell_quantity");

        //1、可用余额+X
        BigDecimal euuser_after_availabl =eu_user.v1_exchange_info_return_available();  //获取承兑商现有可用余额
        BigDecimal eu_eusd_before_available = (BigDecimal) context.getAttribute("eu_eusd_before_available");  //获取承兑商原有可用余额
        log.info("eusd_sell_3_13,承兑商可用余额增加，参数计算的数值是：");
        log.info("eusd_sell_3_13,eu_eusd_before_available："+eu_eusd_before_available);        // null
        log.info("eusd_sell_3_13,EUSD_sell_quantity是："+EUSD_sell_quantity);
        log.info("eusd_sell_3_13,euuser_after_availabl是："+euuser_after_availabl);
        if(eu_eusd_before_available.add(EUSD_sell_quantity).compareTo(euuser_after_availabl)==0){
            result1=true;
        }else{
            result1=false;
        }
        log.info("result1:"+result1);


        // 2、冻结余额-X
        BigDecimal euuser_after_trade =eu_user.v1_exchange_info_return_trade();     //获取承兑商现有冻结余额
        BigDecimal final_euuser_after_trade= (BigDecimal) context.getAttribute("final_euuser_after_trade");      //获取承兑商原有冻结余额
        log.info("eusd_sell_3_13,承兑商冻结余额减少，参数计算的数值是：");
        log.info("eusd_sell_3_13,euuser_after_trade是："+euuser_after_trade);
        log.info("eusd_sell_3_13,EUSD_sell_quantity是："+EUSD_sell_quantity);
        log.info("eusd_sell_3_13,final_euuser_after_trade是："+final_euuser_after_trade);
        if(EUSD_sell_quantity.add(euuser_after_trade).compareTo(final_euuser_after_trade)==0){
            result2=true;
        }else{
            result2=false;
        }
        log.info("result2:"+result2);

        //3 判断
        if(result2 & result2){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        log.info("eusd_sell_3_13，测试结束");
    }





    @Test(dependsOnMethods = { "eusd_sell_3_13" },description ="用户点击收款成功后，承兑商【承兑】【支付】今日累积支付金额+X")
    //十四、系统自动完成订单（用户收款成功后），承兑商【承兑】【支付】今日累积支付金额+X
    public void eusd_sell_3_14(ITestContext context) throws IOException {
        log.info("eusd_sell_3_14:用户点击收款成功后，承兑商【承兑】【支付】今日累积支付金额+X");
        String eu_token = (String) context.getAttribute("eu_token");
        User eu_user = new User(eu_token);
        BigDecimal eu_eusd_before_buy_rmb_today = (BigDecimal) context.getAttribute("eu_eusd_before_buy_rmb_today");
        BigDecimal after_sell_rmb_today = eu_user.v1_exchange_info_return_sell_rmb_day();
        //原今日累积支付金额  +  下单金额 == 现今日累积支付金额，说明支付金额有增加，测试通过        =》数值不相等则为正确，后续获取汇率再计算
        //eu_eusd_before_buy_rmb_today > after_sell_rmb_today
        log.info("eusd_sell_3_14,参与比较的两个数字（after_sell_rmb_today>sell_rmb_today说明测试通过）是：");
        log.info("eusd_sell_3_14,after_sell_rmb_today是："+after_sell_rmb_today);
        log.info("eusd_sell_3_14,eu_eusd_before_buy_rmb_today："+eu_eusd_before_buy_rmb_today);
        if((after_sell_rmb_today.compareTo(eu_eusd_before_buy_rmb_today))>0){
            Assert.assertTrue(true);
        }else{
            Assert.assertFalse(false);
        }
        log.info("eusd_sell_3_14,测试结束");
    }

}
