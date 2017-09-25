# Keybase Notary

First, you must install Catena to your local Maven repository:

	git clone https://github.com/alinush/catena-java
	cd catena-java/
	./install-catena.sh

To then run/test the notary on your local machine:

	./run-server.sh -b

To clear your work environment between tests:
	
	rm -r regtest-server/
	ps -ef | grep bitcoin

and kill all processes that appear (except for grep itself).
