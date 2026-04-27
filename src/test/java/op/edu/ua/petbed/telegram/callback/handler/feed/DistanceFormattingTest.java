package op.edu.ua.petbed.telegram.callback.handler.feed;

import op.edu.ua.petbed.feed.FeedService;
import op.edu.ua.petbed.telegram.service.TelegramMessageService;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DistanceFormattingTest {

    @Mock
    FeedService feedService;

    @Mock
    TelegramMessageService messageService;

    @InjectMocks
    FeedViewNextCallbackHandler underTest;

    @Test
    void formatDistance_500m_returnsMetersFormat() {
        String result = (String) ReflectionTestUtils.invokeMethod(underTest, "formatDistance", 500.0);
        assertThat(result).isEqualTo("📍 500м від вас");
    }

    @Test
    void formatDistance_1500m_returnsKilometersFormat() {
        String result = (String) ReflectionTestUtils.invokeMethod(underTest, "formatDistance", 1500.0);
        assertThat(result).isEqualTo("📍 1.5 км від вас");
    }

    @Test
    void formatDistance_1000m_returnsKilometersFormat() {
        String result = (String) ReflectionTestUtils.invokeMethod(underTest, "formatDistance", 1000.0);
        assertThat(result).isEqualTo("📍 1.0 км від вас");
    }

    @Test
    void formatDistance_50m_returnsMetersFormat() {
        String result = (String) ReflectionTestUtils.invokeMethod(underTest, "formatDistance", 50.0);
        assertThat(result).isEqualTo("📍 50м від вас");
    }
}