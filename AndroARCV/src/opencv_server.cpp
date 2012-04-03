/*
 * Socket.cpp
 *
 * Author: alex.m.damian@gmail.com
 */

#include <stdio.h>
#include <string.h>

#include "Socket.h"

namespace comm_androar_cv {

#define PORT 6667
// TODO(alex): fix this hard coding

int main(int argc, char** argv) {
	Socket server_socket(PORT);
	server_socket.initSocket();
	Socket java_client = server_socket.acceptConnections();

  /*
  printf("Hi there, from  %s#\n",inet_ntoa(pin.sin_addr));
  printf("Coming from port %d\n",ntohs(pin.sin_port));
  */
/*
  if (recv(sd_current, dir, sizeof(dir), 0) == -1) {
	  perror("recv");
	  exit(1);
  }


  if (send(sd_current, dir, strlen(dir), 0) == -1) {
	  perror("send");
	  exit(1);
  }
*/
	java_client.closeSocket();
	server_socket.closeSocket();
  
  return 0;
}

} // namespace comm_androar_cv
