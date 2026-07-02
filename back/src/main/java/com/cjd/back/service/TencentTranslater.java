package com.cjd.back.service;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.tmt.v20180321.TmtClient;
import com.tencentcloudapi.tmt.v20180321.models.TextTranslateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author:WalterChan
 * @Decription:TencentTranslater
 * @Date Created in 2026-07-01-17:44
 */
@Service
@Slf4j
public class TencentTranslater implements ITranslate{
    //腾讯翻译一次性最大翻译字符
    private static final int MAX_TRANSLATE_TEXT_LENGTH = 6000;

    private TmtClient tmtClient;

    public TencentTranslater(
            @Value("${tencet.translate.secret-id}")
            String secretId,
            @Value("${tencet.translate.secret-key}")
            String secretKey,
            @Value("${tencet.translate.region}")
            String region
    ) {
        Credential credential = new Credential(secretId, secretKey);
        this.tmtClient = new TmtClient(credential, region);
    }


    @Override
    public String translate(String source) {
        return transferBySegments(source);
    }

    /**
     * Split long text into multiple TextTranslateRequest calls and merge translated chunks.
     *
     * @param text source text
     * @return translated full text
     */
    public String transferBySegments(String text) {
        log.info("腾讯翻译开始");
        if (text == null || text.isBlank()) {
            return text;
        }
        if ( text.length() <= MAX_TRANSLATE_TEXT_LENGTH) {
            return translateOnce(text);
        }

        StringBuilder translatedText = new StringBuilder();
        for (String segment : splitText(text)) {
            translatedText.append(translateOnce(segment));
        }
        return translatedText.toString();
    }

    static List<String> splitText(String text) {
        List<String> segments = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int maxEnd = Math.min(start + MAX_TRANSLATE_TEXT_LENGTH, text.length());
            int end = findSplitEnd(text, start, maxEnd);
            segments.add(text.substring(start, end));
            start = end;
        }
        return segments;
    }

    private static int findSplitEnd(String text, int start, int maxEnd) {
        if (maxEnd == text.length()) {
            return maxEnd;
        }

        for (int i = maxEnd - 1; i > start; i--) {
            char current = text.charAt(i);
            if (isSentenceBoundary(current)) {
                return i + 1;
            }
        }

        for (int i = maxEnd - 1; i > start; i--) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i + 1;
            }
        }

        if (Character.isHighSurrogate(text.charAt(maxEnd - 1))) {
            return maxEnd - 1;
        }
        return maxEnd;
    }

    private static boolean isSentenceBoundary(char current) {
        return current == '.'
                || current == '?'
                || current == '!'
                || current == ';'
                || current == '\n'
                || current == '\r';
    }

    private String translateOnce(String text)  {
        TextTranslateRequest translateRequest = new TextTranslateRequest();
        translateRequest.setSourceText(text);
        translateRequest.setSource("en");
        translateRequest.setTarget("zh");
        translateRequest.setProjectId(0L);
        try {
            String targetText = tmtClient.TextTranslate(translateRequest).getTargetText();
            //腾讯云限制每秒 5 次
            Thread.sleep(250);
            return targetText;
        } catch (TencentCloudSDKException | InterruptedException e) {
            log.error("调用腾讯云翻译失败，textLength={}", text.length(), e);
            throw new RuntimeException("腾讯翻译异常"+e.getMessage());
        }
    }
}
