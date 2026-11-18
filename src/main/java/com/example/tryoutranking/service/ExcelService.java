package com.example.tryoutranking.service;

import com.example.tryoutranking.model.Player;
import com.example.tryoutranking.model.PlayerStatus;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class ExcelService {
    public static final List<String> HEADERS = List.of(
            "Rank", "First Name", "Last Name", "Position", "Phone", "Email", "DOB", "Status", "Notes"
    );

    public void save(Path file, List<Player> players) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Tryout Ranking");
            CreationHelper helper = workbook.getCreationHelper();

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setDataFormat(helper.createDataFormat().getFormat("@"));

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(helper.createDataFormat().getFormat("dd/mm/yyyy"));

            CellStyle notesStyle = workbook.createCellStyle();
            notesStyle.cloneStyleFrom(textStyle);
            notesStyle.setWrapText(true);
            notesStyle.setVerticalAlignment(VerticalAlignment.TOP);

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.size(); i++) {
                Cell cell = header.createCell(i, CellType.STRING);
                cell.setCellValue(HEADERS.get(i));
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < players.size(); i++) {
                Player player = players.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0, CellType.NUMERIC).setCellValue(i + 1);
                setText(row, 1, player.getFirstName(), textStyle);
                setText(row, 2, player.getLastName(), textStyle);
                setText(row, 3, player.getPosition(), textStyle);
                setText(row, 4, player.getPhone(), textStyle);
                setText(row, 5, player.getEmail(), textStyle);

                Cell dob = row.createCell(6);
                if (player.getDateOfBirth() != null) {
                    dob.setCellValue(java.sql.Date.valueOf(player.getDateOfBirth()));
                    dob.setCellStyle(dateStyle);
                }

                setText(row, 7, player.getStatus().toString(), textStyle);
                setText(row, 8, player.getNotes(), notesStyle);
            }

            sheet.createFreezePane(0, 1);
            if (!players.isEmpty()) {
                sheet.setAutoFilter(new CellRangeAddress(0, players.size(), 0, HEADERS.size() - 1));
            } else {
                sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, HEADERS.size() - 1));
            }

            int[] widths = {8, 18, 18, 14, 18, 30, 14, 16, 48};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            try (OutputStream out = Files.newOutputStream(file)) {
                workbook.write(out);
            }
        }
    }

    public List<Player> load(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file); Workbook workbook = WorkbookFactory.create(in)) {
            if (workbook.getNumberOfSheets() == 0) return List.of();
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) return List.of();
            Map<String, Integer> columns = mapColumns(header, formatter);

            requireColumn(columns, "firstname", "First Name");
            requireColumn(columns, "lastname", "Last Name");

            List<Player> players = new ArrayList<>();
            for (int r = header.getRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || rowIsBlank(row, formatter)) continue;

                Player player = new Player();
                player.setFirstName(text(row, columns.get("firstname"), formatter));
                player.setLastName(text(row, columns.get("lastname"), formatter));
                player.setPosition(text(row, columns.get("position"), formatter));
                player.setPhone(text(row, columns.get("phone"), formatter));
                player.setEmail(text(row, columns.get("email"), formatter));
                player.setDateOfBirth(readDate(row, columns.get("dob"), formatter));
                player.setStatus(PlayerStatus.fromString(text(row, columns.get("status"), formatter)));
                player.setNotes(text(row, columns.get("notes"), formatter));

                if (!player.getFirstName().isBlank() || !player.getLastName().isBlank()
                        || !player.getPhone().isBlank() || !player.getEmail().isBlank()) {
                    players.add(player);
                }
            }
            return players;
        }
    }

    private static void setText(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column, CellType.STRING);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private static Map<String, Integer> mapColumns(Row header, DataFormatter formatter) {
        Map<String, Integer> columns = new HashMap<>();
        for (Cell cell : header) {
            String key = normalize(formatter.formatCellValue(cell));
            if (!key.isBlank()) columns.put(key, cell.getColumnIndex());
        }
        // Friendly aliases for older/manual workbooks.
        alias(columns, "firstname", "first");
        alias(columns, "lastname", "last");
        alias(columns, "position", "pos");
        alias(columns, "dob", "dateofbirth");
        return columns;
    }

    private static void alias(Map<String, Integer> columns, String canonical, String alias) {
        if (!columns.containsKey(canonical) && columns.containsKey(alias)) {
            columns.put(canonical, columns.get(alias));
        }
    }

    private static void requireColumn(Map<String, Integer> columns, String key, String display) throws IOException {
        if (!columns.containsKey(key)) {
            throw new IOException("The workbook is missing the required '" + display + "' column.");
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static String text(Row row, Integer index, DataFormatter formatter) {
        if (index == null) return "";
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private static LocalDate readDate(Row row, Integer index, DataFormatter formatter) {
        if (index == null) return null;
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;

        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }

        String raw = formatter.formatCellValue(cell).trim();
        if (raw.isBlank()) return null;
        for (DateTimeFormatter fmt : List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("d/M/uuuu"),
                DateTimeFormatter.ofPattern("d-M-uuuu")
        )) {
            try {
                return LocalDate.parse(raw, fmt);
            } catch (DateTimeParseException ignored) { }
        }
        return null;
    }

    private static boolean rowIsBlank(Row row, DataFormatter formatter) {
        for (Cell cell : row) {
            if (!formatter.formatCellValue(cell).isBlank()) return false;
        }
        return true;
    }
}
