package data;

import java.io.Serializable;
import java.nio.ByteBuffer;

import static data.RPCMessage.REPLY;
import static data.RPCMessage.REQUEST;

/**
 * Message class to define marshalling and un-marshalling.
 *
 * @author michael
 */
public class Message implements Serializable {

    private byte data[] = null;
    private int length = 0;

    /*
    * Perform the marshalling.
    * */
    public void marshal(RPCMessage rpcMessage) {

        length = rpcMessage.getLengthInBytes();
        ByteBuffer byteBuffer = ByteBuffer.allocate(length);
        int index = 0;

        switch (rpcMessage.getMessageType()) {
            case REQUEST:
                byteBuffer.putShort(index, REQUEST);
                break;
            case REPLY:
                byteBuffer.putShort(index, REPLY);
                break;
        }
        index += 2;

        byteBuffer.putLong(index, rpcMessage.getTransactionId());
        index += 8;

        byteBuffer.putLong(index, rpcMessage.getRPCId());
        index += 8;

        byteBuffer.putLong(index, rpcMessage.getRequestId());
        index += 8;

        byteBuffer.putShort(index, rpcMessage.getProcedureId());
        index += 2;

        for (int i = 0; i < rpcMessage.getCsv_data().length(); i++, index += 2) {
            byteBuffer.putChar(index, rpcMessage.getCsv_data().charAt(i));
        }

        byteBuffer.putShort(index, rpcMessage.getStatus());

        data = byteBuffer.array();
    }

    /*
    * Perform the unmarshalling.
    * */
    public RPCMessage unMarshal() {

        RPCMessage rpcMessage = new RPCMessage();
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        int index = 0;

        switch (byteBuffer.getShort(index)) {
            case 0:
                rpcMessage.setMessageType(RPCMessage.MessageType.REQUEST);
                break;
            case 1:
                rpcMessage.setMessageType(RPCMessage.MessageType.REPLY);
                break;
        }
        index += 2;

        rpcMessage.setTransactionId(byteBuffer.getLong(index));
        index += 8;

        rpcMessage.setRPCId(byteBuffer.getLong(index));
        index += 8;

        rpcMessage.setRequestId(byteBuffer.getLong(index));
        index += 8;

        rpcMessage.setProcedureId(byteBuffer.getShort(index));
        index += 2;

        StringBuilder stringBuilder = new StringBuilder();
        for (; index < byteBuffer.array().length - 2; index += 2) {
            stringBuilder.append(byteBuffer.getChar(index));
        }
        rpcMessage.setCsv_data(stringBuilder.toString());

        rpcMessage.setStatus(byteBuffer.getShort(index));

        return rpcMessage;
    }
}
