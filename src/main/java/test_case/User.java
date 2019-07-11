package test_case;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
//import org.apache.http.impl.client.DefaultHttpClient;

//import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;






public class User implements  Runnable {
    public static Log log = LogFactory.getLog(User.class);
    private static ResourceBundle bundle;

    //属性
    private static String response_token = null;        //登录唯一token，login方法获取user_name//
    private static String user_name = null;  //用户姓名
    private static BigInteger id = null;                 //订单唯一ID，buy方法获取
    private static String base_url = "http://47.97.221.140:81/";
    private static LinkedList user_info = null;             //用户信息，创建对象时，执行初始化
    private String mobile;
    private String role;
    CountDownLatch latch;

    //0604，实例化对象，充值EUSD等待多线程中，eu_user的token一直被替换，所以建立数组进行存储
    // LinkedList<LinkedList<String>> save_eu_token = new LinkedList<LinkedList<String>>();
    LinkedList<String> save_eu_user_token = new LinkedList<String>();
    LinkedList<String> save_user_token = new LinkedList<String>();

    //构造方法
    public User() {

    }

    //构造方法，传入token，创建对象
    public User(String response_token) {
        this.response_token = response_token;
        //构造方法中先设置base_url
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        base_url = bundle.getString("base_url");
    }


    /**
     * 构造方法，传入用户身份、手机号、线程，初始化对象
     * 0602
     */
    public User(String role, String mobile, CountDownLatch latch) throws IOException {
        this.mobile = mobile;
        this.role = role;
        this.latch = latch;
        //构造方法中设置base_url
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        base_url = bundle.getString("base_url");

        //1、注册（输入邀请码）
        boolean sms_sendcode_result = sms_sendcode(this.mobile);
        //2、登录获取token，token作为私有变量，被对象其他方法使用
        this.response_token = login_and_return_token(this.mobile);
    }


    /**
     * 实例化对象
     * 0602
     */
    @Override
    public void run() {
        //实例化user、eu_user对象、实例化结果上传数据库、判断实例化结果
        log.info("实例化对象开始,查看本次的role是：" + role + ",查看mobile是:" + mobile);
        try {
            if (role.equals("user")) {
                save_user_token.add(response_token);
                //1、设置支付密码
                boolean add_payment_methods_result = add_payment_methods(save_user_token.getLast());
                Thread.sleep(1 * 1000);
                //2、添加支付方式
                boolean setpassword_result = setpassword_2(save_user_token.getLast());
                Thread.sleep(1 * 1000);
                //3、充值EUSD并等待解冻
                v1_test_eos(save_user_token.getLast());
                Thread.sleep(15 * 1000);
                v1_test_eos(save_user_token.getLast());
                //4、等待10分钟解冻
                Thread.sleep(10 * 60 * 1000);
                //5、查看EUSD可用余额
                boolean get_EUSD_available_result = get_EUSD_available_for_thread(save_user_token.getLast());

                //6 实例化结果上传数据库、判断实例化结果    //user用户:手机号\设置支付密码结果\添加支付方式结果\EUSD的余额
                LinkedList user_initialzation_result = new LinkedList();
                user_initialzation_result.add(mobile);
                user_initialzation_result.add(role);
                user_initialzation_result.add(get_EUSD_available_result);
                user_initialzation_result.add(add_payment_methods_result);
                user_initialzation_result.add(setpassword_result);
                log.info("初始化user用户中,查看初始化的结果数组:" + user_initialzation_result);
                before_fun.insert_initialzation_result(user_initialzation_result);

                //6、查看初始化的结果
                log.info("普通用户初始化，此处记录初始化的结果：");
                log.info("普通用户初始化，当前初始化的手机号是：" + mobile);
                log.info("普通用户初始化，当前token是：" + save_user_token.getLast());
                log.info("普通用户初始化，添加收付款方式:" + add_payment_methods_result);
                log.info("普通用户初始化，设置支付密码:" + setpassword_result);
                log.info("普通用户初始化，充值并等待10分钟，查看EUSD余额是否大于0:" + get_EUSD_available_result);
                log.info("普通用户初始化方法结束");
                log.info("countDown-1");
                latch.countDown();

            } else if (role.equals("eu_user")) {
                save_eu_user_token.add(response_token);//存储当前的token和手机号
                //1、添加支付方式
                boolean add_payment_methods_result = add_payment_methods(save_eu_user_token.getLast());
                //2、设置支付密码
                boolean setpassword_result = setpassword_2(save_eu_user_token.getLast());
                //3、普通用户调用接口直接成为承兑商
                boolean becomeExchanger_return_code = test_becomeExchanger(save_eu_user_token.getLast());
                //4、承兑商USDT充值10000
                boolean test_usdt_result = test_usdt(save_eu_user_token.getLast());

                //5、充值EUSD并等待解冻
                v1_test_eos(save_eu_user_token.getLast());
                Thread.sleep(15 * 1000);
                v1_test_eos(save_eu_user_token.getLast());
                Thread.sleep(10 * 60 * 1000);
                //6、查看EUSD可用余额
                boolean get_EUSD_available_result = get_EUSD_available_for_thread(save_eu_user_token.getLast());

                //7、EUSD从【资产】页面划转到【承兑页面】
                boolean v1_exchange_transferInfo = v1_exchange_transferInfo(save_eu_user_token.getLast());
                Thread.sleep(5 * 1000);

                //8、承兑商【承兑】页面开启收款服务、支付服务（微信/银行卡/支付宝）
                boolean va_buy_start_result = va_buy_start(true, save_eu_user_token.getLast());

                //9、承兑商【承兑】页面开启支付服务（微信/银行卡/支付宝）
                boolean va_sell_start_result = va_sell_start(true, save_eu_user_token.getLast());


                //10、 实例化结果上传数据库、判断实例化结果    //user用户:手机号\设置支付密码结果\添加支付方式结果\EUSD的余额
                LinkedList eu_user_initialzation_result = new LinkedList();
                eu_user_initialzation_result.add(mobile);
                eu_user_initialzation_result.add(role);
                eu_user_initialzation_result.add(get_EUSD_available_result);
                eu_user_initialzation_result.add(add_payment_methods_result);
                eu_user_initialzation_result.add(setpassword_result);

                eu_user_initialzation_result.add(becomeExchanger_return_code);
                eu_user_initialzation_result.add(test_usdt_result);
                eu_user_initialzation_result.add(v1_exchange_transferInfo);
                eu_user_initialzation_result.add(va_buy_start_result);
                eu_user_initialzation_result.add(va_sell_start_result);

                log.info("初始化eu_user用户中,查看初始化的结果数组:" + eu_user_initialzation_result);
                before_fun.insert_initialzation_result(eu_user_initialzation_result);

                //11、查看初始化结果
                log.info("承兑商用户初始化，此处记录初始化的结果：");
                log.info("承兑商用户初始化，当前初始化的手机号是：" + mobile);
                log.info("承兑商用户初始化，当前token是" + save_eu_user_token.getLast());
                log.info("承兑商用户初始化，添加支付方式:" + add_payment_methods_result);
                log.info("承兑商用户初始化，设置支付密码:" + setpassword_result);
                log.info("普通用户调用接口直接成为承兑商:" + becomeExchanger_return_code);
                log.info("承兑商用户初始化，USDT充值10000,:" + test_usdt_result);
                log.info("承兑商用户初始化，充值并等待10分钟，查看EUSD余额，大于0为正确:" + get_EUSD_available_result);
                log.info("承兑商用户初始化，EUSD从【资产】页面划转到【承兑页面】:" + v1_exchange_transferInfo);
                log.info("承兑商用户初始化，【承兑】页面开启收款服务:" + va_buy_start_result);
                log.info("承兑商用户初始化，【承兑】页面开启支付服务:" + va_sell_start_result);
                log.info("承兑商用户初始化方法结束：");

                log.info("countDown-1");
                latch.countDown();
            } else {
                log.info("run()方法中初始化用户操作异常，请检查");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            log.info("实例化对象并把数据保存到数据库方法结束");
        }
    }


    /**
     * 初始化对象：添加收付款设置（默认新建账户为开启状态，服务器返回的status=1说明可用，返回0是禁用）
     * return: boolen
     */
    public boolean add_payment_methods(String eu_user_response_token) throws IOException {
        log.info("添加收付款设置方法开始:");
        boolean return_result;
        String result;
        //一、拼接URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_payment_methods");
        //存储数据，用于传递参数、与返回值比较
        String account = bundle.getString("v1_payment_methods_account");
        String bank = new String(bundle.getString("v1_payment_methods_bank").getBytes("ISO-8859-1"), "gbk");                //java中properties配置文件默认的编码为：ISO-8859-1，是不支持中文的，所以会乱码,需要转码
        String name = new String(bundle.getString("v1_payment_methods_name").getBytes("ISO-8859-1"), "gbk");
        int type = Integer.parseInt(bundle.getString("v1_payment_methods_type_4"));

        //二、发送请求，获取返回token
        HttpPost post = new HttpPost(base_url + url);
        //1、header数据
        post.addHeader("client-type", "app");
        post.addHeader("cookie", "token=" + eu_user_response_token);
        log.info("添加收付款方式中，当前mobile是：" + mobile);
        log.info("添加收付款方式中，当前token是：" + eu_user_response_token);
        //2、data数据
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("account", account);
        jsonParam.put("bank", bank);
        jsonParam.put("name", name);
        jsonParam.put("type", type);
        jsonParam.put("verify_code", bundle.getString("verify_code"));

        log.info("添加收付款设置方法，此次发送请求的请求头信息是:" + jsonParam.toString());
        StringEntity entity = new StringEntity(jsonParam.toString(), "utf-8");
        post.setEntity(entity);
        //3、发送请求，获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(post);
        result = EntityUtils.toString(response.getEntity());
        log.info("添加收付款设置方法，请求返回的result是:" + result);

        //4、抽取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        Integer getcode = (Integer) resultjson.get("code");
        Header[] header = response.getAllHeaders();
        log.info("添加收付款设置方法，请求返回的header是:" + Arrays.toString(header));  //此处查看Content-Length的长度，会出现服务器返回200，但是长度过小，无返回内容的情况。
        if (getcode == 200) {
            log.info("添加收付款设置方法,服务器返回200，测试继续");
            //3、抽取数据(银行卡名称、银行卡账号、账户类型4）
            JSONObject responseData = (JSONObject) resultjson.get("data");
            //银行卡号
            String getaccount = responseData.get("account").toString();
            log.info("添加收付款设置方法,服务器返回的accounts是:" + getaccount);
            //银行名称
            String getbank = responseData.get("bank").toString();
            log.info("添加收付款设置方法,服务器返回的bank是:" + getbank);
            //交易方式状态
            String getstatus = responseData.get("status").toString();
            log.info("添加收付款设置方法,服务器返回的getstatus是:" + getstatus);
            if (getaccount.equals(account) & (getbank.equals(bank))) {
                log.info("添加收付款设置方法成功，当前账户状态（1为可用，0为禁用）是：" + getstatus);
                return_result = true;
            } else {
                log.error("添加收付款设置方法失败");
                return_result = false;
            }
        } else {
            log.info("添加收付款设置方法,服务器返回不是200，测试异常，请检查");
            return_result = false;
        }
        return return_result;
    }


    /**
     * 初始化对象：短信验证码方式添加支付密码
     * return: boolean status,支付密码状态，0是未设置，1是可用，2是停用
     */
    public boolean setpassword_2(String eu_user_response_token) throws IOException {
        log.info("短信验证码方式添加支付密码方法开始:");
        boolean return_result;
        String result;
        //一、拼接URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_payment_password_setpassword");

        //二、发送请求，获取返回token
        HttpPost post = new HttpPost(base_url + url);
        //1、header数据
        post.addHeader("client-type", "app");
        post.addHeader("cookie", "token=" + eu_user_response_token);
        //2、data数据
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("method", 2);
        jsonParam.put("newpwd", bundle.getString("newpwd"));
        jsonParam.put("verify_code", bundle.getString("verify_code"));

        log.info("短信验证码方式添加支付密码方式中，当前mobile是：" + mobile);
        log.info("短信验证码方式添加支付密码方式中，当前token是：" + eu_user_response_token);

        log.info("短信验证码方式添加支付密码方法，此次发送请求的请求头信息是:" + jsonParam.toString());
        StringEntity entity = new StringEntity(jsonParam.toString(), "utf-8");
        post.setEntity(entity);
        //3、发送请求，获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(post);
        result = EntityUtils.toString(response.getEntity());
        log.info("短信验证码方式添加支付密码方法，请求返回的result是:" + result);

        //4、抽取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        Integer getcode = (Integer) resultjson.get("code");
        Header[] header = response.getAllHeaders();
        log.info("短信验证码方式添加支付密码方法，请求返回的header是:" + Arrays.toString(header));  //此处查看Content-Length的长度，会出现服务器返回200，但是长度过小，无返回内容的情况。
        if (getcode == 200) {
            log.info("短信验证码方式添加支付密码方法,服务器返回200，测试继续");
            //3、抽取数据
            JSONObject responseData = (JSONObject) resultjson.get("data");
            int getstatus = (int) responseData.get("status");
            if (getstatus == 1) {
                log.info("短信验证码方式添加支付密码方法,返回的status是(1正确)" + getstatus);
                return_result = true;
            } else {
                log.error("短信验证码方式添加支付密码方法,返回的status是(1正确)" + getstatus);
                return_result = false;
            }
        } else {
            log.info("短信验证码方式添加支付密码方法,服务器返回不是200，测试异常，请检查");
            return_result = false;
        }
        return return_result;
    }


    /**
     * 初始化对象：充值2次EUSD
     * return BigDecimal eusd_available
     * 0602
     */
    public void v1_test_eos(String eu_user_response_token) throws IOException {
        log.info("充值EUSD方法开始:");
        BigDecimal eusd_available = null;
        String result;
        //一、拼接URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_test_ros");

        //二、发送请求
        HttpGet get = new HttpGet(base_url + url);
        //1、header数据
        get.addHeader("client-type", "app");
        get.addHeader("cookie", "token=" + eu_user_response_token);

        //3、发送请求，获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(get);
        log.info("充值EUSD方法，每次充值后等待15秒");
        result = EntityUtils.toString(response.getEntity());
        log.info("充值EUSD方法，请求返回的result是:" + result);
        //4、抽取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        Integer getcode = (Integer) resultjson.get("code");
        if (getcode == 200) {
            log.info("充值EUSD方法,服务器返回200，充值成功");
        } else {
            log.error("充值EUSD方法,服务器返回不是200，测试异常，请检查");
        }
    }


//    /**
//    * 普通用户初始化(调用其他方法初始化）
//     *
//    * */
//    public void user_initilaize(String mobile) throws IOException, InterruptedException {
//        log.info("普通用户初始化方法开始");
//
//        //1、注册（输入邀请码）
//        boolean sms_sendcode_result = sms_sendcode(mobile);
//        //2、登录获取token，token作为私有变量，被对象其他方法使用
//        String token = login_and_return_token(mobile);
//        //3、设置支付密码、添加支付方式
//        boolean add_payment_methods_result= add_payment_methods();
//        boolean setpassword_result = setpassword_2();
//        //4、充值EUSD,获取EUSD可用余额
//        BigDecimal eusd_available =v1_test_eos(response_token);
//        Thread.sleep(15*1000);      //等待15秒让EUSD解冻
//        log.info("普通用户初始化方法,初始化的结果是：");
//        log.info("sms_sendcode_result:"+sms_sendcode_result);
//        log.info("add_payment_methods_result:"+add_payment_methods_result);
//        log.info("setpassword_result:"+setpassword_result);
//        log.info("eusd_available:"+eusd_available);
//        log.info("普通用户初始化方法结束");
//    }


//    /**
//    * 承兑商用户初始化(调用普通用户的初始化方法+承兑商专属的收款服务、支付服务）
//    * */
//
//    public void eu_user_initilaize(String mobile) throws IOException, InterruptedException {
//        log.info("承兑商用户初始化方法开始");
//        //1、注册（输入邀请码）；2、登录获取token，token作为私有变量，被对象其他方法使用；3、设置支付密码、添加支付方式；4、充值EUSD,获取EUSD可用余额
//        user_initilaize(mobile);
//        //2、普通用户调用接口直接成为承兑商
//        boolean becomeExchanger_return_code = test_becomeExchanger();
//        //3、承兑商USDT充值10000
//        int test_usdt_code= test_usdt();
//        //4、EUSD从【资产】页面划转到【承兑页面】
//        int exchange_transferInfo_code = v1_exchange_transferInfo();
//        //5、承兑商【承兑】页面开启收款服务、支付服务（微信/银行卡/支付宝）
//        int buy_start_code = va_buy_start(true);
//        int sell_start_code = va_sell_start(true);
//
//        log.info("承兑商用户初始化方法,初始化的结果是（此处先调用普通用户的初始化方法）：");
//        log.info("becomeExchanger_return_code:"+becomeExchanger_return_code);
//        log.info("test_usdt_code:"+test_usdt_code);
//        log.info("exchange_transferInfo_code:"+exchange_transferInfo_code);     //200
//        log.info("buy_start_code:"+buy_start_code);
//        log.info("sell_start_code:"+sell_start_code);
//        log.info("承兑商用户初始化方法结束：");
//    }


    /**
     * 初始化对象：输入邀请码
     * param: String mobile   （携带的邀请码是13275046710一级代理指定的邀请码，写在.properties文件里）
     * return int code
     * 0602
     */
    public static boolean sms_sendcode(String mobile) throws IOException {
        log.info("输入邀请码方法开始");
        String result;
        boolean return_result;
        //一、拼接URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_sms_sendcode");

        //二、发送请求，获取返回token
        HttpPost post = new HttpPost(base_url + url);
        //1、header数据
        post.addHeader("client-type", "app");
        //2、data数据
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("action", "login");
        jsonParam.put("invite_code", bundle.getString("invite_code"));
        jsonParam.put("mobile", mobile);
        jsonParam.put("national_code", bundle.getString("national_code_86"));
        log.info("输入邀请码方法，此次发送请求的请求头信息是:" + jsonParam.toString());
        StringEntity entity = new StringEntity(jsonParam.toString(), "utf-8");
        post.setEntity(entity);
        //3、发送请求，获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(post);
        result = EntityUtils.toString(response.getEntity());
        log.info("输入邀请码方法，请求返回的result(200表示成功,first:false表示不是第一次登录)是:" + result);  //result，code=200，服务器返回成功；同个手机号可能会有不同的UID，UID相同，才会把已登录的账号顶掉； UID与国家地区有关，如果报错，查看national_code国家地区码有没有错误；
        //4、抽取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        Integer getcode = (Integer) resultjson.get("code");
        if (getcode == 200) {
            log.info("输入邀请码方法,服务器返回200，测试继续");
            return_result = true;
        } else {
            log.error("输入邀请码方法,服务器返回不是200，测试异常，请检查");
            return_result = false;
        }
        return return_result;
    }


    /*
     * EUSD，获取购买的数量
     * return :BigInteger EUSD_buy_quantity
     *
     * */
    public BigDecimal get_EUSD_buy_quantity() {
        log.info("获取EUSD购买数量方法开始");
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String string_EUSD_buy_quantity = bundle.getString("EUSD_buy_quantity");
        BigDecimal EUSD_buy_quantity = new BigDecimal(string_EUSD_buy_quantity);
        log.info("获取EUSD购买数量方法结束");
        return EUSD_buy_quantity;
    }

    /**
     * USDT/EUSD，获取USDT转账的数量
     * return :BigDecimal EUSD_sell_quantity
     */
    public BigDecimal get_USDT_and_EUSD_transfer_amount() {
        log.info("获取USDT转账的数量方法开始");
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String string_transfer_amount = bundle.getString("transfer_amount");
        BigDecimal USDT_transfer_amount = new BigDecimal(string_transfer_amount);
        log.info("获取USDT转账的数量方法结束");
        return USDT_transfer_amount;
    }


    /**
     * EUSD，获取出售的数量
     * return :BigDecimal EUSD_sell_quantity
     */
    public BigDecimal get_EUSD_sell_quantity() {
        log.info("获取EUSD出售数量方法开始");
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String string_EUSD_sell_quantity = bundle.getString("EUSD_sell_quantity");
        BigDecimal EUSD_sell_quantity = new BigDecimal(string_EUSD_sell_quantity);
        log.info("获取EUSD出售数量方法结束");
        return EUSD_sell_quantity;
    }


    /**
     * 获取测试的手机号  (此方法仅用于获取普通用户，用户手机号存储在.properties文件里面。 承兑商手机号是随机匹配的)
     * return: String mobile
     */
    public String get_user_mobile() {
        log.info("获取测试的手机号方法开始");
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String user_mobile = bundle.getString("user_mobile");
        log.info("获取测试的手机号方法结束");
        return user_mobile;
    }


    /**
     * 登录并获取token（获取token后，把token设置成属性，供其他方法使用）
     * param: String mobile
     * return: String response_token
     * 0602
     */
    public String login_and_return_token(String mobile) throws IOException {
        log.info("用户登录后获取token方法开始:");
        String result;
        //一、拼接URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_user_login");

        //二、发送请求，获取返回token
        HttpPost post = new HttpPost(base_url + url);
        //1、header数据
        post.addHeader("client-type", "app");
        //2、data数据
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("invite_code", bundle.getString("invite_code"));      //邀请码
        jsonParam.put("mobile", mobile);
        jsonParam.put("national_code", bundle.getString("national_code_86"));
        jsonParam.put("verify_code", bundle.getString("verify_code"));
        log.info("用户登录后获取token方法，此次发送请求的请求头信息是:" + jsonParam.toString());
        StringEntity entity = new StringEntity(jsonParam.toString(), "utf-8");
        post.setEntity(entity);
        //3、发送请求，获取返回
        DefaultHttpClient client = new DefaultHttpClient();

        HttpResponse response = client.execute(post);
        result = EntityUtils.toString(response.getEntity());
        log.info("用户登录后获取token方法，请求返回的result是:" + result);  //result，code=200，服务器返回成功；同个手机号可能会有不同的UID，UID相同，才会把已登录的账号顶掉； UID与国家地区有关，如果报错，查看national_code国家地区码有没有错误；

        //4、抽取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        Integer getcode = (Integer) resultjson.get("code");
        Header[] header = response.getAllHeaders();
        log.info("用户登录并获取token方法，请求返回的header是:" + Arrays.toString(header));//此处查看Content-Length的长度，会出现服务器返回200，但是长度过小，无返回内容的情况。
        if (getcode == 200) {
            log.info("用户登录并获取token方法,服务器返回200，测试继续");
            //获取cookie里面的token
            CookieStore cookieStore = client.getCookieStore();
            List<Cookie> cookies = cookieStore.getCookies();
            response_token = cookies.get(0).getValue();
            log.info("用户登录并获取token方法,返回的token是：" + response_token);
        } else {
            log.error("用户登录并获取token方法,服务器返回不是200，测试异常");
        }
        return response_token;
    }


    /**
     * 修改用户名
     */
    public String change_user_name() throws IOException {
        log.info("修改用户名方法开始:");
        String result;
        String change_name;
        //一、拼接URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_user_info");

        //二、发送请求，获取返回token
        HttpPut put = new HttpPut(base_url + url);
        //1、header数据
        put.addHeader("client-type", "app");
        put.addHeader("cookie", "token=" + response_token);
        //2、data数据
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("name", bundle.getString("change_name"));

        log.info("修改用户名方法，此次发送请求的请求头信息是:" + jsonParam.toString());
        StringEntity entity = new StringEntity(jsonParam.toString(), "utf-8");
        put.setEntity(entity);
        //3、发送请求，获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(put);
        result = EntityUtils.toString(response.getEntity());
        log.info("修改用户名方法，请求返回的result是:" + result);
        //4、抽取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        Integer getcode = (Integer) resultjson.get("code");
        JSONObject responseData = (JSONObject) resultjson.get("data");
        change_name = (String) responseData.get("name");
        log.info("修改用户名方法,获取的用户名是：" + change_name);
        return change_name;
    }


    /**
     * 获取EUSD的可用余额
     * 内部params：实例化对象后，对象的token.（每次实例化对象，都必须先登录获取token，token传给对象的私有属性response_token,其他方法使用这个属性发送请求）
     * return： BigDecimal available；
     * （服务器返回数据是"available":1004800000,此方法返回数据是：1004800000,实际可用100480）
     */
    public BigDecimal get_EUSD_available() throws IOException {
        log.info("获取EUSD可用余额方法开始");
        String result;

        //一、获取URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_eos");

        //二、发送请求，获取返回
        HttpGet get = new HttpGet(base_url + url);
        //1、header数据
        get.addHeader("client-type", "app");
        get.addHeader("cookie", "token=" + response_token);
        //2、发送请求获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(get);
        result = EntityUtils.toString(response.getEntity());
        log.info("获取EUSD可用余额方法，请求的响应是：" + result);
        //3、抽取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        JSONObject responseData = (JSONObject) resultjson.get("data");
        JSONObject records = (JSONObject) responseData.get("balance");
        Object objectavailable = records.get("available");
        BigDecimal eusd_available = new BigDecimal(String.valueOf(objectavailable));
        log.info("获取EUSD可用余额方法，获取avilable可用余额是：" + eusd_available);
        return eusd_available;
    }


    /**
     * 获取EUSD的冻结余额
     * 内部params：对象的token
     * return: BigDecimal frozen；
     * （服务器返回数据是"available":1000000,此方法返回数据是:1000000,实际冻结100）
     */
    public BigDecimal get_EUSD_frozen() throws IOException {
        log.info("获取EUSD冻结余额方法开始");
        String result;
        //一、获取URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_eos");
        //二、发送请求，获取返回
        HttpGet get = new HttpGet(base_url + url);
        //1、header数据
        get.addHeader("client-type", "app");
        get.addHeader("cookie", "token=" + response_token);
        //2、发送请求
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(get);
        result = EntityUtils.toString(response.getEntity());
        log.info("获取EUSD冻结余额方法，请求的响应是：" + result);
        //3、抽取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        JSONObject responseData = (JSONObject) resultjson.get("data");
        JSONObject records = (JSONObject) responseData.get("balance");
        Object objectfrozen = records.get("frozen");
        BigDecimal eusd_frozen = new BigDecimal(String.valueOf(objectfrozen));
        log.info("获取EUSD冻结余额方法，获取的冻结余额是：" + eusd_frozen);
        return eusd_frozen;
    }


    /**
     * 生成验证token信息（通过支付密码生成），用于确认支付
     * 内部params：对象的token
     * 内部params：当前账户设置的支付密码，通过bundle在.properties里面获取
     * return String passwd_token
     */
    public String get_payment_password_token() throws IOException {
        log.info("生成验证token信息（通过支付密码生成），用于确认收款、支付");
        String result;
        //一、获取URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_payment_password_verifybypassword");
        //二、发送请求，获取返回
        HttpPost post = new HttpPost(base_url + url);
        //1、header数据
        post.addHeader("client-type", "app");
        post.addHeader("cookie", "token=" + response_token);
        //2、data数据
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("password", bundle.getString("pay_passwd"));  //默认值是12345
        StringEntity entity = new StringEntity(jsonParam.toString(), "utf-8");
        post.setEntity(entity);
        //3、发送请求,获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(post);
        result = EntityUtils.toString(response.getEntity());
        log.info("生成验证token信息（通过支付密码生成），用于确认收款、支付，请求的响应是：" + result);

        //4、抽取数据，返回token
        JSONObject resultjson = JSONObject.parseObject(result);
        JSONObject responseData = (JSONObject) resultjson.get("data");
        String password_token = (String) responseData.get("token");
        log.info("生成验证token信息（通过支付密码生成），用于确认收款、支付，获取的支付token是：" + password_token);
        return password_token;
    }


    /**
     * 获取第一个收付款方式       (购买前需要设置“收付款设置”，里面添加收付款账号，可以是微信、支付宝、银行卡；没有收付款方式无法进行交易； 收付款方式唯一固定，就是pay_id）
     * 内部params：token
     * return：BigInteger pay_id
     */
    public BigInteger get_payment_methods_return_pay_id() throws IOException {
        log.info("获取第一个收付款方式方法开始");
        String result;
        BigInteger pay_id = null;
        //一、获取URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_payment_methods");

        //二、发送请求，获取返回token
        HttpGet get = new HttpGet(base_url + url);
        //1、header数据
        get.addHeader("client-type", "app");
        get.addHeader("cookie", "token=" + response_token);
        //2、发送请求
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(get);
        result = EntityUtils.toString(response.getEntity());
        log.info("获取第一个收付款方式方法，请求的响应是：" + result);
        //3、抽取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        Integer getcode = (Integer) resultjson.get("code");
        if (getcode == 200) {
            log.info("获取第一个收付款方式方法，服务器返回200");
            JSONObject responseData = (JSONObject) resultjson.get("data");
            JSONArray responselist = (JSONArray) responseData.get("list");
            if (responselist.size() != 0) {
                log.info("获取第一个收付款方式方法，服务器返回200,返回的收款方式list列表不为0");
                JSONObject first = (JSONObject) responselist.get(0);
                pay_id = new BigInteger(first.getString("pmid"));
                log.info("获取第一个收付款方式方法，返回的pay_id是：" + pay_id);
                return pay_id;
            } else {
                log.error("获取第一个收付款方式方法，返回的收款方式list列表为0，请检查当前用户是否已设置收付款方式，测试异常");
            }
        } else {
            log.error("获取第一个收付款方式方法，服务器返回不是200，请检查,测试异常");
        }
        return pay_id;
    }


    /**
     * 输入金额，点击购买，返回订单唯一id
     * 内部params：pay_id ，用户设置/选择的收付款方式，13275046710这个账号只设置了一个银行卡收付款方式
     * 内部params：token
     * 内部params：quantity，金额，通过bundle在.properties里面获取
     * return：BigInteger id ,id作为对象的私有变量，供其他方法使用
     */
    public BigInteger execute_buy_return_id() throws IOException {
        log.info("执行购买，返回订单唯一ID方法开始");
        String result;
        //一、获取URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_buy");

        //二、发送请求，获取返回token
        HttpPost post = new HttpPost(base_url + url);
        //1、header数据
        post.addHeader("client-type", "app");
        post.addHeader("cookie", "token=" + response_token);
        //2、data数据
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("pay_id", get_payment_methods_return_pay_id());
        jsonParam.put("quantity", Integer.valueOf(bundle.getString("EUSD_buy_quantity")));      //quantity固定100
        jsonParam.put("token", get_payment_password_token());       //  获取确认支付token
        StringEntity entity = new StringEntity(jsonParam.toString(), "utf-8");
        post.setEntity(entity);
        //3、发送请求获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(post);
        result = EntityUtils.toString(response.getEntity());
        log.info("执行购买，返回订单唯一ID方法，请求的响应是：" + result);
        //4、抽取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        Integer getcode = (Integer) resultjson.get("code");
        if (getcode == 200) {
            log.info("执行购买，返回订单唯一ID方法,服务器返回200");
            JSONObject responseData = (JSONObject) resultjson.get("data");
            if (responseData.size() != 0) {
                log.info("执行购买，返回订单唯一ID方法,data数据不为0");
                id = new BigInteger(String.valueOf(responseData.get("id")));
                log.info("执行购买，返回订单唯一ID方法，返回的ID是：" + id);
                ;
            } else {
                log.error("执行购买，返回订单唯一ID方法,data数据长度为0，测试异常");
            }
        } else {
            log.error("执行购买，返回订单唯一ID方法,服务器返回不是200，测试异常");
        }
        return id;
    }


    /**
     * 根据订单ID，获取承兑商的手机号
     * 内部params：token
     * 内部params：id
     * return： String eu_mobile
     */
    public String according_to_orders_id_return_eu_mobile() throws IOException {
        log.info("根据订单id，获取匹配的承兑商手机号方法开始");
        String result;
        String eu_mobile;
        //一、获取URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_orders");
        String order_id = String.valueOf(id);
        //二、发送请求，获取返回token
        HttpGet get = new HttpGet(base_url + url + order_id);
        //1、header数据
        get.addHeader("client-type", "app");
        get.addHeader("cookie", "token=" + response_token);
        //2、发送请求获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(get);
        result = EntityUtils.toString(response.getEntity());
        log.info("根据订单id，获取匹配的承兑商手机号方法，请求的响应是：" + result);
        //3、抽取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        JSONObject responseData = (JSONObject) resultjson.get("data");
        eu_mobile = String.valueOf(responseData.get("eu_mobile"));
        log.info("根据订单id，获取匹配的承兑商手机号方法，匹配承兑商的手机号是：" + eu_mobile);
        return eu_mobile;
    }


    /**
     * 查看【我的】【我的订单】页面第一个订单状态    （用于判断是否生成订单，及订单状态）
     * 内部params：token
     * return：int status
     */
    public int get_first_orders_status_by_id() throws IOException {
        log.info(" 查看【我的】【我的订单】页面第一个订单状态方法开始");
        String result;

        //一、获取URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_orders_limit_20_page_0");

        //二、发送请求，获取返回token
        HttpGet get = new HttpGet(base_url + url);
        //1、header数据
        get.addHeader("client-type", "app");
        get.addHeader("cookie", "token=" + response_token);

        //2、发送请求获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(get);
        result = EntityUtils.toString(response.getEntity());
        log.info("查看【我的】【我的订单】页面第一个订单状态方法,请求的响应是：" + result);
        //3、抽取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        JSONObject responseData = (JSONObject) resultjson.get("data");
        JSONArray responselist = (JSONArray) responseData.get("list");
        JSONObject first = (JSONObject) responselist.get(0);            //获取第一个订单
        int status = Integer.parseInt(first.getString("status"));
        log.info("查看【我的】【我的订单】页面第一个订单状态方法,第一个订单状态status是：" + status);
        BigInteger return_id = new BigInteger(first.getString("id"));
        if (return_id.equals(id)) {
            if (status == 1) {
                log.info("查看【我的】【我的订单】页面第一个订单状态方法，当前订单状态为未付款");
                return status;
            } else if (status == 2) {
                log.info("查看【我的】【我的订单】页面第一个订单状态方法，当前订单状态为已付款未确认");
            } else if (status == 3) {
                log.info("查看【我的】【我的订单】页面第一个订单状态方法，当前订单状态为已确认");
            } else if (status == 4) {
                log.info("查看【我的】【我的订单】页面第一个订单状态方法，当前订单状态为已取消");
            } else if (status == 5) {
                //log.info("查看【我的】【我的订单】页面第一个订单状态方法，当前订单状态为已过期（自动取消）");
                log.info("查看【我的】【我的订单】页面第一个订单状态方法，当前订单状态为‘0605修改，该状态在写代码时未明确意义，待跟踪。’");
            } else if (status == 6) {
                log.info("查看【我的】【我的订单】页面第一个订单状态方法，当前订单状态为已过期（自动取消）");
            } else if (status == 7) {
                log.info("查看【我的】【我的订单】页面第一个订单状态方法，当前订单状态为转帐中");
            } else if (status == 8) {
                log.info("查看【我的】【我的订单】页面第一个订单状态方法，当前订单状态为已完成");
            } else {
                log.error("查看【我的】【我的订单】页面第一个订单状态方法，订单状态异常，请检查");
            }
        } else {
            log.error("查看【我的】【我的订单】页面第一个订单状态方法，最新下单的ID没有在第一行");
        }
        return status;
    }



    /**
     * 承兑商查看【承兑】【收款订单】页面第一个订单状态   （用于判断是否生成订单，及订单状态）
     * 内部params：token
     * return：int status
     */
    public int get_eu_orders_buy_status_by_id() throws IOException {
        log.info("承兑商查看【承兑】【收款订单】页面第一个订单状态方法开始 ");
        String result;
        //一、获取URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_orders_exchanger_limit_10_page_1_side_1");
        //二、发送请求，获取返回token
        HttpGet get = new HttpGet(base_url + url);
        //1、header数据
        get.addHeader("client-type", "app");
        get.addHeader("cookie", "token=" + response_token);
        //2、发送请求获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(get);
        result = EntityUtils.toString(response.getEntity());
        log.info("承兑商查看【承兑】【收款订单】页面第一个订单状态方法，请求的响应是：" + result);
        //3、抽取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        JSONObject responseData = (JSONObject) resultjson.get("data");
        JSONArray responselist = (JSONArray) responseData.get("list");
        JSONObject first = (JSONObject) responselist.get(0);
        int status = Integer.parseInt(first.getString("status"));
        BigInteger return_id = new BigInteger(first.getString("id"));
        if ((status == 1) && (return_id.equals(id))) {
            log.info("承兑商查看【承兑】【收款订单】页面第一个订单状态方法，当前订单状态为未付款");
        } else if ((status == 2) && (return_id.equals(id))) {
            log.info("承兑商查看【承兑】【收款订单】页面第一个订单状态方法，当前订单状态为已付款未确认");
        } else if ((status == 3) && (return_id.equals(id))) {
            log.info("承兑商查看【承兑】【收款订单】页面第一个订单状态方法，当前订单状态为已确认");
        } else if ((status == 4) && (return_id.equals(id))) {
            log.info("承兑商查看【承兑】【收款订单】页面第一个订单状态方法，当前订单状态为已取消");
        } else if ((status == 5) && (return_id.equals(id))) {
            log.info("承兑商查看【承兑】【收款订单】页面第一个订单状态方法，当前订单状态为已过期（自动取消）");
        } else if ((status == 6) && (return_id.equals(id))) {
            log.info("承兑商查看【承兑】【收款订单】页面第一个订单状态方法，当前订单状态为转帐中");
        } else if ((status == 7) && (return_id.equals(id))) {
            log.info("承兑商查看【承兑】【收款订单】页面第一个订单状态方法，当前订单状态为转帐中");
        } else if ((status == 8) && (return_id.equals(id))) {
            log.info("承兑商查看【承兑】【收款订单】页面第一个订单状态方法，当前订单状态为已完成");
        }
        return status;
    }

    /**
     * 承兑商用户，查看【承兑】【支付订单】页面中，第一个订单
     * 内部params：token
     * return：int status
     */
    public int get_eu_orders_buy_status_by_id_side_2() throws IOException {
        log.info("承兑商用户，查看【承兑】【支付订单】页面中，第一个订单状态方法开始");
        String result;
        //一、获取URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_orders_exchanger_limit_10_page_1_side_2");
        //二、发送请求，获取返回token
        HttpGet get = new HttpGet(base_url + url);
        //1、header数据
        get.addHeader("client-type", "app");
        get.addHeader("cookie", "token=" + this.response_token);
        //2、发送请求获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(get);
        result = EntityUtils.toString(response.getEntity());
        log.info("承兑商用户，查看【承兑】【支付订单】页面中，第一个订单状态方法，请求的响应是：" + result);
        //3、抽取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        JSONObject responseData = (JSONObject) resultjson.get("data");
        JSONArray responselist = (JSONArray) responseData.get("list");
        JSONObject first = (JSONObject) responselist.get(0);
        int status = Integer.parseInt(first.getString("status"));
        BigInteger return_id = new BigInteger(first.getString("id"));
        if ((status == 1) && (return_id.equals(id))) {
            log.info("承兑商用户，查看【承兑】【支付订单】页面中，第一个订单状态方法，当前订单状态为未付款");
        } else if ((status == 2) && (return_id.equals(id))) {
            log.info("承兑商用户，查看【承兑】【支付订单】页面中，第一个订单状态方法，当前订单状态为已付款未确认");
        } else if ((status == 3) && (return_id.equals(id))) {
            log.info("承兑商用户，查看【承兑】【支付订单】页面中，第一个订单状态方法，当前订单状态为已确认");
        } else if ((status == 4) && (return_id.equals(id))) {
            log.info("承兑商用户，查看【承兑】【支付订单】页面中，第一个订单状态方法，当前订单状态为已取消");
        } else if ((status == 5) && (return_id.equals(id))) {
            log.info("承兑商用户，查看【承兑】【支付订单】页面中，第一个订单状态方法，当前订单状态为已过期（自动取消）");
        } else if ((status == 6) && (return_id.equals(id))) {
            log.info("承兑商用户，查看【承兑】【支付订单】页面中，第一个订单状态方法，当前订单状态为转帐中");
        } else if ((status == 7) && (return_id.equals(id))) {
            log.info("承兑商用户，查看【承兑】【支付订单】页面中，第一个订单状态方法，当前订单状态为转帐中");
        } else if ((status == 8) && (return_id.equals(id))) {
            log.info("承兑商用户，查看【承兑】【支付订单】页面中，第一个订单状态方法，当前订单状态为已完成");
        }
        return status;
    }


    /**
     * 根据订单ID取消订单:购买取消
     * 内部params：token
     * params：BigInteger id_by_cancel 要取消的订单ID
     * return：int result_code
     */
    public int by_cancel(BigInteger id_by_cancel) throws IOException {
        log.info("by_cancel，根据订单ID取消订单方法开始");
        String result;

        //一、获取URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_buy");

        //二、发送请求，获取返回token
        HttpPost post = new HttpPost(base_url + url + "/" + id_by_cancel + "/cancel");
        //1、header数据
        post.addHeader("client-type", "app");
        post.addHeader("cookie", "token=" + response_token);
        //2、发送请求获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(post);
        result = EntityUtils.toString(response.getEntity());
        System.out.println("by_cancel，根据订单ID取消订单方法，请求的响应是：");
        System.out.println(result);
        //3、抽取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        int result_code = (Integer) resultjson.get("code");
        log.info("by_cancel，根据订单ID取消订单方法,服务器返回的状态码是（200说明取消成功）：" + result_code);
        return result_code;
    }


    /*
     * 根据订单ID取消订单：用户下单购买EUSD，承兑商点击取消
     * 内部params：token
     * params：BigInteger id_by_cancel 要取消的订单ID
     * return：int result_code
     * */
    public int sell_cancel(BigInteger id_by_cancel) throws IOException {
        log.info("sell_cancel，根据订单ID取消订单,返回响应方法开始");
        String result;

        //一、获取URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_sell");

        //二、发送请求，获取返回token
        HttpPost post = new HttpPost(base_url + url + "/" + id_by_cancel + "/cancel");
        //1、header数据
        post.addHeader("client-type", "app");
        post.addHeader("cookie", "token=" + response_token);
        //2、发送请求获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(post);
        result = EntityUtils.toString(response.getEntity());
        System.out.println("sell_cancel，根据订单ID取消订单,返回响应方法，请求的响应是：");
        System.out.println(result);
        //3、抽取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        int result_code = (Integer) resultjson.get("code");
        log.info("sell_cancel，根据订单ID取消订单,返回响应方法，服务器返回的状态码是（200说明取消成功）：" + result_code);
        return result_code;
    }


    /**
     * 支付订单(点击确认支付）
     * 内部params：token
     * params：id_by_pay，订单唯一ID
     * return int status
     */

    public int buy_pay(BigInteger id_by_pay) throws IOException {
        log.info("支付订单(点击确认支付）方法开始");
        String result;

        //一、获取URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_buy");
        //二、发送请求，获取返回token
        HttpPost post = new HttpPost(base_url + url + "/" + id_by_pay + "/pay");
        //1、header数据
        post.addHeader("client-type", "app");
        post.addHeader("cookie", "token=" + response_token);
        //2、发送请求获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(post);
        result = EntityUtils.toString(response.getEntity());
        log.info("支付订单(点击确认支付）方法，请求的响应是：" + result);
        //3、抽取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        int result_code = (Integer) resultjson.get("code");
        log.info("支付订单(点击确认支付）方法，服务器返回的状态码是（200说明支付成功）：" + result_code);
        return result_code;
    }


    /**
     * 输入手机号、转账金额，执行转账
     * 内部params:String mobile
     * 内部params：token
     * return：result_code
     */
    public int eos_transfer(String transfer_mobile) throws IOException {

        log.info("输入手机号、转账金额，执行转账方法开始");
        String result;

        //一、获取URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_eos_transfer");

        //二、发送请求，获取返回token
        HttpPost post = new HttpPost(base_url + url);
        //1、header数据
        post.addHeader("client-type", "app");
        post.addHeader("cookie", "token=" + response_token);
        //2、data数据
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("amount", Integer.valueOf(bundle.getString("transfer_amount")));
        jsonParam.put("mobile", transfer_mobile);
        jsonParam.put("national_code", bundle.getString("transfer_national"));
        jsonParam.put("token", get_payment_password_token());       //输入支付密码获取token
        StringEntity entity = new StringEntity(jsonParam.toString(), "utf-8");
        post.setEntity(entity);
        //3、发送请求，获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(post);
        result = EntityUtils.toString(response.getEntity());
        log.info("输入手机号、转账金额，执行转账，请求的响应是：" + result);
        //4、提取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        int result_code = (Integer) resultjson.get("code");
        log.info("输入手机号、转账金额，执行转账，服务器返回的状态码是（200说明转账成功）：" + result_code);
        return result_code;
    }


    /**
     * 查看承兑商eusd资产信息-可用余额接口
     * 内部params：token
     * return:BigInteger available
     * time: 0524
     */
    public BigDecimal v1_exchange_info_return_available() throws IOException {
        log.info("查看承兑商eusd资产信息-可用余额接口方法开始");
        String result;
        //一、获取URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_exchange_info");

        //二、发送请求，获取返回token
        HttpGet get = new HttpGet(base_url + url);
        //1、header数据
        get.addHeader("client-type", "app");
        get.addHeader("cookie", "token=" + response_token);

        //3、发送请求，获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(get);
        result = EntityUtils.toString(response.getEntity());
        log.info("查看承兑商eusd资产信息-可用余额接口方法，获取的返回是：" + result);
        //4、提取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        JSONObject responseData = (JSONObject) resultjson.get("data");
        Integer int_available = (Integer) responseData.get("available");
        BigDecimal available = BigDecimal.valueOf(int_available);
        log.info("查看承兑商eusd资产信息-可用余额接口方法，获取的available返回是：" + available);
        log.info(available);
        return available;
    }


    /*
     * 查看承兑商eusd资产信息-今日累积收款接口：获取承兑商”今日累积收款“数值
     * 内部params：token
     * return:BigDecimal sell_rmb_day
     * */
    public BigDecimal v1_exchange_info_return_buy_rmb_day() throws IOException {
        log.info("查看承兑商eusd资产信息-今日累积收款接口方法开始");
        String result;
        Integer buy_rmb_today;
        //一、获取URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_exchange_info");
        //二、发送请求，获取返回token
        HttpGet get = new HttpGet(base_url + url);
        //1、header数据
        get.addHeader("client-type", "app");
        get.addHeader("cookie", "token=" + response_token);

        //3、发送请求，获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(get);
        result = EntityUtils.toString(response.getEntity());
        log.info("查看承兑商eusd资产信息-今日累积收款接口方法，获取的返回是：" + result);
        //4、提取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        JSONObject responseData = (JSONObject) resultjson.get("data");
        //当承兑商设置”单日收款限额“的时候，buy_rmb_day参数才会出现
        if (responseData.get("buy_rmb_today") != null) {
            buy_rmb_today = (Integer) responseData.get("buy_rmb_today");
            log.info("查看承兑商eusd资产信息-今日累积收款接口方法，获取的buy_rmb_day返回是：" + buy_rmb_today);
            return BigDecimal.valueOf(buy_rmb_today);
        } else {
            log.error("查看承兑商eusd资产信息-今日累积收款接口方法,当前登录用户并未设置“今日累积收款金额”");
            return BigDecimal.valueOf(0);//如果承兑商不设置“单日收款限额”，返回0
        }
    }


    /**
     * 查看承兑商eusd资产信息-今日累积支付接口：获取承兑商”今日累积支付“数值
     * 内部params：token
     * return:BigDecimal sell_rmb_day   如果没有冻结金额,不会返回trade参数,所以返回0
     */
    public BigDecimal v1_exchange_info_return_sell_rmb_day() throws IOException {
        log.info("查看承兑商eusd资产信息-今日累积支付接口方法开始");
        String result;
        Integer sell_rmb_today;
        //一、获取URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_exchange_info");
        //二、发送请求，获取返回token
        HttpGet get = new HttpGet(base_url + url);
        //1、header数据
        get.addHeader("client-type", "app");
        get.addHeader("cookie", "token=" + response_token);

        //3、发送请求，获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(get);
        result = EntityUtils.toString(response.getEntity());
        log.info("查看承兑商eusd资产信息-今日累积支付接口方法，获取的返回是：" + result);
        //4、提取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        JSONObject responseData = (JSONObject) resultjson.get("data");
        //当承兑商设置”单日收款金额“的时候，buy_rmb_day参数才会出现
        if (responseData.get("buy_rmb_today") != null) {
            sell_rmb_today = (Integer) responseData.get("buy_rmb_today");
            log.info("查看承兑商eusd资产信息-今日累积支付接口方法，获取的sell_rmb_day返回是：" + sell_rmb_today);
            return BigDecimal.valueOf(sell_rmb_today);
        } else {
            log.error("查看承兑商eusd资产信息-今日累积支付接口方法,当前登录用户并未设置“今日累积收款金额”");
            return BigDecimal.valueOf(0);
        }
    }


    /**
     * 查看承兑商eusd资产信息-可用余额接口(【承兑】【承兑资产】模块的”冻结“)
     * 内部params：token
     * return:BigInteger available;   如果没有冻结金额,不会返回trade参数,所以返回0
     */
    public BigDecimal v1_exchange_info_return_trade() throws IOException {
        log.info("查看承兑商eusd资产信息-冻结金额接口方法开始");
        String result;
        BigDecimal trade = null;
        //一、获取URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_exchange_info");

        //二、发送请求，获取返回token
        HttpGet get = new HttpGet(base_url + url);
        //1、header数据
        get.addHeader("client-type", "app");
        get.addHeader("cookie", "token=" + response_token);

        //3、发送请求，获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(get);
        result = EntityUtils.toString(response.getEntity());
        log.info("查看承兑商eusd资产信息-冻结金额接口方法，获取的返回是：" + result);
        //4、提取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        JSONObject responseData = (JSONObject) resultjson.get("data");
        if (responseData.get("trade") != null) {       //说明当前用户有冻结余额
            Integer int_trade = (Integer) responseData.get("trade");
            trade = BigDecimal.valueOf(int_trade);
            log.info("查看承兑商eusd资产信息-冻结金额接口方法，获取的返回是：" + trade);
        } else {      //说明当前用户无冻结余额，返回0
            trade = BigDecimal.valueOf(0);
        }
        return trade;
    }

    /*
     * 承兑商点击【确认收款】
     * params:订单唯一id,在测试过程中，context获取先获取并保存订单唯一ID，调用此方法时，传入ID
     * return:int状态码
     *
     * */
    public int v1_buy_id_confirm(BigInteger order_id) throws IOException {
        log.info("承兑商点击【确认收款】接口方法开始");
        String result;
        //一、获取URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_buy");

        //二、发送请求，获取返回token
        HttpPost post = new HttpPost(base_url + url + "/" + order_id + "/confirm");
        //1、header数据
        post.addHeader("client-type", "app");
        post.addHeader("cookie", "token=" + response_token);
        //2、data数据
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("token", get_payment_password_token()); //调用方法获取支付token
        StringEntity entity = new StringEntity(jsonParam.toString(), "utf-8");
        post.setEntity(entity);
        //3、发送请求，获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(post);
        result = EntityUtils.toString(response.getEntity());
        log.info("承兑商点击【确认收款】接口方法，请求的响应是：" + result);
        //4、提取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        int getcode = (int) resultjson.get("code");
        log.info("承兑商点击【确认收款】接口方法，返回的状态码是：" + getcode);
        log.info(getcode);
        return getcode;
    }


    /*
     * 输入金额，点击出售，返回订单唯一id
     * 内部params：pay_id ，用户设置/选择的收付款方式，15594978571这个账号只设置了一个银行卡收付款方式
     * 内部params：token
     * 内部params：quantity，金额，通过bundle在.properties里面获取
     * return：BigInteger id ,id作为对象的私有变量，供其他方法使用
     *
     * */
    //执行出售操作，匹配到承兑商,返回订单ID，根据订单ID获取承兑商的手机号
    public BigInteger execute_sell_return_id() throws IOException {
        log.info("执行出售，返回订单唯一ID方法开始");
        String result;
        //一、获取URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_sell");
        //二、发送请求，获取返回token
        HttpPost post = new HttpPost(base_url + url);
        //1、header数据
        post.addHeader("client-type", "app");
        post.addHeader("cookie", "token=" + response_token);
        //2、data数据
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("pay_id", get_payment_methods_return_pay_id());
        jsonParam.put("quantity", Integer.valueOf(bundle.getString("EUSD_sell_quantity")));
        jsonParam.put("token", get_payment_password_token());
        StringEntity entity = new StringEntity(jsonParam.toString(), "utf-8");
        post.setEntity(entity);
        //3、发送请求获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(post);
        result = EntityUtils.toString(response.getEntity());
        log.info("执行出售，返回订单唯一ID方法，请求的响应是：" + result);


        //4、抽取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        Integer getcode = (Integer) resultjson.get("code");
        if (getcode == 200) {
            log.info("执行出售，返回订单唯一ID方法,服务器返回200");
            JSONObject responseData = (JSONObject) resultjson.get("data");
            if (responseData.size() != 0) {
                log.info("执行出售，返回订单唯一ID方法,data数据不为0");
                id = new BigInteger(String.valueOf(responseData.get("id")));
                log.info("执行出售，返回订单唯一ID方法，返回的ID是：" + id);
                ;
            } else {
                log.error("执行出售，返回订单唯一ID方法,data数据长度为0，测试异常");
            }
        } else {
            log.error("执行出售，返回订单唯一ID方法,服务器返回不是200，测试异常");
        }
        return id;
    }


    /*
     * 承兑商根据订单ID点击【我已付款】
     * 内部params：token
     * params：BigInteger id 订单ID
     * return：int result_code
     * */
    public int sell_id_pay(BigInteger id_by_cancel) throws IOException {
        log.info("sell_id_pay，承兑商点击【我已付款】方法开始");
        String result;

        //一、获取URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_sell");
        int result_code = 0;

        //二、发送请求，获取返回token
        HttpPost post = new HttpPost(base_url + url + "/" + id_by_cancel + "/pay");
        //1、header数据
        post.addHeader("client-type", "app");
        post.addHeader("cookie", "token=" + response_token);

        //2、data数据
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("pay_id", get_payment_methods_return_pay_id());
        StringEntity entity = new StringEntity(jsonParam.toString(), "utf-8");
        post.setEntity(entity);

        //3、发送请求获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(post);
        result = EntityUtils.toString(response.getEntity());
        System.out.println("sell_id_pay，承兑商点击【我已付款】方法开始,请求的响应是：");
        System.out.println(result);
        //4、抽取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        result_code = (Integer) resultjson.get("code");
        if (result_code == 200) {
            log.info("sell_id_pay，承兑商点击【我已付款】方法开始，服务器返回的状态码是（200说明确认支付成功）：" + result_code);
        } else {
            log.error("sell_id_pay，承兑商点击【我已付款】方法开始，服务器返回的状态码是（200说明确认支付成功）：" + result_code + ",测试异常，请检查");
        }
        return result_code;
    }


    /*
     * 用户点击【我已收款】
     * params:订单唯一id,在测试过程中，context获取先获取并保存订单唯一ID，调用此方法时，传入ID
     * return:int状态码
     *
     * */
    public int v1_sell_id_confirm(BigInteger order_id) throws IOException {
        log.info("用户点击【我已收款】接口方法开始");
        String result;
        //一、获取URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_sell");

        //二、发送请求，获取返回token
        HttpPost post = new HttpPost(base_url + url + "/" + order_id + "/confirm");
        //1、header数据
        post.addHeader("client-type", "app");
        post.addHeader("cookie", "token=" + response_token);
        //2、data数据
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("token", get_payment_password_token()); //调用方法获取支付token
        StringEntity entity = new StringEntity(jsonParam.toString(), "utf-8");
        post.setEntity(entity);
        //3、发送请求，获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(post);
        result = EntityUtils.toString(response.getEntity());
        log.info("用户点击【我已收款】接口方法，请求的响应是：" + result);
        //4、提取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        int getcode = (int) resultjson.get("code");
        log.info("用户点击【我已收款】接口方法，返回的状态码是：" + getcode);
        log.info(getcode);
        return getcode;
    }


    /**
     * 获取USDT可用余额
     * params:token
     * return:BigDecimal usdt_available
     */
    public BigDecimal v1_usdt_return_available() throws IOException {
        log.info("获取承兑商USDT可用余额方法开始");
        BigDecimal usdt_available;
        String result;
        //一、获取URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_usdt");

        //二、发送请求，获取返回token
        HttpGet get = new HttpGet(base_url + url);
        //1、header数据
        get.addHeader("client-type", "app");
        get.addHeader("cookie", "token=" + response_token);
        //2、发送请求，获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(get);
        result = EntityUtils.toString(response.getEntity());
        log.info("获取承兑商USDT可用余额方法开始请求的响应是：" + result);
        //3、提取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        JSONObject responseData = (JSONObject) resultjson.get("data");
        JSONObject records = (JSONObject) responseData.get("balance");
        Object available = records.get("available");
        log.info("获取承兑商USDT可用余额方法，获取avilable可用余额是：" + available);
        usdt_available = new BigDecimal(String.valueOf(available));
        return usdt_available;
    }


    /**
     * 获取USDT抵押余额
     * params:token
     * return:String mortgaged
     */
    public BigDecimal v1_usdt_return_mortgaged() throws IOException {
        log.info("获取承兑商USDT抵押余额方法开始");
        String result;
        BigDecimal usdt_mortgaged;
        //一、获取URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_usdt");

        //二、发送请求，获取返回token
        HttpGet get = new HttpGet(base_url + url);
        //1、header数据
        get.addHeader("client-type", "app");
        get.addHeader("cookie", "token=" + response_token);
        //2、发送请求，获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(get);
        result = EntityUtils.toString(response.getEntity());
        log.info("获取承兑商USDT可用抵押余额开始请求的响应是：" + result);
        //3、提取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        JSONObject responseData = (JSONObject) resultjson.get("data");
        JSONObject records = (JSONObject) responseData.get("balance");
        String mortgaged = (String) records.get("mortgaged");
        log.info("获取承兑商USDT抵押余额方法，获取mortgaged抵押余额是：" + mortgaged);
        usdt_mortgaged = new BigDecimal(String.valueOf(mortgaged));
        return usdt_mortgaged;
    }


    /*
     * 点击抵押USDT接口
     * params:token
     * params：BigDecimal available，测试过程中传入
     * return:int状态码
     *
     * */
    public int v1_usdt_mortgage(BigDecimal available) throws IOException {
        log.info("点击抵押USDT接口方法开始");
        String result;
        int getcode = 0;
        String str_available = String.valueOf(available);
        log.info("点击抵押USDT接口方法,传入的str_amount是:" + str_available);
        //一、获取URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_usdt_mortgage");

        //二、发送请求，获取返回token
        HttpPost post = new HttpPost(base_url + url);
        //1、header数据
        post.addHeader("client-type", "app");
        post.addHeader("cookie", "token=" + response_token);

        //2、data数据
        JSONObject jsonParam = new JSONObject();
        //jsonParam.put("amount", Integer.valueOf(bundle.getString("amount")));
        jsonParam.put("amount", str_available);     //测试过程中，传入String数据
        StringEntity entity = new StringEntity(jsonParam.toString(), "utf-8");
        post.setEntity(entity);

        //3、发送请求，获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(post);
        result = EntityUtils.toString(response.getEntity());
        log.info("点击抵押USDT接口方法开始请求的响应是：" + result);

        //4、提取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        getcode = (int) resultjson.get("code");
        if (getcode == 200) {
            log.info("点击抵押USDT接口方法，返回的code是：" + getcode);
        } else {
            log.error("点击抵押USDT接口方法，返回的code是：" + getcode + ",返回异常，请检查");
        }
        return getcode;
    }


    /**
     * 点击赎回USDT接口
     * params:token
     * params：BigDecimal available，测试过程中传入
     * return:int状态码
     */
    public int v1_usdt_release(BigDecimal amount) throws IOException {
        log.info("点击赎回USDT接口方法开始");
        String result;
        int getcode = 0;
        String str_available = String.valueOf(amount);
        log.info("点击赎回USDT接口方法,传入的str_amount是:" + str_available);
        //一、获取URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_usdt_release");

        //二、发送请求，获取返回token
        HttpPost post = new HttpPost(base_url + url);
        //1、header数据
        post.addHeader("client-type", "app");
        post.addHeader("cookie", "token=" + response_token);

        //2、data数据
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("amount", str_available);     //测试过程中，传入String数据
        StringEntity entity = new StringEntity(jsonParam.toString(), "utf-8");
        post.setEntity(entity);

        //3、发送请求，获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(post);
        result = EntityUtils.toString(response.getEntity());
        log.info("点击赎回USDT接口方法,请求的响应是：" + result);

        //4、提取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        getcode = (int) resultjson.get("code");
        if (getcode == 200) {
            log.info("点击赎回USDT接口方法，返回的code是：" + getcode);
        } else {
            log.error("点击赎回USDT接口方法，返回的code是：" + getcode + ",返回异常，请检查");
        }
        return getcode;
    }


    /**
     * USDT资产详情页面，获取第一个订单状态方法   （USDT抵押赎回时，【资产】【资产详情】页面会有订单显示，此接口获取订单列表）
     * params:token
     * params：BigDecimal amount，抵押/赎回金额，测试过程中传入
     * return:int数组，里面存放订单状态码
     */
    public int[] v1_usdt_records(BigDecimal amount) throws IOException {
        log.info("资产详情页面，获取第一个订单状态和类型方法");
        String result;
        String string_return_amount = null;
        int return_status = 0;
        int return_type = 0;
        //定义int类型的数组，用于存放订单的type和status
        int[] arr = new int[2];

        //一、获取URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_usdt_records");

        //二、发送请求，获取返回token
        HttpGet get = new HttpGet(base_url + url);
        //1、header数据
        get.addHeader("client-type", "app");
        get.addHeader("cookie", "token=" + response_token);


        //3、发送请求，获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(get);
        result = EntityUtils.toString(response.getEntity());
        log.info("USDT资产详情页面，获取第一个订单状态和类型方法,请求的响应是：" + result);

        //4、提取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        log.info("USDT资产详情页面，获取第一个订单状态和类型方法，服务器返回200");
        JSONObject responseData = (JSONObject) resultjson.get("data");
        JSONArray responselist = (JSONArray) responseData.get("list");
        if (responselist.size() != 0) {
            log.info("USDT资产详情页面，获取第一个订单状态和类型方法，服务器返回200,返回的收款方式list列表不为0");
            JSONObject first = (JSONObject) responselist.get(0);
            BigDecimal return_amount = new BigDecimal(first.getString("amount"));
            log.info("USDT资产详情页面，获取第一个订单状态和类型方法，返回的return_amount是：" + return_amount);
            log.info("USDT资产详情页面，获取第一个订单状态和类型方法，传入的amount是：" + amount);
            //if(Long.valueOf(amount).equals( Long.valueOf(return_amount))){
            if (return_amount.compareTo(amount) == 0) {
                return_status = Integer.parseInt(first.getString("status"));
                return_type = Integer.parseInt(first.getString("type"));
                if (return_type == 1) {
                    log.info("USDT资产详情页面，获取第一个订单状态和类型方法，当前订单类型是“转入”");
                    arr[0] = 1;
                } else if (return_type == 2) {
                    log.info("USDT资产详情页面，获取第一个订单状态和类型方法，当前订单类型是“转出”");
                    arr[0] = 2;
                } else if (return_type == 3) {
                    log.info("USDT资产详情页面，获取第一个订单状态和类型方法，当前订单类型是“抵押”");
                    arr[0] = 3;
                    if (return_status == 1) {
                        log.info("USDT资产详情页面，获取第一个订单状态和类型方法，当前订单状态是“抵押中”");
                        arr[1] = 1;
                    } else if (return_status == 2) {
                        log.info("USDT资产详情页面，获取第一个订单状态和类型方法，当前订单状态是“已抵押”");
                        arr[1] = 2;
                    } else if (return_status == 3) {
                        log.info("USDT资产详情页面，获取第一个订单状态和类型方法，当前订单状态是“赎回中”");
                        arr[1] = 3;
                    } else if (return_status == 5) {
                        log.info("USDT资产详情页面，获取第一个订单状态和类型方法，当前订单状态是“已赎回”");
                        arr[1] = 4;
                    }
                } else if (return_type == 4) {
                    log.info("USDT资产详情页面，获取第一个订单状态和类型方法，当前订单类型是“赎回”");
                    arr[0] = 4;
                    if (return_status == 1) {
                        log.info("USDT资产详情页面，获取第一个订单状态和类型方法，当前订单状态是“抵押中”");
                        arr[1] = 1;
                    } else if (return_status == 2) {
                        log.info("USDT资产详情页面，获取第一个订单状态和类型方法，当前订单状态是“已抵押”");
                        arr[1] = 2;
                    } else if (return_status == 3) {
                        log.info("USDT资产详情页面，获取第一个订单状态和类型方法，当前订单状态是“赎回中”");
                        arr[1] = 3;
                    } else if (return_status == 5) {
                        log.info("USDT资产详情页面，获取第一个订单状态和类型方法，当前订单状态是“已赎回”");
                        arr[1] = 5;
                    }
                } else {
                    log.error("USDT资产详情页面，获取第一个订单状态和类型方法，当前订单类型异常，请检查");
                }
            } else {
                log.error("USDT资产详情页面，获取第一个订单状态和类型方法，当前订单状态异常，请检查");
            }
        }
        return arr;
    }


    /**
     * EUSD资产详情页面，获取第一个订单状态方法   （EUSD抵押赎回时，【资产】【资产详情】页面会有订单显示，此接口获取订单列表）
     * params:token
     * params：BigDecimal amount，抵押/赎回金额，测试过程中传入
     * return:int数组，里面存放订单状态码
     */
    public int[] v1_eusd_records(BigDecimal quantity) throws IOException, InterruptedException {
        log.info("EUSD资产详情页面，获取第一个订单状态和类型方法");
        Thread.sleep(5 * 60 * 1000);    //status参数出来,需要等5分钟
        String result;
        String string_return_quantity = null;
        int return_status = 0;
        int return_type = 0;
        //定义int类型的数组，用于存放订单的type和status
        int[] arr = new int[2];

        //一、获取URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_eusd_records");

        //二、发送请求，获取返回token
        HttpGet get = new HttpGet(base_url + url);
        //1、header数据
        get.addHeader("client-type", "app");
        get.addHeader("cookie", "token=" + response_token);
        //3、发送请求，获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(get);
        result = EntityUtils.toString(response.getEntity());
        log.info("EUSD资产详情页面，获取第一个订单状态和类型方法,请求的响应是：" + result);
        //4、提取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        log.info("EUSD资产详情页面，获取第一个订单状态和类型方法，服务器返回200");
        JSONObject responseData = (JSONObject) resultjson.get("data");
        JSONArray responselist = (JSONArray) responseData.get("list");
        if (responselist.size() != 0) {
            log.info("EUSD资产详情页面，获取第一个订单状态和类型方法，服务器返回200,返回的收款方式list列表不为0");
            JSONObject first = (JSONObject) responselist.get(0);
            BigDecimal return_quantity = new BigDecimal(first.getString("quantity"));
            log.info("EUSD资产详情页面，获取第一个订单状态和类型方法，返回的return_quantity是：" + return_quantity);
            log.info("EUSD资产详情页面，获取第一个订单状态和类型方法，传入的quantity是：" + quantity);
            for (int v = 0; v < first.size(); v++) {
                if (first.get("ttype").toString() != null) {
                    return_type = Integer.parseInt(first.getString("ttype"));
                } else if (first.get("type").toString() != null) {
                    return_type = Integer.parseInt(first.getString("type"));
                } else {
                    System.out.println("没有获取到对象");
                }
            }
            //获取return_status和return_type参数进行比较
            if (return_quantity.compareTo(quantity) == 0) {
                return_status = Integer.parseInt(first.getString("status"));
                //return_type有两种结果,一种是ttype,一种是type
                for (int v = 0; v < first.size(); v++) {
                    if (first.get("ttype").toString() != null) {
                        return_type = Integer.parseInt(first.getString("ttype"));
                    } else if (first.get("type").toString() != null) {
                        return_type = Integer.parseInt(first.getString("type"));
                    } else {
                        System.out.println("没有获取到对象");
                    }
                }
                //开始判断
                if (return_type == 1) {
                    log.info("EUSD资产详情页面，获取第一个订单状态和类型方法，当前订单类型是“购买”");
                    arr[0] = 1;
                } else if (return_type == 2) {
                    log.info("EUSD资产详情页面，获取第一个订单状态和类型方法，当前订单类型是“出售”");
                    arr[0] = 2;
                } else if (return_type == 3) {
                    log.info("EUSD资产详情页面，获取第一个订单状态和类型方法，当前订单类型是“转出”");
                    arr[0] = 3;
                    if (return_status == 1) {
                        log.info("EUSD资产详情页面，获取第一个订单状态和类型方法，当前订单状态是“目前无区分都是已完成”");
                        arr[1] = 1;
                    } else {
                        log.error("EUSD资产详情页面，获取第一个订单状态和类型方法，当前订单状态异常，请检查");
                    }
                } else if (return_type == 4) {
                    log.info("EUSD资产详情页面，获取第一个订单状态和类型方法，当前订单类型是“0527未知,但测试通过”");
                    arr[0] = 4;
                    if (return_status == 1) {
                        log.info("EUSD资产详情页面，获取第一个订单状态和类型方法，当前订单状态是“目前无区分都是已完成”");
                        arr[1] = 1;
                    }
                } else if (return_type == 5) {
                    log.info("EUSD资产详情页面，获取第一个订单状态和类型方法，当前订单类型是“转入承兑承兑商仅可见”");
                    arr[0] = 5;
                } else if (return_type == 6) {
                    log.info("EUSD资产详情页面，获取第一个订单状态和类型方法，当前订单类型是“承兑转出承兑商仅可见”");
                    arr[0] = 6;
                } else if (return_type == 7) {
                    log.info("EUSD资产详情页面，获取第一个订单状态和类型方法，当前订单类型是“应用收入”");
                    arr[0] = 7;
                } else if (return_type == 8) {
                    log.info("EUSD资产详情页面，获取第一个订单状态和类型方法，当前订单类型是“应用支出”");
                    arr[0] = 8;
                } else if (return_type == 9) {
                    log.info("EUSD资产详情页面，获取第一个订单状态和类型方法，当前订单类型是“USDT抵押承兑商仅可见”");
                    arr[0] = 9;
                    if (return_status == 1) {
                        log.info("EUSD资产详情页面，获取第一个订单状态和类型方法，当前订单状态是“目前无区分都是已完成”");
                        arr[1] = 1;
                    } else {
                        log.error("EUSD资产详情页面，获取第一个订单状态和类型方法，当前订单状态异常，请检查");
                    }
                } else if (return_type == 10) {
                    log.info("EUSD资产详情页面，获取第一个订单状态和类型方法，当前订单类型是“USDT赎回承兑商仅可见”");
                    arr[0] = 10;
                    if (return_status == 1) {
                        log.info("EUSD资产详情页面，获取第一个订单状态和类型方法，当前订单状态是“目前无区分都是已完成”");
                        arr[1] = 1;
                    } else {
                        log.error("EUSD资产详情页面，获取第一个订单状态和类型方法，当前订单状态异常，请检查");
                    }
                } else if (return_type == 11) {
                    log.info("EUSD资产详情页面，获取第一个订单状态和类型方法，当前订单类型是“分润”");
                    arr[0] = 11;
                }
            } else {
                log.error("EUSD资产详情页面，获取第一个订单状态和类型方法，当前订单异常，请检查");
            }
        }
        return arr;
    }


    /**
     * 关闭平台所有承兑商方法，,静态方法，user.test_otcclear()直接调用
     * return boolean
     */
    public static boolean test_otcclear() throws IOException {
        log.info("关闭平台所有承兑商方法");
        String result;
        boolean return_result;
        //一、拼接URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_test_otcclear");

        //二、发送请求，获取返回
        HttpGet get = new HttpGet(base_url + url);
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(get);
        result = EntityUtils.toString(response.getEntity());
        log.info("关闭平台所有承兑商方法，请求返回的result是:" + result);

        //三、抽取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        Integer getcode = (Integer) resultjson.get("code");
        if (getcode == 200) {
            log.info("关闭平台所有承兑商方法，服务器返回200，测试继续");
            return_result = true;
        } else {
            log.info("关闭平台所有承兑商方法，服务器返回不是200，测试异常，请检查");
            return_result = false;
        }
        return return_result;
    }


    /**
     * 调用接口直接成为承兑商
     * params   token
     * return    T/F
     */
    public boolean test_becomeExchanger(String eu_user_response_token) throws IOException, InterruptedException {
        log.info("调用接口直接成为承兑商方法开始");
        String result;
        boolean return_code;
        //一、拼接URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_test_becomeExchanger");

        //二、发送请求，获取返回token
        HttpGet get = new HttpGet(base_url + url);
        //1、header数据
        get.addHeader("client-type", "app");
        get.addHeader("cookie", "token=" + eu_user_response_token);
//        log.info("查看成为承兑商的账户，当前用户的token是："+eu_user_response_token);
//        log.info("查看成为承兑商的账户，当前用户的mobile是："+mobile);

        //3、发送请求，获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(get);
        result = EntityUtils.toString(response.getEntity());
        log.info("调用接口直接成为承兑商方法开始，请求返回的result是:" + result);

        //4、抽取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        Integer getcode = (Integer) resultjson.get("code");
        if (getcode == 200) {
            log.info("调用接口直接成为承兑商方法开始，服务器返回200，测试继续");
            return_code = true;
        } else {
            log.info("调用接口直接成为承兑商方法开始，服务器返回不是200，测试异常，请检查");
            return_code = false;
        }
        Thread.sleep(10 * 1000);
        log.info("调用接口成为承兑商后，等待10秒，方便下一步充值USDT");
        return return_code;
    }


//    /**
//    * 申请成为承兑商   (流程普通用户先调用本接口申请，再调用后台接口同意申请。  此方法主要是被admin_exchangersverify方法调用）
//     * return: BigInteger id (id传给’审核通过承兑商接口‘）
//     *
//    *
//     * @return*/
//    public BigInteger exchange_apply(String mobile) throws IOException {
//        log.info("申请成为承兑商方法开始:");
//        String result;
//        //一、拼接URL
//        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
//        String url = bundle.getString("v1_exchange_apply");
//
//        //二、发送请求，获取返回token
//        HttpPost post = new HttpPost(base_url + url);
//        //1、header数据
//        post.addHeader("client-type", "app");
//        post.addHeader("cookie", "token=" + response_token);
//        //2、data数据
//        JSONObject jsonParam = new JSONObject();
//        jsonParam.put("mobile", mobile);
//        jsonParam.put("wechat", bundle.getString("wechat"));
//        StringEntity entity = new StringEntity(jsonParam.toString(), "utf-8");
//        post.setEntity(entity);
//        //3、发送请求，获取返回
//        DefaultHttpClient client = new DefaultHttpClient();
//        HttpResponse response = client.execute(post);
//        result = EntityUtils.toString(response.getEntity());
//        log.info("申请成为承兑商方法，请求返回的result是:"+result);
//
//        //4、抽取数据
//        JSONObject resultjson = JSONObject.parseObject(result);
//        Integer getcode = (Integer) resultjson.get("code");
//        if(getcode==200){
//            log.info("申请成为承兑商方法，服务器返回200，测试继续");
//            JSONObject responseData = (JSONObject) resultjson.get("data");
//            String status= (String) responseData.get("status");
//            int id = (int) responseData.get("id");
//            if(status.equals("pending")&(id != 0)){
//                log.info("申请成为承兑商方法,服务器返回的用户状态（”pending“为正确）是："+status);
//                log.info("申请成为承兑商方法,服务器返回的用户ID（不为0则正确）是："+id);
//            }
//            //解析数据
//        }else{
//            log.info("申请成为承兑商方法，服务器返回不是200，测试异常，请检查");
//        }
//        return id;
//    }


//    /**
//    * 后台管理员登录
//    *
//    * */
//    public String v1_admin_login() throws IOException {
//        log.info("后台管理员登录方法开始");
//        String result;
//        String return_name=null;
//        //一、拼接URL
//        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
//        String url = bundle.getString("v1_admin_login");
//
//        //二、发送请求，获取返回token
//        HttpPost post = new HttpPost(base_url + url + id);
//        //1、header数据
//        post.addHeader("type", "web");
//
//        //2、data数据
//        JSONObject jsonParam = new JSONObject();
//        jsonParam.put("verify_code", );         //谷歌身份验证器，验证码
//        jsonParam.put("email", "gst@123");
//        jsonParam.put("password", "123456");
//        StringEntity entity = new StringEntity(jsonParam.toString(), "utf-8");
//        post.setEntity(entity);
//
//        //3、发送请求，获取返回
//        DefaultHttpClient client = new DefaultHttpClient();
//        HttpResponse response = client.execute(post);
//        result = EntityUtils.toString(response.getEntity());
//        log.info("后台管理员登录方法，请求返回的result是:"+result);
//
//        //4、抽取数据
//        JSONObject resultjson = JSONObject.parseObject(result);
//        Integer getcode = (Integer) resultjson.get("code");
//        if(getcode==200){
//            log.info("后台管理员登录方法，服务器返回200，测试继续");
//            JSONObject responseData = (JSONObject) resultjson.get("data");
//            return_name = (String) responseData.get("name");
//            if(return_name.equals("s")){
//                log.info("后台管理员登录方法,当前登录的管理员是："+return_name);
//            }
//        }else{
//            log.info("后台管理员登录方法，服务器返回不是200，测试异常，请检查");
//        }
//        return return_name;
//    }
//}


//    /**
//    * 审核通过承兑商（后台）//让一个普通用户通过审核               !!!!!!后台账户登录！！！！！！！
//     * params: BigIntegerid
//     * return ：BigInteger return_id
//     *
//    * */
//    public BigInteger admin_exchangersverify(String mobile) throws IOException {
//        log.info("审核通过承兑商（后台）方法开始");
//        String result;
//        BigInteger return_id = null;
//        //一、拼接URL
//        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
//        String url = bundle.getString("v1_admin_exchangersverify_approve");
//        //获取用户id
//        BigInteger id = exchange_apply(mobile);
//
//        //二、发送请求，获取返回token
//        HttpPost post = new HttpPost(base_url + url + id);
//        //1、header数据
//        post.addHeader("client-type", "web");
//
//        //3、发送请求，获取返回
//        DefaultHttpClient client = new DefaultHttpClient();
//        HttpResponse response = client.execute(post);
//        result = EntityUtils.toString(response.getEntity());
//        log.info("审核通过承兑商（后台）方法，请求返回的result是:"+result);
//
//        //4、抽取数据
//        JSONObject resultjson = JSONObject.parseObject(result);
//        Integer getcode = (Integer) resultjson.get("code");
//        if(getcode==200){
//            log.info("审核通过承兑商（后台）方法，服务器返回200，测试继续");
//            JSONObject responseData = (JSONObject) resultjson.get("data");
//            return_id = (BigInteger) responseData.get("id");
//            if(return_id.compareTo(id)==0){
//                log.info("审核通过承兑商（后台）方法,服务器返回return_id与传入id相等:"+return_id+","+id);
//            }
//        }else{
//            log.info("审核通过承兑商（后台）方法，服务器返回不是200，测试异常，请检查");
//        }
//        return return_id;
//    }


    /**
     * 承兑商USDT充值  (每次冲10000）
     * return:boolean   test_usdt_result
     */
    public boolean test_usdt(String eu_user_response_token) throws IOException {
        log.info("承兑商USDT充值方法开始");
        String result;
        boolean test_usdt_result;
        int code = 0;
        //一、拼接URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_test_usdt");

        //二、发送请求，获取返回token
        HttpGet get = new HttpGet(base_url + url);
        //1、header数据
        get.addHeader("client-type", "app");
        get.addHeader("cookie", "token=" + eu_user_response_token);

        //3、发送请求，获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(get);

        result = EntityUtils.toString(response.getEntity());
        log.info("承兑商USDT充值方法，请求返回的result是:" + result);

        //4、抽取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        Integer getcode = (Integer) resultjson.get("code");
        if (getcode == 200) {
            log.info("承兑商USDT充值方法，服务器返回200，充值成功,getcode是：" + getcode);
            //code=getcode;
            test_usdt_result = true;
        } else {
            log.info("承兑商USDT充值方法，服务器返回不是200，测试异常，请检查，getcode是：" + getcode);
            //code=getcode;
            test_usdt_result = false;
        }
        return test_usdt_result;
    }


    /**
     * 把承兑商【资产】页面的EUSD划转到【承兑】页面        (【承兑】页面，EUSD资产为0的时候，开启“收款服务”会报错)
     */
    public boolean v1_exchange_transferInfo(String eu_user_response_token) throws IOException {
        log.info("把承兑商【资产】页面的EUSD划转到【承兑】页面方法开始");
        String result;
        //int code=0;
        boolean v1_exchange_transferInfo_result;
        //一、拼接URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_exchange_transferInto");

        //二、发送请求，获取返回token
        HttpPost post = new HttpPost(base_url + url);
        //1、header数据
        post.addHeader("client-type", "app");
        post.addHeader("cookie", "token=" + eu_user_response_token);
        //2、data数据
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("quantity", get_EUSD_available());   //转账，获取当前EUSD的全部可用余额
        log.info("此处打log，查看可转账的余额：" + get_EUSD_available());
        StringEntity entity = new StringEntity(jsonParam.toString(), "utf-8");
        post.setEntity(entity);

        //3、发送请求，获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(post);
        result = EntityUtils.toString(response.getEntity());
        log.info("把承兑商【资产】页面的EUSD划转到【承兑】页面方法，请求返回的result是:" + result);

        //4、抽取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        Integer getcode = (Integer) resultjson.get("code");
        if (getcode == 200) {
            log.info("把承兑商【资产】页面的EUSD划转到【承兑】页面方法，服务器返回200，测试通过。 " + getcode);
            //code=getcode;
            v1_exchange_transferInfo_result = true;
        } else {
            log.info("把承兑商【资产】页面的EUSD划转到【承兑】页面方法，服务器返回不是200，测试异常，请检查。" + getcode);
            //code=getcode;
            v1_exchange_transferInfo_result = false;
        }
        return v1_exchange_transferInfo_result;
    }


    /**
     * 承兑商设置收款服务开启、关闭
     * paaram boolean boolean_value
     * return int code
     */
    public boolean va_buy_start(boolean boolean_value, String eu_user_response_token) throws IOException {
        log.info("开启指定承兑商：承兑商设置收款服务开启、关闭开始");
        //log.info("开启指定承兑商，当前被开启承兑商的mobiles是："+mobile);
        String result;
        //int code=0;
        boolean va_buy_start_result;
        //一、拼接URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_buy_start");

        //二、发送请求，获取返回token
        HttpPost post = new HttpPost(base_url + url);
        //1、header数据
        post.addHeader("client-type", "app");
        post.addHeader("cookie", "token=" + eu_user_response_token);
        //2、data数据
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("able", boolean_value);
        jsonParam.put("day_limit", 0);                  //0523,手机设置数值，抓包看并未改变，所以数值先为0
        jsonParam.put("low_limit", 0);
        StringEntity entity = new StringEntity(jsonParam.toString(), "utf-8");
        post.setEntity(entity);

        //3、发送请求，获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(post);
        result = EntityUtils.toString(response.getEntity());
        log.info("开启指定承兑商：承兑商设置收款服务开启、关闭，请求返回的result是:" + result);        //{"code":1503, "msg":"可以出售的EUSD太少了"}   17：47  0524

        //4、抽取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        Integer getcode = (Integer) resultjson.get("code");
        if (getcode == 200) {
            log.info("开启指定承兑商：承兑商设置收款服务开启、关闭，服务器返回200,getcode是：" + getcode);
            //code=getcode;
            va_buy_start_result = true;
        } else {
            log.info("开启指定承兑商：承兑商设置收款服务开启、关闭，服务器返回不是200，测试异常，请检查，getcode是：" + getcode);
            //code=getcode;
            va_buy_start_result = false;
        }
        return va_buy_start_result;
    }


    /**
     * 承兑商设置支付服务开启、关闭
     * return int code
     */

    public boolean va_sell_start(boolean boolean_value, String eu_user_response_token) throws IOException {
        log.info("开启指定承兑商：承兑商设置支付服务开启、关闭开始");
        String result;
        //int code=0;
        boolean va_sell_start_result;
        //一、拼接URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_sell_start");
        int[] pay_type = new int[]{1, 2, 4};      //微信、支付宝、银行卡都支持
        //二、发送请求，获取返回token
        HttpPost post = new HttpPost(base_url + url);
        //1、header数据
        post.addHeader("client-type", "app");
        post.addHeader("cookie", "token=" + eu_user_response_token);
        //2、data数据
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("able", boolean_value);
        jsonParam.put("day_limit", 0);                  //0523,手机设置数值，抓包看并未改变，所以数值先为0
        jsonParam.put("low_limit", 0);
        jsonParam.put("pay_type", pay_type);   //传入list
        StringEntity entity = new StringEntity(jsonParam.toString(), "utf-8");
        post.setEntity(entity);
        log.info("gst，查看发送的header:" + jsonParam.toString());  //此处查看Content-Length的长度，会出现服务器返回200，但是长度过小，无返回内容的情况。

        //3、发送请求，获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(post);
        result = EntityUtils.toString(response.getEntity());
        log.info("开启指定承兑商：承兑商设置支付服务开启、关闭，请求返回的result是:" + result);

        Header[] header = response.getAllHeaders();
        log.info("gst，查看返回的header:" + Arrays.toString(header));  //此处查看Content-Length的长度，会出现服务器返回200，但是长度过小，无返回内容的情况。

        //4、抽取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        Integer getcode = (Integer) resultjson.get("code");
        if (getcode == 200) {
            log.info("开启指定承兑商：承兑商设置支付服务开启、关闭，服务器返回200，充值成功,getcode是：" + getcode);
            //code=getcode;
            va_sell_start_result = true;
        } else {
            log.info("开启指定承兑商：承兑商设置支付服务开启、关闭，服务器返回不是200，测试异常，请检查，getcode是：" + getcode);
            //code=getcode;
            va_sell_start_result = false;
        }
        return va_sell_start_result;
    }


    /**
     * 测试前初始化用户多线程中，获取EUSD的可用余额
     * params： token
     * return： BigDecimal available；
     * （服务器返回数据是"available":1004800000,此方法返回数据是：1004800000,实际可用100480）
     */
    public boolean get_EUSD_available_for_thread(String response_token) throws IOException {
        log.info("测试前初始化用户多线程中，获取EUSD的可用余额方法开始");
        String result;
        boolean get_EUSD_available_result;

        //一、获取URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_eos");

        //二、发送请求，获取返回
        HttpGet get = new HttpGet(base_url + url);
        //1、header数据
        get.addHeader("client-type", "app");
        get.addHeader("cookie", "token=" + response_token);
        //2、发送请求获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(get);
        result = EntityUtils.toString(response.getEntity());
        log.info("测试前初始化用户多线程中，获取EUSD的可用余额方法，请求的响应是：" + result);
        //3、抽取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        JSONObject responseData = (JSONObject) resultjson.get("data");
        JSONObject records = (JSONObject) responseData.get("balance");
        Object objectavailable = records.get("available");
        BigDecimal eusd_available = new BigDecimal(String.valueOf(objectavailable));
        log.info("测试前初始化用户多线程中，获取EUSD的可用余额方法，获取avilable可用余额是：" + eusd_available);
        if (eusd_available.compareTo(new BigDecimal(0)) > 0) {
            get_EUSD_available_result = true;
        } else {
            get_EUSD_available_result = false;
        }
        return get_EUSD_available_result;
    }












///#################################################

    /**
     * 创建申诉，返回订单状态（1，等待中）
     * parm: BigInteger order_id
     * return: int status
     * 0621gst
     */
    public int orders_id_appeal(BigInteger order_id) throws IOException {
        log.info("创建申诉方法开始");
        String result;
        int status;

        //一、获取URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_orders");

        //二、发送请求，获取返回token
        HttpPost post = new HttpPost(base_url +url+ id + "/appeal");
        //1、header数据
        post.addHeader("client-type", "app");
        post.addHeader("cookie", "token=" + this.response_token);

        //2、data数据
        JSONObject jsonParam = new JSONObject();
        String context=new String(bundle.getString("appael_reason_1").getBytes("ISO-8859-1"), "gbk");
        jsonParam.put("order_id", id);
        jsonParam.put("context", context);
        jsonParam.put("type",6);        //int类型
        jsonParam.put("wechat", "12345678");

        StringEntity entity = new StringEntity(jsonParam.toString(), "utf-8");
        post.setEntity(entity);

        //3、发送请求，获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(post);
        result = EntityUtils.toString(response.getEntity());
        log.info("创建申诉方法，请求返回的result是:" + result);

        //4、抽取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        JSONObject data = (JSONObject) resultjson.get("data");
        status = (int) data.get("status");
        if (status == 1) {
            log.info("创建申诉方法，申诉后，订单状态是1，等待中，测试继续");
        } else if(status == 2){
            log.info("创建申诉方法，申诉后，订单状态是2，处理中，测试继续");
        }else if(status == 3){
            log.info("创建申诉方法，申诉后，订单状态是3，已处理，测试继续");
        }else{
            log.error("创建申诉方法，申诉后，订单状态异常，请处理");
        }
        return status;
    }


    /**
     * 解决申诉（取消申诉），返回服务器状态码，服务器返回200表示取消成功，返回的data数据是一个申诉对象
     * parm: BigInteger order_id
     * return: int status
     * 0621gst
     */
    public int orders_id_appeal_cancel(BigInteger order_id) throws IOException {
        log.info("解决申诉方法开始");
        String result;
        int getcode;       //保存服务器返回的状态码

        //一、获取URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_orders");

        //二、发送请求，获取返回token
        HttpPut put = new HttpPut(base_url +url+ id + "/appeal");
        //1、header数据
        put.addHeader("client-type", "app");
        put.addHeader("cookie", "token=" + this.response_token);

//        //2、data数据
//        JSONObject jsonParam = new JSONObject();
//        jsonParam.put("context", bundle.getString("appael_reason_1"));      //申诉原因
//        jsonParam.put("order_id", id);
//        jsonParam.put("type", bundle.getString("appael_type_6"));       //申诉类型
//        StringEntity entity = new StringEntity(jsonParam.toString(), "utf-8");
//        put.setEntity(entity);

        //3、发送请求，获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(put);
        result = EntityUtils.toString(response.getEntity());
        log.info("解决申诉方法，请求返回的result是:" + result);

//        Header[] header= response.getAllHeaders();
//        log.info("gst，查看返回的header:"+Arrays.toString(header));  //此处查看Content-Length的长度，会出现服务器返回200，但是长度过小，无返回内容的情况。

        //4、抽取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        getcode = (int) resultjson.get("code");
        if(getcode==200){
            log.info("解决申诉方法，服务器返回200，取消申诉成功");
        }else{
            log.info("解决申诉方法，服务器返回不是200，取消申诉失败");
        }


        return getcode;
    }




    /**
     * 返回申诉状态（创建申诉后，返回appael_status)
     * 内部params：token
     * param :BigInteger order_id
     * return int appeal_status
     * gst0621
     */
    public int get_appael_status_by_id(BigInteger order_id) throws IOException {
        log.info(" 返回申诉状态方法开始");
        String result;

        //一、获取URL
        bundle = ResourceBundle.getBundle("application", Locale.CHINA);
        String url = bundle.getString("v1_orders");

        //二、发送请求，获取返回token
        HttpGet get = new HttpGet(base_url + url + order_id);
        //1、header数据
        get.addHeader("client-type", "app");
        get.addHeader("cookie", "token=" + response_token);

        //2、发送请求获取返回
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(get);
        result = EntityUtils.toString(response.getEntity());
        log.info("返回申诉状态方法,请求的响应是：" + result);
        //3、抽取数据
        JSONObject resultjson = JSONObject.parseObject(result);
        JSONObject responseData = (JSONObject) resultjson.get("data");
        int appeal_status = (int) responseData.get("appeal_status");
        if(appeal_status==1){
            log.info("返回申诉状态方法，订单状态是等待处理，：" + result);
        }else if(appeal_status==2){
            log.info("返回申诉状态方法，订单状态是正在处理，：" + result);
        }else if(appeal_status==3){
            log.info("返回申诉状态方法，订单状态是以解决，：" + result);
        }else{
            log.error("返回申诉状态方法，返回状态异常，请检查");
        }
        return appeal_status;
    }
}




























