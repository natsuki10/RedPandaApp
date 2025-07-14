package com.example.redpandaapp.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.redpandaapp.model.RedPanda;
import com.example.redpandaapp.service.ExcelImportService;

/*
 * レッサーパンダ一覧取得コントローラークラス
 */

@Controller
public class RedPandaController {

    @Autowired
    private ExcelImportService excelImportService;

    @GetMapping("/redpandas")
    public String listRedPandas(Model model) {
        String url = "https://ckan.odp.jig.jp/dataset/d62824ca-8b19-4d8f-b81d-7f7cc114f25d/resource/ccc95c6d-e3d0-4dd6-99fb-163704f5ab33/download/-.xlsx";
        List<RedPanda> pandaList = excelImportService.loadRedPandas(url);

        if (pandaList.isEmpty()) {
            model.addAttribute("error", "レッサーパンダ一覧の取得に失敗しました。");
        } else {
            model.addAttribute("pandaList", pandaList);
        }

        return "redpanda_list";
    }
}
