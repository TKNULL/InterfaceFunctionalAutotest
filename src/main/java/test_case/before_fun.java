package test_case;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class before_fun {
    public static Log log = LogFactory.getLog(before_fun.class);
    private static String[] telFirst="134,135,136,137,138,139,150,151,152,157,158,159,130,131,132,155,156,133,153".split(",");

    //声明Connection对象
    public static Connection connection;
    //驱动名称
    public static String  driver = "com.mysql.jdbc.Driver";
    //声明数据库连接、账号、密码
    static String  url = "jdbc:mysql://localhost:3306/autotest?useSSL=false";
    //static String  url = "jdbc:mysql://l72.17.0.4:3306/autotest?useSSL=false";
    static String user = "root";
    static String password = "123456";

    //构造方法
    public before_fun(){

    }



    /**
    * 获取指定类型账号个数
    * return int num
    * 0610 gst
    *
    *
    * */
    public static int get_role_account_status( String role,String account_status){
        log.info("获取指定类型账号个数方法开始");
        int account_status_num =0;
        String sql =null;

        try {
            Class.forName(driver);
            //1、连接数据库
            connection = DriverManager.getConnection(url, user, password);
            //2、定义SQL语句
            if(account_status.equals("null")){
                sql = "  select * from test_user_info where account_status is null and role ='"+role+"';";
            }else{
                sql = "  select * from test_user_info where account_status='"+account_status+"'and role ='"+role+"';";
            }

            log.info("获取指定类型账号个数方法，执行的sql语句是：" + sql);
            //3、执行语句并获取返回
            PreparedStatement result = connection.prepareStatement(sql);
            ResultSet resultSet = result.executeQuery();
            while (resultSet.next()) {
                account_status_num++;
            }
            log.info("获取指定类型账号个数方法，数值是：" + account_status_num);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return account_status_num;
    }



    /**
     * 保证数据库有足够的手机号可以使用
     * param: String role ,int num
     * return: LinkedList  account_status_mobile_list
     * 0610gst
     * */
    public static LinkedList<String> initialization_mobile( String role,String account_status,int except_num){         // [[mobile,role],[]，[]，[]，[]，[]]
        log.info("保证数据库有足够的手机号可以使用方法开始");
        //1、获取现可用手机号个数
        int mobile_num = get_role_account_status(role,account_status);
        log.info("保证数据库有足够的手机号可以使用方法，现可用手机号个数是："+mobile_num);
        log.info("保证数据库有足够的手机号可以使用方法，预期手机号个数是："+except_num);
        //2 账号不足，重新创建并添加到数据库
        if (mobile_num<except_num){
            int create_num = except_num - mobile_num;
            log.info("保证数据库有足够的手机号可以使用方法，本次需要创建的手机号个数是："+create_num);
            //调用创建单个手机号方法
            LinkedList create_mobiles_list= create_mobiles(create_num);
            //创建的手机号插入数据库
            insert_mobile(create_mobiles_list);
            //手机号添加身份
            insert_role(role,create_mobiles_list);
            //递归（避免新创建的手机号仍然与数据库重复的情况）
            initialization_mobile(role,account_status,except_num);
        }else{
            log.info("保证数据库有足够的手机号可以使用方法，当前数据库有足够的手机号");
        }

        //2、返回手机号数组
        LinkedList <String >account_status_mobile_list=get_account_status_mobiles(role,account_status);
        log.info("保证数据库有足够的手机号可以使用方法，返回的手机号列表是："+account_status_mobile_list);
        return account_status_mobile_list;
    }






    /**
     * 手机号添加到数据库
     * parm: LinkedList<String > create_mobiles_list ,[monile1,monile2......]
     * return void
     * 0610gst
     *
     * */
    public static void insert_mobile(  LinkedList<String > create_mobiles_list ){
        log.info("手机号添加到数据库方法开始");
        try {
            Class.forName(driver);
            //1、连接数据库
            connection = DriverManager.getConnection(url,user,password);
            //2、定义SQL语句
            for(int i=0; i<create_mobiles_list.size();i++){     //6
                String sql = "insert ignore test_user_info (mobiles) values('" + create_mobiles_list.get(i)+"'" +"); "  ;           //insert ignore忽略重复数据，只插入不重复的数据。
                log.info("手机号添加到数据库，执行的SQL语句是："+sql);
                PreparedStatement result = connection.prepareStatement(sql);
                result.execute();
            }
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }finally {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 给数据库中的手机号添加身份
     *parm: int   user_num,eu_user_num
     * return void
     *0610gst
     *
     * */
    public static void insert_role(String role, LinkedList <String> mobiles){
        for(int i =0 ;i<mobiles.size();i++){
            try {
                Class.forName(driver);
                //1、连接数据库
                connection = DriverManager.getConnection(url,user,password);
                String insert_role_sql =" update test_user_info set role ='"+role+"' where mobiles ='"+mobiles.get(i)+"';";

                //2、添加user
                log.info("给数据库中的手机号添加身份,此处执行的sql语句是："+insert_role_sql);
                PreparedStatement result1 = connection.prepareStatement(insert_role_sql);
                boolean resultSet0 = result1.execute();
                log.info("给数据库中的手机号添加身份,执行添加user的SQL语句的结果是："+resultSet0);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }catch (SQLException e) {
                e.printStackTrace();
            }finally {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        log.info("给数据库中的手机号添加身份,添加结束");
    }




    /**
    * 根据指定的手机号list，返回[role,mobiles]
    * parm: linklist mobiles
    * return linklist role_and_mobiles
    * 0610gst
    *
    *
    * */
    public static LinkedList <String> get_role_and_mobiles(LinkedList<String> mobiles){
        LinkedList role_and_mobiles = new LinkedList();
        for(int i=0;i<mobiles.size();i++){
            try {
                Class.forName(driver);
                //1、连接数据库
                connection = DriverManager.getConnection(url,user,password);
                String sql = "select role,mobiles from test_user_info where mobiles='"+mobiles.get(i)+"';";
                log.info("根据指定的手机号list返回role_and_list,此处执行的sql语句是："+sql);
                PreparedStatement result = connection.prepareStatement(sql);
                ResultSet resultSet = result.executeQuery();
                while (resultSet.next()){
                    role_and_mobiles.add(resultSet.getString(1));
                    role_and_mobiles.add(resultSet.getString(2));
                }
                log.info("根据指定的手机号list返回role_and_list,while内的数组是："+role_and_mobiles);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }catch (SQLException e) {
                e.printStackTrace();
            }finally {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        log.info("根据指定的手机号list返回role_and_list,for内的数组是："+role_and_mobiles);
        return role_and_mobiles;
    }




    /**
     * 获取account_status值为指定状态的的手机号列表
     * parma String account_status状态   String role
     * return: int  LinkedList   account_status_mobiles
     * 0610gst
     *
     * */
    public static LinkedList<String> get_account_status_mobiles(String role,String account_status) {
        log.info("获取account_status值为指定状态的的手机号列表方法开始");
        LinkedList <String >account_status_mobiles = new LinkedList<String>();
        String sql =null;

        //1、获取user_mobile_not_used列表
        try {
            Class.forName(driver);
            //1、连接数据库
            connection = DriverManager.getConnection(url, user, password);
            //2、定义SQL语句
            if(account_status.equals("null")){
                sql = " select * from test_user_info where account_status is null and role='"+role+"';" ;
            }else{
                sql = " select * from test_user_info where account_status ='"+account_status+"' and role='"+role+"';" ;
            }

            log.info("获取account_status值为指定状态的的手机号列表方法，执行的sql语句是：" + sql);
            //3、执行语句并获取返回
            PreparedStatement result = connection.prepareStatement(sql);
            ResultSet resultSet = result.executeQuery();
            while (resultSet.next()) {
                account_status_mobiles.add(String.valueOf(resultSet.getLong(2)));
            }
            log.info("获取account_status值为指定状态的的手机号列表方法，手机号列表是：" + account_status_mobiles);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return account_status_mobiles;
    }




    /**初始化过程中,初始化结果上传到数据库
     * parm：LinkedList 初始化参数
     * return void
     * 0610gst
     * */
    public static void insert_initialzation_result( LinkedList initialzation_result) {
        //查找手机号和角色,添加该行的数据
        try {
            Class.forName(driver);
            //1、连接数据库
            connection = DriverManager.getConnection(url, user, password);
            //2 获取手机号和身份
            log.info("把初始化结果添加到数据库,当前获取的数组是:" + initialzation_result);
            //承兑商和普通用户共有的参数

            String mobile = (String) initialzation_result.get(0);
            String role = (String) initialzation_result.get(1);
            boolean eusd_available = (boolean) initialzation_result.get(2);
            boolean add_payment_methods_result = (boolean) initialzation_result.get(3);
            boolean setpassword_result = (boolean) initialzation_result.get(4);

            if (role.equals("user")) {
                String user_account_status=null;
                if(eusd_available && add_payment_methods_result && setpassword_result){
                    user_account_status="available";
                    log.info("初始化成功，account_status=available");
                }else{
                    user_account_status="discard";
                    log.info("初始化失败，account_status=discard");
                }
                String sql = "update  test_user_info set eusd_available=" + eusd_available + ",add_payment_method=" + add_payment_methods_result + ", set_psword=" + setpassword_result + ", account_status='"+user_account_status+"' where (mobiles=" + mobile + " and role ='" + role + "')  ;";
                log.info("把user初始化结果添加到数据库,sql语句是:" + sql);
                //执行sql
                PreparedStatement result = connection.prepareStatement(sql);
                result.execute();

            } else if (role.equals("eu_user")) {
                String eu_user_account_status=null;
                //承兑商独有的参数
                boolean becomeExchanger_return_code = (boolean) initialzation_result.get(5);
                boolean test_usdt_code = (boolean) initialzation_result.get(6);
                boolean exchange_transferInfo_code = (boolean) initialzation_result.get(7);
                boolean buy_start_code = (boolean) initialzation_result.get(8);
                boolean sell_start_code = (boolean) initialzation_result.get(9);
                if(eusd_available && add_payment_methods_result && setpassword_result && becomeExchanger_return_code && test_usdt_code &&  exchange_transferInfo_code &&  buy_start_code && sell_start_code){
                    eu_user_account_status="available";
                    log.info("初始化成功，eu_user_account_status=available");
                }else{
                    eu_user_account_status="discard";
                    log.info("初始化失败，eu_user_account_status=discard");
                }
                String sql = "update  test_user_info set eusd_available=" + eusd_available + ",add_payment_method=" + add_payment_methods_result + ",   set_psword=" + setpassword_result + "  ,eu_become_exchanger=" + becomeExchanger_return_code + ",eu_acceptance_usdt_available=" + test_usdt_code + " ,eu_exchange_transferInfo=" + exchange_transferInfo_code + "  ,eu_buy_start=" + buy_start_code + ",eu_sell_start=" + sell_start_code + ", account_status='"+eu_user_account_status+"' where (mobiles=" + mobile + " and role ='" + role + "')  ;";
                log.info("把eu初始化结果添加到数据库,sql语句是:" + sql);


                PreparedStatement result = connection.prepareStatement(sql);
                result.execute();
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }














































    /**
    * 获取指定类型、个数个账号
    * return  LinkedList<LinkedList<String>>   [[user, 15108708247], [user, 13007100247]]
    * 0602
    **/
    public static LinkedList<LinkedList<String>> get_mobiles(String role ,String account_status, int mobile_num){
        LinkedList <LinkedList<String>> mobiles_roles = new LinkedList<>();

        //1、获取user_mobile_not_used列表
        try {
            Class.forName(driver);
            //1、连接数据库
            connection = DriverManager.getConnection(url, user, password);
            //2、定义SQL语句
            //String sql = "select mobiles from test_user_info where role='"+role+"'and account_status is null limit "+ mobile_num+"; " ;
            String sql = "select mobiles from test_user_info where role='"+role+"'and account_status = '"+account_status+"' limit "+ mobile_num+"; " ;
            log.info("获取指定类型、指定个数个账号方法，执行的sql语句是：" + sql);
            //3、执行语句并获取返回
            PreparedStatement result = connection.prepareStatement(sql);
            ResultSet resultSet = result.executeQuery();
            while (resultSet.next()){
                LinkedList <String> single_mobile_role = new LinkedList<String>();
                single_mobile_role.add("user");
                single_mobile_role.add(resultSet.getString(1));
                mobiles_roles.add(single_mobile_role);
            }
            log.info("获取指定类型、指定个数个账号方法，mobiles_roles:"+mobiles_roles);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return mobiles_roles;
    }


    /**
    * 已经使用的手机号，account_status标注为used
    *
    * */
    public static void insert_is_used(LinkedList<String> used_mobiles){
        //1、获取user_mobile_not_used列表
        try {
            Class.forName(driver);
            //1、连接数据库
            connection = DriverManager.getConnection(url, user, password);
            for(int i=0; i<used_mobiles.size();i++){
                log.info("已使用的手机号，account_status改为used,循环的个数是："+used_mobiles.size());
                log.info("已使用的手机号，account_status改为used,循环是：："+used_mobiles);
                //2、定义SQL语句
                String sql="update test_user_info set account_status ='used' where mobiles = "+used_mobiles.get(i)+"   ;";
                log.info("");
                log.info("已使用的手机号，account_status改为used，执行的sql语句是：" + sql);
                //3、执行语句并获取返回
                PreparedStatement result = connection.prepareStatement(sql);
                boolean resultSet = result.execute();
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }




//
//    /**
//     * 从数据库获取手机号，创建承兑商[]和普通用户[],
//     * parm:LinkedList num，  [普通账户个数，承兑商账户个数]
//     * return  [user, 15766554433, user, 15766554434, user, 15766554435, user, 15766554436, eu_user, 15766554437, eu_user, 15766554438, eu_user, 15766554439, eu_user, 15766554430, eu_user, 15766554233]
//     *
//     * */
//
//    public static LinkedList<String> initialization_user_and_eu_user(LinkedList<Integer> role_list) {
//        //1、创建列表存储数据
//        LinkedList<String> user_account_and_eu_user_account_mobile = new LinkedList<String>();
//        LinkedList<String> user_account_mobiles = new LinkedList<String>();
//        user_account_mobiles.add("user");
//        LinkedList<String> eu_user_account_mobiles = new LinkedList<String>();
//        eu_user_account_mobiles.add("eu_user");
//
//        //2、获取预计实例化的user_account、eu_user_account和数据库可用手机号数值
//        int user_account = role_list.get(0);
//        int eu_user_account = role_list.get(1);
//        //从数据库获取可用的手机号，[mobile,mobile......]
//        LinkedList is_used_null_mobiles = get_is_used_null_mobiles();    // [mobile,mobile,mobile.........]
//        log.info("此时获取的是：");
//        log.info("预计user_account个数是:" + user_account);
//        log.info("预计eu_user_account个数是:" + eu_user_account);
//        log.info("is_used值为null的手机号列表个数是:" + is_used_null_mobiles.size());
//
//        //先创建user数据，再创建eu_user数据
//        for(int i =0;i<is_used_null_mobiles.size();i++){
//            if (user_account > 0) {
//                user_account_and_eu_user_account_mobile.add("user");
//                user_account_and_eu_user_account_mobile.add((String) is_used_null_mobiles.get(i));
//                user_account--;
//            }else if(eu_user_account > 0){
//                user_account_and_eu_user_account_mobile.add("eu_user");
//                user_account_and_eu_user_account_mobile.add((String) is_used_null_mobiles.get(i));
//                eu_user_account--;
//            }else{
//                log.info("数据库可用账号大于所需账号");
//            }
//        }
//        log.info("user_account_and_eu_user_account_mobile："+user_account_and_eu_user_account_mobile);
//        return user_account_and_eu_user_account_mobile;
//    }
//





    /**
     * 获取数据库中is_used值为null的手机号个数
     *return: int num
     *
     * */
    public static int get_is_used_null() {
        log.info("获取数据库中is_used值为null的手机号个数方法开始");
        int is_used_null_num =0;

        //1、获取user_mobile_not_used列表
        try {
            Class.forName(driver);
            //1、连接数据库
            connection = DriverManager.getConnection(url, user, password);
            //2、定义SQL语句
            String sql = " select * from test_user_info where is_used  is null ; ";
            log.info("获取数据库中is_used值为null的手机号个数方法，执行的sql语句是：" + sql);
            //3、执行语句并获取返回
            PreparedStatement result = connection.prepareStatement(sql);
            ResultSet resultSet = result.executeQuery();
            while (resultSet.next()) {
                is_used_null_num++;
            }
            log.info("获取数据库中is_used值为null的手机号个数方法，数值是：" + is_used_null_num);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return is_used_null_num;
    }



    /**
     * 获取数据库中initialzation_is_true值为true的手机号个数
     *return: int num
     *
     * */
    public static int get_initialzation_true() {
        log.info("获取数据库中initialzation_is_true值为true的手机号个数方法开始");
        int initialzation_true_num =0;

        //1、获取user_mobile_not_used列表
        try {
            Class.forName(driver);
            //1、连接数据库
            connection = DriverManager.getConnection(url, user, password);
            //2、定义SQL语句
            String sql = " select * from test_user_info where Initialization_is_true =\"true\" and is_used is null ; ";
            log.info("获取数据库中initialzation_true_num值为true的手机号个数方法，执行的sql语句是：" + sql);
            //3、执行语句并获取返回
            PreparedStatement result = connection.prepareStatement(sql);
            ResultSet resultSet = result.executeQuery();
            while (resultSet.next()) {
                initialzation_true_num++;
            }
            log.info("获取数据库中initialzation_true_num值为true的手机号个数方法，数值是：" + initialzation_true_num);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return initialzation_true_num;
    }









    /**
     * 传入数值，创建单个手机号
     *return:List<String> create_mobiles_list
     *
     *
     * */
    public static LinkedList<String> create_mobiles(int num){
        LinkedList<String> create_mobiles_list = new LinkedList<String>();
        // 一、根据传入num生成手机号
        log.info("创建手机号方法，传入的num是："+num);
        for(int i =0; i<num;i++){
            String mobile=build_mobile();
            create_mobiles_list.add(mobile);
        }
        log.info("创建手机号方法，创建的手机是："+create_mobiles_list);
        return create_mobiles_list;
    }












    /**
     * 获取数据库中所有手机号   (一个账号只能有一个身份，不能同时是承兑商和用户)
     * return[mobile,mobile......]
     *
     *
     * */
    public static LinkedList<String> get_all_mobiles(){
        log.info("获取数据库中所有手机号方法开始");
        LinkedList<LinkedList<String>>  all_mobiles_and_roles = new LinkedList<LinkedList<String>>();
        LinkedList<String> mobiles_and_roles = new LinkedList<>();
        LinkedList<String> all_mobiles = new LinkedList<>();
        LinkedList<String> all_roles = new LinkedList<>();
        //1、获取user_mobile列表
        try {
            Class.forName(driver);
            //1、连接数据库
            connection = DriverManager.getConnection(url,user,password);
            //2、定义SQL语句
            String sql = "select * from test_user_info ;" ;
            //3、执行语句并获取返回
            PreparedStatement result = connection.prepareStatement(sql);
            ResultSet resultSet = result.executeQuery();
            while(resultSet.next()){
                all_mobiles.add(String.valueOf(resultSet.getLong(2)));
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }catch (SQLException e) {
            e.printStackTrace();
        }finally {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        log.info("获取数据库中所有手机号方法,返回的数据是："+all_mobiles);
        return all_mobiles;
    }






    /**
     * 随机数
     * */
    public static int getNum(int start,int end) {
        return (int)(Math.random()*(end-start+1)+start);
    }


    /**
     * 生成手机号
     *
     * */
    public static String build_mobile(){
        int index=getNum(0,telFirst.length-1);
        String first=telFirst[index];
        String second=String.valueOf(getNum(1,888)+10000).substring(1);
        String third=String.valueOf(getNum(1,9100)+10000).substring(1);
        return first+second+third;
    }


    /**
     * 获取testng测试文件路径
     *
     *
     * */
    public static File get_test_case_num(){
        //获取测试文件路径
        File getpath = new File("");
        String root_path = getpath.getAbsolutePath();
        String detail_path = "\\src\\main\\resources\\testng.xml";
        String path =  root_path+detail_path;
        File get_testng_file = new File(path);
        log.info("获取的testng文件路径是："+get_testng_file);        //E:\JAVAWORKSPACE\autotest-master_0602\src\main\resources\testng.xml

        return get_testng_file;
    }

}



































//##########################################################################







//
//    /**
//     * 插入user_mobile数据
//     * param: list mobile
//     *
//     *
//     * */
//    public static void insert_user_mobile(LinkedList<LinkedList<String>> insert_user_mobiles){         // [[mobile,role],[]，[]，[]，[]，[]]
//        log.info("连接数据库，插入数据到user_mobile方法开始");
//        try {
//            Class.forName(driver);
//            //1、连接数据库
//            connection = DriverManager.getConnection(url,user,password);
//            //2、定义SQL语句
//            for(int i=0; i<insert_user_mobiles.size();i++){     //6
////                String mobile = insert_user_mobiles.get(i).get(0);
////                String role = insert_user_mobiles.get(i).get(1);
//                String sql = "insert into test_user_info (mobiles,role) values('" + insert_user_mobiles.get(i).get(0) +"'"    + ","   +"'" +insert_user_mobiles.get(i).get(1)   + "'); "  ;
//                log.info("SQL语句是："+sql);
//                PreparedStatement result = connection.prepareStatement(sql);
//                result.execute();
//            }
//        } catch (ClassNotFoundException ex) {
//            ex.printStackTrace();
//        } catch (SQLException ex) {
//            ex.printStackTrace();
//        }finally {
//            try {
//                connection.close();
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//
//
//    /**
//     * 获取获取user_mobile,eu_mobile可用账户方法
//     * return： LinkedList [[int user_mobile可用账户，int eu_mobile可用账户]]
//     *
//     *LinkedList<LinkedList<String>>
//     *
//     * @return*/
//    public static LinkedList<LinkedList<String>> get_all_available_mobiles(){
//        log.info("连接数据库，获取user_mobile,eu_mobile可用账户列表方法开始");
//        LinkedList<LinkedList<String>> all_available_mobiles = new LinkedList<LinkedList<String>>();
//        LinkedList<String> user_mobiles_avaiable = new LinkedList<String>();
//        LinkedList<String> eu_mobiles_avaiabl = new LinkedList<String>();
//
//        //1、获取user_mobile_not_used列表
//        try {
//            Class.forName(driver);
//            //1、连接数据库
//            connection = DriverManager.getConnection(url,user,password);
//            //2、定义SQL语句
//            String sql = " select * from test_user_info where is_used =\" \" and role ='user' ; ";
//            log.info("执行的sql语句是："+sql);
//            //3、执行语句并获取返回
//            PreparedStatement result = connection.prepareStatement(sql);
//            ResultSet resultSet = result.executeQuery();
//            while(resultSet.next()){
//                long user_mobile =resultSet.getLong(2);
//                user_mobiles_avaiable.add(String.valueOf(user_mobile));
//            }
//            log.info("连接数据库，获取user_mobile可用账户列表方法,执行SQL语句后，获取的返回是："+user_mobiles_avaiable);
//
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }catch (SQLException e) {
//            e.printStackTrace();
//        }finally {
//            try {
//                connection.close();
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
//        }
//        //2、获取eu_mobile_not_used列表
//        try {
//            Class.forName(driver);
//            //1、连接数据库
//            connection = DriverManager.getConnection(url,user,password);
//            //2、定义SQL语句
//            String sql = " select * from test_user_info where is_used =\" \" and role ='user' ; ";
//            log.info("执行的sql语句是："+sql);
//            //3、执行语句并获取返回
//            PreparedStatement result = connection.prepareStatement(sql);
//            ResultSet resultSet = result.executeQuery();
//            log.info("~~~~~~~~~~~~");
//            while(resultSet.next()){
//                long eu_mobile =resultSet.getLong(2);
//                eu_mobiles_avaiabl.add(String.valueOf(eu_mobile));
//            }
//            log.info("连接数据库，获取eu_mobile可用账户列表方法,执行SQL语句后，获取的返回是："+eu_mobiles_avaiabl);
//
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }catch (SQLException e) {
//            e.printStackTrace();
//        }finally {
//            try {
//                connection.close();
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
//        }
//        //3、存储数据
//        all_available_mobiles.add(user_mobiles_avaiable);
//        all_available_mobiles.add(eu_mobiles_avaiabl);
//        log.info("获取获取user_mobile,eu_mobile可用账户方法,返回的数据是："+all_available_mobiles);
//        return all_available_mobiles;
//    }
//
//    /**
//     * 获取user_mobile,eu_mobile可用账户个数方法
//     * return： LinkedList [int user_mobile可用个数，int eu_mobile可用个数]
//     *
//     *
//     * */
//
//    public static LinkedList<Integer> get_all_available_mobile_num() {
//        log.info("连接数据库，获取user_mobile,eu_mobile可用账户个数方法开始");
//        LinkedList <Integer> result_list = new LinkedList<Integer>();
//        int user_mobile_not_use =0;
//        int eu_mobile_not_use =0;
//        //1、获取user_mobile_not_use数值
//        try {
//            Class.forName(driver);
//            //1、连接数据库
//            connection = DriverManager.getConnection(url,user,password);
//            //2、定义SQL语句
//            String sql = " select * from test_user_info where is_used =\" \" and role ='user' ; ";
//
//            //3、执行语句并获取返回
//            PreparedStatement result = connection.prepareStatement(sql);
//            ResultSet resultSet = result.executeQuery();
//            while(resultSet.next()){
//                user_mobile_not_use++;
//            }
//            log.info("连接数据库，获取user_mobile可用账户个数方法,执行SQL语句后，获取的返回是："+user_mobile_not_use);
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }catch (SQLException e) {
//            e.printStackTrace();
//        }finally {
//            try {
//                connection.close();
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
//        }
//        //2、获取eu_mobile_not_use数值
//        try {
//            Class.forName(driver);
//            //1、连接数据库
//            connection = DriverManager.getConnection(url,user,password);
//            //2、定义SQL语句
//            //String sql = " select * from test_user_info where eu_mobile_is_used =\" \" ;";
//            String sql = " select * from test_user_info where is_used =\" \" and role ='eu_user' ; ";
//            //3、执行语句并获取返回
//            PreparedStatement result = connection.prepareStatement(sql);
//            ResultSet resultSet = result.executeQuery();
//            while(resultSet.next()){
//                eu_mobile_not_use++;
//            }
//            log.info("连接数据库，获取eu_mobile可用账户个数方法,执行SQL语句后，获取的返回是："+eu_mobile_not_use);
//
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }catch (SQLException e) {
//            e.printStackTrace();
//        }finally {
//            try {
//                connection.close();
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
//        }
//        //3、存储数据
//        result_list.add(user_mobile_not_use);
//        result_list.add(eu_mobile_not_use);
//        log.info("连接数据库，获取数据库可用账户个数方法,返回的列表是："+result_list);
//        return result_list;
//    }
//
//    /**
//     * 根据传入的手机号，判断是否有重复号码,如果有重复手机号，删除重复手机号，记录数字，并重新创建手机号
//     * params
//     *
//     * */
//    public  void judge_is_repeat(LinkedList<LinkedList<String>> new_user_mobiles_and_eu_mobiles){
//        //1、选择eu_mobile和user_mobile的所有手机号
//        //2、分别遍历便利两个list,记录两个list重复的个数
//        //3、创建指定个数个手机号
//        LinkedList<String > new_user_mobiles = new_user_mobiles_and_eu_mobiles.get(0);  //获取被比较的new_user_mobiles
//        LinkedList<String > new_eu_mobiles = new_user_mobiles_and_eu_mobiles.get(1);    //获取被比较的new_eu_mobiles
//
//    }
//
//
//    /**
//     *与数据库手机号做对比，如果有，则创建新手机号，如果没有，则可以实例化对象
//     *
//     *
//     * */
//    public static LinkedList<LinkedList<String>> is_repeat( LinkedList<String > new_user_mobiles,LinkedList<String > new_eu_mobiles){
//        //存储需要创建的手机号个数
//        int user_mobiles_need_create =0;
//        int eu_mobiles_need_create =0;
//
//        //存储最后确定的可以实例化的手机号
//        LinkedList<LinkedList<String>> final_user_mobiles_and_eu_mobiles = new LinkedList<LinkedList<String>>();
//        //获取数据库中所有手机号，以便去重
//        LinkedList<String> all_mobiles =get_all_mobiles();
//        for(int i=0;i<all_mobiles.size();i++){
//            //此处传入的new_user_mobiles和new_eu_mobiles之间不会有重复的数值
//            if(!new_user_mobiles.contains(all_mobiles.get(i))){
//                log.info("判断是否重复方法，new_user_mobiles中重复数值是："+all_mobiles.get(i));
//                user_mobiles_need_create++;
//                //重新创建手机号,存储成[mobile,user_mobile]格式
//                //创建手机号方法，[[mobile,user_mobile]]
//                //递归调用is_repeat（）
//                //存储最后确定的可以实例化的手机号
//            }else if(!new_eu_mobiles.contains(all_mobiles.get(i))){
//                log.info("判断是否重复方法，new_eu_mobiles获取的重复数值是："+all_mobiles.get(i));
//                //重新创建手机号,存储成[mobile,eu_mobile]格式
//                //创建手机号方法，[[mobile,eu_mobile]]
//                //递归调用is_repeat（）
//                //存储最后确定的可以实例化的手机号
//            }else{
//                log.info("新创建的手机号，与数据库手机号对比，未发现重复手机号，可以执行初始化");
//                //存储最后确定的可以实例化的手机号
//            }
//        }
//        log.info("与数据库手机号做对比并去重，最后确定可以实例化的手机号码是："+final_user_mobiles_and_eu_mobiles);
//        return final_user_mobiles_and_eu_mobiles;
//    }
//
//
//    /**
//     * 生成的手机号存储到数据库，如果有重复的手机号，重新生成，如果没有，测试继续
//     *
//     * */
//    public static void  save_mobile(LinkedList<LinkedList<String >>  mobiles){
//        //声明Connection对象
//        Connection connection;
//        //驱动名称
//        String driver = "com.mysql.jdbc.Driver";
//        //声明数据库连接、账号、密码
//        String url = "jdbc:mysql://localhost:3306/autotest";
//        String user = "root";
//        String password = "123456";
//        //一、数据存入数据库
//        //加载驱动
//        try {
//            log.info("连接数据库开始");
//            Class.forName(driver);
//            //1、连接数据库
//            connection = DriverManager.getConnection(url,user,password);
//            //2、创建statement对象来指定SQL语句
//            Statement statement = connection.createStatement();
//            log.info("数据库存储数据开始");
//            //3、执行sql语句，传入手机号
//            // //[[15705704100, user], [15605044400, eu_user], [15700612470, user], [13905812305, eu_user]]
//            for(int i =0;i<mobiles.size();i++){
//                String sql_save_mobile ="INSERT INTO test_user_info(mobile,role) VALUES(" + mobiles.get(i).get(0)+  "," +"\"" + mobiles.get(i).get(1)+ "\"" + ");";
//                log.info("此时执行的SQL语句是："+sql_save_mobile);
//                boolean execute_result = statement.execute(sql_save_mobile);
//                log.info("执行的SQL语句的结果是："+execute_result);
//            }
//        } catch(ClassNotFoundException e) {
//            e.printStackTrace();
//        } catch(SQLException e) {
//            //数据库连接失败异常处理
//            e.printStackTrace();
//        }catch (Exception e) {
//            e.printStackTrace();
//        }finally{
//            log.info("连接数据库结束");
//        }
//        //二、如果数据有重复，新建数据
//    }
//
//
//    /**
//     * 根据传入num生成双倍的手机号，检查数据是否与数据库重复，没有重复的话，存储到数据库里面,并返回map类型的数据
//     *
//     */
//    public static LinkedList<LinkedList<String>> start_build_double_mobile(int num ){
//        List<String> mobiles_list = new LinkedList<String>();
//        Map<String,String> mobiles_map = new HashMap<String,String>();
//        LinkedList <LinkedList<String>> mobiles = new LinkedList<LinkedList<String>>();
//        // 一、根据传入num生成双倍的手机号
//        for(int i =0; i<num*2;i++){
//            String mobile=build_mobile();
//            mobiles_list.add(mobile);
//        }
//        log.info("before_suite,根据传入num创建双倍的手机号");
//        log.info("before_suite,传入的num是："+num);
//        log.info("before_suite,生成的手机号list是："+mobiles_list);
//
//        // 二、检查数据是否与数据库重复
//        //连接数据库，检查手机号是否重复
//
//        //三、存储list
//        for (int j =0;j <mobiles_list.size();j++){
//            if( (j>1)&(j/2==0)){
//                mobiles_map.put(mobiles_list.get(j-1),mobiles_list.get(j));
//            }
//        }
//        //存储LinkedList
//        for (int i=0;i<mobiles_list.size();i++){
//            LinkedList<String> user_info = new LinkedList<>();
//            user_info.add(mobiles_list.get(i).toString());
//            if(i%2==0){
//                user_info.add("user");
//            }else{
//                user_info.add("eu_user");
//            }
//            mobiles.add(user_info);   //[[mobile,user],[mobile,eu_user],[],[],]
//        }
//        System.out.println("before_suite,生成的LinkedList是："+mobiles);
//        System.out.println(mobiles.size());
//        //   [[13905881818, user], [15607060386, eu_user], [13307690276, user], [15907850155, eu_user], [13101016001, user], [13601322762, eu_user]]
//        return mobiles;
//    }
//
//
//    /**
//     * 根据传入num生成手机号，检查数据是否与数据库重复，没有重复的话，存储到数据库里面,并返回map类型的数据
//     *
//     */
//    public static LinkedList<LinkedList<String>> start_build_single_mobile(int num ){
//        List<String> mobiles_list = new LinkedList<String>();
//        Map<String,String> mobiles_map = new HashMap<String,String>();
//        LinkedList <LinkedList<String>> mobiles = new LinkedList<LinkedList<String>>();
//        // 一、根据传入num生成手机号
//        for(int i =0; i<num;i++){
//            String mobile=build_mobile();
//            mobiles_list.add(mobile);
//        }
//        log.info("before_suite,根据传入num创建双倍的手机号");
//        log.info("before_suite,传入的num是："+num);
//        log.info("before_suite,生成的手机号list是："+mobiles_list);
//
//        //三、存储list
//        for (int j =0;j <mobiles_list.size();j++){
//            if( (j>1)&(j/2==0)){
//                mobiles_map.put(mobiles_list.get(j-1),mobiles_list.get(j));
//            }
//        }
//        //存储LinkedList
//        for (int i=0;i<mobiles_list.size();i++){
//            LinkedList<String> user_info = new LinkedList<>();
//            user_info.add(mobiles_list.get(i).toString());
//
//            if(i%2==0){
//                user_info.add("user");
//            }else{
//                user_info.add("eu_user");
//            }
//            mobiles.add(user_info);   //[[mobile,user],[mobile,eu_user],[],[],]
//        }
//        System.out.println("before_suite,生成的LinkedList是："+mobiles);
//        System.out.println(mobiles.size());
//        //   [[13905881818, user], [15607060386, eu_user], [13307690276, user], [15907850155, eu_user], [13101016001, user], [13601322762, eu_user]]
//        return mobiles;
//    }
//
//
//
//
//
//
//
//
//
//
//    /**
//     * 数据上传数据库
//     *
//     *
//     * */
//    public static void add_info() throws ClassNotFoundException, SQLException {
//        //声明对象、驱动、用户名和密码
//        Connection connection;
//        String driver = "com.mysql.jdbs.Driver";
//        String url = "jdbc:mysql://localhost:3306/autotest";
//        String user = "root";
//        String password = "123456";
//
//        Class.forName(driver);
//        connection = DriverManager.getConnection(url,user,password);
//        Statement statement = connection.createStatement();
//        //上传数据
//        String sql = "select * from emp";
//        ResultSet rs = statement.executeQuery(sql);
//
//        //获取数据
//        String role = null;
//        String mobile = null;
//        while(rs.next()){
//            role = rs.getString("role");
//            mobile=rs.getString("mobile");
//        }
//    }


