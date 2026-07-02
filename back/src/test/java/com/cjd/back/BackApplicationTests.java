package com.cjd.back;

import com.cjd.back.entity.FinancialNew;
import com.cjd.back.service.IScrapeNews;
import com.cjd.back.service.ITranslate;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class BackApplicationTests {

    @Resource
    IScrapeNews cnbcScrapeNews;

    @Resource
    ITranslate tencentTranslate;
    @Test
    void contextLoads() {

        System.out.println("Hello World");
    }

    @Test
    void testScrapeNews() {
        List<FinancialNew> financialNews = cnbcScrapeNews.scrapeNews();
        Assertions.assertNotNull(financialNews);

    }

    @Test
    void testTranslate() {
        String translate = tencentTranslate.translate("Hello How are you;I am fine thank you and you");
        Assertions.assertNotNull(translate);

    }


}
