# The Client
The client, as described by Nathan in the root README, 
is the client that connects to the given Minecraft server.

## Running the client
To run the client, you will have to build it first:\
From the root directory, run `./gradlew :client:build`.

Once the client has been built, you can run the program:\
Run the command `java -jar ./client/build/libs/yescom.jar -h <server ip>`.

## Accounts
To specify **Mojang** accounts for the client to use, you can use the *accounts.txt* (depends on the argument, see below) file to specify accounts.

The synatx is as follows:
- The accounts must be separated by new lines.
- The accounts must be in the format `email:password`, where a valid email is `username@domain`.
- If the line does not start with an email, it will not be read, so you can use punctuation characters such as `#` to "comment" out accounts.

## The config
The client can be configured to use config rules from *config.yml* (depends on the argument, see below), there are quite a few config rules, a lot of them are self explanatory given their names, but I will run through a few of them below:
- `dont_show_emails` - replaces occurrences of emails with `[REDACTED]` in console.
- `host_name` - the name of the YC server **(not Minecraft server)** to connect to.
- `host_port` - the port of the YC server to connect to.
- `username` - the username to use for the YC server.
- `group_name` - the name of the group that the user belongs to in the YC server.
- `password` - the password of the account for the YC server.
- `login_time` - how often to log accounts in to the Minecraft server, in milliseconds.
- `queries_per_tick` - the *maximum* number of queries to process per tick.
- `log_out_health` - the health at which to log accounts out at.
- `health_relog_time` - how long to wait before relogging an account that logged out due to low health, in milliseconds.
- `type` - an enum representing which type of loaded chunk check to use, `DIGGING` or `INVALID_MOVE`.
- `arzi_mode` - better for higher ping, constantly reopens the storage, can limit functionality in other ways however.
- `reopen_time` - how long to wait before reopening the storage if it fails to.
- `max_open_attempts` - the maximum number of failed open attempts before relogging the account.
- `digging_timeout` - how long to wait for a server response before the chunk is declared as unloaded.
- `render_distance` - the render distance of the server * 2 + 1, so for example, if the render distance is 6, you would enter 13

## Command line options
The client has different command line options that can be used:
- `-l`, `--logLevel` - used to set the log level of the program, valid options are `finest`, `finer`, `fine`, `config`, `info`, `warning` and `severe`.
- `-af`, `--accountsFile` - the txt file for the accounts, see above for correct formatting, `accounts.txt` by default.
- `-rf`, `--configFile` - the YAML file for the config, see above for valid rules, `config.yml` by default.
- `-h`, `--host` - the IP address of the server to connect to **(required)**.
- `-p`, `--port` - the port of the server you want to connect to, `25565` by default.
- `-noyc`, `--noYCConnection` - runs the client standalone, no server required.
- `-n`, `--handlerName` - the name of the handler, used for YC server connections.
