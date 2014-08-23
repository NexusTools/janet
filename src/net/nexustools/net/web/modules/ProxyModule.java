///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//
//package net.nexustools.net.web.modules;
//
//import java.util.Map;
//import net.nexustools.net.web.WebClient;
//import net.nexustools.net.web.WebRequest;
//
///**
// *
// * @author kate
// */
//public class ProxyModule implements WebModule {
//    
//    private final String urlPrefix;
//    public ProxyModule(String urlPrefix) {
//        this.urlPrefix = urlPrefix;
//    }
//
//    @Override
//    public boolean handle(WebClient client, final WebRequest request) {
//        client.proxy(new WebRequest() {
//            @Override
//            public String method() {
//                return request.method();
//            }
//            @Override
//            public String path() {
//                return urlPrefix + request.path();
//            }
//            @Override
//            public Map<String, String> get() {
//                return request.get();
//            }
//            @Override
//            public Map<String, String> post() {
//                return request.post();
//            }
//            @Override
//            public String header(String key) {
//                return request.header(key);
//            }
//            @Override
//            public Iterable<String> headers(String key) {
//                return request.headers(key);
//            }
//            @Override
//            public Iterable<String> headerKeys() {
//                return request.headerKeys();
//            }
//            @Override
//            public boolean hasHeader(String key) {
//                return request.hasHeader(key);
//            }
//        });
//		return true;
//    }
//    
//}
