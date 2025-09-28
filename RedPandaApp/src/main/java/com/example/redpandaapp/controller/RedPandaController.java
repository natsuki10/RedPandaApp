package com.example.redpandaapp.controller;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.asset.base:https://storage.googleapis.com/redpandaapp-202509-assets}")
    private String assetBase;
    private static final String EXCEL_URL =
        "https://ckan.odp.jig.jp/dataset/d62824ca-8b19-4d8f-b81d-7f7cc114f25d/resource/ccc95c6d-e3d0-4dd6-99fb-163704f5ab33/download/-.xlsx";

    // ===== 一覧（カード表示・在園/過去在園・検索・ページング） =====
    @GetMapping("/redpandas")
    public String listRedPandas(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size,
            Model model) {

        List<RedPanda> all = excelImportService.loadRedPandas(EXCEL_URL);
        if (all == null) all = List.of();

        final String qq = (q == null) ? "" : q.trim().toLowerCase();
        List<RedPanda> filtered = (qq.isEmpty()) ? all : all.stream().filter(p ->
                contains(p.getName(), qq) || contains(p.getFather(), qq) ||
                contains(p.getMother(), qq) || contains(p.getFeature(), qq) ||
                contains(p.getOriginZoo(), qq)
        ).toList();

        // 在園 / 過去在園 を分け、生年月日降順でソート
        List<RedPanda> inPark = filtered.stream()
                .filter(this::isInPark)
                .sorted(Comparator.comparing(RedPanda::getBirthDate, nullsLastDesc()))
                .toList();

        List<RedPanda> past = filtered.stream()
                .filter(p -> !isInPark(p))
                .sorted(Comparator.comparing(RedPanda::getBirthDate, nullsLastDesc()))
                .toList();

        // サムネ（最初の1枚）
        List<PandaCard> inParkCards = inPark.stream()
                .map(p -> new PandaCard(p, firstImageUrl(p.getName())))
                .toList();
        List<PandaCard> pastCards = past.stream()
                .map(p -> new PandaCard(p, firstImageUrl(p.getName())))
                .toList();

        // ページング
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

    // ===== 詳細（画像スライド + 投稿一覧） =====
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

    // ===== helper =====
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

    // ===== 画像URLの生成（サーバ内の列挙に頼らず、GCSの存在をHEADで確認） =====
    private List<String> imageUrls(String name) {
        String key = normalizeName(name);

        // 代表的なファイル名パターンを生成（拡張子違い・連番など）
        List<String> candidates = new ArrayList<>();
        // 例: 「カイ」「kai」/ 連番「カイ1..20」など（必要に応じて拡張）
        String original = name;
        String asciiLike = key; // 正規化後（半角・小文字・記号除去）

        // 拡張子候補
        String[] exts = new String[] { "jpg", "jpeg", "png" };

        // そのまま
        for (String ext : exts) {
            candidates.add(original + "." + ext);
        }
        // 正規化名（ASCII っぽい）
        for (String ext : exts) {
            candidates.add(asciiLike + "." + ext);
        }
        // 連番 1..20（上限は適宜調整）
        for (int i=1; i<=20; i++) {
            for (String ext : exts) {
                candidates.add(original + i + "." + ext);
                candidates.add(asciiLike + i + "." + ext);
            }
        }

        // 実在チェック（GCS に HEAD）→ あったものだけ /pandas/{filename} として返す
        List<String> urls = new ArrayList<>();
        for (String fn : candidates) {
            if (existsOnGcs(fn)) {
                urls.add("/pandas/" + fn); // 実体はリダイレクトで GCS に飛ぶ
            }
        }

        // 重複除去
        return urls.stream().distinct().toList();
    }

    private String firstImageUrl(String name) {
        List<String> all = imageUrls(name);
        return all.isEmpty() ? "/pandas/placeholder.jpg" : all.get(0);
    }

    // 名前の正規化（全角→半角/互換正規化・小文字化・空白/記号除去）
    private static final Pattern DROP = Pattern.compile("[\\p{Punct}\\p{Space}＿　・/（）()［］\\[\\]…‥‐―ー~〜・]+");
    private String normalizeName(String s) {
        if (s == null) return "";
        String nfkc = Normalizer.normalize(s, Normalizer.Form.NFKC);
        nfkc = nfkc.toLowerCase();
        nfkc = DROP.matcher(nfkc).replaceAll("");
        return nfkc;
    }

    // GCS に HEAD（公開オブジェクト前提）
    private boolean existsOnGcs(String filename) {
        try {
            String enc = URLEncoder.encode(filename, StandardCharsets.UTF_8);
            // path segment だけをエンコードしたいので、スペース等以外の %2F を戻す
            enc = enc.replace("+", "%20"); // 空白は %20
            URL url = new URL(assetBase + "/pandas/" + enc);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setInstanceFollowRedirects(true);
            con.setRequestMethod("HEAD");
            con.setConnectTimeout(2000);
            con.setReadTimeout(2000);
            int code = con.getResponseCode();
            return (code >= 200 && code < 400); // 200 or 3xx(署名URL/リダイレクト) を存在とみなす
        } catch (Exception e) {
            return false;
        }
    }
}
