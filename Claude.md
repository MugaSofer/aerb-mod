# AERB Mod Development Notes

## Character Sheet Plan

### Phase 1 - Basic Display (DONE)
- CharacterSheetScreen with Stats button in inventory
- PHY = min(POW, SPD, END) + 1
- Base stats: POW 2, SPD 2, END 2
- Skills section: Blood Magic, Bone Magic (hardcoded 0)

### Phase 2 - Dynamic Stats
- Use calculatePOW/SPD/END methods (already exist in code)
- Status effect mappings:
  - POW: Strength, Jump Boost
  - SPD: Speed, Haste
  - END: Regeneration, Resistance
- Show bonuses in green when active

### Phase 3 - Persistent Skill Data
- Store skill levels per player
- Save/load with player data

### Phase 4 - Level Up Mechanism
- XP system for skills
- Skill progression

### UI Improvements Needed
- Scrollable skills list using EntryListWidget (for when there are many skills)
- Currently uses TextWidget which doesn't scroll

## Stat Formulas
- PHY = min(POW, SPD, END) + 1
- Each status effect adds (amplifier + 1) to its stat
