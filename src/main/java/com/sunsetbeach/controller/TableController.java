package com.sunsetbeach.controller;

import com.sunsetbeach.api.TablesApi;
import com.sunsetbeach.model.OkTrue;
import com.sunsetbeach.model.Table;
import com.sunsetbeach.model.TableInput;
import com.sunsetbeach.service.TableService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TableController implements TablesApi {

    private final TableService tableService;

    public TableController(TableService tableService) {
        this.tableService = tableService;
    }

    @Override
    public ResponseEntity<List<Table>> listTables() {
        return ResponseEntity.ok(tableService.list());
    }

    @Override
    public ResponseEntity<Table> createTable(TableInput tableInput) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tableService.create(tableInput));
    }

    @Override
    public ResponseEntity<Table> updateTable(String id, TableInput tableInput) {
        return ResponseEntity.ok(tableService.update(id, tableInput));
    }

    @Override
    public ResponseEntity<OkTrue> deleteTable(String id) {
        tableService.delete(id);
        return ResponseEntity.ok(new OkTrue(true));
    }
}
