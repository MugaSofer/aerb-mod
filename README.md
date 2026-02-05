# Aerb Mod

A Minecraft Fabric mod (1.21.11) inspired by Worth the Candle by Alexander Wales. Adds magic systems and skills from Aerb, the setting of Worth the Candle.

## Features

### Skills System

Players have skills that start **locked** (-1) and can be unlocked and leveled up. View your skills in the **Stats** tab (accessible from inventory).

**Current Skills:**
- **Blood Magic** - Magic powered by your pumping blood
- **Bone Magic** - Magic using bones as a catalyst
- **One-Handed Weapons** - Combat with axes and swords.
- **Parry** - Deflect attacks while swinging your weapon

Skills show as "Locked" in grey until unlocked. Use `/setskill` to unlock and level skills.

### Skill XP System

Skills gain XP through relevant actions and level up automatically. XP needed for next level = (level + 2)².

**XP Sources:**
- **One-Handed**: Deal damage with sword/axe, kill enemies
- **Parry**: Attempt parries (success or failure)
- **Blood Magic**: Hold Aarde's Touch (1 XP per 6 seconds), cast blood spells
- **Bone Magic**: Cast bone spells

**Messages:**
- "Skill unlocked: X!" - First XP gain unlocks the skill
- "Skill increased: X lvl Y!" - Level up notification

XP values are configurable in `config/aerb_xp.json`.

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
| Claret Spear | Blood Magic 25 | Throwable blood spear (costs 1 heart while held) |

**Claret Spear Details:**
- Reduces max HP by 1 heart while held in main hand
- Right-click to throw as a projectile
- Damage: 6 base + 0.5 per Blood Magic level
- On throw: max HP restores but the health is spent (blood forms the spear)
- On unequip without throwing: max HP and health both restore
- Can be used for parrying like other weapons

**Bone Magic Spells:**
| Spell | Unlock Level | Effect |
|-------|--------------|--------|
| Physical Tapping | Bone Magic 0 | Speed II, Haste II, Strength II, Regen I (costs bone) |
| Power Tapping | Bone Magic 10 | Strength IV, Jump Boost IV (costs bone) |
| Speed Tapping | Bone Magic 10 | Speed IV, Haste IV (costs bone) |
| Endurance Tapping | Bone Magic 10 | Regen II, Resistance IV, Saturation II (costs bone) |

### Spell Discovery

When you obtain a new spell for the first time, you'll see "Spell discovered: [name]!" This is tracked persistently - you only see the message once per spell.

### Parry System

Swing a weapon or tool to enter a brief parry window (0.5 seconds). Frontal attacks during this window trigger a parry attempt.

**Parry Roll:**
- Player: `1d100 + (SPD × Parry level)`
- Enemy: `1d100 + modifier`
- Player wins ties

**Modifiers:**
- Projectiles (arrows, etc.): +25

**On Success:**
- Attack is completely blocked
- Weapon takes durability damage equal to the attack (threshold of 3)

**Skill Unlock:**
Your first parry attempt (success or fail) unlocks the Parry skill.

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

### Virtues System

Skill-granted abilities (virtues) have their own inventory, similar to spells. Access via the **Virtues** tab.

Virtues come in two types:
- **Active virtues** can be stored in virtue inventory, hotbar, or offhand
- **Passive virtues** (marked "Passive" in tooltip) can only be stored in the virtue inventory

When you obtain a new virtue for the first time, you'll see "New Virtue: [name]!"

### Virtues

**Blood Magic Virtues:**
| Virtue | Unlock Level | Type | Effect |
|--------|--------------|------|--------|
| Hypertension | Blood Magic 20 | Passive | Doubles max HP (10 → 20 hearts). Take extra damage while above 50% HP based on damage type. |

**Hypertension Damage Multipliers:**
- Pointy damage (e.g. arrow, cactus, thorns): 1.25x
- Blunt damage (e.g. fall, mob attack): 1.5x
- Other damage (e.g. fire, magic, drowning): 2.0x

These multipliers are configurable in `config/aerb_hypertension.json`.

### Description Config

Spell and virtue descriptions are stored in `config/aerb_descriptions.json` and can be edited freely. Each item has an array of description lines that appear as gray italic text in tooltips.

Example format:
```json
{
  "descriptions": {
    "aardes_touch": [
      "A glowing flame that can light things on fire.",
      "The first spell of Blood Magic."
    ]
  }
}
```

The config file is created with defaults on first run and reloaded on game startup.

**Parry Virtues:**
| Virtue | Unlock Level | Type | Effect |
|--------|--------------|------|--------|
| Prescient Blade | Parry 20 | Passive | Halves the projectile modifier for parry rolls (+25 → +12) |
| Prophetic Blade | Parry 40 | Active | When in hotbar: parry from any direction, auto-switch to best weapon and parry if not equipped when attacked|

**One-Handed Weapons Virtues:**
| Virtue | Unlock Level | Type | Effect |
|--------|--------------|------|--------|
| Riposter | One-Handed 40 | Active | When in hotbar: on successful parry with sword or axe, immediately counterattack |

### Navigation

All screens (Inventory, Stats, Spells, Virtues) have navigation tabs on the left side for easy switching.

### Death Persistence

Spells, virtues, and skills are **preserved on death**. When you respawn:
- All spells remain in your spell inventory; any in your hotbar or offhand return to your spell list
- All virtues remain in your virtue inventory; any in your hotbar or offhand return to your virtues list
- All skill levels and discoveries are kept

Spells and virtues cannot be dropped - they automatically return to you.

### Mobs

**Undead** - Zombie variants from the Risen Lands
- Glowing red eyes (requires [Entity Texture Features](https://modrinth.com/mod/entitytexturefeatures) for true emissive glow)
- Takes **4x damage** when hit in the heart (chest area, 40-70% up the body)
- Unaffected by sunlight
- Cannot pick up items from the ground
- Cannot hold weapons (will drop any weapons they're spawned with)
- Can wear armor
- No baby variants
- Spawn egg available in creative inventory
- When 25 or more undead are gathered together, they will attempt to form...

**Lesser Umbral Undead** - Large, quadrupedal conglomerations of undead corpses
- Can absorb undead (and vanilla zombies, if present) to heal and grow. (1 undead = 10 HP)
- Can smash their way through walls to chase their prey
- Undead fall off them as they take damage. This can reduce their size
	- Though once they contain at least 20 corpses, they can't be forced below that size
- Collapse into a group of undead when they die

### Skin Magic / Tattoos (WIP)

Tattoos appear directly on your character's skin. They only show on exposed skin areas - not through clothing.

**Tattoos:**
| Tattoo | Effect |
|--------|--------|
| Fall Rune | Single-use. Automatically activates when you're about to hit the ground at dangerous velocity, slowing your fall to a safe speed. |

**Custom Skin Masks:**

If you use a custom Minecraft skin, you'll need to create a matching skin mask so tattoos appear correctly on your character:

1. Create a 64x64 grayscale PNG with the same UV layout as Minecraft skins
2. Paint **white (255)** on areas that are exposed skin
3. Paint **black (0)** on areas that are clothed/covered
4. Save as `skin_mask_<yourname>.png` in `assets/aerb/textures/entity/`

Default masks are provided for Steve and Alex. Without a custom mask, tattoos may appear through clothing or be hidden on exposed skin.

## Known Issues

- **Mouse briefly flickers to center** when switching between Spells and Virtues tabs (restores immediately)
- Mixin for Entity.dropStack shows remap warning (doesn't affect functionality)
- Getting spells from creative inventory doesn't trigger discovery message
- Claret Spear texture does not face the direction it's thrown

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

**Spell names:** `aardes_touch`, `crimson_fist`, `sanguine_surge`, `claret_spear`, `physical_tapping`, `power_tapping`, `speed_tapping`, `endurance_tapping`

### Virtue Commands
```
/givevirtue <virtue>                - Give yourself a virtue
/givevirtue <player> <virtue>       - Give a player a virtue
/takevirtue <virtue>                - Remove a virtue from yourself
/takevirtue <player> <virtue>       - Remove a virtue from a player
```

**Virtue names:** `hypertension`, `prescient_blade`, `prophetic_blade`, `riposter`

### Mob Commands
```
/spawnhorde <number>				- Spawns a horde of undead. Defaults to 25 undead (which should immediately begin trying to form an Umbral Undead.)
/spawnumbral <corpses>				- Spawns a Lesser Umbral Undead. Defaults to one composed of 30 corpses; can go as high as 255. (<20 corpses will spawn a weakened but full-sized Umbral Undead.)
```

### Tattoo Commands
```
/givetattoo <tattoo>                - Give yourself a tattoo (1 charge)
/givetattoo <tattoo> <charges>      - Give yourself a tattoo with specific charges (-1 for unlimited)
/givetattoo <player> <tattoo>       - Give a player a tattoo
/taketattoo <tattoo>                - Remove a tattoo from yourself
/taketattoo <player> <tattoo>       - Remove a tattoo from a player
```

**Tattoo names:** `fall_rune`, `icy_devil`

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
