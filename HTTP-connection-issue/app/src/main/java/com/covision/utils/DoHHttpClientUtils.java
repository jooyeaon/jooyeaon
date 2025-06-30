package com.covision.utils;

import android.content.Context;

import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.ConnectionPool;
import okhttp3.Dns;
import okhttp3.EventListener;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.dnsoverhttps.DnsOverHttps;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DoHHttpClientUtils {
    private static final String TAG = "DoHHttpClient";

    private static final String DOH_URL = "https://dns.google/dns-query";
    private static final String BOOTSTRAP_IP = "8.8.8.8";

    private static volatile OkHttpClient instance;

    private DoHHttpClientUtils() {
    }

    public static OkHttpClient getInstance() {
        if (instance == null) {
            synchronized (DoHHttpClientUtils.class) {
                if (instance == null) {
                    instance = buildClient();
                }
            }
        }
        return instance;
    }

    private static OkHttpClient buildClient() {
        OkHttpClient bootstrapClient = new OkHttpClient();

        DnsOverHttps dohDns = null;
        try {
            dohDns = new DnsOverHttps.Builder()
                    .client(bootstrapClient)
                    .url(HttpUrl.get(DOH_URL))
                    .bootstrapDnsHosts(Collections.singletonList(InetAddress.getByName(BOOTSTRAP_IP)))
                    .includeIPv6(false)
                    .build();
        } catch (UnknownHostException e) {
            MOUtils.WriteLog(TAG, "DNS 설정 실패, 기본 DNS로 fallback: " + e.getMessage());
        }

        return new OkHttpClient.Builder()
                .dns(dohDns != null ? dohDns : Dns.SYSTEM)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .connectionPool(new ConnectionPool(20, 10, TimeUnit.MINUTES))
                .eventListener(new EventListener() {
                    @Override
                    public void connectionAcquired(Call call, Connection connection) {
                        MOUtils.WriteLog(TAG, "Connection 재사용됨: " + connection.toString());
                    }

                    @Override
                    public void connectionReleased(Call call, Connection connection) {
                        MOUtils.WriteLog(TAG, "Connection 해제됨: " + connection.toString());
                    }

                    @Override
                    public void callStart(Call call) {
                        MOUtils.WriteLog(TAG, "요청 시작: " + call.request().url());
                    }

                    @Override
                    public void callEnd(Call call) {
                        MOUtils.WriteLog(TAG, "요청 종료");
                    }

                    @Override public void dnsStart(Call call, String domainName) {
                        MOUtils.WriteLog(TAG, "DNS 시작: " + domainName);
                    }

                    @Override public void dnsEnd(Call call, String domainName, List<InetAddress> inetAddressList) {
                        MOUtils.WriteLog(TAG, "DNS 완료");
                    }

                    @Override public void connectStart(Call call, InetSocketAddress inetSocketAddress, Proxy proxy) {
                        MOUtils.WriteLog(TAG, "연결 시작");
                    }

                    @Override public void secureConnectStart(Call call) {
                        MOUtils.WriteLog(TAG, "TLS 핸드셰이크 시작");
                    }

                    @Override public void responseHeadersEnd(Call call, Response response) {
                        MOUtils.WriteLog(TAG, "응답 헤더 수신 완료");
                    }
                })
                .build();
    }

}
