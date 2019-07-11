package basic;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
//import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
* 发送消息
*
*
*
* */
public class sendWeChatMsg {
    private DefaultHttpClient httpClient;
    private HttpPost httpPost;
    private HttpGet httpGet;
    public static final String CONTENT_TYPE = "Content-Type";
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static Gson gson = new Gson();


    /**
    * 微信授权请求,获取授权响应内容
    * parm:String get_token_url
    * return: response
    *
    *
    * */
    public String toAuth(String get_token_url) throws IOException {
        httpGet = new HttpGet(get_token_url);
        httpClient = new DefaultHttpClient();
        HttpResponse response = httpClient.execute(httpGet);

//        httpClient = HttpClients.createDefault();
//        CloseableHttpResponse response = httpClient.execute(httpGet);
        String resp;
        HttpEntity entity = response.getEntity();
        resp = EntityUtils.toString(entity, "utf-8");
        EntityUtils.consume(entity);
        LoggerFactory.getLogger(getClass()).info(" resp:{}", resp);
        return resp;
    }


    /**
    * 获取toAuth
    *
    *
    *
    * */
    public String getToken(String corpid,String corpsecret) throws IOException {
        sendWeChatMsg sw = new sendWeChatMsg();
        urlData urlData = new urlData();
        urlData.setGet_token_url(corpid,corpsecret);
        String resp = sw.toAuth(urlData.get_token_url());

        Map<String, Object> map = gson.fromJson(resp, new TypeToken<Map<String, Object>>() {}.getType());
        //System.out.println("gst测试："+map);       //map是：{errcode=0.0, errmsg=ok, access_token=-vo8KXNyyocH7pu8yx7vs3fdNL_1v2clzsOcZWRAifnaTXQn4kSry8mON5beB67wfDNbKNlCY7alIJSDuefwyaVZEC4HK71vvvotGJJxyYTfjJwtNAG3tFEcuPdmw57qeVunxvxE_0H9R-A7iVWbEDwynb5fKtEW-dEUYgs4jpCuIf_8wu3UiGkondVV8xFhHohKA_ckbZg1FmfKjyZWWQ, expires_in=7200.0}
        return map.get("access_token").toString();
    }


    /**
    * 创建微信发送post请求
    *
    *
    *
    * */
    public String createPostData(String touser,String msgtype,int application_id,String contentKey,String contentValue){
        weChatData wcd = new weChatData();
        wcd.setTouser(touser);
        wcd.setAgentid(application_id);
        wcd.setMsgtype(msgtype);
        Map<Object, Object> content = new HashMap<Object, Object>();
        content.put(contentKey,contentValue+"\n--------\n"+dateFormat.format(new Date()));
        wcd.setText(content);
        return gson.toJson(wcd);
    }


    /**
    * 创建微信发送post请求实体
    *
    *
    *
    * */
    public String post(String charset, String contentType, String url, String data,String token) throws IOException {
        httpPost = new HttpPost(url+token);
        httpPost.setHeader(CONTENT_TYPE, contentType);
        httpPost.setEntity(new StringEntity(data, charset));

        httpClient = new DefaultHttpClient();
        HttpResponse response = httpClient.execute(httpPost);


        String resp;
        HttpEntity entity = response.getEntity();
        resp = EntityUtils.toString(entity, charset);
        EntityUtils.consume(entity);
        LoggerFactory.getLogger(getClass()).info(
                "call [{}], param:{}, resp:{}", url, data, resp);
        return resp;
    }

}























