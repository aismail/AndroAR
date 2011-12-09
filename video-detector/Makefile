#OPENCV=/home/andrei/RepoS/OpenCV/OpenCV-2.3.1/l
all: main.o netutils.o imaging.o
	g++ -o main main.o netutils.o imaging.o -ggdb `pkg-config --cflags opencv` `pkg-config --libs opencv`
	
main.o: main.cpp
	g++ -ggdb `pkg-config --cflags opencv` -c main.cpp `pkg-config --libs opencv`
	
netutils.o: netutils.cpp
	g++ -c -ggdb netutils.cpp

imaging.o: imaging.cpp
	g++ -c -ggdb imaging.cpp

clean:
	rm -rf *.o main
