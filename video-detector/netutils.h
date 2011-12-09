#ifndef _NETUTILS_H
#define _NETUTILS_H

#include "opencv2/opencv.hpp"
#include "opencv2/core/core.hpp"
#include "opencv2/features2d/features2d.hpp"
#include "opencv2/highgui/highgui.hpp"


#define SERVER_PORT 3333
#define MAXPENDING 10
#define RCVBUFSIZE 1024
#define MAXFILESIZE 1000000

void initServer();
void closeServer();

cv::Mat getFrame();
void sendResult(char res);
#endif
