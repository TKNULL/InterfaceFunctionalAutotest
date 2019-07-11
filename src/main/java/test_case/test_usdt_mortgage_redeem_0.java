package test_case;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.LinkedList;


/**
 * USDT抵押-赎回流程一：全部抵押后赎回
 * 前提：1、承兑商账户登录，USDT账户有余额；
 * 备注：USDT抵押时，有冻结，所以需要等待，赎回时没有冻结，是立即赎回
 * 测试步骤：
 *      1、承兑用户登录，查看USDT余额，有余额的话，点击进行抵押，抵押全部,等待15秒
 *      2、抵押成功后，USDT预期：【资产】【USDT】可用余额为0         usdt_available
 *      3、抵押成功后，USDT预期：【资产】【USDT】已抵押余额+X        usdt_frozen
 *      4、抵押成功后，USDT预期：【资产】【USDT】【资产详情】页面订单状态和类型为“抵押”“已抵押”     v1/usdt/records?page=0&per_page=20
 *
 *      5、抵押成功后，EUSD预期：【资产】【EUSD】可用余额不变    before_eusd_available
 *      6、抵押成功后，EUSD预期：【资产】【EUSD】抵押余额+X      before_eusd_frozen
 *
 *      7、抵押成功后，EUSD预期：2分钟后，【资产】【EUSD】可用余额+X       before_eusd_available_add
 *      8、抵押成功后，EUSD预期：2分钟后，【资产】【EUSD】冻结金额-X       before_eusd_frozen_reduce
 *      9、抵押成功后，EUSD预期：【资产】【EUSDU】【资产详情】页面显示记录     v1/eos/records?type=1%2C4%2C5%2C7%2C9%2C11&page=0&limit=0
 *
 *
 *
 *      10、点击全部赎回，等待1分钟
 *      11、赎回成功后，USDT预期：【资产】【USDT】可用余额为全部              after_usdt_available
 *      12、赎回成功后，USDT预期：【资产】【USDT】已抵押余额为0               after_usdt_mortgaged
 *      13、赎回成功后，USDT预期：【资产】【USDT】【资产详情】页面显示记录
 *
 *      14、赎回成功后，EUSD预期：【资产】【EUSD】可用余额减少                after_eusd_available
 *      15、赎回成功后，EUSD预期：【资产】【EUSD】冻结金额不变                after_eusd_frozen
 *      16、赎回成功后，EUSD预期：【资产】【EUSDU】【资产详情】页面显示记录
 *
 *
 * */




public class test_usdt_mortgage_redeem_0 {
    public static Log log = LogFactory.getLog(test_usdt_mortgage_redeem_0.class);

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
        LinkedList<LinkedList<String>> eu_user_mobiles_roles = before_fun.get_mobiles("eu_user","available",1);


        //获取列表中,第一个eu_user手机号
        String eu_user_mibile = eu_user_mobiles_roles.get(0).get(1);
        context.setAttribute("eu_user_mibile",eu_user_mibile);
        log.info("test_usdt_mortgage_redeem_0用例获得的手机账号是：");
        //log.info("user_mobile："+user_mobile);
        log.info("eu_user_mibile："+eu_user_mibile);

        //3、存储账号token、EUSD的各种余额
        User eu_user = new User();
        String eu_token = eu_user.login_and_return_token(eu_user_mibile);
        //获取EUSD可用余额、冻结余额
        BigDecimal eusd_available =eu_user.get_EUSD_available();
        BigDecimal eusd_frozen =eu_user.get_EUSD_frozen();
        context.setAttribute("eu_token",eu_token);
        context.setAttribute("eusd_available",eusd_available);
        context.setAttribute("eusd_frozen",eusd_frozen);
        //获取USDT可用余额、抵押余额
        log.info("由于EUSD和USDT小数点不同,此处做处理,10000.0000=>10000 0000:");
        BigDecimal usdt_available = eu_user.v1_usdt_return_available(); //10000.0000
            //数据拼接转换,//10000.0000==>100000000开始
            //先转成double
            DecimalFormat df = new DecimalFormat( "0");
            Double dou_usdt_available = usdt_available.doubleValue();
            log.info("usdt_mortgage_redeem_0_6,进行数据转换");
            log.info("usdt_mortgage_redeem_0_6,dou_usdt_available:"+dou_usdt_available);
            //再转成String,并去除小数点和最后一位(因为转double的时候,会带XX.0)
            String str_usdt_availabl = String.valueOf(dou_usdt_available).replace(".","");
            String str_usdt_availabl_1 =str_usdt_availabl.substring(0,str_usdt_availabl.length()-1);
            String str_usdt_availabl_2 =str_usdt_availabl_1+"0000";
            log.info("usdt_mortgage_redeem_0_6,str_usdt_availabl_2:"+str_usdt_availabl_2);
            //再转成BigDecimal进行比较
            BigDecimal bigDecimal_usdt_available = new BigDecimal(str_usdt_availabl_2);
            log.info("usdt_mortgage_redeem_0_6,bigDecimal_usdt_available:"+bigDecimal_usdt_available);
            //数据拼接转换,//10000.0000==>100000000结束
        BigDecimal usdt_mortgaged = eu_user.v1_usdt_return_mortgaged();
        context.setAttribute("usdt_available",usdt_available);
        context.setAttribute("change_usdt_available",bigDecimal_usdt_available);
        context.setAttribute("usdt_mortgaged",usdt_mortgaged);

        log.info("test_usdt_mortgage_redeem_0,测试前记录所有账号的数据信息：");
        log.info("eusd_available:"+eusd_available);
        log.info("eusd_frozen:"+eusd_frozen);
        log.info("usdt_available:"+usdt_available);
        log.info("change_usdt_available:"+bigDecimal_usdt_available);       //usdt_available的类型是10000.0000,而其他EUSD数据是1000 0000,为了和EUSD进行比较,新建bigDecimal_usdt_available,该数据是usdt_available转化的1000 0000
        log.info("usdt_mortgaged:"+usdt_mortgaged);

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
        String eu_user_mibile = (String) context.getAttribute("eu_user_mibile");
        LinkedList<String> used_mobiles = new LinkedList<String>();
        used_mobiles.add(eu_user_mibile);
        before_fun.insert_is_used(used_mobiles);
        log.info("after_class结束");
    }





    @Test(description = "承兑用户登录，查看USDT余额，有余额的话，点击进行抵押，抵押全部"  )
    //一、承兑用户登录，查看USDT余额，有余额的话，点击进行抵押，抵押全部
    public void usdt_mortgage_redeem_0_1(ITestContext context) throws IOException, InterruptedException {
        log.info("usdt_mortgage_redeem_0_1:承兑用户登录，查看USDT余额，有余额的话，点击进行抵押，抵押全部");
        String eu_token = (String) context.getAttribute("eu_token");
        User eu_user = new User(eu_token);
        //获取usdt可用余额
        BigDecimal usdt_available = (BigDecimal) context.getAttribute("usdt_available");
        log.info("usdt_mortgage_redeem_0_1,执行抵押的USDT金额是:"+usdt_available);
        //进行抵押，抵押全部
        int getcode = eu_user.v1_usdt_mortgage(usdt_available);
        log.info("usdt_mortgage_redeem_0_1,返回的getcode是(200说明抵押成功)："+getcode);
        Assert.assertEquals(200,getcode);
        log.info("usdt_mortgage_redeem_0_1,测试结束");
    }


    @Test(dependsOnMethods = { "usdt_mortgage_redeem_0_1" },description = "抵押成功后，USDT预期：【资产】【USDT】可用余额为0" )
    //二、抵押成功后，USDT预期：【资产】【USDT】可用余额为0
    public void usdt_mortgage_redeem_0_2(ITestContext context) throws IOException, InterruptedException {
        Thread.sleep(10*1000);
        log.info("usdt_mortgage_redeem_0_2:抵押成功后，USDT预期：【资产】【USDT】可用余额为0");
        String eu_token = (String) context.getAttribute("eu_token");
        User eu_user = new User(eu_token);
        // 获取现USDT可用余额，如果抵押成功的话，可用余额应该是0.0000
        BigDecimal before_usdt_available = eu_user.v1_usdt_return_available();
        log.info("usdt_mortgage_redeem_0_2,获得的可用余额(0.0000为正确)是："+before_usdt_available);
        BigDecimal result = new BigDecimal("0.0000");
        if(before_usdt_available.compareTo(result)==0){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        log.info("usdt_mortgage_redeem_0_2,测试结束");
    }



    @Test(dependsOnMethods = { "usdt_mortgage_redeem_0_2" },description = "抵押成功后，USDT预期：【资产】【USDT】已抵押金额增加")
    //三、抵押成功后，USDT预期：【资产】【USDT】已抵押金额增加
    public void usdt_mortgage_redeem_0_3(ITestContext context) throws InterruptedException, IOException {
        log.info("usdt_mortgage_redeem_0_3:抵押成功后，USDT预期：【资产】【USDT】可用余额为0");
        Thread.sleep(10*1000);
        String eu_token = (String) context.getAttribute("eu_token");
        User user = new User(eu_token);
        // 原已抵押金额 +  本次抵押金额 = 现已抵押金额，测试通过
        //1、获得原已抵押金额
        BigDecimal usdt_mortgaged = (BigDecimal) context.getAttribute("usdt_mortgaged");
        //2、获得本次抵押金额（由于是抵押的全部，所以直接获取测试前的USDT可用余额）
        BigDecimal usdt_available = (BigDecimal) context.getAttribute("usdt_available");
        //3、获取现已抵押金额(before_usdt_mortgaged中的before是指抵押成功后的指，后面的after是指赎回成功后的值）
        BigDecimal before_usdt_mortgaged = user.v1_usdt_return_mortgaged();
        log.info("usdt_mortgage_redeem_0_3,原已抵押金额 +  本次抵押金额 = 现已抵押金额，测试通过,参与计算的数值是：");
        log.info("usdt_mortgage_redeem_0_3,usdt_mortgaged是："+usdt_mortgaged);
        log.info("usdt_mortgage_redeem_0_3,usdt_available是："+usdt_available);
        log.info("usdt_mortgage_redeem_0_3,before_usdt_mortgaged："+before_usdt_mortgaged);
        if(usdt_mortgaged.add(usdt_available).compareTo(before_usdt_mortgaged)==0){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        log.info("usdt_mortgage_redeem_0_3,测试结束");
    }


    @Test(dependsOnMethods = { "usdt_mortgage_redeem_0_3" },description = "抵押成功后，USDT预期：【资产】【USDT】【资产详情】页面订单状态和类型为“抵押”“已抵押”")
    //四、抵押成功后，USDT预期：【资产】【USDT】【资产详情】页面订单状态和类型为“抵押”“已抵押”
    public void usdt_mortgage_redeem_0_4(ITestContext context) throws IOException, InterruptedException {
        log.info("usdt_mortgage_redeem_0_4,抵押成功后，USDT预期：【资产】【USDT】【资产详情】页面订单状态和类型为“抵押”“已抵押”");
        Thread.sleep(10*1000);
        String eu_token = (String) context.getAttribute("eu_token");
        User user = new User(eu_token);

        //2、获取下单金额，根据下单金额获取订单，然后获取订单的状态
        BigDecimal amount = (BigDecimal) context.getAttribute("usdt_available");

        int arr[] = user.v1_usdt_records( amount);
        log.info("usdt_mortgage_redeem_0_4,抵押成功后，USDT预期：【资产】【USDT】【资产详情】页面订单状态(3为正确)是："+arr[0]);
        log.info("usdt_mortgage_redeem_0_4,抵押成功后，USDT预期：【资产】【USDT】【资产详情】页面订单类型是(2为正确)是："+arr[1]);
        if((arr[0]==3)&(arr[1]==2)){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        log.info("usdt_mortgage_redeem_0_4,测试结束");
    }



    @Test(dependsOnMethods = { "usdt_mortgage_redeem_0_4" },description = "抵押成功后，EUSD预期：【资产】【EUSD】可用余额不变")
    //五、抵押成功后，EUSD预期：【资产】【EUSD】可用余额不变
    public void usdt_mortgage_redeem_0_5(ITestContext context) throws IOException, InterruptedException {
        log.info("usdt_mortgage_redeem_0_5,抵押成功后，EUSD预期：【资产】【EUSD】可用余额不变");
        Thread.sleep(10*1000);
        String eu_token = (String) context.getAttribute("eu_token");
        User user = new User(eu_token);
        //对比抵押前、抵押后的EUSD可用余额，由于抵押成功后抵押的金额会先冻结，所以EUSD可用余额是没有变化的，此处判断相等，则测试通过。

        //1、获取抵押前，EUSD的可用余额
        BigDecimal eusd_available= (BigDecimal) context.getAttribute("eusd_available");         //获取资产页面的
        //2、获取抵押后，EUSD的可用余额
        BigDecimal before_eusd_available = user.get_EUSD_available();
        log.info("usdt_mortgage_redeem_0_5,参与比较的2个数值是（2个数值相等，则测试通过）：");
        log.info("usdt_mortgage_redeem_0_5，eusd_available是："+eusd_available);
        log.info("usdt_mortgage_redeem_0_5，before_eusd_availablee是："+before_eusd_available);
        if(eusd_available.compareTo(before_eusd_available)==0){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        log.info("usdt_mortgage_redeem_0_5,测试结束");
    }




    @Test(dependsOnMethods = { "usdt_mortgage_redeem_0_5" },description = "抵押成功后，EUSD预期：【资产】【EUSD】冻结余额+X")
    //六、抵押成功后，EUSD预期：【资产】【EUSD】冻结余额+X
    public void usdt_mortgage_redeem_0_6(ITestContext context) throws IOException, InterruptedException {
        log.info("usdt_mortgage_redeem_0_6,抵押成功后，EUSD预期：【资产】【EUSD】抵押余额增加");
        Thread.sleep(10*1000);
        String eu_token = (String) context.getAttribute("eu_token");
        User user = new User(eu_token);
        //抵押前eusd冻结余额 + 抵押金额 = 抵押后eusd冻结余额，测试通过。（usdt抵押后，抵押的金额先在eusd冻结）
        //1、获取抵押前EUSD的冻结余额
        BigDecimal eusd_frozen= (BigDecimal) context.getAttribute("eusd_frozen");
        //2、获取本次抵押金额（由于是抵押全部，所以获取测试前EUSD的全部金额）
        BigDecimal usdt_available = (BigDecimal) context.getAttribute("usdt_available");
        BigDecimal change_usdt_available = (BigDecimal) context.getAttribute("change_usdt_available");
        //3、获取抵押后的EUSD冻结余额
        BigDecimal before_eusd_frozen = user.get_EUSD_frozen();
        log.info("usdt_mortgage_redeem_0_6,抵押前eusd冻结余额 + 抵押金额 = 抵押后eusd冻结余额，测试通过,参与计算的数值是：");
        log.info("usdt_mortgage_redeem_0_6,eusd_frozen是："+eusd_frozen);
        log.info("usdt_mortgage_redeem_0_6,usdt_available是："+usdt_available);               //10000.0000
        log.info("usdt_mortgage_redeem_0_6,change_usdt_available是："+change_usdt_available);
        log.info("usdt_mortgage_redeem_0_6,before_eusd_frozen是："+before_eusd_frozen);       //10000 0000返回数据无小数点
        if(eusd_frozen.add(change_usdt_available).compareTo(before_eusd_frozen)==0){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        context.setAttribute("before_eusd_frozen",before_eusd_frozen);      //用于5分钟解冻后比较
        log.info("usdt_mortgage_redeem_0_6，测试完成");
    }




    @Test(dependsOnMethods = { "usdt_mortgage_redeem_0_6" },description = "抵押成功后，EUSD预期：5分钟后，【资产】【EUSD】可用余额+X")
    //七、抵押成功后，EUSD预期：5分钟后，【资产】【EUSD】可用余额+X
    public void usdt_mortgage_redeem_0_7(ITestContext context) throws IOException, InterruptedException {
        log.info("usdt_mortgage_redeem_0_7,抵押成功后，EUSD预期：5分钟后，【资产】【EUSD】可用余额+X");
        Thread.sleep(5*60*1000);
        String eu_token = (String) context.getAttribute("eu_token");
        User user = new User(eu_token);

        //获取原EUSD可用余额
        BigDecimal eusd_available= (BigDecimal) context.getAttribute("eusd_available");
        //获取抵押数值
        BigDecimal usdt_available= (BigDecimal) context.getAttribute("usdt_available");
        BigDecimal change_usdt_available= (BigDecimal) context.getAttribute("change_usdt_available");
        //获取现EUSD可用余额
        BigDecimal before_eusd_available = user.get_EUSD_available();
        //原EUSD可用余额+抵押数值=现EUSD可用余额
        log.info("usdt_mortgage_redeem_0_7,原EUSD可用余额+USDT抵押数值=现EUSD可用余额，则测试通过，参与计算的数值是：");
        log.info("usdt_mortgage_redeem_0_7,eusd_available是："+eusd_available);
        log.info("usdt_mortgage_redeem_0_7,usdt_available是："+usdt_available);
        log.info("usdt_mortgage_redeem_0_7,before_eusd_available是："+before_eusd_available);
        if(eusd_available.add(change_usdt_available).compareTo(before_eusd_available)==0){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        context.setAttribute("before_eusd_available",before_eusd_available);
        log.info("usdt_mortgage_redeem_0_6，测试完成");

    }


    @Test(dependsOnMethods = { "usdt_mortgage_redeem_0_7" },description = "抵押成功后，EUSD预期：5分钟后，【资产】【EUSD】冻结金额-X")
    //八、抵押成功后，EUSD预期：5分钟后，【资产】【EUSD】冻结金额-X
    public void usdt_mortgage_redeem_0_8(ITestContext context) throws IOException, InterruptedException {
        log.info("usdt_mortgage_redeem_0_8,抵押成功后，EUSD预期：5分钟后，【资产】【EUSD】冻结金额-X");
        //登录并等待5分钟（等解冻）
        String eu_token = (String) context.getAttribute("eu_token");
        User user = new User(eu_token);
        //获取原EUSD冻结金额（是抵押成功后的冻结金额，不是最初的冻结金额）
        BigDecimal before_eusd_frozen = (BigDecimal) context.getAttribute("before_eusd_frozen");
        //获取USDT抵押金额
        BigDecimal usdt_available = (BigDecimal) context.getAttribute("usdt_available");
        BigDecimal change_usdt_available = (BigDecimal) context.getAttribute("change_usdt_available");
        //获取现冻结金额
        BigDecimal before_eusd_frozen_reduce =user.get_EUSD_frozen();
        log.info("usdt_mortgage_redeem_0_8,现冻结金额+USDT抵押金额=原EUSD冻结金额，则测试通过。 参与比较的数值是：");
        log.info("usdt_mortgage_redeem_0_8,before_eusd_frozen_reduce是："+before_eusd_frozen_reduce);
        context.setAttribute("before_eusd_frozen_reduce",before_eusd_frozen_reduce);
        log.info("usdt_mortgage_redeem_0_8,usdt_available是："+usdt_available);
        log.info("usdt_mortgage_redeem_0_8,before_eusd_frozen是："+before_eusd_frozen);
        //现冻结金额+USDT抵押金额=原EUSD冻结金额
        if(before_eusd_frozen_reduce.add(change_usdt_available).compareTo(before_eusd_frozen)==0){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        log.info("usdt_mortgage_redeem_0_8测试结束");
    }






    @Test(dependsOnMethods = { "usdt_mortgage_redeem_0_8" },description = "抵押成功后，EUSD预期：【资产】【EUSD】【资产详情】页面显示记录")
    //九、抵押成功后，EUSD预期：【资产】【EUSD】【资产详情】页面显示记录
    public void usdt_mortgage_redeem_0_9(ITestContext context) throws IOException, InterruptedException {
        log.info("usdt_mortgage_redeem_0_9,抵押成功后，EUSD预期：【资产】【EUSD】【资产详情】页面显示记录");
        Thread.sleep(10*1000);
        String eu_token = (String) context.getAttribute("eu_token");
        User user = new User(eu_token);

        //2、获取下单金额，根据下单金额获取订单，然后获取订单的状态
        BigDecimal quantity = (BigDecimal) context.getAttribute("change_usdt_available");
        int arr[] = user.v1_eusd_records( quantity);
        //v1_eos_records_page_0_limit_0
        log.info("usdt_mortgage_redeem_0_9,抵押成功后，EUSD预期：【资产】【EUSD】【资产详情】页面订单状态(9为正确)是："+arr[0]);
        log.info("usdt_mortgage_redeem_0_9,抵押成功后，EUSD预期：【资产】【EUSD】【资产详情】页面订单类型是(1为正确)是："+arr[1]);
        if((arr[0]==9)&(arr[1]==1)){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        log.info("usdt_mortgage_redeem_0_9,测试结束");
    }




    //点击赎回
    @Test(dependsOnMethods = { "usdt_mortgage_redeem_0_9" },description = "赎回成功后，USDT预期：【资产】【USDT】已抵押余额为0")
    //十、赎回成功后，USDT预期：【资产】【USDT】已抵押余额为0
    public void usdt_mortgage_redeem_0_10(ITestContext context) throws IOException {
        log.info("usdt_mortgage_redeem_0_10,赎回成功后，USDT预期：【资产】【USDT】已抵押余额为0");
        String eu_token = (String) context.getAttribute("eu_token");
        User user = new User(eu_token);
        //获取已抵押金额,点击全部赎回,服务器返回200则说明点击赎回成功
        BigDecimal before_usdt_mortgaged = user.v1_usdt_return_mortgaged();
        context.setAttribute("before_usdt_mortgaged",before_usdt_mortgaged);
        int v1_usdt_release=user.v1_usdt_release (before_usdt_mortgaged);
        BigDecimal after_usdt_mortgaged = user.v1_usdt_return_mortgaged();
        log.info("usdt_mortgage_redeem_0_10,v1_usdt_release的值(200则正确)是:"+v1_usdt_release);
        log.info("usdt_mortgage_redeem_0_10,after_usdt_mortgaged的值(0则正确)是:"+after_usdt_mortgaged);
        //获得现已抵押余额,=0则测试通过
        //如果赎回成功(服务器返回200),且已抵押金额为0,则测试通过
        if((200==v1_usdt_release)&(after_usdt_mortgaged.compareTo(new BigDecimal(0))==0)){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        log.info("usdt_mortgage_redeem_0_10,测试结束");
    }



    @Test(dependsOnMethods = { "usdt_mortgage_redeem_0_10" },description = "赎回成功后，USDT预期：【资产】【USDT】【资产详情】页面订单状态为:“抵押”“已抵押”")
    //十一、赎回成功后，USDT预期：【资产】【USDT】【资产详情】页面订单状态为:“抵押”“已抵押”
    public void usdt_mortgage_redeem_0_11(ITestContext context) throws IOException, InterruptedException {
        log.info("usdt_mortgage_redeem_0_11,抵押成功后，USDT预期：【资产】【USDT】【资产详情】页面订单状态和类型为“抵押”“已抵押”");
        Thread.sleep(10*1000);
        String eu_token = (String) context.getAttribute("eu_token");
        User user = new User(eu_token);

        //2、获取下单金额，根据下单金额获取订单，然后获取订单的状态
        BigDecimal amount = (BigDecimal) context.getAttribute("usdt_available");

        int arr[] = user.v1_usdt_records( amount);
        log.info("usdt_mortgage_redeem_0_4,抵押成功后，USDT预期：【资产】【USDT】【资产详情】页面订单状态(4为正确)是："+arr[0]);
        log.info("usdt_mortgage_redeem_0_4,抵押成功后，USDT预期：【资产】【USDT】【资产详情】页面订单类型是(5为正确)是："+arr[1]);
        if((arr[0]==4)&(arr[1]==5)){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        log.info("usdt_mortgage_redeem_0_4,测试结束");

    }




    @Test(dependsOnMethods = { "usdt_mortgage_redeem_0_11" },description = "赎回成功后，EUSD预期：【资产】【EUSD】可用余额减少"        )
    //十二、赎回成功后，EUSD预期：【资产】【EUSD】可用余额减少
    public void usdt_mortgage_redeem_0_12(ITestContext context) throws IOException {
        log.info("usdt_mortgage_redeem_0_12测试开始");
        String eu_token = (String) context.getAttribute("eu_token");
        User user = new User(eu_token);
        //获取前EUSD可用资产(抵押成功后的可用资产,before_)
        BigDecimal before_eusd_available = (BigDecimal) context.getAttribute("before_eusd_available");

        //获取赎回的金额
        BigDecimal before_usdt_mortgaged = (BigDecimal) context.getAttribute("before_usdt_mortgaged");

            //数据拼接转换,//10000.0000==>100000000开始
            //先转成double
            DecimalFormat df = new DecimalFormat( "0");
            Double dou_before_usdt_mortgaged= before_usdt_mortgaged.doubleValue();
            log.info("usdt_mortgage_redeem_0_12,进行数据转换");
            log.info("usdt_mortgage_redeem_0_12,dou_before_usdt_mortgaged:"+dou_before_usdt_mortgaged);
            //再转成String,并去除小数点和最后一位(因为转double的时候,会带XX.0)
            String str_before_usdt_mortgaged = String.valueOf(dou_before_usdt_mortgaged).replace(".","");
            String str_before_usdt_mortgaged_1 =str_before_usdt_mortgaged.substring(0,str_before_usdt_mortgaged.length()-1);
            String str_before_usdt_mortgaged_2 =str_before_usdt_mortgaged_1+"0000";
            log.info("usdt_mortgage_redeem_0_12,str_before_usdt_mortgaged_2:"+str_before_usdt_mortgaged_2);
            //再转成BigDecimal进行比较
            BigDecimal bigDecimal_before_usdt_mortgaged_2 = new BigDecimal(str_before_usdt_mortgaged_2);
            log.info("usdt_mortgage_redeem_0_12,bigDecimal_usdt_available:"+bigDecimal_before_usdt_mortgaged_2);
            //数据拼接转换,//10000.0000==>100000000结束



        //获取现EUSD可用资产
        BigDecimal after_eusd_available = user.get_EUSD_available();

        //前EUSD可用资产-赎回金额=现EUSD可用资产
        log.info("usdt_mortgage_redeem_0_12,前EUSD可用资产-赎回金额=现EUSD可用资产,则测试通过,参与计算的数值是:");
        log.info("usdt_mortgage_redeem_0_12,before_eusd_available是:"+before_eusd_available);
        log.info("usdt_mortgage_redeem_0_12,bigDecimal_before_usdt_mortgaged_2是:"+bigDecimal_before_usdt_mortgaged_2);        //数据可能不对,有加.
        log.info("usdt_mortgage_redeem_0_12,after_eusd_available是:"+after_eusd_available);
        if(before_eusd_available.subtract(bigDecimal_before_usdt_mortgaged_2).compareTo(after_eusd_available)==0){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        log.info("usdt_mortgage_redeem_0_12测试结束");
    }



    @Test(dependsOnMethods = { "usdt_mortgage_redeem_0_12" },description = "赎回成功后，EUSD预期：【资产】【EUSD】冻结金额不变"   )
    //十三、赎回成功后，EUSD预期：【资产】【EUSD】冻结金额不变
    public void usdt_mortgage_redeem_0_13(ITestContext context) throws IOException, InterruptedException {
        log.info("usdt_mortgage_redeem_0_13,赎回成功后，EUSD预期：【资产】【EUSD】冻结金额不变");
        String eu_token = (String) context.getAttribute("eu_token");
        User user = new User(eu_token);

        //获取原EUSD冻结金额（是抵押成功后的冻结金额，不是最初的冻结金额）
        BigDecimal before_eusd_frozen_reduce = (BigDecimal) context.getAttribute("before_eusd_frozen_reduce");

        //获取现冻结金额
        BigDecimal after_eusd_frozen =user.get_EUSD_frozen();

        //原EUSD冻结金额=现EUSD冻结金额,则测试通过
        log.info("usdt_mortgage_redeem_0_13,现冻结金额+USDT抵押金额=原EUSD冻结金额，则测试通过。 参与比较的数值是：");
        log.info("usdt_mortgage_redeem_0_13,before_eusd_frozen_reduce是："+before_eusd_frozen_reduce);
        log.info("usdt_mortgage_redeem_0_13,after_eusd_frozen是："+after_eusd_frozen);
        //现冻结金额+USDT抵押金额=原EUSD冻结金额
        if(before_eusd_frozen_reduce.compareTo(after_eusd_frozen)==0){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        log.info("usdt_mortgage_redeem_0_13测试结束");
    }




    @Test(dependsOnMethods = { "usdt_mortgage_redeem_0_13" },description = "赎回成功后，EUSD预期：【资产】【EUSD】【资产详情】页面显示记录"  )
    //十四、赎回成功后，EUSD预期：【资产】【EUSD】【资产详情】页面显示记录
    public void usdt_mortgage_redeem_0_14(ITestContext context) throws IOException, InterruptedException {
        log.info("usdt_mortgage_redeem_0_14,抵押成功后，EUSD预期：【资产】【USDT】【资产详情】页面显示记录");
        Thread.sleep(10*1000);
        String eu_token = (String) context.getAttribute("eu_token");
        User user = new User(eu_token);

        //2、获取下单金额，根据下单金额获取订单，然后获取订单的状态
        BigDecimal quantity = (BigDecimal) context.getAttribute("change_usdt_available");
        Thread.sleep(3*60*1000);  //status参数出来,需要等3分钟
        int arr[] = user.v1_eusd_records(quantity);

        log.info("usdt_mortgage_redeem_0_14,抵押成功后，EUSD预期：【资产】【EUSD】【资产详情】页面订单状态(10为正确)是："+arr[0]);
        log.info("usdt_mortgage_redeem_0_14,抵押成功后，EUSD预期：【资产】【EUSD】【资产详情】页面订单类型是(1为正确)是："+arr[1]);
        if((arr[0]==10)&(arr[1]==1)){
            Assert.assertTrue(true);
        }else{
            Assert.assertTrue(false);
        }
        log.info("usdt_mortgage_redeem_0_14,测试结束");
    }
}
