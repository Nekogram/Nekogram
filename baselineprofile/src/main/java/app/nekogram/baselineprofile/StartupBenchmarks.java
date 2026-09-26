package app.nekogram.baselineprofile;

import androidx.benchmark.macro.BaselineProfileMode;
import androidx.benchmark.macro.CompilationMode;
import androidx.benchmark.macro.StartupMode;
import androidx.benchmark.macro.StartupTimingMetric;
import androidx.benchmark.macro.junit4.MacrobenchmarkRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import kotlin.Unit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

/**
 * This test class benchmarks the speed of app startup.
 * Run this benchmark to verify how effective a Baseline Profile is.
 * It does this by comparing {@code CompilationMode.None}, which represents the app with no Baseline
 * Profiles optimizations, and {@code CompilationMode.Partial}, which uses Baseline Profiles.
 * <p>
 * Run this benchmark to see startup measurements and captured system traces for verifying
 * the effectiveness of your Baseline Profiles. You can run it directly from Android
 * Studio as an instrumentation test, or run all benchmarks for a variant, for example benchmarkRelease,
 * with this Gradle task:
 * <pre>
 * ./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest
 * </pre>
 * <p>
 * You should run the benchmarks on a physical device, not an Android emulator, because the
 * emulator doesn't represent real world performance and shares system resources with its host.
 * <p>
 * For more information, see the <a href="https://d.android.com/macrobenchmark#create-macrobenchmark">Macrobenchmark documentation</a>
 * and the <a href="https://d.android.com/topic/performance/benchmarking/macrobenchmark-instrumentation-args">instrumentation arguments documentation</a>.
 **/
@RunWith(AndroidJUnit4.class)
@LargeTest
public class StartupBenchmarks {

    @Rule
    public MacrobenchmarkRule rule = new MacrobenchmarkRule();

    @Test
    public void startupCompilationNone() {
        benchmark(new CompilationMode.None());
    }

    @Test
    public void startupCompilationBaselineProfiles() {
        benchmark(new CompilationMode.Partial(BaselineProfileMode.Require));
    }

    private void benchmark(CompilationMode compilationMode) {
        // The application id for the running build variant is read from the instrumentation arguments.
        String targetAppId = InstrumentationRegistry.getArguments().getString("targetAppId");
        if (targetAppId == null) {
            throw new RuntimeException("targetAppId not passed as instrumentation runner arg");
        }
        rule.measureRepeated(
                targetAppId,
                Collections.singletonList(new StartupTimingMetric()),
                compilationMode,
                StartupMode.COLD,
                10,
                setupScope -> {
                    setupScope.pressHome();
                    return Unit.INSTANCE;
                },
                measureScope -> {
                    measureScope.startActivityAndWait();

                    // TODO Add interactions to wait for when your app is fully drawn.
                    // The app is fully drawn when Activity.reportFullyDrawn is called.
                    // For Jetpack Compose, you can use ReportDrawn, ReportDrawnWhen and ReportDrawnAfter
                    // from the AndroidX Activity library.

                    // Check the UiAutomator documentation for more information on how to
                    // interact with the app.
                    // https://d.android.com/training/testing/other-components/ui-automator
                    return Unit.INSTANCE;
                }
        );
    }
}