# SkyCounter

SkyCounter is a client-side Fabric mod for **Minecraft 1.21.1** designed for **Hypixel Skyblock**. It displays a customizable, live-updating Head-Up Display (HUD) showing your mob kill counts based on your Bestiary data fetched directly from the Hypixel API. It's main use is for Corleone Boss farming.

---

## Features

- **Live HUD Display**: Shows a customizable HUD overlay with a custom mob icon/head and your current kill count.
- **Hypixel API Integration**: Automatically fetches and syncs your Bestiary stats periodically or on-demand.
- **Session Kills & Drop Tracking**: Track local session kills, monitor drop percentages (such as Corleonite tracking from inventory/NBT parsing), and sound alerts for special events.
- **Customizable Mobs**: Switch between default tracked mobs (Treasure Hoarder, Corleone, Zealot) or add/remove custom mobs with custom IDs, names, and textures using in-game commands.
- **Keybinding Support**: Toggle the HUD visibility on/off easily using a hotkey (Default: `H`).

---

## Setup & Configuration

Before using the mod, you need to input your Hypixel API Key and your un-dashed player UUID. You can configure these either by editing the config file or using the in-game commands.

### In-Game Commands

* `/skycounter set_api <API_KEY>` (or `/skycounter api <API_KEY>`) — Sets your Hypixel API key.
* `/skycounter set_uuid <UUID>` (or `/skycounter uuid <UUID>`) — Sets your un-dashed player UUID.
* `/skycounter position <X> <Y>` — Sets the screen coordinates for the HUD element.

---

## Usage & Command Reference

### Mob Tracking Commands
* `/skycounter treasure_hoarder` — Quickly switch to tracking Treasure Hoarders.
* `/skycounter corleone` — Quickly switch to tracking Corleone.
* `/skycounter zealot` — Quickly switch to tracking Zealots.
* `/skycounter switch <id>` — Switch to any custom-configured mob by its internal ID.
* `/skycounter list` — Lists all currently configured mobs available for tracking.
* `/skycounter add <id> <name> [texture]` — Add a new custom mob to the tracking list.
* `/skycounter remove <id>` — Remove a mob from the tracking list.

### Session & Drop Rate Commands
* `/skycounter session toggle` — Toggle the local session kill counter mode.
* `/skycounter session add` — Manually increment session kills.
* `/skycounter session reset` — Reset session kills back to zero.
* `/skycounter session corleonite` — Show current Corleonite drop counts and calculated drop percentage rates.

### Keybindings
* Press **`H`** (Default) to toggle the visibility of the overlay HUD on your screen.

---

## Disclaimer

I have nothing to do with the Hypixel Name or Trademark of Hypixel if there is one.