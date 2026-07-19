package io.github.ahmedabadawi.asn1java.handwritten.vehicle;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class VehicleCodecTest {

  private static final VehicleCodec CODEC = new VehicleCodec();
  private static final HexFormat HEX = HexFormat.of();
  private static final Path GOLDEN_DIR =
      Paths.get(System.getProperty("user.dir")).getParent().resolve("golden-tests/vehicle");

  private String goldenHex(String name) throws IOException {
    return Files.readString(GOLDEN_DIR.resolve(name + ".hex")).strip();
  }

  private Vehicle valid1() {
    return new Vehicle(1234, new Propulsion.Gasoline(new GasEngine(1600, 4)));
  }

  private Vehicle valid2() {
    return new Vehicle(5678, new Propulsion.Electric(new ElectricMotor(150, 75)));
  }

  private Vehicle valid3() {
    return new Vehicle(42, new Propulsion.None());
  }

  @Test
  void encodeValid1() throws IOException {
    // Given
    var vehicle = valid1();

    // When
    var encoded = CODEC.encode(vehicle);

    // Then
    assertThat(HEX.formatHex(encoded)).isEqualTo(goldenHex("valid-1"));
  }

  @Test
  void encodeValid2() throws IOException {
    // Given
    var vehicle = valid2();

    // When
    var encoded = CODEC.encode(vehicle);

    // Then
    assertThat(HEX.formatHex(encoded)).isEqualTo(goldenHex("valid-2"));
  }

  @Test
  void encodeValid3() throws IOException {
    // Given
    var vehicle = valid3();

    // When
    var encoded = CODEC.encode(vehicle);

    // Then
    assertThat(HEX.formatHex(encoded)).isEqualTo(goldenHex("valid-3"));
  }

  @Test
  void decodeValid1() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-1"));

    // When
    var decoded = CODEC.decode(bytes);

    // Then
    assertThat(decoded.id()).isEqualTo(1234);
    assertThat(decoded.propulsion()).isEqualTo(new Propulsion.Gasoline(new GasEngine(1600, 4)));
  }

  @Test
  void decodeValid2() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-2"));

    // When
    var decoded = CODEC.decode(bytes);

    // Then
    assertThat(decoded.id()).isEqualTo(5678);
    assertThat(decoded.propulsion()).isEqualTo(new Propulsion.Electric(new ElectricMotor(150, 75)));
  }

  @Test
  void decodeValid3() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-3"));

    // When
    var decoded = CODEC.decode(bytes);

    // Then
    assertThat(decoded.id()).isEqualTo(42);
    assertThat(decoded.propulsion()).isEqualTo(new Propulsion.None());
  }

  @Test
  void construct_WhenIdExceedsMax_ShouldThrowIllegalArgumentException() {
    // When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> new Vehicle(65536, new Propulsion.None()));

    // Then
    assertThat(thrown).hasMessageContaining("id must be <= 65535");
  }

  @Test
  void construct_WhenPropulsionIsNull_ShouldThrowIllegalArgumentException() {
    // When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> new Vehicle(1, null));

    // Then
    assertThat(thrown).hasMessageContaining("propulsion must not be null");
  }

  @Test
  void construct_WhenGasolineValueIsNull_ShouldThrowIllegalArgumentException() {
    // When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> new Propulsion.Gasoline(null));

    // Then
    assertThat(thrown).hasMessageContaining("value must not be null");
  }

  @Test
  void construct_WhenElectricValueIsNull_ShouldThrowIllegalArgumentException() {
    // When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> new Propulsion.Electric(null));

    // Then
    assertThat(thrown).hasMessageContaining("value must not be null");
  }
}
