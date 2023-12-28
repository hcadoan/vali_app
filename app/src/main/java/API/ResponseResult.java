package API;

import java.util.List;

public class ResponseResult {
    private String msg;
    private String msg_vi;
    private List<String> errors;

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getMsg_vi() {
        return msg_vi;
    }

    public void setMsg_vi(String msg_vi) {
        this.msg_vi = msg_vi;
    }

    @Override
    public String toString() {
        return "ResponseResult{" +
                "msg='" + msg + '\'' +
                ", msg_vi='" + msg_vi + '\'' +
                '}';
    }
}
