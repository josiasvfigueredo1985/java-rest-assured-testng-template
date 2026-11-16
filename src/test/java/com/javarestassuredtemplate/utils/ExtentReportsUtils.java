package com.javarestassuredtemplate.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.javarestassuredtemplate.GlobalParameters;
import com.javarestassuredtemplate.enums.AuthenticationType;
import groovy.json.JsonException;
import groovy.json.JsonOutput;
import groovy.json.JsonSlurper;
import io.restassured.http.Header;
import io.restassured.response.Response;
import org.testng.ITestResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtentReportsUtils
{
    private static final String REDACTED = "[REDACTED]";
    private static final String SENSITIVE_FIELD_NAME =
            "(?:password|passwd|secret|token|access[_-]?token|refresh[_-]?token|api[_-]?key|authorization|credential|client[_-]?secret|private[_-]?key|session[_-]?id|email|phone|ssn|address|username|(?:first|last|full)[_-]?name|name|date[_-]?of[_-]?birth|dob)";
    private static final Pattern SENSITIVE_JSON_VALUE = Pattern.compile(
            "(?i)([\"']?" + SENSITIVE_FIELD_NAME + "[\"']?\\s*[:=]\\s*)(\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|[^,}&\\s<]+)");
    private static final Pattern SENSITIVE_XML_VALUE = Pattern.compile(
            "(?is)(<([\\w:.-]*" + SENSITIVE_FIELD_NAME + "[\\w:.-]*)(?:\\s[^>]*)?>).*?(</\\2\\s*>)");
    private static final Pattern URL_USER_INFO = Pattern.compile("(?i)(://)[^/@\\s]+@");
    private static final Pattern SENSITIVE_NAME = Pattern.compile("(?i)(?:" + SENSITIVE_FIELD_NAME + "|auth|cookie|social[_-]?security)");
    public static ExtentReports EXTENT_REPORT = null;
    public static ExtentTest TEST;
    public static ExtentSparkReporter HTML_REPORTER = null;
    static String reportName = GlobalParameters.REPORT_NAME + "_" + GeneralUtils.getNowDate("yyyy-MM-dd_HH-mm-ss");
    static String reportsPath = GlobalParameters.REPORT_PATH;
    static String fileName = reportName+".html";
    static String fullReportFilePath = reportsPath + "/"+ reportName +"/" + fileName;

    public static void createReport(){
        if (EXTENT_REPORT == null){
            HTML_REPORTER = new ExtentSparkReporter(fullReportFilePath);
            EXTENT_REPORT = new ExtentReports();
            EXTENT_REPORT.attachReporter(HTML_REPORTER);
        }
    }

    public static void addTest(String testName, String testCategory){
        TEST = EXTENT_REPORT.createTest(testName).assignCategory(testCategory.replace("Tests",""));
    }

    public static void addRestTestInfo(int methodLevel, String text){
        TEST.log(Status.PASS, GeneralUtils.getMethodNameByLevel(methodLevel) + " || " + redactSensitiveData(text));
    }

    public static void addRestTestInfo(String url, String requestService, String method, Map<String, String> headers, Map<String, String> cookies, Map<String, String> queryParameters, String jsonBody, AuthenticationType authenticationType, String authenticatorUser, String authenticatorPassword, Response response){
        String allHeaders = "";
        String allCookies = "";
        String allParameters = "";
        String allResponseHeaders = "";

        for(Map.Entry<String, String> queryParameter : queryParameters.entrySet()){
            allParameters = allParameters + "\n" + "<i>" + queryParameter.getKey() + "</i>" + " = " + redactSensitiveValue(queryParameter.getKey(), queryParameter.getValue());
        }

        for(Map.Entry<String, String> header : headers.entrySet()){
            allHeaders = allHeaders + "\n" + "<i>" + header.getKey() + "</i>" + " = " + redactSensitiveValue(header.getKey(), header.getValue());
        }

        for(Map.Entry<String, String> cookie : cookies.entrySet()){
            allCookies = allCookies + "\n" + "<i>" + cookie.getKey() + "</i>" + " = " + REDACTED;
        }

        for(Header responseHeader : response.getHeaders().asList()){
            allResponseHeaders = allResponseHeaders + "\n" + responseHeader.getName() + ": " + redactSensitiveValue(responseHeader.getName(), responseHeader.getValue());
        }

        TEST.log(Status.INFO, "<pre>" + "<b>URL: </b>" + redactUrl(url) + "</pre>");
        TEST.log(Status.INFO, "<pre>" + "<b>REQUEST: </b>" + redactSensitiveData(requestService) + "</pre>");
        TEST.log(Status.INFO, "<pre>" + "<b>METHOD: </b>" + redactSensitiveData(method) + "</pre>");

        if (!allParameters.equals("")){
            TEST.log(Status.INFO, "<pre>" + "<b>PARAMETERS: </b>" + "\n" + allParameters + "</pre>");
        }

        if(jsonBody!=null){
            TEST.log(Status.INFO, "<pre>" + "<b>JSON BODY: </b>" + "\n" + redactSensitiveData(jsonBody) + "</pre>");
        }

        if (!allHeaders.equals("")){
            TEST.log(Status.INFO, "<pre>" + "<b>HEADERS: </b>" + "\n" + allHeaders + "</pre>");
        }

        if (!allCookies.equals("")){
            TEST.log(Status.INFO, "<pre>" + "<b>COOKIES: </b>" + "\n" + allCookies + "</pre>");
        }

        if (!authenticationType.equals(AuthenticationType.NONE)){
            TEST.log(Status.INFO, "<pre>" + "<b>AUTHENTICATOR TYPE: "+authenticationType.toString()+"</b>" + "\n" + "<b>USER: </b>"+ REDACTED + "\n" + "<b>PASSWORD: </b>" + REDACTED + "</pre>");
        }

        TEST.log(Status.INFO, "<pre>" + "<b>STATUS CODE: </b>" + response.statusCode() + "</pre>");
        TEST.log(Status.INFO, "<pre>" + "<b>RESPONSE HEADERS: </b>" + "\n" + allResponseHeaders + "</pre>");
        TEST.log(Status.INFO, "<pre>" + "<b>PAYLOAD: </b>" + "\n" + GeneralUtils.formatJson(redactSensitiveData(response.asString())) + "</pre>");
    }

    public static void addSoapTestInfo(String url, Map<String, String> headers, Map<String, String> cookies, String xmlBody, AuthenticationType authenticationType, String authenticatorUser, String authenticatorPassword, Response response){
        String allHeaders = "";
        String allCookies = "";
        String allResponseHeaders = "";

        for(Map.Entry<String, String> header : headers.entrySet()){
          allHeaders = allHeaders + "\n" + "<i>" + header.getKey() + "</i>" + " = " + redactSensitiveValue(header.getKey(), header.getValue());
        }

        for(Map.Entry<String, String> cookie : cookies.entrySet()){
          allCookies = allCookies + "\n" + "<i>" + cookie.getKey() + "</i>" + " = " + REDACTED;
        }

        for(Header responseHeader : response.getHeaders().asList()){
          allResponseHeaders = allResponseHeaders + "\n" + responseHeader.getName() + ": " + redactSensitiveValue(responseHeader.getName(), responseHeader.getValue());
        }

        TEST.log(Status.INFO, "<pre>" + "<b>URL: </b>" + redactUrl(url) + "</pre>");
        TEST.log(Status.INFO, "<pre>" + "<b>REQUEST: </b><textarea>" + redactSensitiveData(xmlBody) + "</textarea>");

        if (!allHeaders.equals("")){
            TEST.log(Status.INFO, "<pre>" + "<b>HEADERS: </b>" + "\n" + allHeaders + "</pre>");
        }

        if (!allCookies.equals("")){
            TEST.log(Status.INFO, "<pre>" + "<b>COOKIES: </b>" + "\n" + allCookies + "</pre>");
        }

        if (!authenticationType.equals(AuthenticationType.NONE)){
            TEST.log(Status.INFO, "<pre>" + "<b>AUTHENTICATOR TYPE: "+authenticationType.toString()+"</b>" + "\n" + "<b>USER: </b>"+ REDACTED + "\n" + "<b>PASSWORD: </b>" + REDACTED + "</pre>");
        }

        TEST.log(Status.INFO, "<pre>" + "<b>STATUS CODE: </b>" + response.statusCode() + "</pre>");
        TEST.log(Status.INFO, "<pre>" + "<b>RESPONSE HEADERS: </b>" + "\n" + allResponseHeaders + "</pre>");
        TEST.log(Status.INFO, "<b>PAYLOAD: </b> <textarea>" + "\n" + redactSensitiveData(response.asString()) + "</textarea>");
    }

    public static void addTestResult(ITestResult result){
        switch (result.getStatus())
        {
            case ITestResult.FAILURE:
                TEST.log(Status.FAIL, "Test Result: " + Status.FAIL + "<pre>" + "Message: " + redactSensitiveData(result.getThrowable().toString()) + "</pre>" + "<pre>" + "Stack Trace: " + redactSensitiveData(GeneralUtils.getAllStackTrace(result)) + "</pre>");
                break;
            case ITestResult.SKIP:
                TEST.log(Status.SKIP, "Test Result: " + Status.SKIP + "<pre>" + "Message: " + redactSensitiveData(result.getThrowable().toString()) + "</pre>" + "<pre>" + "Stack Trace: " + redactSensitiveData(GeneralUtils.getAllStackTrace(result)) + "</pre>");
                break;
            default:
                TEST.log(Status.PASS, "Test Result: " + Status.PASS);
                break;
        }
    }

    public static void generateReport(){
        EXTENT_REPORT.flush();
    }

    static String redactSensitiveValue(String name, String value){
        if (isSensitiveName(name)){
            return REDACTED;
        }
        return redactSensitiveData(value);
    }

    static String redactSensitiveData(String value){
        if (value == null){
            return null;
        }

        try {
            Object parsedJson = new JsonSlurper().parseText(value);
            return JsonOutput.toJson(redactJsonValue(parsedJson));
        } catch (JsonException e) {
            return redactSensitiveText(value);
        }
    }

    static String redactUrl(String url){
        if (url == null){
            return null;
        }
        return URL_USER_INFO.matcher(redactSensitiveData(url)).replaceAll("$1[REDACTED]@");
    }

    private static Object redactJsonValue(Object value){
        if (value instanceof Map<?, ?>){
            Map<String, Object> redactedMap = new LinkedHashMap<>();
            for(Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()){
                String key = String.valueOf(entry.getKey());
                redactedMap.put(key, isSensitiveName(key) ? REDACTED : redactJsonValue(entry.getValue()));
            }
            return redactedMap;
        }
        if (value instanceof List<?>){
            List<Object> redactedList = new ArrayList<>();
            for(Object item : (List<?>) value){
                redactedList.add(redactJsonValue(item));
            }
            return redactedList;
        }
        if (value instanceof String){
            return redactSensitiveText((String) value);
        }
        return value;
    }

    private static String redactSensitiveText(String value){
        Matcher jsonMatcher = SENSITIVE_JSON_VALUE.matcher(value);
        String redactedValue = jsonMatcher.replaceAll("$1\"[REDACTED]\"");
        Matcher xmlMatcher = SENSITIVE_XML_VALUE.matcher(redactedValue);
        return xmlMatcher.replaceAll("$1[REDACTED]$3");
    }

    private static boolean isSensitiveName(String name){
        return name != null && SENSITIVE_NAME.matcher(name).find();
    }
}
