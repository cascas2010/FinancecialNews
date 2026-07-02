package com.cjd.back.controller;

import com.cjd.back.entity.FinancialNew;
import com.cjd.back.service.WebService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Author:WalterChan
 * @Decription:WebNewsController
 * @Date Created in 2026-07-02-11:35
 */

@RestController
public class WebNewsController {

    @Resource
    WebService webService;
    @RequestMapping("/show")
    public List<FinancialNew> showNews(){
       return webService.showNews();
    }
}
