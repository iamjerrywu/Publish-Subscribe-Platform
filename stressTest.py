import os
import sys
import random

if __name__ == "__main__":
	repeat_times = int(sys.argv[1])
	for time in range(repeat_times):
		os.system("mvn test -Dtest=ConformanceTest#PubRegisterTest")
	for time in range(repeat_times):
	    os.system("mvn test -Dtest=ConformanceTest#PubSetReadyTest")
	for time in range(repeat_times):
	    os.system("mvn test -Dtest=ConformanceTest#PubPublishTest")
	for time in range(repeat_times):
	    os.system("mvn test -Dtest=ConformanceTest#SubRegistrationTest")
	for time in range(repeat_times):
	    os.system("mvn test -Dtest=ConformanceTest#SubReplicationTest")
	for time in range(repeat_times):
	    os.system("mvn test -Dtest=ConformanceTest#SubReadTest")
	for time in range(repeat_times):
	    os.system("mvn test -Dtest=ConformanceTest#SubDeletionTest")




