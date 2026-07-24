# farm-manager-ai - Rulebook & Memory Constitution

## Build and Test Commands
* Build Backend: `mvn clean package -f backend/pom.xml`
* Test Backend: `mvn test -f backend/pom.xml`
* Build Frontend: `npm run build --prefix frontend`
* Test Frontend: `CI=true npm test --prefix frontend`

## Coding Guidelines
* Backend: Java Spring Boot REST API, JdbcTemplate database integration.
* Frontend: React SPA, modular components, clean styling.
* Codebase Knowledge Graph: This project uses Graphify. Use the `query_graph` or `shortest_path` MCP tools (or CLI `python -m graphify query "<question>"`) to navigate codebase structure.

## Obsidian & MemPalace Memory Protocol ("New Meta")
This project is connected to a local Obsidian memory vault and MemPalace vector database to prevent agent amnesia:
* Vault Path: `C:/Users/VEDANTH/HermesMemory`
* Rules:
  1. **Read Active Context**: At the start of any session, check `C:/Users/VEDANTH/HermesMemory/identity.md` and `C:/Users/VEDANTH/HermesMemory/essential_story.md` to load recent learnings, tasks, and context.
  2. **Query Vector Archives**: Use the `mempalace_search` tool (or CLI `mempalace search "<query>"`) to query past conversations and research files.
  3. **Update Memory Palace**: Upon completing a task, update `essential_story.md` in the vault, and call `mempalace_add_drawer` to file new technical learnings under wing `farm-manager-ai` (or use CLI `mempalace mine`).
  4. **Visual Code Navigation**: Consult `graphify-out/GRAPH_REPORT.md` or use the Graphify tools.
