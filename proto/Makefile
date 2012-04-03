all: java cpp

java:
	protoc --java_out=. comm.proto image_features.proto
	cp -r com/ ../AndroARComm/src/

cpp:
	protoc --cpp_out=. comm.proto image_features.proto
	cp -r *.cc ../AndroARCV/src/
	cp -r *.h ../AndroARCV/src/

clean:
	rm -rf *.java *.cc *.h *.py com
