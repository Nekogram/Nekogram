package app.nekogram.baselineprofile;

import androidx.benchmark.macro.junit4.BaselineProfileRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import kotlin.Unit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This test class generates a basic startup baseline profile for the target package.
 * <p>
 * We recommend you start with this but add important user flows to the profile to improve their performance.
 * Refer to the <a href="https://d.android.com/topic/performance/baselineprofiles">baseline profile documentation</a>
 * for more information.
 * <p>
 * You can run the generator with the "Generate Baseline Profile" run configuration in Android Studio or
 * the equivalent {@code generateBaselineProfile} gradle task:
 * <pre>
 * ./gradlew :TMessagesProj_App:generateReleaseBaselineProfile
 * </pre>
 * The run configuration runs the Gradle task and applies filtering to run only the generators.
 * <p>
 * Check <a href="https://d.android.com/topic/performance/benchmarking/macrobenchmark-instrumentation-args">documentation</a>
 * for more information about instrumentation arguments.
 * <p>
 * After you run the generator, you can verify the improvements running the {@link StartupBenchmarks} benchmark.
 * <p>
 * When using this class to generate a baseline profile, only API 33+ or rooted API 28+ are supported.
 * <p>
 * The minimum required version of androidx.benchmark to generate a baseline profile is 1.2.0.
 **/
@RunWith(AndroidJUnit4.class)
@LargeTest
public class BaselineProfileGenerator {
    @Rule
    public BaselineProfileRule baselineProfileRule = new BaselineProfileRule();

    @Test
    public void generate() {
        // The application id for the running build variant is read from the instrumentation arguments.
        String targetAppId = InstrumentationRegistry.getArguments().getString("targetAppId");
        if (targetAppId == null) {
            throw new RuntimeException("targetAppId not passed as instrumentation runner arg");
        }
        baselineProfileRule.collect(
                /* packageName = */ targetAppId,
                /* maxIterations = */ 15,
                /* stableIterations = */ 3,
                /* outputFilePrefix = */ null,
                // See: https://d.android.com/topic/performance/baselineprofiles/dex-layout-optimizations
                /* includeInStartupProfile = */ true,
                scope -> {
                    // This block defines the app's critical user journey. Here we are interested in
                    // optimizing for app startup. But you can also navigate and scroll
                    // through your most important UI.

                    // Start default activity for your app
                    scope.pressHome();
                    scope.startActivityAndWait();

                    // TODO Write more interactions to optimize advanced journeys of your app.
                    // For example:
                    // 1. Wait until the content is asynchronously loaded
                    // 2. Scroll the feed content
                    // 3. Navigate to detail screen

                    // Check UiAutomator documentation for more information how to interact with the app.
                    // https://d.android.com/training/testing/other-components/ui-automator

                    return Unit.INSTANCE;
                });
    }
}