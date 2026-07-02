package com.cjd.back.service;

import com.cjd.back.entity.FinancialNew;

import java.util.List;

/**
 * @Author:WalterChan
 * @Decription:抓取新闻接口
 * @Date Created in 2026-07-02-10:40
 */

public interface IScrapeNews {
    List<FinancialNew> scrapeNews();
}
