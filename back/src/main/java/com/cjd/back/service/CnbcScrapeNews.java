package com.cjd.back.service;

import com.cjd.back.entity.FinancialNew;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * @Author:WalterChan
 * @Decription:CnbcScrapeNews
 * @Date Created in 2026-07-02-10:42
 */
@Service
@Slf4j
public class CnbcScrapeNews implements IScrapeNews{
    //官网
    private static final String CNBC_URL = "https://www.cnbc.com/";
    private static final String RIVER_PLUS_ID = "Home Page International-riverPlus";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

    @Override
    public List<FinancialNew> scrapeNews() {
        return getAllOriginalNews();
    }

    /**
     * 仿照浏览器获取Document
     * @param url
     * @return
     * @throws IOException
     */
    @NotNull
    private static Document getDocument(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .referrer("https://www.google.com/")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .followRedirects(true)
                .maxBodySize(0)
                .timeout(30000)
                .get();
    }

    /**
     * 获取所有未翻译的新闻
     * @return
     */
    public   List<FinancialNew> getAllOriginalNews() {
        try {

            Document doc = getDocument(CNBC_URL);

            Element riverPlus = doc.getElementById(RIVER_PLUS_ID);
            if (riverPlus == null) {
                riverPlus = doc.selectFirst("[data-test^=riverPlus]");
            }
            Assert.notNull(riverPlus,"Cannot find element: " + RIVER_PLUS_ID);


            Elements cards = riverPlus.select("div.RiverPlusCard-container");
            Assert.notEmpty(cards,"div.RiverPlusCard-container is Empty ");

            //收集主页新闻
            List<FinancialNew> newArrayList = new ArrayList<>();
            StringBuilder result = new StringBuilder();
            log.info("获取主业新闻");
            for (int i = 0; i < cards.size(); i++) {

                Element card = cards.get(i);
                Element headlineLink = findHeadlineLink(card);
                if (headlineLink == null) {
                    continue;
                }

                String title = headlineLink.text();
                String link = headlineLink.absUrl("href");
                String publishedAt = firstText(card, ".RiverByline-bylineContainer .RiverByline-datePublished");
                String authors = joinTexts(card.select(".RiverByline-bylineContainer .RiverByline-authorByline a"));
                String image = firstAttr(card, "img", "abs:src");

                FinancialNew subNew = new FinancialNew();
                subNew.setTitle(title);
                subNew.setLink(link);

                result.append(i + 1).append(". ").append(title).append(System.lineSeparator());
                appendField(result, "Link", link);
                appendField(result, "Published", publishedAt);
                appendField(result, "Authors", authors);
                appendField(result, "Image", image);
                result.append(System.lineSeparator());
                newArrayList.add(subNew);
            }
            //遍历子新闻
            log.info("获取子新闻");
            List<FinancialNew> financialNews = scrapingSubNew(newArrayList);
            return financialNews;

        } catch (IOException e) {
            throw new RuntimeException("Fail to scrape CNBC river plus news: " + e.getMessage());
        }
    }

    /**
     * 遍历新闻列表中的详情页链接，抓取每篇文章正文，并保存到 FinancialNew 的 content 字段中。
     *
     * @param newArrayList 已包含标题和详情页链接的新闻列表
     * @return 补充了正文 content 的新闻列表
     */
    public List<FinancialNew> scrapingSubNew(List<FinancialNew> newArrayList){
        for (FinancialNew financialNew : newArrayList) {
            String link = financialNew.getLink();
            if (link == null || link.isBlank()) {
                continue;
            }
            try {
                Document doc = getDocument(link);
                financialNew.setContent(parseArticleContent(doc));
            } catch (IOException e) {
                financialNew.setContent("Error scraping article content: " + e.getMessage());
            }
        }
        return newArrayList;
    }
    /**
     * 从 CNBC 文章详情页中提取 div.group 区域的正文，并转换成适合阅读的纯文本。
     *
     * @param doc Jsoup 解析后的文章详情页文档
     * @return 清洗后的正文内容
     */
    private String parseArticleContent(Document doc) {
        Elements articleGroups = doc.select("div.ArticleBody-articleBody div.group");
        if (articleGroups.isEmpty()) {
            articleGroups = doc.select("div.group:has(p)");
        }
        if (articleGroups.isEmpty()) {
            return "";
        }

        StringBuilder content = new StringBuilder();
        for (Element articleGroup : articleGroups) {
            articleGroup.select("script, style, noscript, button, svg, [data-module=mps-slot], .BoxInline-container, .MidResponsive-midResponsiveContainer").remove();
            for (Element child : articleGroup.children()) {
                appendArticleElement(content, child);
            }
        }

        return content.toString()
                .replace("\u00a0", " ")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    /**
     * 将正文中的段落和列表元素追加为纯文本，保留自然换行和项目符号。
     *
     * @param content 正文文本构建器
     * @param element 当前待处理的 HTML 元素
     */
    private void appendArticleElement(StringBuilder content, Element element) {
        if ("p".equals(element.tagName())) {
            appendParagraph(content, element.text());
            return;
        }
        if ("ul".equals(element.tagName()) || "ol".equals(element.tagName())) {
            for (Element item : element.select("> li")) {
                appendParagraph(content, "- " + item.text());
            }
            return;
        }
        for (Element child : element.children()) {
            appendArticleElement(content, child);
        }
    }

    /**
     * 向正文中追加一个段落，自动跳过空文本，并在段落之间补空行。
     *
     * @param content 正文文本构建器
     * @param text    段落文本
     */
    private void appendParagraph(StringBuilder content, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        if (!content.isEmpty()) {
            content.append(System.lineSeparator()).append(System.lineSeparator());
        }
        content.append(text.trim());
    }

    /**
     * 从单个新闻卡片中找到真正的标题链接。
     * CNBC Pro 卡片前面可能会有一个 Pro 标识链接，这里会跳过它，优先返回新闻正文链接。
     *
     * @param card 单个 River Plus 新闻卡片元素
     * @return 新闻标题对应的 a 标签；如果没有找到则返回 null
     */
    private Element findHeadlineLink(Element card) {
        Elements links = card.select(".RiverHeadline-headline > a[href]");
        for (Element link : links) {
            if (!link.hasClass("ProPill-proPillLink")) {
                return link;
            }
        }
        return links.isEmpty() ? null : links.last();
    }

    /**
     * 根据 CSS 选择器获取第一个匹配元素的文本内容。
     *
     * @param parent   查询范围的父元素
     * @param cssQuery CSS 选择器
     * @return 第一个匹配元素的文本；如果没有匹配元素则返回空字符串
     */
    private String firstText(Element parent, String cssQuery) {
        Element element = parent.selectFirst(cssQuery);
        return element == null ? "" : element.text();
    }

    /**
     * 根据 CSS 选择器获取第一个匹配元素的指定属性值。
     *
     * @param parent   查询范围的父元素
     * @param cssQuery CSS 选择器
     * @param attrName 属性名，例如 href、src、abs:src
     * @return 第一个匹配元素的属性值；如果没有匹配元素则返回空字符串
     */
    private String firstAttr(Element parent, String cssQuery, String attrName) {
        Element element = parent.selectFirst(cssQuery);
        return element == null ? "" : element.attr(attrName);
    }

    /**
     * 将多个元素的文本内容用逗号拼接起来，常用于合并多个作者名。
     *
     * @param elements Jsoup 元素集合
     * @return 逗号分隔的文本；如果集合为空则返回空字符串
     */
    private String joinTexts(Elements elements) {
        StringJoiner joiner = new StringJoiner(", ");
        for (Element element : elements) {
            String text = element.text();
            if (!text.isBlank()) {
                joiner.add(text);
            }
        }
        return joiner.toString();
    }

    /**
     * 在字段值不为空时，将字段名和字段值追加到最终输出结果中。
     *
     * @param result 最终输出内容的 StringBuilder
     * @param name   字段名，例如 Link、Published、Authors、Image
     * @param value  字段值
     */
    private void appendField(StringBuilder result, String name, String value) {
        if (value != null && !value.isBlank()) {
            result.append("   ").append(name).append(": ").append(value).append(System.lineSeparator());
        }
    }
}
