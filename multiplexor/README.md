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

My part is working now. The Multiplexor takes several programs at the same
time, each on its own thread. A program sends REGISTER to say what its name is
and gets a reply back. After that, anything it sends goes on to the program
named in the DESTINATION field. If the message is written wrong, or nobody with
that name is connected, an ERROR comes back to whoever sent it.

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
- [x] deliver messages to the right place
- [x] send ERROR back when something is wrong
- [ ] unit tests (this is on Bivek's chart as one of my deliverables)

## Errors it sends

An ERROR looks like `ERROR|mux|<your name>|<reason>|<what went wrong>`.

| Reason | What it means |
| --- | --- |
| `BAD_MESSAGE` | The line did not have 5 fields, or the type was not one of the 5 |
| `NOT_REGISTERED` | The program sent something before sending REGISTER |
| `UNKNOWN_DESTINATION` | Nobody with that name is connected |

Any `|` in the last field gets swapped for a `/`, otherwise the ERROR itself
would have more than 5 fields and you would not be able to read it.

## Questions for Bivek

1. The spec has an ERROR type but does not say what goes in its ACTION and
   VALUE fields. I picked a short reason and then the text that caused the
   problem. Is that okay?
2. The spec says a STATE message goes to the controller and to JavaFX, but
   DESTINATION only holds one name. How should a device send one state message
   to both of them?
3. The architecture doc says the lights go red if the controller disconnects,
   but it also says the Multiplexor does not make traffic decisions. Which
   module is supposed to do that?
