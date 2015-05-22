import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import retrofit.RestAdapter;
import retrofit.client.Response;
import retrofit.http.GET;
import rx.observers.TestObserver;

public class SomeTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9091);
    private TestObserver<String> testObserver;

    @Before
    public void setUp() {
        testObserver = new TestObserver<>();
    }

    @Test
    public void shouldName() {
        // given
        stubFor(get(urlEqualTo("/login"))
            .willReturn(aResponse()
                    .withStatus(201)
                    .withBody("alamakota")));

        RestAdapter adapter = new RestAdapter.Builder()
                .setEndpoint("http://127.0.0.1:9091")
                .build();
        LoginService loginService = adapter.create(LoginService.class);

        // when
        Response response = loginService.login();

        // then
        assertThat(response.getStatus()).isEqualTo(200);
    }

    public interface LoginService {

        @GET("/login")
        Response login();
    }
}
