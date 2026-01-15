# AERB Mod Development Notes

## Character Sheet Plan

### Phase 1 - Basic Display (DONE)
- CharacterSheetScreen with Stats button in inventory
- PHY = min(POW, SPD, END) + 1
- Base stats: POW 2, SPD 2, END 2
- Skills section: Blood Magic, Bone Magic (hardcoded 0)

### Phase 2 - Dynamic Stats (DONE)
- Uses calculatePOW/SPD/END methods with TextWidget.setMessage() for updates
- Status effect mappings:
  - POW: Strength, Jump Boost
  - SPD: Speed, Haste
  - END: Regeneration, Resistance
- Bonuses shown in green (e.g. "POW: 2 (+2)")

### Phase 3 - Persistent Skill Data
- Store skill levels per player
- Save/load with player data

### Phase 4 - Level Up Mechanism
- XP system for skills
- Skill progression

## Spell Slots System (WIP)

### Done
- SpellSlots data attachment class (src/main/java/mugasofer/aerb/spell/SpellSlots.java)
- Visual 3-slot display in character sheet
- Tab system to separate Stats page from Spells page

### TODO
- Click-to-equip functionality
- Client/server data sync for persistence

### UI Improvements Needed
- Scrollable skills list (for when there are many skills)
- NOTE: EntryListWidget doesn't work because its entries use drawTextWithShadow which doesn't render in this screen
- Need alternative approach: either custom scroll logic with TextWidgets, or figure out why direct drawing fails

## Stat Formulas
- PHY = min(POW, SPD, END) + 1
- Each status effect adds (amplifier + 1) to its stat
