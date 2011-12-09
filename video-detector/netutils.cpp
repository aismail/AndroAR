#include <stdio.h>
#include <sys/types.h> 
#include <sys/socket.h> 
#include <arpa/inet.h>  
#include <stdlib.h>     
#include <string.h>     
#include <unistd.h>     
#include <errno.h>

#include "opencv2/opencv.hpp"
#include "opencv2/core/core.hpp"
#include "opencv2/features2d/features2d.hpp"
#include "opencv2/highgui/highgui.hpp"


#include "netutils.h"

#define min(a,b) (((a) < (b)) ? (a) : (b))

using namespace cv;
using namespace std;

int serverSock, clientSocket;                        /* Socket */
struct sockaddr_in serverAddr; /* Local address */
struct sockaddr_in clientAddr; /* Client address */


unsigned short echoServPort;     /* Server port */
int recvMsgSize;                 /* Size of received message */
socklen_t clientLen;

void initServer(){
	
	/* Create socket for sending/receiving datagrams */
    //if ((sock = socket(PF_INET, SOCK_DGRAM, IPPROTO_UDP)) < 0)
    //	printf("socket() failed");
    
    /* Create socket for incoming connections */
    if ((serverSock = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP)) < 0)
        printf("socket() failed");
    
    echoServPort = SERVER_PORT;
    
    /* Construct local address structure */
    memset(&serverAddr, 0, sizeof(serverAddr));   /* Zero out structure */
    serverAddr.sin_family = AF_INET;                /* Internet address family */
    serverAddr.sin_addr.s_addr = htonl(INADDR_ANY); /* Any incoming interface */
    serverAddr.sin_port = htons(echoServPort);      /* Local port */

	

    /* Bind to the local address */
    if (bind(serverSock, (struct sockaddr *) &serverAddr, sizeof(serverAddr)) < 0)
        perror("bind() failed");    
    
    /* Mark the socket so it will listen for incoming connections */
    if (listen(serverSock, MAXPENDING) < 0)
        printf("listen() failed");
        
    printf("Waiting for client\n");    
    
	/* Wait for a client to connect */
	if ((clientSocket = accept(serverSock, (struct sockaddr *) &clientAddr, &clientLen)) < 0)
		printf("accept() failed");
		
	
}

// Nicely close connection
void closeServer(){
	close(clientSocket);
}

/*
cv::Mat getFrame(){
	
	unsigned char *buf, *stamp, *check, ok = 1;
	unsigned int len = 0, count = 0, rc = 0, timeout = 0;
	
    int recvMsgSize;                  
	
	buf = (unsigned char *) malloc(MAXFILESIZE * sizeof(unsigned char));
	
	//stamp = (unsigned char*) malloc(1024 * sizeof(unsigned char));
	//check = (unsigned char*) malloc(1024 * sizeof(unsigned char));
	
    //rc = send(clientSocket, buf, 1024, 0);
    //flush(clientSocket);
    //strncpy(stamp, "deadbeef", 8);
    do{
        
        recvMsgSize = recv(clientSocket, buf + count, RCVBUFSIZE, MSG_DONTWAIT);
         
        
        if(recvMsgSize > 0){
			count += recvMsgSize;
			printf("recv: %d\n", recvMsgSize);
			timeout = 0;
		}
        
        //recv(clientSocket, check, 8, MSG_PEEK | MSG_DONTWAIT);
        
        //if(memcmp(check, stamp, 8) == 0){
		//	recv(clientSocket, check, 8, 0);
		//	break;
		//}
		
		if(memcmp(buf + count - 8, stamp, 8) == 0){
			count -= 8;
			break;
		}
        
		//int rc = send(clientSocket, &ok, sizeof(unsigned char), MSG_DONTWAIT); 
        //if (rc != EAGAIN && rc != EWOULDBLOCK)
        //    break;
		timeout++;
    } while (timeout < 1000000);
    //if (send(clientSocket, &ok, sizeof(unsigned char), 0) != recvMsgSize)
    //        printf("send() failed");
            
    printf("len = %d, buf = ", count);
    
    for(int i = 0; i < 5; i++)
		printf("%c ", buf[i]);
	printf("\n");
	
    //printf("Time: %f\n", (float) ((clock() - milis) / CLOCKS_PER_SEC / 1000));
    if(count == 0){
		free(buf);
		free(stamp);
		free(check);
		
		return Mat();
    }

    vector<uchar> v;
    
    int i;
    for(i = 0; i < count; i++)
		v.push_back(buf[i]);
		
	//free(buf);
    free(stamp);
    free(check);
    
	return cv::imdecode(cv::Mat(vector<uchar>(buf, buf + sizeof(buf)/sizeof(unsigned char))), 1);
	
}
*/

cv::Mat getFrame(){
	
	unsigned char *buf, ok = 1;
	unsigned int len = 0, count = 0, b1, b2, b3, b4;
	b1 = b2 = b3 = b4 = 0;

    int recvMsgSize;                  
	//clock_t milis = clock();
    
    if ((recvMsgSize = recv(clientSocket, &b1, sizeof(unsigned char), 0)) < 0)
        printf("recv() failed");
    if ((recvMsgSize = recv(clientSocket, &b2, sizeof(unsigned char), 0)) < 0)
        printf("recv() failed");
    if ((recvMsgSize = recv(clientSocket, &b3, sizeof(unsigned char), 0)) < 0)
        printf("recv() failed");
    if ((recvMsgSize = recv(clientSocket, &b4, sizeof(unsigned char), 0)) < 0)
        printf("recv() failed");
    b4 <<= 24;
    b3 <<= 16;
    b2 <<= 8;
    len = b4 | b3 | b2 | b1;

	buf = (unsigned char *) malloc(len * sizeof(unsigned char));
  
    while (count < len || recvMsgSize == 0){

        if ((recvMsgSize = recv(clientSocket, buf + count, min(RCVBUFSIZE, len - count), 0)) < 0)
            printf("recv() failed");
            
        count += recvMsgSize;
        
    }
    //printf("Time: %f\n", (float) ((clock() - milis) / CLOCKS_PER_SEC / 1000));
    
    vector<uchar> v(buf, buf + len + 1);
    free(buf);
	return cv::imdecode(v, 1);
	
	
	
}

void sendResult(char res){
	int sendMsgSize = 0;
    if ((sendMsgSize = send(clientSocket, &res, sizeof(char), 0)) < 1)
        printf("send() failed");
	
}
