# AERB Mod Development Notes

## Workflow
- **Wait for testing before committing code**: Don't commit or push code changes until the user confirms they work in-game. Documentation/notes are fine without testing.
- **Speak up**: User sees this as a partnership. If something isn't engaging, seems like a bad idea, or you have any concerns, say so.

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
