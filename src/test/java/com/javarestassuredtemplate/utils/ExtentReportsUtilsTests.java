package com.javarestassuredtemplate.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ExtentReportsUtilsTests
{
    @Test
    public void redactsSensitiveJsonValues(){
        String reportText = ExtentReportsUtils.redactSensitiveData(
                "{\"profile\":{\"password\":\"p@ssword\", \"email\":\"user@example.com\", \"address\":{\"street\":\"1 Main St\"}}, \"pet\":{\"name\":\"doggie\"}, \"status\":\"available\"}");

        Assert.assertFalse(reportText.contains("p@ssword"));
        Assert.assertFalse(reportText.contains("user@example.com"));
        Assert.assertFalse(reportText.contains("1 Main St"));
        Assert.assertFalse(reportText.contains("doggie"));
        Assert.assertTrue(reportText.contains("available"));
    }

    @Test
    public void redactsSensitiveXmlValues(){
        String reportText = ExtentReportsUtils.redactSensitiveData(
                "<Credentials><Password>p@ssword</Password><Username>test-user</Username></Credentials>");

        Assert.assertFalse(reportText.contains("p@ssword"));
        Assert.assertFalse(reportText.contains("test-user"));
        Assert.assertTrue(reportText.contains("[REDACTED]"));
    }

    @Test
    public void redactsSensitiveHeaderAndCookieValues(){
        Assert.assertEquals(ExtentReportsUtils.redactSensitiveValue("Authorization", "Bearer abc123"), "[REDACTED]");
        Assert.assertEquals(ExtentReportsUtils.redactSensitiveValue("X-Api-Key", "abc123"), "[REDACTED]");
        Assert.assertEquals(ExtentReportsUtils.redactSensitiveValue("Cookie", "session=abc123"), "[REDACTED]");
    }

    @Test
    public void redactsCredentialsAndTokensFromUrls(){
        String reportUrl = ExtentReportsUtils.redactUrl(
                "https://service:password@example.test/pets?access_token=abc123&status=available");

        Assert.assertFalse(reportUrl.contains("password"));
        Assert.assertFalse(reportUrl.contains("abc123"));
        Assert.assertTrue(reportUrl.contains("example.test"));
        Assert.assertTrue(reportUrl.contains("status=available"));
    }
}
