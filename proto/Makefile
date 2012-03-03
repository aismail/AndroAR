all: java

java:
	protoc --java_out=. comm.proto image_features.proto
	cp -r com/ ../AndroARComm/src/

clean:
	rm -rf *.java *.cpp *.py com
