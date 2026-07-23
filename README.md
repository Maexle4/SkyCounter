# SkyCounter

SkyCounter is an advanced, client-side Fabric mod built specifically for **Minecraft 1.21.1** tailored for **Hypixel Skyblock** players. It provides a seamless, live-updating Head-Up Display (HUD) overlay that tracks your mob kill counts by pulling real-time Bestiary data straight from the official Hypixel API. Whether you are grinding regular mobs or hunting rare bosses, SkyCounter keeps all your critical stats front and center without cluttering your screen.

---

## Key Features

* **Live HUD Overlay:** Displays a clean, customizable overlay featuring custom mob heads/icons alongside your real-time kill counts directly on your game screen.
* **Hypixel API Integration:** Periodically and automatically synchronizes with your profile's Bestiary data, ensuring your kill records remain accurate and up-to-date.
* **Session Kills & Advanced Drop Tracking:** Go beyond standard API stats by enabling the local session mode to track kills per play session. It includes sophisticated inventory and NBT data parsing to monitor rare drops like Corleonite and compute precise drop rate percentages.
* **Special Corleone Assistance:** Features dedicated automation for Corleone hunters. When a Corleone spawns, the mod triggers an immediate triple-anvil sound alert so you never miss it. Furthermore, it automatically starts a 2-minute timer upon its defeat and plays an experience orb sound reminder to help you track spawn windows.
* **Dynamic Custom Mobs:** Easily switch between pre-configured targets (like Treasure Hoarders, Corleones, and Zealots) or use in-game commands to add, modify, or remove any custom mob using its internal ID, custom name, and custom textures.
* **Convenient Keybindings:** Toggle the entire HUD visibility on or off instantly using a customizable hotkey (default is set to `H`).

---

## Setup & Configuration

To make the mod function correctly, you must link it to your Hypixel account by providing your API Key and your un-dashed player UUID. You can do this quickly via in-game commands:

* `/skycounter set_api <API_KEY>` (or `/skycounter api <API_KEY>`) — Sets your personal Hypixel API key.
* `/skycounter set_uuid <UUID>` (or `/skycounter uuid <UUID>`) — Sets your un-dashed player UUID.
* `/skycounter position <X> <Y>` — Adjusts the exact screen coordinates where the HUD overlay is rendered.

---

## Complete Command Reference

### Mob Tracking & Management

* `/skycounter treasure_hoarder` — Instantly switches your active tracking target to Treasure Hoarders.
* `/skycounter corleone` — Instantly switches your active tracking target to Corleone.
* `/skycounter zealot` — Instantly switches your active tracking target to Zealots.
* `/skycounter switch <id>` — Switches tracking to any custom mob using its unique internal ID.
* `/skycounter list` — Prints a complete list of all currently configured mobs available for tracking in your game chat.
* `/skycounter add <id> <name> [texture]` — Adds a brand new custom mob to your configuration list.
* `/skycounter remove <id>` — Deletes an existing mob entry from your tracking list.

### Session Management & Drop Rates

* `/skycounter session toggle` — Enables or disables the local session kill counter mode.
* `/skycounter session add` — Manually increments your current session kill tally by one.
* `/skycounter session reset` — Resets your current session kills back to zero.
* `/skycounter session corleonite` — Pulls and displays your current Corleonite inventory count alongside your calculated drop-rate percentage for the active session.

### Controls

* Press **`H`** (Default) to toggle the visibility of the overlay HUD on your screen.

---

## Disclaimer

I have nothing to do with the Hypixel Name or Trademark of Hypixel if there is one.
