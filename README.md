# YesCom - Anarchy Coord Exploit
PogDog Software Suite Presents:

Yescom© - Sequel to Nocom; a project carried out by NerdsInc
Nocom information found here: https://github.com/nerdsinspace/nocom-explanation/blob/main/README.md
The program does everything that the nerds did with theirs, but more practicaly implemented.

![Image](rect_4k.png)

# Exploit Behind It

What makes this all possible is very simple, although is sorta tricky to see in the code. I don't care enough to do a fully "why this works" explanation but I'll sum it
up quickly:

* Open any type of foreign container (chest, ender chest, hopper, NOT your inventory).
* Move into any chunk with SPacketPlayerPosition (as well as confirming the teleport with CPacketConfirmTeleport).
* Listen for if you recive a SPacketCloseWindow, which can conclude the chunk is loaded; if you don't recieve the SPacketCloseWindow no one is there

# How Does The Program Work?

The program is essentially split up into 3 different programs which run with 1 api/protocol. 2 clients, 1 server. Here is a rundown/sum-up of what each program is and a bit about what they do:

* Server: This is pretty much just a "bridge" in a way. It only really handles requests and data between the 2 clients and is meant to be run in the backround and not really interacted with by the user. Not much to say about this, if you want to look into how the server works feel free.
* Client 1 (minecraft client): This is essentially the client that interacts with the minecraft server. It also isn't really meant to be interacted with much by the user, it is what handles all packets which need to be queued and sent to the minecraft server, all the accounts and keeping them logged in, as well as all the tracking and scanning process logic. Definitley the most interesting of the few and could run standalone if stripped of some stuff. If you're wondering where the actual coord exploit shit is, well this is it.
* Client 2 (viewer): This is the part that the user actually sees. Written in python because Node is bad, it is what is run of people's PCs instead of our server. People can have multiple of these open, and it synchronously displays all coord exploit data to anyone who is running their own instance of it. You can use this to not only view all the tracked players and loaded chunks, but you can start processes, select chunks to be loaded, and even add and remove accounts from the botnet.

This is a very good way of doing it, as anyone with a registered account can open up the viewer, select the minecraft server they want to be coord exploiting on, add more accounts if they want and start viewing data, querying chunks, etc. All the other programs are running 24/7 on servers and can be accessed whenever needed.


# Was My Base Found?

Even though at the time of writing this I have basically 0 bases, I can already tell you that if you spend time travelling on the nether highways to get to a base/stash, you are fucked. This program will pick you up, track you interdimentionally, and archive whatever you go to. Obviously not everyone will be affected by it, but if you do anything except sit at your base there is a very likely chance we've tracked you to some degree.
