# Tryout Ranking

Built for the King's College London Squash Club and used by the club captains to rank players and pick teams at the 2026/27 tryouts.

A lightweight JavaFX desktop application for manually ranking sports-club tryout players. The ranking lives entirely in memory while the app is open; Excel `.xlsx` workbooks are the save/open format. There is no database, SQL, JDBC, or Spring.

## Features

- Excel-like `TableView<Player>` with compact rows and resizable columns
- Rank derived from the authoritative master `ObservableList<Player>`
- Drag-and-drop row reordering with insertion-line feedback
- Move Up / Down / Top / Bottom and direct move-to-rank
- Add, edit, delete, and inline edit common fields
- Status ComboBox cell (`Unreviewed`, `Selected`, `Maybe`, `Reserve`, `Rejected`, `Withdrawn`)
- Search without changing underlying ranking or rank numbers
- Excel `.xlsx` save/load using Apache POI
- Phone numbers written as text so leading zeros are preserved
- Bold/frozen/filterable Excel header, sensible widths, wrapped notes
- New/Open/Close unsaved-change protection
- Undo/redo for ranking, add, delete, edit, and status changes
- Modified marker (`*`) in the title bar
- Keyboard shortcuts for ranking and undo/redo

## Requirements

- JDK 21 or later
- Nothing else: the Maven Wrapper (`mvnw`) downloads the right Maven version for you, and Maven fetches JavaFX and the other libraries

## Run

macOS / Linux:

```bash
./mvnw clean test     # run the tests
./mvnw javafx:run     # start the app
```

Windows:

```bat
mvnw.cmd clean test
mvnw.cmd javafx:run
```

If you already have Maven installed, `mvn` works in place of `./mvnw`.

In IntelliJ IDEA, open the folder (it is detected as a Maven project), set the Project SDK to JDK 21+, then run the Maven goal `javafx:run`.

## Keyboard shortcuts

- `Alt + Up` — move selected player up
- `Alt + Down` — move selected player down
- `Ctrl/Cmd + Shift + Up` — move to top
- `Ctrl/Cmd + Shift + Down` — move to bottom
- `Ctrl/Cmd + Z` — undo
- `Ctrl/Cmd + Y` — redo
- `Delete` — delete selected player

## Project layout

```text
src/main/java/com/example/tryoutranking/
├── Main.java
├── controller/
│   ├── MainController.java
│   └── PlayerDialogController.java
├── model/
│   ├── Player.java
│   └── PlayerStatus.java
├── service/
│   ├── ExcelService.java
│   ├── PlayerListStateCommand.java
│   ├── RankingService.java
│   └── UndoManager.java
└── util/
    └── ValidationUtil.java

src/main/resources/
├── css/application.css
└── fxml/
    ├── main.fxml
    └── player-dialog.fxml

src/test/java/com/example/tryoutranking/service/
├── ExcelServiceTest.java
└── RankingServiceTest.java
```

## Ranking design

`Player` deliberately has no persistent rank property. Rank is always `masterList.indexOf(player) + 1`. The search table uses a `FilteredList`, but the master list remains authoritative. The Rank column therefore continues to show the true ranking even while only a subset is visible.

## Excel format

The first worksheet uses these columns:

`Rank | First Name | Last Name | Position | Phone | Email | DOB | Status | Notes`

When opening a workbook, physical row order is authoritative. The Rank column is informational and is not used to reorder imported rows.
