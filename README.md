# Aerb Mod

A Minecraft Fabric mod (1.21.11) inspired by "Worth the Candle". Adds magic systems, skills, and a custom spell inventory.

## Features

### Skills System

Players have skills that start **locked** (-1) and can be unlocked and leveled up. View your skills in the **Stats** tab (accessible from inventory).

**Current Skills:**
- **Blood Magic** - Magic powered by your pumping blood
- **Bone Magic** - Magic using bones as a catalyst

Skills show as "Locked" in grey until unlocked. Use `/setskill` to unlock and level skills.

### Spell System

Spells are special items that can only be stored in:
- The **Spell Inventory** (27 slots, accessed via Spells tab)
- Your **hotbar** (9 slots)
- Your **offhand**

Spells cannot be dropped - they automatically return to you. They cannot be placed in chests or your main inventory.

### Spells

**Blood Magic Spells:**
| Spell | Unlock Level | Effect |
|-------|--------------|--------|
| Aarde's Touch | Blood Magic 0 | Glowing flame, can light things on fire |
| Crimson Fist | Blood Magic 2 | Strength IV buff (costs health) |
| Sanguine Surge | Blood Magic 5 | Jump Boost IV buff (costs health) |

**Bone Magic Spells:**
| Spell | Unlock Level | Effect |
|-------|--------------|--------|
| Physical Tapping | Bone Magic 0 | Speed II, Haste II, Strength II, Regen I (costs bone) |
| Power Tapping | Bone Magic 10 | Strength IV, Jump Boost IV (costs bone) |
| Speed Tapping | Bone Magic 10 | Speed IV, Haste IV (costs bone) |
| Endurance Tapping | Bone Magic 10 | Regen II, Resistance IV, Saturation II (costs bone) |

### Spell Discovery

When you obtain a new spell for the first time, you'll see "Spell discovered: [name]!" This is tracked persistently - you only see the message once per spell.

### Character Sheet

Access via the **Stats** tab from your inventory. Shows:

**Physical Stats:**
- **PHY** = min(POW, SPD, END) + 1
- **POW** - Base 2, boosted by Strength/Jump Boost effects
- **SPD** - Base 2, boosted by Speed/Haste effects
- **END** - Base 2, boosted by Regeneration/Resistance effects

Active buffs show as green bonuses (e.g., "POW: 2 (+2)")

**Skills:**
- Shows all skills with current level
- Locked skills appear greyed out

### Navigation

All screens (Inventory, Stats, Spells) have navigation tabs on the left side for easy switching.

## Commands

All commands support tab-completion.

### Skill Commands
```
/setskill <skill> <level>           - Set your own skill level (-1 to lock)
/setskill <player> <skill> <level>  - Set another player's skill level
/getskill <skill>                   - View your skill level
```

### Spell Commands
```
/givespell <spell>                  - Give yourself a spell
/givespell <player> <spell>         - Give a player a spell
/takespell <spell>                  - Remove a known spell from yourself
/takespell <player> <spell>         - Remove a known spell from a player
```

**Spell names:** `aardes_touch`, `crimson_fist`, `sanguine_surge`, `physical_tapping`, `power_tapping`, `speed_tapping`, `endurance_tapping`

## Quick Start

1. Run `/setskill blood_magic 0` to unlock Blood Magic and receive Aarde's Touch
2. Run `/setskill bone_magic 0` to unlock Bone Magic and receive Physical Tapping
3. Press E to open inventory, then click the **Spells** tab to see your spell inventory
4. Drag spells to your hotbar or offhand to use them
5. Bone spells require bones in your inventory to cast

## Building

```bash
./gradlew build
```

## Running (Development)

```bash
./gradlew runClient
```
