package basic;

/**
* 发送微信消息
*
*
*
*
* */

public class weChatData {
    private String touser;      //消息接收者，企业微信账户
    private String msgtype;     //消息类型
    private int agentid;        //应用的ID
    private Object text;        //实际接收map类型数据

    //设置、获取
    public void setText(Object text){
        this.text=text;
    }
     public Object getText(){
         return text;
     }

     //设置、获取消息类型
     public void setMsgtype(String msgtype){
        this.msgtype=msgtype;
     }
     public String getMsgtype(){
        return msgtype;
     }


     //设置、获取应用的id
    public void setAgentid(int agentid){
        this.agentid=agentid;
    }
    public int getAgentid(){
        return getAgentid();
    }

    public String getTouser(){
        return touser;
    }
    public void setTouser(String touser){
        this.touser=touser;
    }
}


















