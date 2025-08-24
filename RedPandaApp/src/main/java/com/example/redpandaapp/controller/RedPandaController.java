package com.example.redpandaapp.controller;

import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.redpandaapp.model.DiaryPost;
import com.example.redpandaapp.model.RedPanda;
import com.example.redpandaapp.repository.DiaryPostRepository;
import com.example.redpandaapp.service.ExcelImportService;

@Controller
public class RedPandaController {

    private final ExcelImportService excelImportService;
    private final DiaryPostRepository diaryRepo;

    public RedPandaController(ExcelImportService excelImportService,
                              DiaryPostRepository diaryRepo) {
        this.excelImportService = excelImportService;
        this.diaryRepo = diaryRepo;
    }

    private static final String EXCEL_URL =
        "https://ckan.odp.jig.jp/dataset/d62824ca-8b19-4d8f-b81d-7f7cc114f25d/resource/ccc95c6d-e3d0-4dd6-99fb-163704f5ab33/download/-.xlsx";

    /** 図鑑一覧（検索＋ページング＋在園個体を上に） */
    @GetMapping("/redpandas")
    public String listRedPandas(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Model model) {

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

        // 検索
        List<RedPanda> filtered = (q == null || q.isBlank())
                ? all
                : all.stream().filter(p ->
                        contains(p.getName(), q) ||
                        contains(p.getFather(), q) ||
                        contains(p.getMother(), q) ||
                        contains(p.getFeature(), q) ||
                        contains(p.getOriginZoo(), q)
                ).toList();

        // 在園 (= 死亡日が空 && 他園移動日が空) を上、在園でない個体は生年月日が新しい順
        var inPark = filtered.stream()
                .filter(p -> isBlank(p.getDeathDate()) && isBlank(p.getMovedOutDate()))
                .sorted(Comparator.comparing(RedPanda::getBirthDate, nullsLastDesc())) 
                .toList();

        var notInPark = filtered.stream()
                .filter(p -> !(isBlank(p.getDeathDate()) && isBlank(p.getMovedOutDate())))
                .sorted(Comparator.comparing(RedPanda::getBirthDate, nullsLastDesc()))
                .toList();

        var ordered = new java.util.ArrayList<RedPanda>(inPark.size() + notInPark.size());
        ordered.addAll(inPark);
        ordered.addAll(notInPark);

        // 手作りページング
        int total = ordered.size();
        if (size <= 0) size = 20;
        if (page < 0) page = 0;
        int from = Math.min(page * size, total);
        int to   = Math.min(from + size, total);
        List<RedPanda> content = ordered.subList(from, to);
        int totalPages = (int) Math.ceil((double) total / size);

        model.addAttribute("pandaList", content);
        model.addAttribute("q", q);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalCount", total);

        return "redpanda_list";
    }

    /** 図鑑詳細（全項目＋その個体の投稿一覧） */
    @GetMapping("/redpandas/{name}")
    public String detail(@PathVariable("name") String name,
                         @RequestParam(name="page", defaultValue="0") int page,
                         @RequestParam(name="size", defaultValue="5") int size,
                         Model model) {

        // Excelから該当個体
        var all = excelImportService.loadRedPandas(EXCEL_URL);
        var maybe = all.stream()
                .filter(p -> p.getName() != null && p.getName().equals(name))
                .findFirst();

        if (maybe.isEmpty()) {
            model.addAttribute("error", "個体が見つかりませんでした: " + name);
            return "redpanda_detail";
        }

        model.addAttribute("panda", maybe.get());

        // 投稿をページング表示
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<DiaryPost> posts = diaryRepo.findByPandaName(name, pageable);
        model.addAttribute("page", posts);
        model.addAttribute("size", size);

        return "redpanda_detail";
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private boolean contains(String s, String q) {
        return s != null && q != null && s.toLowerCase().contains(q.toLowerCase());
    }
    private static Comparator<String> nullsLastDesc() {
        // "YYYY/MM/DD" 文字列 or null を想定。文字列の降順（新しい日付ほど大きい前提）
        return Comparator.nullsLast(Comparator.<String>naturalOrder()).reversed();
    }
}
