# The Linda Distributed Storage Model

### Getting Started
***
The Linda Distributed Storage Model creates a conceptual "global space" to store tuples across an infinite number of nodes.

These instructions will help you deploy a single node in your environment. However, the repetiion of these steps provided will create a peer to peer network of nodes which can read and write tuples into this "global space".

##### Prerequisites
This peer to peer network functions purely on the Java Virtual Machine. Therefore, you will need the **Java Virtual Machine** and **Java Development Kit** to run this program.

### Utilization
***
After cloning this repository to your local machine, you have two options of running this program:
- Run it locally by opening mutliple command lines and consequently, multiple ports.
- Run on different servers.

To begin running the program,
```
$ make
$ Java P2 <hostName>
```
If you run this command on many different servers or command lines, you will open available ports to listen and wait for the Linda commands.

##### Linda Commands
**add**
Add another host into the current network of hosts
```
linda> add {(<hostName>, <IPaddress>, <portNumber>)}
```
**delete**
Remove a host from the current network of hosts
```
linda> delete (<hostName> [{, <hostName> }])
```
**out**
Place this tuple into the tuple space
```
linda> out ("abc", 3)
```
**rd**
Read in the tuple that we are searching for
```
linda> rd ("abc", 3)
```
```
linda> rd ("abc", ?i:int)
```
**in**
Read in the tuple that we are searching for and delete this tuple from the tuple space
```
linda> in ("abc", 3)
```
```
linda> in ("abc", ?i:int)
```


### Examples
***
```
$ Java P2 host1
129.210.16.80 at port number: 9998
linda>
```
```
$ Java P2 host2
129.210.16.81 at port number: 3571
linda> add (host1, 129.210.16.80, 9998)
linda> out("abc", 3)
put tuple ("abc", 3) on 129.210.16.80
linda> in("abc", ?i:int)
get tuple ("abc", 3) on 129.210.16.81
linda>
```

