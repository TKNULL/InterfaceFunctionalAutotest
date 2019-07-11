package basic;


/**
* 微信授权请求
*
*
*
* */

public class urlData {
    private String corpid;          //企业ID
    private String corpsecret;     //应用密钥
    private String get_token_url;   //获取token的url
    private String send_message_url; //发送消息的url


    //Setter,Getter
    public String getCorpid(){
        return corpid;
    }
    public void setCorpid(String corpid){
        this.corpid=corpid;
    }

    //Setter,Getter
    public String getCorpsercret(){
        return corpsecret;
    }
    public void setCorpsercret(String corpsercret){
        this.corpsecret=corpsercret;
    }

    //拼接、设置获取token的url
    public void setGet_token_url(String corpid,String corpsecret){
        this.get_token_url="https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid="+corpid+"&corpsecret="+corpsecret;
    }
    public String get_token_url(){
        return get_token_url;
    }

    //获取发送消息的url
    public String getSend_message_url(){
        send_message_url = "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=";
        return  send_message_url;
    }







































}
