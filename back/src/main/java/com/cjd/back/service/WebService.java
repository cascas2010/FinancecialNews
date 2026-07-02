package com.cjd.back.service;

import com.cjd.back.entity.FinancialNew;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author:WalterChan
 * @Decription:WebService
 * @Date Created in 2026-07-02-11:08
 */
@Service
public class WebService {

    @Resource
    IScrapeNews cnbcScrapeNews;

    @Resource
    ITranslate tencentTranslate;

    public List<FinancialNew> showNews(){
        List<FinancialNew> financialNews = cnbcScrapeNews.scrapeNews();
        List<FinancialNew>  translatedNews = financialNews.stream()
                .map(o -> {
                    String content = o.getContent();
                    String transferred = tencentTranslate.translate(content);
                    o.setContent(transferred);
                    return o;
                })
                .collect(Collectors.toList());
        return translatedNews;

    }
}
