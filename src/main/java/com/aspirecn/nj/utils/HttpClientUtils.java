package com.aspirecn.nj.utils;

import java.io.*;
import java.net.URI;
import java.security.*;
import java.util.*;
import java.util.Map.Entry;
import org.apache.commons.codec.Charsets;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.*;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.*;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.*;
import org.apache.hc.client5.http.ssl.*;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.config.*;
import org.apache.hc.core5.http.io.*;
import org.apache.hc.core5.http.io.entity.*;
import org.apache.hc.core5.http.message.*;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.*;
import org.slf4j.*;

;

/**
 * Description: httpClient工具类
 *
 * @author housy
 * @date Created on 2021年6月02日
 */
public class HttpClientUtils {
    private static final Logger LOG = LoggerFactory.getLogger(HttpClientUtils.class);

    // 编码格式。发送编码格式统一用UTF-8
    private static final String ENCODING = "UTF-8";

    // 设置连接超时时间，单位毫秒。
    private static final int CONNECT_TIMEOUT = 30000;

    // 请求获取数据的超时时间(即响应时间)，单位毫秒。
    private static final int SOCKET_TIMEOUT = 30000;

    protected static PoolingHttpClientConnectionManager connManager = null;

    protected static CloseableHttpClient httpPoolClient = null;

    protected static ConnectionKeepAliveStrategy myStrategy = new ConnectionKeepAliveStrategy() {

        @Override
        public TimeValue getKeepAliveDuration(HttpResponse response, HttpContext context) {
            // Honor 'keep-alive' header
            BasicHeaderElementIterator it = new BasicHeaderElementIterator(
                    response.headerIterator(HttpHeaders.KEEP_ALIVE));
            while (it.hasNext()) {
                HeaderElement he = it.next();
                String param = he.getName();
                String value = he.getValue();
                if (value != null && "timeout".equalsIgnoreCase(param)) {
                    try {
                        return TimeValue.ofMilliseconds(Long.parseLong(value) * 1000);
                    } catch(NumberFormatException ignore) {
                    }
                }
            }
            return TimeValue.ofMilliseconds(30 * 1000L);
        }

    };


    static {
        SSLConnectionSocketFactory scsf = null;
        try {
            scsf = new SSLConnectionSocketFactory(
                    SSLContexts.custom().loadTrustMaterial(new TrustSelfSignedStrategy()).build(),
                    NoopHostnameVerifier.INSTANCE);
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            LOG.error("创建SSL连接失败");
        }
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("https", scsf)
                .register("http", new PlainConnectionSocketFactory())
                .build();

        connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);

        // 将最大连接数增加到100
        connManager.setMaxTotal(100);
        // 将每个路由基础的连接增加到20
        connManager.setDefaultMaxPerRoute(20);

        connManager.setDefaultSocketConfig(SocketConfig.custom()
                .setSoTimeout(Timeout.ofMilliseconds(5000))
                .build());

        try {
            httpPoolClient = HttpClients.custom()
                    .setKeepAliveStrategy(myStrategy)
                    .setConnectionManager(connManager).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }

    }

    /**
     * 发送get请求；不带请求头和请求参数
     *
     * @param url 请求地址
     * @return
     * @throws Exception
     */
    public static HttpClientResult doGet(String url) throws Exception {
        return doGet(url, null, null);
    }

    /**
     * 发送get请求；带请求参数
     *
     * @param url 请求地址
     * @param params 请求参数集合
     * @return
     * @throws Exception
     */
    public static HttpClientResult doGet(String url, Map<String, String> params) throws Exception {
        return doGet(url, null, params);
    }

    /**
     * 发送get请求；带请求头和请求参数
     *
     * @param url 请求地址
     * @param headers 请求头集合
     * @param params 请求参数集合
     * @return
     * @throws Exception
     */
    public static HttpClientResult doGet(String url, Map<String, String> headers, Map<String, String> params) throws Exception {
        // 创建httpClient对象
        if (httpPoolClient == null) {
            httpPoolClient = HttpClients.custom()
                    .setKeepAliveStrategy(myStrategy)
                    .setConnectionManager(connManager).build();
        }

        // 创建访问的地址
        URIBuilder uriBuilder = new URIBuilder(url);
        if (params != null) {
            Set<Entry<String, String>> entrySet = params.entrySet();
            for (Entry<String, String> entry : entrySet) {
                uriBuilder.setParameter(entry.getKey(), entry.getValue());
            }
        }

        // 创建http对象
        HttpGet httpGet = new HttpGet(uriBuilder.build());
        /**
         * setConnectTimeout：设置连接超时时间，单位毫秒。
         * setConnectionRequestTimeout：设置从connect Manager(连接池)获取Connection
         * 超时时间，单位毫秒。这个属性是新加的属性，因为目前版本是可以共享连接池的。
         * setSocketTimeout：请求获取数据的超时时间(即响应时间)，单位毫秒。 如果访问一个接口，多少时间内无法返回数据，就直接放弃此次调用。
         */
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(SOCKET_TIMEOUT))
                .setResponseTimeout(Timeout.ofMilliseconds(CONNECT_TIMEOUT)).build();
        httpGet.setConfig(requestConfig);

        // 设置请求头
        packageHeader(headers, httpGet);

        // 执行请求并获得响应结果
        return getHttpClientResult(httpPoolClient, httpGet);
    }

    /**
     * 发送post请求；不带请求头和请求参数
     *
     * @param url 请求地址
     * @return
     * @throws Exception
     */
    public static HttpClientResult doPost(String url) throws Exception {
        return doPost(url, null, null);
    }


    /**
     * 用户form提交
     * @param url
     * @param paramMap
     * @return
     */
    public static HttpClientResult postFormBody(String url, Map paramMap) {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost httpPost =   new HttpPost(url);
        HttpClientResult result = new HttpClientResult();
        List params = new ArrayList();
        try {
            if (paramMap != null) {
                NameValuePair pair = null;
                Set<Entry<String, String>> entrySet = paramMap.entrySet();
                for (Entry<String, String> entry : entrySet) {
                    // 设置到请求头到HttpRequestBase对象中
                    pair = new BasicNameValuePair(entry.getKey(), entry.getValue());
                    params.add(pair);
                }
            }
            httpPost.setEntity(new UrlEncodedFormEntity(params,Charsets.toCharset("UTF-8")));
            httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");
            // 执行请求并获得响应结果
            result = getHttpClientResult(client, httpPost);
        } catch (Exception e) {
            LOG.error("postFormBody_exception", e);
        } finally {
            // 释放资源
            try {
                release(client);
            } catch (IOException e) {
                LOG.error("release_exception", e);
            }
        }
        return result;
    }

    /**
     * 发送post请求；带请求参数
     *
     * @param url 请求地址
     * @param params 参数集合
     * @return
     * @throws Exception
     */
    public static HttpClientResult doPost(String url, Map<String, String> params) throws Exception {
        return doPost(url, null, "");
    }

    /**
     * 发送post请求；不带请求头
     * @param url 请求地址
     * @param jsonString 请求参数 json字符串
     * @return
     * @throws Exception
     */
    public static HttpClientResult doPost(String url, String jsonString) throws Exception {

        // 创建httpClient对象
        return doPost(url, null, jsonString);
    }

    /**
     * 发送post请求；带请求头和请求参数
     *
     * @param url 请求地址
     * @param headers 请求头集合
     * @param params 请求参数集合
     * @return
     * @throws Exception
     */
    public static HttpClientResult doPost(String url, Map<String, String> headers, String params) throws Exception {
        // 创建httpClient对象
        if (httpPoolClient == null) {
            httpPoolClient = HttpClients.custom()
                    .setKeepAliveStrategy(myStrategy)
                    .setConnectionManager(connManager).build();
        }

        // 创建http对象
        HttpPost httpPost = new HttpPost(url);
        LOG.info("Executing request {} {}",httpPost.getMethod(), httpPost.getUri());
        /**
         * setConnectTimeout：设置连接超时时间，单位毫秒。
         * setConnectionRequestTimeout：设置从connect Manager(连接池)获取Connection
         * 超时时间，单位毫秒。这个属性是新加的属性，因为目前版本是可以共享连接池的。
         * setSocketTimeout：请求获取数据的超时时间(即响应时间)，单位毫秒。 如果访问一个接口，多少时间内无法返回数据，就直接放弃此次调用。
         */
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(SOCKET_TIMEOUT))
                .setResponseTimeout(Timeout.ofMilliseconds(CONNECT_TIMEOUT)).build();
        httpPost.setConfig(requestConfig);
        packageHeader(headers, httpPost);

        // 封装请求参数
        packageParam(params, httpPost);

        Iterator<Header> headerNames = httpPost.headerIterator();

        StringBuilder headerStr = new StringBuilder();
        while(headerNames.hasNext()) {//判断是否还有下一个元素
            Header nextElement = headerNames.next();//获取headerNames集合中的请求头
            headerStr.append(nextElement.getName() + ":" + nextElement.getValue()).append("\n");
        }
        LOG.info("requst_url:{}, request.header={},request.body:{}",url, headerStr, params);

        return getHttpClientResult(httpPoolClient, httpPost);
    }

    /**
     * 发送put请求；不带请求参数
     *
     * @param url 请求地址
     * @return
     * @throws Exception
     */
    public static HttpClientResult doPut(String url) throws Exception {
        return doPut(url);
    }

    /**
     * 发送put请求；带请求参数
     *
     * @param url 请求地址
     * @param headers 请求头
     * @param params 参数集合
     * @return
     * @throws Exception
     */
    public static HttpClientResult doPut(String url,Map<String, String> headers, String params) throws Exception {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPut httpPut = new HttpPut(url);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(SOCKET_TIMEOUT))
                .setResponseTimeout(Timeout.ofMilliseconds(CONNECT_TIMEOUT)).build();
        httpPut.setConfig(requestConfig);

        packageHeader(headers,httpPut);

        packageParam(params, httpPut);

        try {
            return getHttpClientResult(httpClient, httpPut);
        } finally {
            release(httpClient);
        }
    }

    /**
     * 发送put请求；带请求参数
     *
     * @param url 请求地址
     * @param params 参数集合
     * @return
     * @throws Exception
     */
    public static HttpClientResult doPut(String url, String params) throws Exception {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPut httpPut = new HttpPut(url);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(SOCKET_TIMEOUT))
                .setResponseTimeout(Timeout.ofMilliseconds(CONNECT_TIMEOUT)).build();
        httpPut.setConfig(requestConfig);

        packageParam(params, httpPut);

        try {
            return getHttpClientResult(httpClient, httpPut);
        } finally {
            release(httpClient);
        }
    }

    /**
     * 发送delete请求；带请求参数
     *
     * @param url 请求地址
     * @param params 参数集合
     * @return
     * @throws Exception
     */
    public static HttpClientResult doDelete(String url, Map<String, String> params) throws Exception {
        if (params == null) {
            params = new HashMap<String, String>();
        }

        params.put("_method", "delete");
        return doPost(url, params);
    }

    /**
     * Description: 封装请求头
     * @param params
     * @param httpMethod
     */
    public static void packageHeader(Map<String, String> params, HttpUriRequestBase httpMethod) {
        // 封装请求头
        if (params != null) {
            Set<Entry<String, String>> entrySet = params.entrySet();
            for (Entry<String, String> entry : entrySet) {
                // 设置到请求头到HttpRequestBase对象中
                httpMethod.setHeader(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Description: 封装请求参数
     *
     * @param body
     * @param httpMethod
     * @throws UnsupportedEncodingException
     */
    public static void packageParam(String body, HttpUriRequestBase httpMethod)
            throws UnsupportedEncodingException {
        // 封装请求参数
        if (StringUtils.isNoneEmpty(body)) {
            // 设置到请求的http对象中
            httpMethod.setEntity(new StringEntity(body, Charsets.toCharset("utf-8")));
        }
    }

    /**
     * Description: 获得响应结果
     *
     * @param httpClient
     * @param httpMethod
     * @return
     * @throws Exception
     */
    public static HttpClientResult getHttpClientResult(CloseableHttpClient httpClient, HttpUriRequestBase httpMethod)
            throws Exception {
        final HttpClientResponseHandler<HttpClientResult> responseHandler = response -> {
            HttpClientResult result = new HttpClientResult();
            final int status = response.getCode();
            result.setCode(status);
            if (status >= HttpStatus.SC_SUCCESS) {
                final HttpEntity entity = response.getEntity();
                try {
                    if (entity != null) {
                        result.setContent(EntityUtils.toString(entity, ENCODING));
                    }
                } catch (final ParseException ex) {
                    throw new ClientProtocolException(ex);
                }
            } else {
                throw new ClientProtocolException("Unexpected response status: " + status);
            }
            return result;
        };
        // 执行请求
        HttpClientResult httpResponse = httpClient.execute(httpMethod, responseHandler);

        // 获取返回结果
        if (httpResponse != null && httpResponse.getCode() == HttpStatus.SC_SUCCESS) {
            LOG.debug("requst_url: {}, request_body={}, response.status={}, response.content:{}",
                    httpMethod.getUri(),
                    httpMethod.getEntity() != null ? EntityUtils.toString(httpMethod.getEntity()) : "",
                    httpResponse.getCode(), httpResponse.getContent());
            return httpResponse;
        } else {
            LOG.error("requst_url_error: {}, request_body={}",
                    httpMethod.getUri(), httpMethod.getEntity() != null ? EntityUtils.toString(httpMethod.getEntity()) : "");
        }
        return new HttpClientResult(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Description: 释放资源
     *
     * @param httpClient
     * @throws IOException
     */
    public static void release(CloseableHttpClient httpClient) throws IOException {
        if (httpClient != null) {
            httpClient.close();
        }
    }

    public static HttpClientResult doHttpsPost(String url, Map<String, String> headers, String requestParam)  throws Exception {
        return doHttpsPost(url,headers,requestParam,"UTF-8");
    }

    public static HttpClientResult doHttpsPost(String url, Map<String, String> headers, String requestParam,String enCoding)  throws Exception {
        if (httpPoolClient == null) {
            httpPoolClient = HttpClients.custom()
                    .setKeepAliveStrategy(myStrategy)
                    .setConnectionManager(connManager).build();
        }
        HttpPost httppost = new HttpPost(url);
        if (headers != null) {
            Set<Entry<String, String>> entrySet = headers.entrySet();
            for (Entry<String, String> entry : entrySet) {
                // 设置到请求头到HttpRequestBase对象中
                httppost.addHeader(entry.getKey(), entry.getValue());
            }
        } else {
            httppost.addHeader("Content-Type", "application/xml");
        }

        //其他方法添加参数...
        StringEntity paraEntity = new StringEntity(requestParam, Charsets.toCharset(enCoding));//解决中文乱码问题

        httppost.setEntity(paraEntity);

        // set Timeout
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(SOCKET_TIMEOUT))
                .setResponseTimeout(Timeout.ofMilliseconds(CONNECT_TIMEOUT)).build();
        httppost.setConfig(requestConfig);

        return getHttpClientResult(httpPoolClient, httppost);
    }


    /**
     * 发送delete请求；带请求参数
     *
     * @param url 请求地址
     * @return
     * @throws Exception
     */
    public static HttpClientResult doDelete(String url, Map<String, String> headers, String requestParam)
            throws Exception {
        if (httpPoolClient == null) {
            httpPoolClient = HttpClients.custom()
                    .setKeepAliveStrategy(myStrategy)
                    .setConnectionManager(connManager).build();
        }

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(SOCKET_TIMEOUT))
                .setResponseTimeout(Timeout.ofMilliseconds(CONNECT_TIMEOUT)).build();

        HttpUriRequestBase httpDelete = new HttpUriRequestBase("DELETE", new URI(url));
        // 设置http header
        if (headers != null) {
            Set<Entry<String, String>> entrySet = headers.entrySet();
            for (Entry<String, String> entry : entrySet) {
                // 设置到请求头到HttpRequestBase对象中
                httpDelete.addHeader(entry.getKey(), entry.getValue());
            }
        }
        //其他方法添加参数...
        StringEntity paraEntity = new StringEntity(requestParam, Charsets.toCharset(ENCODING));
        httpDelete.setEntity(paraEntity);
        httpDelete.setConfig(requestConfig);

        Iterator<Header> headerNames = httpDelete.headerIterator();

        StringBuilder headerStr = new StringBuilder();
        //判断是否还有下一个元素
        while(headerNames.hasNext()) {
            //获取headerNames集合中的请求头
            Header nextElement = headerNames.next();
            headerStr.append(nextElement.getName() + ":" + nextElement.getValue()).append("\n");
        }
        LOG.info("request_url:{}, request.header={}, request.body:{}",url, headerStr, requestParam);
        // get http status code
        HttpClientResult result = getHttpClientResult(httpPoolClient, httpDelete);
        LOG.info("request未获得返回, " +
                "request_url:{}, request.params={}, response.status={}",url, requestParam, result.getCode());

        return result;
    }
}