package io.github.ahmedabadawi.asn1java.sample;

import io.github.ahmedabadawi.asn1java.sample.vehiclemodule.ElectricMotor;
import io.github.ahmedabadawi.asn1java.sample.vehiclemodule.GasEngine;
import io.github.ahmedabadawi.asn1java.sample.vehiclemodule.Propulsion;
import io.github.ahmedabadawi.asn1java.sample.vehiclemodule.Vehicle;
import io.github.ahmedabadawi.asn1java.sample.vehiclemodule.VehicleCodec;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class VehicleCodecApprovalTest {

  private static final VehicleCodec CODEC = new VehicleCodec();
  private static final HexFormat HEX = HexFormat.of();
  private static final Path GOLDEN_DIR = Paths.get(
      System.getProperty("user.dir")).getParent().resolve("golden-tests/vehicle");

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
  void encode_WhenValid1_ShouldMatchGoldenHex() throws IOException {
    // Given
    var vehicle = valid1();

    // When
    var hex = HEX.formatHex(CODEC.encode(vehicle));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-1"));
  }

  @Test
  void encode_WhenValid2_ShouldMatchGoldenHex() throws IOException {
    // Given
    var vehicle = valid2();

    // When
    var hex = HEX.formatHex(CODEC.encode(vehicle));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-2"));
  }

  @Test
  void encode_WhenValid3_ShouldMatchGoldenHex() throws IOException {
    // Given
    var vehicle = valid3();

    // When
    var hex = HEX.formatHex(CODEC.encode(vehicle));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-3"));
  }

  @Test
  void decode_WhenValid1Encoded_ShouldReturnCorrectFields() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-1"));

    // When
    var decoded = CODEC.decode(bytes);

    // Then
    assertThat(decoded.id()).isEqualTo(1234);
    assertThat(decoded.propulsion()).isEqualTo(new Propulsion.Gasoline(new GasEngine(1600, 4)));
  }

  @Test
  void decode_WhenValid2Encoded_ShouldReturnCorrectFields() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-2"));

    // When
    var decoded = CODEC.decode(bytes);

    // Then
    assertThat(decoded.id()).isEqualTo(5678);
    assertThat(decoded.propulsion()).isEqualTo(new Propulsion.Electric(new ElectricMotor(150, 75)));
  }

  @Test
  void decode_WhenValid3Encoded_ShouldReturnCorrectFields() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-3"));

    // When
    var decoded = CODEC.decode(bytes);

    // Then
    assertThat(decoded.id()).isEqualTo(42);
    assertThat(decoded.propulsion()).isEqualTo(new Propulsion.None());
  }
}
