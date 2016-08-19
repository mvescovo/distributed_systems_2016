package data;

import java.io.Serializable;

/**
 * RCP Message class
 *
 * @author michael
 */
public class RPCMessage implements Serializable {

    static final short REQUEST = 0;
    static final short REPLY = 1;
    public enum MessageType {REQUEST, REPLY};

    private MessageType messageType;
    private long TransactionId; /* transaction id */
    private long RPCId; /* Globally unique identifier */
    private long RequestId; /* Client request message counter */
    private short procedureId; /* e.g.(1,2,3,4) */
    private String csv_data; /* data as comma separated values*/
    private short status;

    public RPCMessage() {}

    /*
    * Get the length of the message for use with marshalling and unmarshalling.
    * */
    int getLengthInBytes() {
        return 2 + 8 + 8 + 8 + 2 + (csv_data.length() * 2) + 2;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public long getTransactionId() {
        return TransactionId;
    }

    public void setTransactionId(long transactionId) {
        TransactionId = transactionId;
    }

    public long getRPCId() {
        return RPCId;
    }

    public void setRPCId(long RPCId) {
        this.RPCId = RPCId;
    }

    public long getRequestId() {
        return RequestId;
    }

    public void setRequestId(long requestId) {
        RequestId = requestId;
    }

    public short getProcedureId() {
        return procedureId;
    }

    public void setProcedureId(short procedureId) {
        this.procedureId = procedureId;
    }

    public String getCsv_data() {
        return csv_data;
    }

    public void setCsv_data(String csv_data) {
        this.csv_data = csv_data;
    }

    public short getStatus() {
        return status;
    }

    public void setStatus(short status) {
        this.status = status;
    }
}
