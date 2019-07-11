package test_case;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.TestNG;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class main {
    static Log log = LogFactory.getLog(before_fun.class);

    public static void main(String [] args) throws InterruptedException, IOException {
        //初始化账号
        initialzation();
        //调起测试
        //startTest();
    }
    private static int  time=0;


    //初始化账号
    public  static void initialzation() throws IOException, InterruptedException {
        log.info("测试前先查看当前time是："+time);

        //一、获取user、eu_user的预期账号个数、实际账号个数
        //1、预期账号个数；创建列表保存数据
        int user_except_mobile_num = 0;         //user预期个数
        int eu_user_except_mobile_num = 50;      //eu_user预期个数
        LinkedList <String >user_except_mobiles_list = new LinkedList<String>();
        LinkedList <String >eu_user_except_mobiles_list = new LinkedList<String>();
        LinkedList <String> user_role_and_mobiles = new LinkedList<String>();
        LinkedList <String> eu_user_role_and_mobiles = new LinkedList<String>();

        //2、获取数据库实际可用账号个数
        int user_account_status=0;
        int eu_account_status=0;
        if(time==0){        //第一次运行时，数据库account_status一定是null，第二次运行时，account_status只会是available和discard。
          time=time+1;
            user_account_status=before_fun.get_role_account_status("user","null"); //数据库  user,null可用个数
            log.info("第一次，获取user_account_status=null，user_account_status:"+user_account_status);
            eu_account_status=before_fun.get_role_account_status("eu_user","null");    //数据库  eu_user,null可用个数
            log.info("第一次，获取eu_user_account_status=null,eu_account_status:"+eu_account_status);

        }else{
            //第一次运行的时候，已经创建了账号并初始化了，所以此次检查是否有available账号
            user_account_status=before_fun.get_role_account_status("user","available"); //数据库  user,null可用个数
            log.info("不是第一次，获取user_account_status=available，user_account_status，可以用的账号个数是:"+user_account_status);
            eu_account_status=before_fun.get_role_account_status("eu_user","available");    //数据库  eu_user,null可用个数
            log.info("不是第一次，获取eu_user_account_status=available,eu_account_status，可以用的账号个数是:"+eu_account_status);
        }
        log.info("user_except_mobile_num:"+user_except_mobile_num);
        log.info("user_account_status:"+user_account_status);
        log.info("eu_user_except_mobile_num:"+eu_user_except_mobile_num);
        log.info("eu_account_status:"+eu_account_status);


        //二、创建手机号，实例化对象
        if((user_except_mobile_num>user_account_status) ||  (eu_user_except_mobile_num>eu_account_status)){          //逻辑有问题，两个任意一个
            log.info("user_except_mobile_num:"+user_except_mobile_num);
            log.info("user_account_status:"+user_account_status);
            log.info("eu_user_except_mobile_num:"+eu_user_except_mobile_num);
            log.info("eu_account_status:"+eu_account_status);
            //创建user
            int user_build_mobile_num = user_except_mobile_num-user_account_status;
            if(user_build_mobile_num>0){
                user_except_mobiles_list=before_fun.initialization_mobile("user","null",user_build_mobile_num);        //补充user手机号（不够则新建，递归去除重复数据）
                user_role_and_mobiles = before_fun.get_role_and_mobiles(user_except_mobiles_list);      //返回数组
            }
            //创建eu
            int eu_build_mobile_num = eu_user_except_mobile_num-eu_account_status;
            if(eu_build_mobile_num>0){
                eu_user_except_mobiles_list=before_fun.initialization_mobile("eu_user","null",eu_build_mobile_num);
                eu_user_role_and_mobiles = before_fun.get_role_and_mobiles(eu_user_except_mobiles_list);
            }
            log.info("user_except_mobiles补充的手机号是："+user_except_mobiles_list);
            log.info("eu_user_except_mobiles补充的手机号是："+eu_user_except_mobiles_list);
            log.info("user_role_and_mobiles："+user_role_and_mobiles);
            log.info("eu_user_role_and_mobiles："+eu_user_role_and_mobiles);

            //三、实例化对象
            //1、创建需要实例化的手机号数组
            LinkedList <String>instantiation_mobiles = new LinkedList<String>();
            instantiation_mobiles.addAll(user_role_and_mobiles);
            instantiation_mobiles.addAll(eu_user_role_and_mobiles);
            log.info("实例化对象的数组："+instantiation_mobiles);
            //2、线程个数
            int thread_num = instantiation_mobiles.size();      //线程个数
            //3、初始化对象,数据库填入初始化步骤结果，如果结果里面有失败的，account_status格添加废弃，如果全部成功，添加available
            CountDownLatch latch=new CountDownLatch(thread_num/2);     //传入需要做的线程个数
            for(int i=0; i<instantiation_mobiles.size();i++) {
                log.info("instantiation_mobiles.size()是："+instantiation_mobiles.size());
                log.info("当前i是："+i);
                //根据关键字创建相应的角色,并把角色信息上传数据库
                if(instantiation_mobiles.get(i).equals("user")){
                    String role = "user";
                    String mobile = instantiation_mobiles.get(i+1);     //"user" 下一个的手机号
                    User user = new User(role,mobile,latch);
                    new Thread(user).start();
                } else if(instantiation_mobiles.get(i).equals("eu_user")){    //创建eu_user对象
                    String role = "eu_user";
                    String mobile = instantiation_mobiles.get(i+1);     //"eu_user" 下一个的手机
                    User eu_user = new User(role,mobile,latch);
                    new Thread(eu_user).start();
                }
            }
            latch.await();
            //四、递归
            log.info("开始递归判断初始化是否完成");
            initialzation();

        }else{
            log.info("账号够用，无需初始化，可直接调起测试");
        }

    }



    //调起测试
    public static void startTest(){
        log.info("初始化完成，调起testng文件，开始测试");

        //方法一、直接调用用例类
//        TestNG testNG = new TestNG();
//        testNG.setTestClasses(new Class[]{test_eusd_buy_0.class});
//        testNG.run();

        //方法二、调用用例的xml文件
        TestNG testNG = new TestNG();
        List<String> suites = new ArrayList<String>();
        String testng_file = String.valueOf(before_fun.get_test_case_num());
        //suites.add("E:\\JAVAWORKSPACE\\autotest-master_0602\\src\\main\\resources\\testng.xml");        //xml的绝对路径
        suites.add(testng_file);
        testNG.setTestSuites(suites);
        testNG.run();
    }
}
