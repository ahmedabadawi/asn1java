package io.github.ahmedabadawi.asn1java.sample;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.github.ahmedabadawi.asn1java.sample.transport.transportmessagemodule.MessageType;
import org.junit.jupiter.api.Test;

class MessageTypeTest {

  @Test
  void construct_WhenValueTooShort_ShouldThrowIllegalArgumentException() {
    // When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> new MessageType("x"));

    // Then
    assertThat(thrown).hasMessageContaining("value length must be >= 2");
  }

  @Test
  void construct_WhenValueIsNull_ShouldThrowIllegalArgumentException() {
    // When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> new MessageType(null));

    // Then
    assertThat(thrown).hasMessageContaining("value must not be null");
  }
}
