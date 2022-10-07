import sqlite3
from argparse import ArgumentParser
import json
import datetime
import os


if __name__ == "__main__":
    parser = ArgumentParser()
    parser.add_argument("-q", required=True)
    parser.add_argument("-y", required=True)
    parser.add_argument("-u", required=True)
    parser.add_argument("-o", required=False, default="upgraded.db")

    args = parser.parse_args()

    if os.path.exists(args.o):
        x = input("Database already exists, remove? ")
        if x != "y":
            exit()
        os.remove(args.o)

    print(f"Connecting to Database: {args.o}")
    conn = sqlite3.connect(args.o)

    print("Loading Quote Schema")
    with open("sql/quoteSchema.sql", mode='r') as f:
        quoteSchema = f.read()

    print("Executing Quote Schema")
    conn.execute(quoteSchema)

    addQuoteSt = "INSERT INTO quotes ( user_id, content, created ) VALUES ( ? , ? , ? )"

    print("Creating Quotes")
    with open(args.q, mode='r') as f:
        for line in f.readlines():
            quote = json.loads(line)
            userId = int(quote[0])
            oldTime = quote[1]
            # Skips oldTime[3] == weekday
            newTime = datetime.datetime(
                oldTime[0], oldTime[1], oldTime[2], oldTime[4], oldTime[5])

            content = str(quote[2])
            # print("Creating Quote:", userId, newTime, content)
            conn.execute(addQuoteSt, (userId, content, newTime))

    conn.commit()

    print("Loading Yike Schema")
    with open("sql/yikeSchema.sql", mode='r') as f:
        yikeSchema = f.read()

    print("Executing Yike Schema")
    conn.execute(yikeSchema)

    print("Loading Usermap")
    with open(args.u, mode="r") as f:
        userMap_raw_dirty = f.readlines()
        userMap_raw_clean = []
        for line in userMap_raw_dirty:
            line = line.strip()
            if line.startswith("#"):
                continue
            userMap_raw_clean.append(line)
        userMap_raw = json.loads(" ".join(userMap_raw_clean))

    userMap = {}
    for guild, userList in userMap_raw.items():
        guild = int(guild)
        for user in userList:
            user = int(user)
            if user in userMap:
                print(
                    f"Duplicate userID: {user}, guild 1: {userMap[user]} guild 2: {guild}")
                continue
            userMap[user] = guild

    addYikeSt = "INSERT INTO yikes (guild_id, user_id, count) VALUES ( ?, ?, ?)"

    print("Creating Yikes")
    with open(args.y, mode='r') as f:
        for line in f.readlines():
            yike = json.loads(line)
            userId = int(yike[0])
            count = int(yike[1])

            if count > 0:
                try:
                    guildId = userMap[userId]
                    conn.execute(addYikeSt, (guildId, userId, count))
                except KeyError as e:
                    print('Missing Usermap entry: ', userId)

    conn.commit()

    print("Done")
