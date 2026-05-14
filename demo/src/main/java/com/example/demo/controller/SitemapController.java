package com.example.demo.controller;

import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import java.util.List;
import com.example.demo.dto.SitemapProblemDto;
import com.example.demo.repository.ProblemRepository;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/sitemap")
@RequiredArgsConstructor
public class SitemapController {

    private final ProblemRepository problemRepository;

    @GetMapping("/problems")
    public List<SitemapProblemDto> getProblems() {
        return problemRepository.findForSitemap();
    }
}