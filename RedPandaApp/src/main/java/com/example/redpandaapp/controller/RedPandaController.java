package com.example.redpandaapp.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.redpandaapp.model.RedPanda;
import com.example.redpandaapp.service.ExcelImportService;

@Controller
public class RedPandaController {

    private final ExcelImportService excelImportService;

    public RedPandaController(ExcelImportService excelImportService) {
        this.excelImportService = excelImportService;
    }

    private static final String EXCEL_URL =
        "https://ckan.odp.jig.jp/dataset/d62824ca-8b19-4d8f-b81d-7f7cc114f25d/resource/ccc95c6d-e3d0-4dd6-99fb-163704f5ab33/download/-.xlsx";

    /**
     * 図鑑一覧
     */
    @GetMapping("/redpandas")
    public String listRedPandas(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Model model) {

        // すべて取得（サービス側でURL失敗時はresourcesへフォールバック）
        List<RedPanda> all = excelImportService.loadRedPandas(EXCEL_URL);

        if (all == null || all.isEmpty()) {
            model.addAttribute("error", "レッサーパンダ一覧の取得に失敗しました。");
            model.addAttribute("pandaList", List.of());
            model.addAttribute("q", q);
            model.addAttribute("page", 0);
            model.addAttribute("size", size);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalCount", 0);
            return "redpanda_list";
        }

        // 検索（名前・父親・母親・特徴・来園元園館など、必要に応じて追加）
        List<RedPanda> filtered = (q == null || q.isBlank())
                ? all
                : all.stream().filter(p ->
                        contains(p.getName(), q) ||
                        contains(p.getFather(), q) ||
                        contains(p.getMother(), q) ||
                        contains(p.getFeature(), q) ||
                        contains(p.getOriginZoo(), q)
                ).toList();

        // 手作りページング
        int total = filtered.size();
        if (size <= 0) size = 20;
        if (page < 0) page = 0;
        int from = Math.min(page * size, total);
        int to   = Math.min(from + size, total);
        List<RedPanda> content = filtered.subList(from, to);
        int totalPages = (int) Math.ceil((double) total / size);

        // 画面へ
        model.addAttribute("pandaList", content);
        model.addAttribute("q", q);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalCount", total);

        return "redpanda_list";
    }

    private boolean contains(String s, String q) {
        return s != null && q != null && s.toLowerCase().contains(q.toLowerCase());
    }
}
