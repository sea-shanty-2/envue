import json

if __name__ == "__main__":
    with open("limited_emoji_unicodes") as fp_limited_raw:
        with open("emojis.json") as fp_all:
            with open("limited_emojis.json", "w+") as fp_limited:
                limited = []
                all_emojis = json.load(fp_all)
                limited_codes = fp_limited_raw.read().split('\n')

                for emoji in all_emojis:
                    if emoji['char'] in limited_codes:
                        limited.append(emoji)

                json.dump(limited, fp_limited)

