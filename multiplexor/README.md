# Multiplexor

This is my part of the project (Utshab). I am building the Multiplexor.

The Multiplexor sits in the middle of the system. The controller, the device
simulators and the test harness all connect to it with sockets, and it passes
messages between them. It does not decide anything about the traffic lights,
it just delivers the messages.

The messages are lines of text in the format
`TYPE|SOURCE|DESTINATION|ACTION|VALUE`, from
`docs/device-interface-specification.md`.

## Where I am so far

The message class works, it can split a line into the 5 fields and put it back
together. The Multiplexor takes several programs at the same time now, each one
on its own thread, and it remembers the name every program registers with. When
a program sends REGISTER it gets a reply back so it knows it worked.

It still does not deliver messages to anybody. That is what I am doing next.

## How to run it

```
javac -d out multiplexor/*.java
java -cp out multiplexor.Multiplexor
```

Then in another terminal, run the little test client I wrote:

```
java -cp out multiplexor.TestClient
```

It sends a few messages and you can watch them show up in the first window.

It uses port 5050. Do not use 5000 on a Mac, macOS already uses that one for
AirPlay and you get an "Address already in use" error.

## To do

- [x] class for reading a message
- [x] open a server socket
- [x] handle more than one program at a time
- [x] remember the name each program registers with
- [ ] deliver messages to the right place
- [ ] send ERROR back when something is wrong
