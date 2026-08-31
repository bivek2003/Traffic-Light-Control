# Multiplexor

This is my part of the project (Member 2). I am building the Multiplexor.

The Multiplexor sits in the middle of the system. The controller, the device
simulators and the test harness all connect to it with sockets, and it passes
messages between them. It does not decide anything about the traffic lights,
it just delivers the messages.

The messages are lines of text in the format
`TYPE|SOURCE|DESTINATION|ACTION|VALUE`, from
`docs/device-interface-specification.md`.

## Where I am so far

Just started. Working on the message class first because it does not need
sockets, so it is easier to get going with.

## To do

- [ ] class for reading a message
- [ ] open a server socket
- [ ] handle more than one program at a time
- [ ] remember the name each program registers with
- [ ] deliver messages to the right place
- [ ] send ERROR back when something is wrong
