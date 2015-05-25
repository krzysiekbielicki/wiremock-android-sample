package com.allegrogroup.testing.wiremock;

import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;

import com.github.tomakehurst.wiremock.admin.AdminTask;
import com.github.tomakehurst.wiremock.admin.AdminTasks;
import com.github.tomakehurst.wiremock.admin.FindRequestsTask;
import com.github.tomakehurst.wiremock.admin.GetRequestCountTask;
import com.github.tomakehurst.wiremock.admin.GlobalSettingsUpdateTask;
import com.github.tomakehurst.wiremock.admin.NewStubMappingTask;
import com.github.tomakehurst.wiremock.admin.RequestSpec;
import com.github.tomakehurst.wiremock.admin.ResetRequestsTask;
import com.github.tomakehurst.wiremock.admin.ResetScenariosTask;
import com.github.tomakehurst.wiremock.admin.ResetTask;
import com.github.tomakehurst.wiremock.admin.ResetToDefaultMappingsTask;
import com.github.tomakehurst.wiremock.admin.RootTask;
import com.github.tomakehurst.wiremock.admin.SaveMappingsTask;
import com.github.tomakehurst.wiremock.admin.ShutdownServerTask;
import com.github.tomakehurst.wiremock.admin.SocketDelayTask;
import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.global.GlobalSettings;
import com.github.tomakehurst.wiremock.global.RequestDelaySpec;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.stubbing.ListStubMappingsResult;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.verification.FindRequestsResult;
import com.github.tomakehurst.wiremock.verification.VerificationResult;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import retrofit.client.ApacheClient;
import retrofit.client.Request;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedString;

public class RetrofitAdminClient implements Admin {

    private static final int BUFFER_SIZE = 0x1000;

    private static final String ADMIN_URL_PREFIX = "http://%s:%d%s/__admin";

    private final String host;
    private final int port;
    private final String urlPathPrefix;

    private final ApacheClient httpClient;
    private final Gson gson;

    public RetrofitAdminClient(String host, int port, String urlPathPrefix) {
        this.host = host;
        this.port = port;
        this.urlPathPrefix = urlPathPrefix;
        gson = new Gson();

        httpClient = new ApacheClient();
    }

    public RetrofitAdminClient(String host, int port) {
        this(host, port, "");
    }

    @Override
    public void addStubMapping(StubMapping stubMapping) {
        postJsonAssertOkAndReturnBody(urlFor(NewStubMappingTask.class), Json.write(stubMapping), HTTP_CREATED);
    }

    @Override
    public ListStubMappingsResult listAllStubMappings() {
        String body = getJsonAssertOkAndReturnBody(
                urlFor(RootTask.class),
                HTTP_OK);
        return Json.read(body, ListStubMappingsResult.class);
    }

    @Override
    public void saveMappings() {
        postJsonAssertOkAndReturnBody(urlFor(SaveMappingsTask.class), null, HTTP_OK);
    }

    @Override
    public void resetMappings() {
        postJsonAssertOkAndReturnBody(urlFor(ResetTask.class), null, HTTP_OK);
    }

    @Override
    public void resetRequests() {
        postJsonAssertOkAndReturnBody(urlFor(ResetRequestsTask.class), null, HTTP_OK);
    }

    @Override
    public void resetScenarios() {
        postJsonAssertOkAndReturnBody(urlFor(ResetScenariosTask.class), null, HTTP_OK);
    }

    @Override
    public void resetToDefaultMappings() {
        postJsonAssertOkAndReturnBody(urlFor(ResetToDefaultMappingsTask.class), null, HTTP_OK);
    }

    @Override
    public VerificationResult countRequestsMatching(RequestPattern requestPattern) {
        String body = postJsonAssertOkAndReturnBody(
                urlFor(GetRequestCountTask.class),
                Json.write(requestPattern),
                HTTP_OK);
        return VerificationResult.from(body);
    }

    @Override
    public FindRequestsResult findRequestsMatching(RequestPattern requestPattern) {
        String body = postJsonAssertOkAndReturnBody(
                urlFor(FindRequestsTask.class),
                Json.write(requestPattern),
                HTTP_OK);
        return Json.read(body, FindRequestsResult.class);
    }

    @Override
    public void updateGlobalSettings(GlobalSettings settings) {
        postJsonAssertOkAndReturnBody(
                urlFor(GlobalSettingsUpdateTask.class),
                Json.write(settings),
                HTTP_OK);
    }

    @Override
    public void addSocketAcceptDelay(RequestDelaySpec spec) {
        postJsonAssertOkAndReturnBody(
                urlFor(SocketDelayTask.class),
                Json.write(spec),
                HTTP_OK);
    }

    @Override
    public void shutdownServer() {
        postJsonAssertOkAndReturnBody(urlFor(ShutdownServerTask.class), null, HTTP_OK);
    }

    public int port() {
        return port;
    }

    private String postJsonAssertOkAndReturnBody(String url, String json, int expectedStatus) {
        try {
            Request post = new Request("POST", url, null,json == null ? null : new TypedString(json));
            Response response = httpClient.execute(post);
            int statusCode = response.getStatus();
            if (statusCode != expectedStatus) {
                throw new VerificationException(
                        "Expected status " + expectedStatus + " for " + url + " but was " + statusCode);
            }

            return readString(response);
        } catch (Exception e) {
            return throwUnchecked(e, String.class);
        }
    }

    private String getJsonAssertOkAndReturnBody(String url, int expectedStatus) {
        Request get = new Request("GET", url, null, null);
        try {
            Response response = httpClient.execute(get);
            int statusCode = response.getStatus();
            if (statusCode != expectedStatus) {
                throw new VerificationException(
                        "Expected status " + expectedStatus + " for " + url + " but was " + statusCode);
            }

            return readString(response);
        } catch (Exception e) {
            return throwUnchecked(e, String.class);
        }
    }

    private String urlFor(Class<? extends AdminTask> taskClass) {
        RequestSpec requestSpec = AdminTasks.requestSpecForTask(taskClass);
        return String.format(ADMIN_URL_PREFIX + requestSpec.path(), host, port, urlPathPrefix);
    }

    static String readString(Response response) throws IOException {
        TypedInput body = response.getBody();
        if (body == null || body instanceof TypedByteArray) {
            return null;
        }

        InputStream is = body.in();
        try {
            byte[] bodyBytes = streamToBytes(is);

            return new String(bodyBytes);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    static byte[] streamToBytes(InputStream stream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (stream != null) {
            byte[] buf = new byte[BUFFER_SIZE];
            int r;
            while ((r = stream.read(buf)) != -1) {
                baos.write(buf, 0, r);
            }
        }
        return baos.toByteArray();
    }
}
