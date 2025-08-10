package com.example.redpandaapp.controller;

import java.util.List;

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

    @GetMapping
    public String list(Model model) {
        model.addAttribute("posts", repo.findAll());
        return "post_list";
    }

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

    @PostMapping
    public String create(@ModelAttribute("post") DiaryPost post) {
        repo.save(post);
        return "redirect:/posts";
    }
}
