package com.example.redpandaapp.controller;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;

import com.example.redpandaapp.model.DiaryPost;
import com.example.redpandaapp.model.RedPanda;
import com.example.redpandaapp.repository.DiaryPostRepository;
import com.example.redpandaapp.service.ExcelImportService;

@Controller
public class RedPandaController {

    private final ExcelImportService excelImportService;
    private final DiaryPostRepository diaryRepo;
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public RedPandaController(ExcelImportService excelImportService,
                              DiaryPostRepository diaryRepo) {
        this.excelImportService = excelImportService;
        this.diaryRepo = diaryRepo;
    }

    private static final String EXCEL_URL =
        "https://ckan.odp.jig.jp/dataset/d62824ca-8b19-4d8f-b81d-7f7cc114f25d/resource/ccc95c6d-e3d0-4dd6-99fb-163704f5ab33/download/-.xlsx";

    // ---------- 一覧（カード表示・在園/過去在園を分ける + ページング + 検索） ----------
    @GetMapping("/redpandas")
    public String listRedPandas(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size, // カードなので12/ページを既定
            Model model) {

        List<RedPanda> all = excelImportService.loadRedPandas(EXCEL_URL);
        if (all == null) all = List.of();

        // 検索（名前・親・特徴・来園元）
        final String qq = (q == null) ? "" : q.trim().toLowerCase();
        List<RedPanda> filtered = (qq.isEmpty()) ? all : all.stream().filter(p ->
                contains(p.getName(), qq) || contains(p.getFather(), qq) ||
                contains(p.getMother(), qq) || contains(p.getFeature(), qq) ||
                contains(p.getOriginZoo(), qq)
        ).toList();

        // 在園 (=死亡日なし && 他園移動日なし)
        List<RedPanda> inPark = filtered.stream()
                .filter(this::isInPark)
                // 生年月日が新しい順（文字列 yyyy/MM/dd 前提：降順）
                .sorted(Comparator.comparing(RedPanda::getBirthDate, nullsLastDesc()))
                .toList();

        List<RedPanda> past = filtered.stream()
                .filter(p -> !isInPark(p))
                .sorted(Comparator.comparing(RedPanda::getBirthDate, nullsLastDesc()))
                .toList();

        // サムネURLを付与したカードに変換
        List<PandaCard> inParkCards = inPark.stream()
                .map(p -> new PandaCard(p, firstImageUrl(p.getName())))
                .toList();
        List<PandaCard> pastCards = past.stream()
                .map(p -> new PandaCard(p, firstImageUrl(p.getName())))
                .toList();

        // カードの手作りページング（在園と過去在園をそれぞれ分ける）
        var inParkPaged = paginate(inParkCards, page, size);
        var pastPaged   = paginate(pastCards, page, size);

        model.addAttribute("q", q);
        model.addAttribute("page", page);
        model.addAttribute("size", size);

        model.addAttribute("inParkTotal", inParkCards.size());
        model.addAttribute("inParkTotalPages", totalPages(inParkCards.size(), size));
        model.addAttribute("inParkCards", inParkPaged);

        model.addAttribute("pastTotal", pastCards.size());
        model.addAttribute("pastTotalPages", totalPages(pastCards.size(), size));
        model.addAttribute("pastCards", pastPaged);

        return "redpanda_cards";
    }

    // ---------- 詳細（③ 画像スライド + 投稿一覧） ----------
    @GetMapping("/redpandas/{name}")
    public String detail(@PathVariable("name") String name,
                         @RequestParam(name="page", defaultValue="0") int page,
                         @RequestParam(name="size", defaultValue="5") int size,
                         Model model) {

        var all = excelImportService.loadRedPandas(EXCEL_URL);
        var pandaOpt = (all == null ? Optional.<RedPanda>empty()
                : all.stream().filter(p -> Objects.equals(p.getName(), name)).findFirst());

        if (pandaOpt.isEmpty()) {
            model.addAttribute("error", "個体が見つかりませんでした: " + name);
            return "redpanda_detail";
        }

        RedPanda panda = pandaOpt.get();
        List<String> images = imageUrls(name);
        if (images.isEmpty()) images = List.of("/pandas/placeholder.jpg");

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<DiaryPost> posts = diaryRepo.findByPandaName(name, pageable);

        model.addAttribute("panda", panda);
        model.addAttribute("images", images);
        model.addAttribute("page", posts);
        model.addAttribute("size", size);
        return "redpanda_detail";
    }

    // ---------- helper ----------
    private boolean isInPark(RedPanda p) {
        return isBlank(p.getDeathDate()) && isBlank(p.getMovedOutDate());
    }
    private boolean contains(String s, String qLower) {
        return s != null && s.toLowerCase().contains(qLower);
    }
    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    private static Comparator<String> nullsLastDesc() {
        return Comparator.nullsLast(Comparator.<String>naturalOrder()).reversed();
    }

    public record PandaCard(RedPanda panda, String thumbUrl) {}

    private List<PandaCard> paginate(List<PandaCard> list, int page, int size) {
        if (size <= 0) size = 12;
        if (page < 0) page = 0;
        int from = Math.min(page * size, list.size());
        int to   = Math.min(from + size, list.size());
        return list.subList(from, to);
    }
    private int totalPages(int total, int size) {
        if (size <= 0) size = 12;
        return (int)Math.ceil((double)total / size);
    }
/*
    private String firstImageUrl(String name) {
        List<String> all = imageUrls(name);
        return all.isEmpty() ? "/pandas/placeholder.jpg" : all.get(0);
    }
*/
    /** /static/pandas/<name>/*.jpg を列挙して URL に変換 */
    private List<String> imageUrls(String name) {
        try {
            String encName = UriUtils.encodePathSegment(name, StandardCharsets.UTF_8);
            List<Resource> found = new ArrayList<>();

            // 1) サブフォルダ方式: /pandas/<name>/*.jpg|png
            found.addAll(Arrays.asList(resolver.getResources("classpath:/static/pandas/" + name + "/*.jpg")));
            found.addAll(Arrays.asList(resolver.getResources("classpath:/static/pandas/" + name + "/*.jpeg")));
            found.addAll(Arrays.asList(resolver.getResources("classpath:/static/pandas/" + name + "/*.png")));

            // 2) フラット方式: /pandas/<name>*.jpg|png
            found.addAll(Arrays.asList(resolver.getResources("classpath:/static/pandas/" + name + "*.jpg")));
            found.addAll(Arrays.asList(resolver.getResources("classpath:/static/pandas/" + name + "*.jpeg")));
            found.addAll(Arrays.asList(resolver.getResources("classpath:/static/pandas/" + name + "*.png")));

            return found.stream()
                    .map(r -> {
                        String fn = Objects.requireNonNull(r.getFilename());
                        String url; // 実際のURLを決める
                        try {
                            String path = r.getURL().toString();
                            if (path.contains("/pandas/" + name + "/")) {
                                // サブフォルダ方式
                                url = "/pandas/" + encName + "/" + fn;
                            } else {
                                // フラット方式
                                url = "/pandas/" + fn;
                            }
                        } catch (Exception e) {
                            url = "/pandas/" + fn;
                        }
                        return url;
                    })
                    .distinct()    // 重複除去
                    .sorted()
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private String firstImageUrl(String name) {
        List<String> all = imageUrls(name);
        return all.isEmpty() ? "/pandas/placeholder.jpg" : all.get(0);
    }
}
