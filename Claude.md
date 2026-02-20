# AERB Mod Development Notes

## Workflow
- **Wait for testing before committing code**: Don't commit or push code changes until the user confirms they work in-game. Documentation/notes are fine without testing.
- **Speak up**: User sees this as a partnership. If something isn't engaging, seems like a bad idea, or you have any concerns, say so.

## Build Commands
Use forward slashes and full path for gradlew on this Windows system:
```bash
C:/Users/IRL/Documents/Claude/aerb-template-1.21.11/gradlew.bat build 2>&1
```

## Adding New Items
New items require THREE resource files (not just model + texture):
1. `assets/aerb/items/<item_name>.json` - Item definition (points to model)
2. `assets/aerb/models/item/<item_name>.json` - Model (points to texture)
3. `assets/aerb/textures/item/<item_name>.png` - Texture

The item definition format (in `items/` folder):
```json
{
  "model": {
    "type": "minecraft:model",
    "model": "aerb:item/<item_name>"
  }
}
```

## Current Status (as of last session)

### Completed Systems
- **Spell Inventory**: 27 slots + offhand, persisted via data attachments
- **Spell Restrictions**: Spells only in spell inv/hotbar/offhand/crafting; auto-return on drop
- **Skill System**: PlayerSkills attachment with server-client sync
- **UI**: Tabs (Inv/Stats/Spells) on all screens, consistent sizing/centering
- **Commands**: /setskill, /getskill with player targeting for multiplayer

### Key Files
- `spell/SpellInventory.java` - spell storage attachment
- `skill/PlayerSkills.java` - skill storage attachment
- `skill/ClientSkillCache.java` - client-side skill cache (synced from server)
- `network/ModNetworking.java` - packets for spell screen + skill sync
- `command/ModCommands.java` - /setskill, /getskill commands
- `screen/SpellSlot.java` - slot that only accepts SpellItems

### Known Issues
- Mixin for Entity.dropStack shows remap warning (doesn't affect functionality)
- Drop prevention uses ENTITY_LOAD event instead (works but brief visual flash)

## Tattoo System (WIP - on `tattoo-texture-compositing` branch)

Tattoos are composited directly onto player skin textures via mixin on `PlayerListEntry.getSkinTextures()`.

### Key Files
- `render/TattooTextureManager.java` - composites tattoos onto skins, caches results
- `mixin/client/AbstractClientPlayerEntityMixin.java` - intercepts skin texture lookup
- `textures/entity/tattoo_test.png` - test tattoo overlay (64x64, same UV as skin)
- `textures/entity/skin_mask_*.png` - masks defining exposed skin areas

### Skin Masks
Masks determine where tattoos can appear (exposed skin vs clothing):
- **Format**: 64x64 grayscale PNG, same UV layout as Minecraft skins
- **White (255)** = exposed skin, tattoos show here
- **Black (0)** = clothed/covered, tattoos hidden
- **Location**: `assets/aerb/textures/entity/skin_mask_<name>.png`

Default masks provided for Steve (wide) and Alex (slim). Custom skins need custom masks - create a mask matching your skin's clothing pattern, name it `skin_mask_<something>.png`.

### UV Layout Reference (64x64 skin)
- **Head**: front x=8-16 y=8-16, sides x=0-8 and x=16-24 y=8-16
- **Body**: front x=20-28 y=20-32, back x=32-40 y=20-32
- **Right Arm**: x=40-56 y=16-32 (wide) or x=40-54 y=16-32 (slim)
- **Left Arm**: x=32-48 y=48-64 (wide) or x=32-46 y=48-64 (slim)
- **Right Leg**: x=0-16 y=16-32
- **Left Leg**: x=16-32 y=48-64

## Origins Integration (Soft Dependency)
Origins mod provides race selection at world creation. AERB adds 6 Aerb species as a pure datapack (no Java code).
- **Files**: `data/aerb/origins/`, `data/aerb/powers/`, `data/origins/origin_layers/origin.json`
- **Races**: Human, Dwarf, Elf, Orc, Halfling, Gnome
- **Wiki reference**: `docs/aerb_mortal_species.md` (local copy of https://wiki.alexanderwales.com/wiki/Mortal_Species)
- Size scaling (dwarf/halfling/gnome) deferred until Pehkui + Tinkerers' Statures integration
- Elf carnivore diet uses `origins:prevent_item_use` with `origins:meat` tag - may need syntax testing

## Next Steps / Ideas
- Phase 4: XP system for skill progression
- Link spell availability to skill levels
- More spells
- More Origins races (Renacim, Penumbral, Animalia, etc.)
- Pehkui integration for size-varying races

## Stat Formulas
- PHY = min(POW, SPD, END) + 1
- POW: Strength, Jump Boost effects
- SPD: Speed, Haste effects
- END: Regeneration, Resistance effects

## Skills Reference (Worth the Candle)

### Currently Implemented
- **Blood Magic** - manipulate your own blood (Aarde's Touch, Crimson Fist, Sanguine Surge, Claret Spear)
- **Bone Magic** - burn bones for effects (Physical/Power/Speed/Endurance Tapping)
- **One-Handed** - sword/axe combat (Riposter virtue)
- **Parry** - deflect attacks while swinging (Prescient/Prophetic Blade virtues)
- **Horticulture** - flower magic (planned)
- **Art** - tattoo magic prerequisite (planned)
- **Skin Magic** - tattoo magic (planned)

### Magic Skills (potential future additions)
- **Gem Magic** - store spells in gems
- **Steel Magic** - metal manipulation
- **Gold Magic** - healing magic (expensive)
- **Ink Magic** - written magic
- **Water Magic** - water manipulation (Excluded)
- **Fire Magic** - fire manipulation (Excluded)
- **Air Magic** - wind/air manipulation (Excluded)
- **Earth Magic** - earth/stone manipulation (Excluded)
- **Ice Magic** - cold/ice manipulation (Excluded)
- **Vibration Magic** - sound/vibration (Excluded)
- **Passion Magic** - emotion manipulation
- **Spirit** - soul manipulation
- **Essentialism** - soul/skill manipulation
- **Revision** - alter recent past
- **Necromancy** - raise undead
- **Pustulance** - disease magic
- **Velocity** - speed manipulation
- **Warding** - protective barriers
- **Still Magic** - entropy manipulation
- **Library Magic** - book-based magic
- **Luck Magic** - probability manipulation

### Combat Skills (potential)
- **Two-Handed** - greatswords, battleaxes
- **Bows** - ranged combat
- **Thrown Weapons** - throwing knives, etc.
- **Unarmed** - hand-to-hand
- **Shields** - blocking
- **Dodge** - evasion
- **Dual Wield** - two weapons

### Other Skills (potential)
- **Athletics** - running, jumping, climbing
- **Stealth** - sneaking
- **Engineering** - crafting/machines
- **Alchemy** - potions
- **Smithing** - metalwork
- **Woodworking** - carpentry
- **Cooking** - food preparation
- **Medicine** - healing without magic
- **Survival** - wilderness skills
- **Rhetoric** - persuasion
- **Intimidation** - fear tactics
- **Languages** - communication
- **Legerdemain** - sleight of hand
