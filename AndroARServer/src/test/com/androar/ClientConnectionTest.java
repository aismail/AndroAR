package test.com.androar;

import static org.junit.Assert.*;

import java.lang.reflect.Method;

import org.junit.Test;

import com.androar.ClientConnection;
import com.androar.comm.CommunicationProtos.ServerMessage;
import com.androar.comm.CommunicationProtos.ServerMessage.ServerMessageType;

public class ClientConnectionTest {

	@Test
	public void testCreateServerMessage_HelloMessage() {
		ServerMessage expected_message = ServerMessage.newBuilder().
				setMessageType(ServerMessageType.HELLO_MESSAGE).build();
		try {
			Method method = ClientConnection.class.getDeclaredMethod(
					"createServerMessage", ServerMessageType.class);
			method.setAccessible(true);
			assertEquals(expected_message, method.invoke(null, ServerMessageType.HELLO_MESSAGE));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
