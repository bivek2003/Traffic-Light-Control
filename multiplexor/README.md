# Multiplexor

This is my part of the project (Utshab). I built the Multiplexor.

The Multiplexor sits in the middle of the system. The controller, the device
simulators and the test harness all connect to it with sockets, and it passes
messages between them. It does not decide anything about the traffic lights,
it just delivers the messages.

The messages are lines of text in the format
`TYPE|SOURCE|DESTINATION|ACTION|VALUE`, from
`docs/device-interface-specification.md`.

## Where the files are

My code moved into the shared `src` folder when Bivek put the project together.

| File | What it does |
| --- | --- |
| `src/main/java/multiplexor/Multiplexor.java` | Has `main`. Opens the server socket, keeps the list of connected programs, delivers the messages |
| `src/main/java/multiplexor/ClientHandler.java` | Looks after one connected program, on its own thread |
| `src/main/java/multiplexor/Message.java` | Splits a line into its five parts and joins them back up |
| `src/test/java/multiplexor/TestClient.java` | A small client I wrote so I can test without typing messages by hand |

## How to run it

Build everything from the repository root:

```sh
mkdir -p out
javac -d out $(find src/main/java src/test/java -name '*.java')
```

Start the Multiplexor. It has to be running before anything else connects:

```sh
java -cp out multiplexor.Multiplexor
```

It uses port 5050. You can give it a different one, like
`java -cp out multiplexor.Multiplexor 6000`.

Do not use port 5000 on a Mac. macOS already uses that one for AirPlay and you
get an "Address already in use" error.

To try it out, open a second terminal and run my test client:

```sh
java -cp out multiplexor.TestClient
```

It sends a few messages and you can watch them show up in the first window.

## How a program connects

1. Open a socket to the Multiplexor's host and port.
2. Send a REGISTER saying what your name is, for example
   `REGISTER|light-north|mux|CONNECT|`.
3. Wait for `STATE|mux|light-north|REGISTERED|OK` to come back. That reply means
   the Multiplexor has saved your name, so you do not have to guess whether it
   worked.
4. After that, send normal messages. Anything you send goes to whoever is named
   in the DESTINATION field.

## What it does with each message

- A REGISTER saves your name and sends the reply back. It does not get passed
  on to anybody else.
- Anything sent before registering is refused, because the Multiplexor would not
  know who to send an answer back to.
- Everything else gets looked up by the name in DESTINATION and passed on
  exactly as it arrived. Nothing in the message is changed.
- When a program disconnects, its name comes off the list straight away so
  nothing gets sent into a socket that is gone.

Several programs can be connected at the same time. Each one gets its own
thread, because reading from a socket makes the program stop and wait, and one
quiet program should not freeze everybody else.

## Errors it sends

An ERROR looks like `ERROR|mux|<your name>|<reason>|<what went wrong>`.

| Reason | What it means |
| --- | --- |
| `BAD_MESSAGE` | The line did not have 5 fields, or the type was not one of the 5 |
| `NOT_REGISTERED` | The program sent something before sending REGISTER |
| `UNKNOWN_DESTINATION` | Nobody with that name is connected |

Any `|` in the last field gets swapped for a `/`. Without that the ERROR itself
would end up with more than 5 fields and the program getting it would not be
able to read it.

## Tests

Two test classes, both plain Java with no test library needed.

`MessageTest` checks the message parsing. It does not open any sockets so it
runs straight away:

```sh
java -ea -cp out multiplexor.MessageTest
```

`MultiplexorTest` checks the delivering and the ERROR replies. It starts a
Multiplexor on port 5599 and connects to it the same way a real program would:

```sh
java -ea -cp out multiplexor.MultiplexorTest
```

Both stop with a message saying which check failed if something is wrong.
