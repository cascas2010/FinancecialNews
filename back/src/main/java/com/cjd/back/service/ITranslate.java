package com.cjd.back.service;

/**
 * @Author:WalterChan
 * @Decription:有第三方翻译接口都需要实现这个接口
 * @Date Created in 2026-07-01-17:43
 */
public interface ITranslate {

    String translate(String source);
}
