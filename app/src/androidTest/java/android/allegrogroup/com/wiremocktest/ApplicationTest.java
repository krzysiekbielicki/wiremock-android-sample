package android.allegrogroup.com.wiremocktest;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.allegrogroup.testing.wiremock.RetrofitAdminClient;
import com.github.tomakehurst.wiremock.client.WireMock;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ApplicationTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(
            MainActivity.class);

    public WireMock wireMock = new WireMock(new RetrofitAdminClient("10.0.2.2", 8080));

    @Before
    public void setUp() {
        wireMock.resetMappings();
    }

    @Test
    public void changeText_sameActivity() throws InterruptedException {
        // given
        String test = "sth";
        wireMock.register(get(urlEqualTo("/ping")).willReturn(aResponse().withStatus(200).withBody(test)));

        // when
        onView(withId(R.id.button)).perform(click());

        // than
        onView(withId(R.id.textView)).check(matches(withText(test)));
    }
}