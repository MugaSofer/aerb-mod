# AERB Mod Development Notes

## Current Status (as of last session)

### Completed Systems
- **Spell Inventory**: 18 slots + offhand, persisted via data attachments
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
- Offhand slot in Spell Inventory is currently non-functional
- "Spells Known" title not showing on spell screen (check titleX/titleY positioning)
- Offhand slot needs a label (currently "Off" label may be mispositioned)

## Next Steps / Ideas
- Phase 4: XP system for skill progression
- Link spell availability to skill levels
- More spells
- Scrollable skills list if many skills added

## Stat Formulas
- PHY = min(POW, SPD, END) + 1
- POW: Strength, Jump Boost effects
- SPD: Speed, Haste effects
- END: Regeneration, Resistance effects
