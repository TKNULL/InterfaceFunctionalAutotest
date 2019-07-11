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
 * EUSD购买流程四：用户购买EUSD下单后，匹配到承兑商生成订单；用户点击【我已付款】；承兑商5分钟内无操作，订单状态变成已逾期；承兑商确认收款，订单完成；
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
 *      1、用户下单，查看是否下单成功
 *      2、下单成功后，用户【我的】【我的订单】页面第一列显示该订单，且订单状态为“未付款”
 *      3、下单成功后，承兑商【承兑】【承兑资产】资金冻结X
 *      4、下单成功后，承兑商【承兑】【收款订单】页面第一列显示该订单，订单状态为“未付款”
 *
 *      5、用户点击【我已付款】，查看付款是否成功
 *      6、用户付款后，用户【我的】【我的订单】页面第一列显示该订单，且订单状态为”已付款待确认”
 *      7、用户付款后，承兑商【承兑】【收款订单】页面第一列显示该订单，订单状态为“已付款”
 *      8、用户付款后，承兑商【承兑】【今日累积收款】金额不变（保存今日累积收款金额，便于参与计算）
 *
 *      9、承兑商5分钟内无操作，订单状态变为“已逾期”（只是前端显示，后台接口没有变化）
 *      10、承兑商点击确认收款后，用户【资产】【EUSD】EUSD的数值不变，购买的EUSD，先冻结X
 *      11、承兑商确认收款后，用户【资产】【EUSD】EUSD，4分钟后解冻,可用余额+X
 *      12、承兑商确认收款后，用户【我的】【我的订单】订单状态为“已确认”
 *      13、承兑商确认收款后，承兑商【承兑】【承兑资产】EUSD数值减少X
 *      14、承兑商确认收款后，承兑商【承兑】【收款订单】页面第一列显示该订单，且订单状态为“已确认”
 *      15、承兑商确认收款后，承兑商【承兑】【今日累积收款】金额增加X
 *
 * */





public class test_eusd_buy_3 {
    public static Log log = LogFactory.getLog(test_eusd_buy_3.class);

    @BeforeMethod
    public void before_methd() throws InterruptedException {
        Thread.sleep(5*1000);
        log.info("test_eusd_buy_2,等待5秒");
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
        log.info("test_eusd_buy_3用例获得的手机账号是：");
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
        BigDecimal eu_eusd_before_available =eu_user.v1_exchange_info_return_available();
        BigDecimal eu_eusd_before_trade =eu_user.v1_exchange_info_return_trade();
        BigDecimal eu_eusd_before_buy_rmb_today = eu_user.v1_exchange_info_return_buy_rmb_day();        //【承兑】【今日累积收款】

        context.setAttribute("eu_token",eu_token);
        context.setAttribute("eu_eusd_before_available",eu_eusd_before_available);
        context.setAttribute("eu_eusd_before_trade",eu_eusd_before_trade);      //forzen   变成trade
        context.setAttribute("eu_eusd_before_buy_rmb_today",eu_eusd_before_buy_rmb_today);

        log.info("eusd_buy_2_1,测试前记录所有账号的数据信息：");
        log.info("user_token:"+user_token);
        log.info("user_eusd_before_available:"+user_eusd_before_available);
        log.info("user_eusd_before_frozen:"+user_eusd_before_frozen);
        log.info("eu_token:"+eu_token);
        log.info("eu_eusd_before_available:"+eu_eusd_before_available);
        log.info("eu_eusd_before_trade:"+eu_eusd_before_trade);
        log.info("eu_eusd_before_buy_rmb_today:"+eu_eusd_before_buy_rmb_today);


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


    @Test(description = "用户下单，判断是否下单成功（根据是否返回订单ID来判断）")
    //一、用户下单，判断是否下单成功（根据是否返回订单ID来判断）
    public void eusd_buy_3_1(ITestContext context) throws IOException {
        log.info("eusd_buy_3_1，根据订单ID判断是否下单成功");
        //1、用户登录
        String user_token = (String) context.getAttribute("user_token");
        User user = new User(user_token);
        //2、下单并获取订单id
        BigInteger id =user.execute_buy_return_id();
        log.info("eusd_buy_3_1，查看下单返回订单的id是："+id);
        if (id!=null){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }

        //查看下单匹配的承兑商与制定的承兑商是否是同一个
        log.info("eusd_buy_3_1，此处需要查看匹配的承兑商，是否是开启的那个承兑商");
        //预计匹配的承兑商
        String eu_user_mobile = (String) context.getAttribute("eu_user_mobile");
        //实际匹配的承兑商
        String get_eu_user_mobile= user.according_to_orders_id_return_eu_mobile();
        log.info("eusd_buy_3_1，预计匹配的承兑商是："+eu_user_mobile);
        log.info("eusd_buy_3_1，实际匹配的承兑商是："+get_eu_user_mobile);


        //2、保存数据供后续请求使用
        //订单唯一ID
        context.setAttribute("id",id);
        //EUSD_buy_quantity是下单的金额，用于在取消订单后，参与计算承兑商账户是否有变化
        context.setAttribute("EUSD_buy_quantity",user.get_EUSD_buy_quantity());
        log.info("eusd_buy_3_1，测试结束");
    }




    @Test(dependsOnMethods = { "eusd_buy_3_1" },description ="下单成功后，用户【我的】【我的订单】页面第一列显示该订单，且订单状态为“未付款”" )
    //二、下单成功后，用户【我的】【我的订单】页面第一列显示该订单，且订单状态为“未付款”
    public void eusd_buy_3_2(ITestContext context) throws IOException {
        log.info("eusd_buy_3_2：下单成功后，用户【我的】【我的订单】页面第一列显示该订单，且订单状态为“未付款”");
        String user_token = (String) context.getAttribute("user_token");
        User user = new User(user_token);
        int status = user.get_first_orders_status_by_id();        //查看普通用户，订单列表第一个订单，返回订单的状态，订单状态1为未付款
        log.info("eusd_buy_3_2:查看订单状态（1为正确）："+status);
        Assert.assertEquals(1,status);
        log.info("eusd_buy_3_2,测试结束");
    }



    @Test(dependsOnMethods ="eusd_buy_3_2",description = "下单成功后，承兑商【承兑】【承兑资产】资金冻结X")
    //三、下单成功后，承兑商【承兑】【承兑资产】资金冻结X
    public void eusd_buy_3_3(ITestContext context)throws IOException {
        log.info("eusd_buy_3_3:下单成功后，承兑商【承兑】【承兑资产】资金冻结X");
        //1、获取承兑商测试前EUSD冻结余额
        BigDecimal eu_eusd_before_trade = (BigDecimal) context.getAttribute("eu_eusd_before_trade");

        //2、获取承兑商token，登录查看下单后承兑商【承兑】EUSD冻结余额
        String eu_token= (String) context.getAttribute("eu_token");
        User eu_user = new User(eu_token);
        BigDecimal eu_eusd_after_frozen = eu_user.v1_exchange_info_return_trade();         //查看承兑商【承兑】页面的EUSD的冻结数值

        //3、获取用户下单的数量
        BigDecimal EUSD_buy_quantity = (BigDecimal) context.getAttribute("EUSD_buy_quantity");

        //4、判断:用户下单后承兑商冻结余额 + 下单数量 = 下单前余额
        log.info("eusd_buy_3_3,用户下单后，承兑商冻结余额 + 下单数量 = 下单前余额,参与计算的数值是：");
        log.info("eusd_buy_3_3,eu_eusd_before_trade是："+eu_eusd_before_trade);
        log.info("eusd_buy_3_3,EUSD_buy_quantity是："+EUSD_buy_quantity);
        log.info("eusd_buy_3_3,eu_eusd_after_frozen是："+eu_eusd_after_frozen);
        if(eu_eusd_before_trade.add(EUSD_buy_quantity).compareTo(eu_eusd_after_frozen)==0){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        context.setAttribute("eu_eusd_after_frozen",eu_eusd_after_frozen);
        log.info("eusd_buy_3_3，测试结束");
    }


    @Test(dependsOnMethods = "eusd_buy_3_3",description ="下单成功后，承兑商【承兑】【收款订单】页面第一列显示该订单，订单状态为“未付款”" )
    //四、下单成功后，承兑商【承兑】【收款订单】页面第一列显示该订单，订单状态为“未付款”
    public void eusd_buy_3_4(ITestContext context) throws IOException {
        log.info("eusd_buy_3_4，下单成功后，承兑商【承兑】【收款订单】页面第一列显示该订单，订单状态为“未付款”");
        String  eu_token = (String) context.getAttribute("eu_token");
        User eu_user = new User(eu_token);
        int status = eu_user.get_eu_orders_buy_status_by_id();
        log.info("eusd_buy_3_4,订单状态是(1为正确）："+status);
        Assert.assertEquals(1,status);
        log.info("eusd_buy_3_4，测试结束");
    }



    @Test(dependsOnMethods = { "eusd_buy_3_4" },description = "用户点击【我已付款】，查看付款是否成功")
    //五、用户点击【我已付款】，查看付款是否成功
    public void eusd_buy_3_5(ITestContext context) throws IOException, InterruptedException {
        log.info("eusd_buy_3_5:用户点击【我已付款】，查看付款是否成功");
        Thread.currentThread().sleep(10*1000);  //点击确认支付不能操作太快
        //普通用户点击确认支付货款，查看返回
        String user_token = (String) context.getAttribute("user_token");
        BigInteger id = (BigInteger) context.getAttribute("id");
        User user = new User(user_token);
        int status =user.buy_pay(id);
        log.info("eusd_buy_3_5,查看服务器返回状态(200说明用户确认支付成功):"+status);
        Assert.assertEquals(200,status);
        log.info("eusd_buy_3_5，测试结束");
    }






    @Test(dependsOnMethods = { "eusd_buy_3_5" },description ="用户付款后，用户【我的】【我的订单】页面第一列显示该订单，且订单状态为”已付款待确认（2）”" )
    //六、用户付款后，用户【我的】【我的订单】页面第一列显示该订单，且订单状态为”已付款待确认（2）”
    public void eusd_buy_3_6(ITestContext context) throws IOException {
        log.info("eusd_buy_3_6:用户付款后，用户【我的】【我的订单】页面第一列显示该订单，且订单状态为”已付款待确认（2）");
        String user_token = (String) context.getAttribute("user_token");
        User user = new User(user_token);
        int status = user.get_first_orders_status_by_id();
        log.info("eusd_buy_3_6,点击确认支付后，查看订单状态(2为正确)："+status);
        Assert.assertEquals(2,status);
        log.info("eusd_buy_3_6，测试结束");
    }




    @Test(dependsOnMethods = { "eusd_buy_3_6" },description = "用户付款后，承兑商【承兑】【收款订单】页面第一列显示该订单，订单状态为“已付款”")
    //七、用户付款后，承兑商【承兑】【收款订单】页面第一列显示该订单，订单状态为“已付款”
    public void eusd_buy_3_7(ITestContext context) throws IOException {
        log.info("eusd_buy_3_7,用户付款后，承兑商【承兑】【收款订单】页面第一列显示该订单，订单状态为“已付款”");
        //1、承兑商登录
        String  eu_token = (String) context.getAttribute("eu_token");
        User eu_user = new User(eu_token);
        //2、查看订单状态
        int eu_status=eu_user.get_eu_orders_buy_status_by_id();
        log.info("eusd_buy_3_7,用户付款后，承兑商【承兑】【收款订单】页面第一列显示该订单，订单状态为（2为正确）:"+eu_status);
        Assert.assertEquals(2,eu_status);
        log.info("eusd_buy_3_7,测试结束");
    }




    @Test(dependsOnMethods = { "eusd_buy_3_7" },description = "用户付款后，承兑商【承兑】【今日累积收款】金额不变（保存今日累积收款金额，便于参与计算）")
    //八、用户付款后，承兑商【承兑】【今日累积收款】金额不变（保存今日累积收款金额，便于参与计算）
    public void eusd_buy_3_8(ITestContext context) throws IOException {
        log.info("eusd_buy_3_8:用户付款后，承兑商【承兑】【今日累积收款】金额不变（保存今日累积收款金额，便于参与计算）");
        //1、承兑商登录
        String eu_token = (String) context.getAttribute("eu_token");
        User eu_user = new User(eu_token);
        //2、获取数据
        //承兑商原今日累积金额
        BigDecimal eu_eusd_before_buy_rmb_today = (BigDecimal) context.getAttribute("eu_eusd_before_buy_rmb_today");
        //承兑商现今日累积金额
        BigDecimal after_buy_rmb_day=eu_user.v1_exchange_info_return_buy_rmb_day();
        //3、比较，原今日累积金额=承兑商现累积金额，则测试通过
        log.info("eusd_buy_3_8,原今日累积金额=承兑商现累积金额，则测试通过，参与比较的数值是：");
        log.info("eusd_buy_3_8,eu_eusd_before_buy_rmb_today金额是"+eu_eusd_before_buy_rmb_today);
        log.info("eusd_buy_3_8,after_buy_rmb_day金额是"+after_buy_rmb_day);
        if(eu_eusd_before_buy_rmb_today.compareTo(after_buy_rmb_day)==0){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        log.info("eusd_buy_3_8,测试完成");
    }



    @Test(dependsOnMethods = { "eusd_buy_3_8" },description = "承兑商6分钟内无操作,订单状态变为“已逾期”，承兑商点击收款，查看订单状态是否更改")
    //九、承兑商6分钟内无操作,订单状态变为“已逾期”
    public void eusd_buy_3_9(ITestContext context) throws IOException, InterruptedException {
        log.info("eusd_buy_3_9,承兑商6分钟内无操作,订单状态仍是“已付款待确认”，但是前端显示”已逾期请尽快确认“，前端显示目前无法用接口测，处理方式待定0613。 承兑商点击收款，查看订单状态是否更改");
        Thread.sleep(6*60*1000);
        //承兑商先登录
        String eu_token = (String) context.getAttribute("eu_token");
        BigInteger order_id = (BigInteger) context.getAttribute("id");
        User eu_user = new User(eu_token);
        //传入订单id,执行点击【我已收款】操作
        int code =eu_user.v1_buy_id_confirm(order_id);
        log.info("eusd_buy_3_9,承兑商点击【我已收款】返回的状态码（200为正确）是："+code);
        Assert.assertEquals(200,code);
        log.info("eusd_buy_3_9，测试结束");
    }





    @Test(dependsOnMethods = { "eusd_buy_3_9" },description ="承兑商确认收款后，用户【我的】【我的订单】订单状态为“已确认”" )
    //十、承兑商点击确认收款，用户【我的】【我的订单】订单状态为“已确认”
    public void eusd_buy_3_10(ITestContext context) throws IOException {
        log.info("eusd_buy_3_10：承兑商确认收款后，用户【我的】【我的订单】订单状态为“已确认”");
        String user_token = (String) context.getAttribute("user_token");
        User user = new User(user_token);
        int status = user.get_first_orders_status_by_id();
        log.info("eusd_buy_3_10:查看订单状态（3为正确)："+status);
        Assert.assertEquals(3,status);
        log.info("eusd_buy_3_10,测试结束");
    }



    @Test(dependsOnMethods = { "eusd_buy_3_10" },description = "承兑商确认收款后，承兑商【承兑】【收款订单】页面第一列显示该订单，且订单状态为“已确认”")
    //十一、平台自动完成订单（承兑商确认收款），承兑商【承兑】【收款订单】页面第一列显示该订单，且订单状态为“已确认”
    public void eusd_buy_3_11(ITestContext context) throws IOException {
        log.info("eusd_buy_3_11,承兑商确认收款后，承兑商【承兑】【收款订单】页面第一列显示该订单，且订单状态为“已确认”");
        String  eu_token = (String) context.getAttribute("eu_token");
        User eu_user = new User(eu_token);
        //查看订单状态
        int eu_status=eu_user.get_eu_orders_buy_status_by_id();
        log.info("eusd_buy_3_11,承兑商确认收款后，承兑商【承兑】【收款订单】页面第一列显示该订单，订单状态为（3为正确）”"+eu_status);
        Assert.assertEquals(3,eu_status);
        log.info("eusd_buy_3_11,测试结束");
    }







    @Test(dependsOnMethods = { "eusd_buy_3_11" },description = "承兑商确认收款后,用户【资产】【EUSD】EUSD的可用余额不变，冻结余额+x")
    //十二、平台自动完成订单（承兑商确认收款），用户【资产】【EUSD】EUSD的可用余额不变，冻结余额+x。
    public void eusd_buy_3_12(ITestContext context) throws IOException {
        log.info("eusd_buy_3_12:承兑商确认收款后，用户【资产】【EUSD】EUSD的可用余额不变，冻结余额+x");
        boolean result1;
        boolean result2;
        //1、用户登录
        String user_token = (String) context.getAttribute("user_token");
        User user = new User(user_token);
        //2、获取原可用余额、现可用余额，相等则测试通过
        BigDecimal user_eusd_before_available = (BigDecimal) context.getAttribute("user_eusd_before_available");
        BigDecimal user_eusd_after_available = user.get_EUSD_available();
        log.info("eusd_buy_3_12,原可用余额=现可用余额,则测试通过，参与比较的数值是：");
        log.info("eusd_buy_3_12,user_eusd_before_available是：" + user_eusd_before_available);
        log.info("eusd_buy_3_12,user_eusd_after_available是：" + user_eusd_after_available);
        if (user_eusd_before_available.compareTo(user_eusd_after_available) == 0) {
            result1=true;
        } else {
            result1=false;
        }
        log.info("result1是："+result1);

        //3、获取原冻结余额、下单数值、现冻结余额，原冻结余额+下单数值=现冻结余额，测试通过
        BigDecimal user_eusd_before_frozen = (BigDecimal) context.getAttribute("user_eusd_before_frozen");
        BigDecimal EUSD_buy_quantity = (BigDecimal) context.getAttribute("EUSD_buy_quantity");
        BigDecimal user_eusd_after_frozen=user.get_EUSD_frozen();

        log.info("eusd_buy_3_12,等待4分钟后，数值解冻，原冻结余额+下单数值=现冻结余额，则测试通过，参与计算的数值是：");
        log.info("eusd_buy_3_12,user_eusd_before_frozen是："+user_eusd_before_frozen);
        log.info("eusd_buy_3_12,EUSD_buy_quantity是："+EUSD_buy_quantity);
        log.info("eusd_buy_3_12,user_eusd_after_trade是："+user_eusd_after_frozen);
        if(user_eusd_before_frozen.add(EUSD_buy_quantity).compareTo(user_eusd_after_frozen)==0){
            result2=true;
        }else{
            result2=false;
        }
        log.info("result2是："+result2);

        //4、判断以上两个测试是否通过
        if(result1==result2){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        context.setAttribute("user_eusd_after_frozen",user_eusd_after_frozen);  //保存冻结余额，用于下一步作比较
        log.info("eusd_buy_3_12,测试结束");
    }







    @Test(dependsOnMethods = { "eusd_buy_3_12" },description ="承兑商确认收款后，用户【资产】【EUSD】EUSD的可用余额不变，冻结余额+x，4分钟后解冻，冻结余额-X,可用余额+X" )
    //十三、承兑商确认收款后，用户【资产】【EUSD】4分钟后解冻，冻结余额-X,可用余额+X
    public void eusd_buy_3_13(ITestContext context) throws IOException, InterruptedException {
        log.info("eusd_buy_3_13:承兑商确认收款后，用户【资产】【EUSD】EUSD的可用余额不变，冻结余额+x，等待4分钟，4分钟后解冻，冻结余额-X,可用余额+X");
        Thread.sleep(6*60*1000);
        boolean result1;
        boolean result2;
        //1、用户登录
        String user_token = (String) context.getAttribute("user_token");
        User user = new User(user_token);
        //2、获取原冻结余额、下单数值、现冻结余额，原冻结余额-下单数值=现冻结余额，说明数值确实解冻了，测试通过
        BigDecimal user_eusd_after_frozen = (BigDecimal) context.getAttribute("user_eusd_after_frozen");
        BigDecimal EUSD_buy_quantity = (BigDecimal) context.getAttribute("EUSD_buy_quantity");
        //BigDecimal user_eusd_final_trade=user.v1_exchange_info_return_trade();        0626，此处应该调用用户接口，怀疑错调成承兑商接口
        BigDecimal user_eusd_final_trade=user.get_EUSD_frozen();
        log.info("eusd_buy_3_13,等待4分钟后，数值解冻，原冻结余额-下单数值=现冻结余额，则测试通过，参与计算的数值是：");
        log.info("eusd_buy_3_13,user_eusd_after_frozen是："+user_eusd_after_frozen);    //此时的冻结余额，是指已经被冻结后的余额
        log.info("eusd_buy_3_13,EUSD_buy_quantity是："+EUSD_buy_quantity);
        log.info("eusd_buy_3_13,user_eusd_final_trade是："+user_eusd_final_trade);
        if(user_eusd_after_frozen.subtract(EUSD_buy_quantity).compareTo(user_eusd_final_trade)==0){
            result1=true;
        }else{
            result1=false;
        }
        log.info("result1是："+result1);

        //3、获取原可用余额、现可用余额，原可用余额+下单数值=现可用余额，说明可用余额增加了，测试通过
        BigDecimal user_eusd_before_available = (BigDecimal) context.getAttribute("user_eusd_before_available");
        BigDecimal user_eusd_after_available = user.get_EUSD_available();
        log.info("eusd_buy_3_13,等待4分钟后，数值解冻，原可用余额+下单数值=现可用余额，则测试通过，参与计算的数值是：");
        log.info("eusd_buy_3_13,user_eusd_before_available是："+user_eusd_before_available);
        log.info("eusd_buy_3_13,EUSD_buy_quantity是："+EUSD_buy_quantity);
        log.info("eusd_buy_3_13,user_eusd_after_available是："+user_eusd_after_available);
        if(user_eusd_before_available.add(EUSD_buy_quantity).compareTo(user_eusd_after_available)==0){
            result2=true;
        }else{
            result2=false;
        }
        log.info("result2是："+result2);

        //4、判断以上两个测试是否通过
        if(result1&result2){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        log.info("eusd_buy_3_13,测试结束");
    }




    @Test(dependsOnMethods = { "eusd_buy_3_13" },description = "承兑商确认收款后，承兑商【承兑】【承兑资产】EUSD数值减少X")
    //十四、平台自动完成订单（承兑商确认收款），承兑商【承兑】【承兑资产】EUSD数值减少X
    public void eusd_buy_3_14(ITestContext context) throws IOException {
        log.info("eusd_buy_3_14:承兑商确认收款后，承兑商【承兑】【承兑资产】EUSD数值减少X");
        //1、承兑商登录
        String eu_token = (String) context.getAttribute("eu_token");
        User eu_user = new User(eu_token);
        //2、获取原可用余额、下单数值、现可用余额，
        BigDecimal eu_eusd_before_available= (BigDecimal) context.getAttribute("eu_eusd_before_available");     //初始化后的可用余额啊
        BigDecimal EUSD_buy_quantity = (BigDecimal) context.getAttribute("EUSD_buy_quantity");
        BigDecimal eu_eusd_after_available = eu_user.v1_exchange_info_return_available();
        log.info("eusd_buy_3_14,原可用余额-下单数值=现可用余额,则测试通过，参与比较的数值是：");
        log.info("eusd_buy_3_14,eu_eusd_before_available是:"+eu_eusd_before_available);
        log.info("eusd_buy_3_14,EUSD_buy_quantity是:"+EUSD_buy_quantity);
        log.info("eusd_buy_3_14,eu_eusd_after_available是:"+eu_eusd_after_available);
        if(eu_eusd_before_available.subtract(EUSD_buy_quantity).compareTo(eu_eusd_after_available)==0){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        log.info("eusd_buy_3_14,测试结束");
    }







    @Test(dependsOnMethods = { "eusd_buy_3_14" },description ="承兑商确认收款后，承兑商【承兑】【今日累积收款】金额增加X" )
    //十五、平台自动完成订单（承兑商确认收款），承兑商【承兑】【今日累积收款】金额增加X
    public void eusd_buy_3_15(ITestContext context) throws IOException {
        log.info("eusd_buy_3_15;承兑商确认收款后，承兑商【承兑】【今日累积收款】金额增加X");
        //1、承兑商登录
        String eu_token= (String) context.getAttribute("eu_token");
        User eu_user = new User(eu_token);
        //2、获取比较的数值
        BigDecimal eu_eusd_before_buy_rmb_today = (BigDecimal) context.getAttribute("eu_eusd_before_buy_rmb_today");
        BigDecimal after_buy_rmb_day=eu_user.v1_exchange_info_return_buy_rmb_day();
        log.info("eusd_buy_3_15,承兑商前今日累积收款数值<现今日累积收款数值，则测试通过，参与比较的数值是：");
        log.info("eusd_buy_3_15,eu_eusd_before_buy_rmb_today是："+eu_eusd_before_buy_rmb_today );
        log.info("eusd_buy_3_15,after_buy_rmb_day是："+after_buy_rmb_day );
        if(eu_eusd_before_buy_rmb_today.compareTo(after_buy_rmb_day)<0){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        log.info("eusd_buy_3_15,测试完成");
    }
}
