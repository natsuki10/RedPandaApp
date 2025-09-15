package com.example.redpandaapp.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.redpandaapp.model.DiaryPost;
import com.example.redpandaapp.model.RedPanda;
import com.example.redpandaapp.repository.DiaryPostRepository;
import com.example.redpandaapp.service.ExcelImportService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/posts")
public class DiaryController {

    private final DiaryPostRepository repo;
    private final ExcelImportService excel;

    public DiaryController(DiaryPostRepository repo, ExcelImportService excel) {
        this.repo = repo;
        this.excel = excel;
    }

    private static final String EXCEL_URL =
            "https://ckan.odp.jig.jp/dataset/d62824ca-8b19-4d8f-b81d-7f7cc114f25d/resource/ccc95c6d-e3d0-4dd6-99fb-163704f5ab33/download/-.xlsx";

    /** 一覧（ページング＆検索／個体名フィルタ） */
    @GetMapping
    public String list(
            @RequestParam(name = "pandaName", required = false) String pandaName, // 図鑑からの完全一致フィルタ
            @RequestParam(name = "q",         required = false) String q,         // 部分一致検索
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<DiaryPost> result;

        if (pandaName != null && !pandaName.isBlank()) {
            // 図鑑の「個体名リンク」から来たときはこちらを優先（完全一致）
            result = repo.findByPandaName(pandaName, pageable);
        } else if (q != null && !q.isBlank()) {
            // 検索（部分一致・大文字小文字無視）
            result = repo.findByPandaNameContainingIgnoreCase(q.trim(), pageable);
        } else {
            // 全件
            result = repo.findAll(pageable);
        }

        model.addAttribute("page", result);
        model.addAttribute("pandaName", pandaName);
        model.addAttribute("q", q);
        model.addAttribute("size", size);
        return "post_list";
    }

    /** 新規投稿フォーム */
    @GetMapping("/new")
    public String showForm(@RequestParam(name = "pandaName", required = false) String pandaName,
                           Model model) {
        List<RedPanda> postables = excel.loadPostableRedPandas(EXCEL_URL);
        model.addAttribute("postables", postables);

        DiaryPost post = new DiaryPost();
        if (pandaName != null && !pandaName.isBlank()) post.setPandaName(pandaName);
        model.addAttribute("post", post);

        return "post_form";
    }

    /** 投稿登録（画像アップロード対応） */
    @PostMapping
    public String create(
            @Valid @ModelAttribute("post") DiaryPost post,
            org.springframework.validation.BindingResult binding,
            @RequestParam(value = "image", required = false) org.springframework.web.multipart.MultipartFile image,
            @org.springframework.beans.factory.annotation.Value("${app.upload-dir}") String uploadDir,
            Model model) throws Exception {

        // 画面に戻るときに必要
        if (binding.hasErrors()) {
            model.addAttribute("postables", excel.loadPostableRedPandas(EXCEL_URL));
            return "post_form";
        }

        // 写真が必須
        if (image == null || image.isEmpty()) {
            binding.rejectValue("imageFilename", "image.required", "写真は必須です。");
            model.addAttribute("postables", excel.loadPostableRedPandas(EXCEL_URL));
            return "post_form";
        }

        if (image != null && !image.isEmpty()) {
            String ct = image.getContentType();
            if (ct == null || !ct.startsWith("image/")) {
                throw new IllegalArgumentException("画像ファイルのみアップロード可能です。");
            }
            String ext = org.springframework.util.StringUtils.getFilenameExtension(image.getOriginalFilename());
            String safeName = java.util.UUID.randomUUID().toString().replace("-", "");
            String filename = (ext == null || ext.isBlank()) ? safeName : safeName + "." + ext;

            java.nio.file.Path dir = java.nio.file.Paths.get(uploadDir).toAbsolutePath().normalize();
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Path dest = dir.resolve(filename);

            image.transferTo(dest.toFile());
            post.setImageFilename(filename);
        }

        repo.save(post);
        return "redirect:/posts";
    }
}
