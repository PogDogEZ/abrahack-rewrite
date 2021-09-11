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

It uses very complex tracking algorithms, but I'll attempt to sum it all up here:

* We start at the world border corner and check every chunk in the world (this can take weeks, but is nessary to make sure we don't skip anyone)
* Upon finding a loaded chunk, we create a tracker to check that chunk as fast as possible
* When the chunk we are tracking unloads, we assume that they have left, and send that chunk to our website where it gets plotted on our master graph

# Was My Base Found?

Yes, if you have played on constantiam.net since febuary of 2017, there is a 100% chance that your base is in our database
