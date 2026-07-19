# UPER Encoding Rules

Reference standard: **ITU-T X.691** — *Information technology – ASN.1 encoding rules:
Specification of Packed Encoding Rules (PER)*
https://www.itu.int/rec/T-REC-X.691/en

UPER = Unaligned Packed Encoding Rules. Tags are never encoded (unlike BER/DER);
fields in a SEQUENCE are written in definition order, bit-packed with no alignment
padding between them.

---

## INTEGER (0..MAX) — Semi-constrained whole number (§12.2.6)

A lower bound with no upper bound. The value is encoded as an offset from the lower
bound, followed by a length determinant and the value bytes.

**Steps:**
1. `offset = value − lower_bound`  (lower_bound = 0 here, so offset = value)
2. Write `n` as one byte: the minimum number of octets needed to hold `offset`
   (at least 1 even for zero)
3. Write `offset` big-endian in exactly `n` bytes

**Examples** (`INTEGER (0..MAX)`):

| value | n    | encoding   |
|-------|------|------------|
| 0     | `01` | `01 00`    |
| 1     | `01` | `01 01`    |
| 2     | `01` | `01 02`    |
| 24    | `01` | `01 18`    |
| 255   | `01` | `01 ff`    |
| 256   | `02` | `02 01 00` |

**`Version` SEQUENCE encoding** (`major INTEGER (0..MAX), minor INTEGER (0..MAX)`):

Fields are encoded in order with no padding between them.

| input              | major bytes | minor bytes | hex        |
|--------------------|-------------|-------------|------------|
| major=1, minor=0   | `01 01`     | `01 00`     | `01010100` |
| major=2, minor=24  | `01 02`     | `01 18`     | `01020118` |

---

## INTEGER (lb..ub) — Constrained whole number (§12.2.3–12.2.5)

Both bounds are known at compile time. The value is encoded as a fixed-width offset
from the lower bound, using the minimum number of bits needed to represent the range.

**Steps:**
1. `range = upper_bound − lower_bound`
2. `bit_count = 32 − Integer.numberOfLeadingZeros(range)` &nbsp;(number of bits to represent values 0..range)
3. `offset = value − lower_bound`
4. Write `offset` in exactly `bit_count` bits, MSB first

If `range == 0` (both bounds are the same), the value is fully determined — write nothing.

**Examples** (`INTEGER (0..255)`, range = 255, bit\_count = 8):

| value | offset | bits (binary) | hex  |
|-------|--------|---------------|------|
| 0     | 0      | `00000000`    | `00` |
| 1     | 1      | `00000001`    | `01` |
| 127   | 127    | `01111111`    | `7f` |
| 255   | 255    | `11111111`    | `ff` |

**Examples** (`INTEGER (10..20)`, range = 10, bit\_count = 4):

| value | offset | bits (binary) | notes       |
|-------|--------|---------------|-------------|
| 10    | 0      | `0000`        | lower bound |
| 15    | 5      | `0101`        |             |
| 20    | 10     | `1010`        | upper bound |

---

## INTEGER (n..n) — Zero-range (§12.2.3, degenerate case)

When `lower_bound == upper_bound` the only legal value is `n`. No bits are written.

---

## SEQUENCE — Field ordering (§18)

Fields are encoded in the order they appear in the ASN.1 definition, with no alignment
padding or tag bytes between them. The concatenated bit streams of all fields form the
final byte array, with the last byte zero-padded on the right if the total bit count is
not a multiple of 8.

---

## BOOLEAN (§12.8)

A boolean value is encoded as a single bit: `1` for TRUE, `0` for FALSE.
No length prefix, no constraint syntax, no offset.

**Steps:**
1. Write 1 bit: `1` if value is TRUE, `0` if FALSE.

**Examples:**

| value | bit | byte (zero-padded) | hex  |
|-------|-----|--------------------|------|
| TRUE  | `1` | `10000000`         | `80` |
| FALSE | `0` | `00000000`         | `00` |

**`Device` SEQUENCE encoding** (`active BOOLEAN`):

| input         | bit | hex  |
|---------------|-----|------|
| active=true   | `1` | `80` |
| active=false  | `0` | `00` |

---

## UTF8String — unconstrained (§26 + §10.7)

An unconstrained UTF8String (no SIZE constraint) is encoded as a length determinant
followed by the raw UTF-8 bytes of the string value.

**Length determinant (§10.7):**
- Byte count < 128: 1 byte (the count itself, high bit = 0).
- Byte count 128–16383: 2 bytes (0x80 | count >> 8, count & 0xFF).

This is distinct from the semi-constrained integer encoding (§12.2.6): there is no
separate "byte-count-of-the-count" prefix.

**Steps:**
1. Encode the string as UTF-8 bytes; let `n` = byte count.
2. Write the length determinant for `n` (1 or 2 bytes per §10.7).
3. Write each of the `n` UTF-8 bytes as 8 bits.

**Examples:**

| value     | UTF-8 bytes                      | length byte | full hex       |
|-----------|----------------------------------|-------------|----------------|
| `"hello"` | `68 65 6c 6c 6f` (5 bytes)       | `05`        | `0568656c6c6f` |
| `""`      | (none)                           | `00`        | `00`           |

---

## Score SEQUENCE — negative and positive lower bounds with MIN/MAX boundary examples

`level INTEGER (1..10)`: range=9, bit\_count=4, lb=1. Encode: `level−1` in 4 bits.  
`points INTEGER (0..999)`: range=999, bit\_count=10, lb=0. Encode: `points` in 10 bits.  
`offset INTEGER (−10..10)`: range=20, bit\_count=5, lb=−10. Encode: `offset+10` in 5 bits.  
Fields packed left-to-right; 19-bit stream zero-padded to 24 bits (3 bytes).

| input                                   | level  | points       | offset  | hex      |
|-----------------------------------------|--------|--------------|---------|----------|
| level=1,  points=0,   offset=−10 (MIN)  | `0000` | `0000000000` | `00000` | `000000` |
| level=5,  points=500, offset=0   (mid)  | `0100` | `0111110100` | `01010` | `47d140` |
| level=10, points=999, offset=10  (MAX)  | `1001` | `1111100111` | `10100` | `9f9e80` |

---

## ENUMERATED — root enumeration (§13)

A root enumeration with N values is encoded as the zero-based ordinal index of the
selected value, treated as a constrained whole number in the range 0..(N-1). This is
identical to `INTEGER (0..N-1)` constrained encoding.

**Steps:**
1. `ordinal` = zero-based position of the selected value in declaration order
2. `range = N − 1`
3. `bit_count = 32 − Integer.numberOfLeadingZeros(range)` (0 if N ≤ 1)
4. Write `ordinal` in exactly `bit_count` bits, MSB first

**Examples** (`Status ::= ENUMERATED { pending, active, inactive }`, N=3, range=2, bit\_count=2):

| value    | ordinal | bits (binary) | hex  |
|----------|---------|---------------|------|
| pending  | 0       | `00`          | `00` |
| active   | 1       | `01`          | `40` |
| inactive | 2       | `10`          | `80` |

---

## INTEGER (MIN..ub) — Upper-bounded (unconstrained) whole number (§12.2.3)

When the lower bound is `MIN` (no lower bound) and the upper bound is a finite number,
the value is encoded as a minimum-length two's-complement signed integer, prefixed with
a 1-byte octet count.

This is the same algorithm as unconstrained INTEGER (§12.2.3 applies to both).

**Steps:**
1. Determine the minimum number of octets `n` needed to represent `value` in big-endian
   two's-complement signed form (at least 1, even for zero):
   - Positive values: `n = ceil((bit_length(value) + 1) / 8)`, where bit_length is
     the position of the most significant `1` bit (i.e. `Long.SIZE − Long.numberOfLeadingZeros(value)`).
     Special case: value 0 requires 1 byte.
   - Negative values: `n = ceil((bit_length(~value) + 1) / 8)`, where `~value` is
     the bitwise complement. Special case: value −1 requires 1 byte (0xFF).
2. Write `n` as one byte.
3. Write `value` big-endian in exactly `n` bytes (two's-complement signed).

The upper bound is validated (value ≤ upper_bound) but does not change the encoding
algorithm — it only restricts which values are legal.

**Examples** (`INTEGER (MIN..0)`, so all values ≤ 0 are legal):

| value  | two's-complement bytes | n    | encoding    |
|--------|------------------------|------|-------------|
| 0      | `00`                   | `01` | `01 00`     |
| −1     | `ff`                   | `01` | `01 ff`     |
| −100   | `9c`                   | `01` | `01 9c`     |
| −128   | `80`                   | `01` | `01 80`     |
| −129   | `ff 7f`                | `02` | `02 ff 7f`  |
| −32768 | `80 00`                | `02` | `02 80 00`  |

---

## INTEGER — Named Numbers

Named numbers (e.g. `INTEGER { low(0), normal(1), high(2) } (0..2)`) are purely
documentation. The encoding is identical to the same integer type without named numbers:
the constraint `(0..2)` governs — constrained whole number with range = 2, bit_count = 2.

Named numbers have no effect on the bit-level encoding.

---

## OCTET STRING — unconstrained and SIZE-constrained (§16)

An OCTET STRING encodes a sequence of bytes. UPER encodes the length (in octets) as a
determinant, then the octets themselves.

Three cases, depending on whether a SIZE constraint is present:

### Unconstrained `OCTET STRING`

Length is encoded with the semi-constrained integer format (§12.2.6): 1-byte octet count
for the length, then the length bytes. Minimum 1-byte count even for empty payload.

**Steps:**
1. Let `n` = byte count of the payload.
2. Write `n` as one byte.
3. Write `n` payload bytes.

| payload (hex)   | n    | encoding     |
|-----------------|------|--------------|
| (empty)         | `00` | `00`         |
| `ff`            | `01` | `01 ff`      |
| `de ad be ef`   | `04` | `04 de ad be ef` |

### `OCTET STRING (SIZE (lb..ub))` — range-constrained length

When `ub < 65536`: Length is encoded as a constrained whole number in the range `0..(ub-lb)`,
using `ceil(log2(ub-lb+1))` bits (or 0 bits if `lb == ub`). Then the `length` bytes follow.

When `ub >= 65536` (§16.7): The length is encoded with the §10.7 unconstrained length
determinant (the actual length value, not an offset from lb): 1 byte if length < 128,
2 bytes otherwise. Then the bytes follow.

**Steps:**
1. `offset = length − lb`
2. `range = ub − lb`
3. `bit_count = 32 − Integer.numberOfLeadingZeros(range)` (0 if range == 0)
4. Write `offset` in `bit_count` bits.
5. Write `length` payload bytes (8 bits each).

**Example** (`OCTET STRING (SIZE (0..255))`, range=255, bit\_count=8):

| payload (hex)   | length | offset | length bits  | encoding hex          |
|-----------------|--------|--------|--------------|-----------------------|
| (empty)         | 0      | 0      | `00000000`   | `00`                  |
| `ab cd`         | 2      | 2      | `00000010`   | `02 ab cd`            |
| `ff ff ff`      | 3      | 3      | `00000011`   | `03 ff ff ff`         |

### `OCTET STRING (SIZE (n..n))` — fixed-size

No length determinant is written. Just the `n` payload bytes (8 bits each).

---

## BIT STRING — unconstrained and SIZE-constrained (§15)

A BIT STRING encodes a sequence of individual bits. UPER encodes the length (in bits) as
a determinant, then the bits themselves.

Three cases, depending on whether a SIZE constraint is present:

### `BIT STRING (SIZE (n..n))` — fixed-size

No length determinant. Write exactly `n` bits from the payload. Padding of the last byte
with zeros is handled by the caller (the SEQUENCE stream). The payload bytes must contain
at least `ceil(n / 8)` bytes; bits beyond `n` are ignored.

**Steps:**
1. Write exactly `n` bits from the payload, MSB first.

**Example** (`BIT STRING (SIZE (8..8))`, 8 bits fixed):

| payload (binary)  | payload (hex) | encoding hex |
|-------------------|---------------|--------------|
| `10110010`        | `b2`          | `b2`         |
| `00000000`        | `00`          | `00`         |
| `11111111`        | `ff`          | `ff`         |

### `BIT STRING (SIZE (lb..ub))` — range-constrained length

Length (in bits) is encoded as a constrained whole number in the range `0..(ub-lb)`, using
`ceil(log2(ub-lb+1))` bits. Then the `length` bits of the payload follow.

**Steps:**
1. `offset = bit_count − lb`
2. `range = ub − lb`
3. `bit_count_field = 32 − Integer.numberOfLeadingZeros(range)` bits
4. Write `offset` in `bit_count_field` bits.
5. Write `bit_count` bits of payload.

### Unconstrained `BIT STRING`

Length in bits is encoded with the semi-constrained integer format (1-byte count prefix),
then the bits. Not yet supported in this implementation.

---

## NULL — zero-bit type (§18.1)

A NULL value carries no information. In UPER, zero bits are written for a NULL field.
The SEQUENCE is encoded as if the NULL field is not there — adjacent fields are packed
directly against each other with no gap.

**Steps:**
1. Write nothing.

**Example** (`Marker ::= SEQUENCE { tag NULL }`):

Any Marker instance encodes to an empty byte array (`""`).

---

## UTF8String (SIZE (lb..ub)) — SIZE-constrained (§26.5)

When a UTF8String carries a SIZE constraint, the byte length of the UTF-8 encoding is
constrained. The length is encoded as a constrained whole number (offset from lb), then
the UTF-8 bytes follow.

**Note:** The SIZE constraint applies to the number of UTF-8 bytes, not to the character
count. For schemas that use ASCII-only content, these are equivalent (1 byte per char).

**Steps:**
1. Encode the string as UTF-8 bytes; let `n` = byte count.
2. `offset = n − lb`
3. `range = ub − lb`
4. `bit_count = 32 − Integer.numberOfLeadingZeros(range)` bits for the length field
5. Write `offset` in `bit_count` bits.
6. Write each of the `n` UTF-8 bytes as 8 bits.

For `UTF8String (SIZE (n..n))` (fixed size): no length field, just the bytes.

**Example** (`UTF8String (SIZE (1..64))`, range=63, bit_count=6):

| value     | n  | offset | bits   | encoding (hex, truncated)   |
|-----------|----|--------|--------|-----------------------------|
| `"hi"`    | 2  | 1      | `000001` | `046869` (6 bits + 2 bytes) |
| `"hello"` | 5  | 4      | `000100` | `106865...` (6 bits + 5 bytes)|

---

## IA5String — 7-bit ASCII (§27)

IA5String is a known-multiplier character string type with alphabet {0..127} (128 chars).
In UPER, each character is encoded as its ASCII value in 7 bits.

For a SIZE-constrained IA5String, the length (in characters) is encoded as a constrained
whole number using the minimum number of bits; for unconstrained, a semi-constrained integer.

**Steps (SIZE-constrained, range `ub - lb`):**
1. `offset = length − lb`
2. `bit_count_field = 32 − Integer.numberOfLeadingZeros(ub − lb)` bits for the length
3. Write `offset` in `bit_count_field` bits
4. For each character, write its ASCII value (0..127) in 7 bits, MSB first

**Example** (`IA5String (SIZE (1..32))`, range=31, bit\_count\_field=5):

| value   | length | offset | length bits | char bits          | total bits | hex       |
|---------|--------|--------|-------------|------------------  |------------|-----------|
| `"hi"`  | 2      | 1      | `00001`     | `1101000 1101001`  | 5+14=19    | `0d0d200` |

---

## VisibleString — printable ASCII (§27)

VisibleString is a known-multiplier type with alphabet {32..126} (95 printable chars).
Each character is encoded as `charValue − 32` in 7 bits (since 2^6=64 < 95 ≤ 2^7=128).

**Steps (SIZE-constrained):** identical to IA5String except each character is encoded
as `charValue − 32` (not the raw ASCII value).

**Example** (`VisibleString (SIZE (1..32))`, range=31, bit\_count\_field=5):

| value   | offset | char 'h' (104) | char 'i' (105)  |
|---------|--------|----------------|-----------------|
| `"hi"`  | 1      | `104-32=72` → `1001000` | `105-32=73` → `1001001` |

---

## Type Reference — delegated encoding

A field whose type is a user-defined name (e.g. `major VersionSingle`) encodes exactly as
the referenced type's own encoding with no wrapper, length prefix, or tag added. The bit
stream produced by encoding a type-reference field is identical to encoding the referenced
type's value as a standalone message.

**Example** (`ProtocolVersion ::= SEQUENCE { major VersionSingle, minor VersionSingle }`
where `VersionSingle ::= INTEGER (0..255)`):

The bit stream for `ProtocolVersion(major=1, minor=0)` is simply the constrained-integer
encoding of `major` (8 bits) concatenated with the constrained-integer encoding of `minor`
(8 bits) — no difference from having written `major INTEGER (0..255)` inline:

| field  | encoded as | bits       | hex |
|--------|-----------|------------|-----|
| major  | INTEGER (0..255), value=1 | `00000001` | `01` |
| minor  | INTEGER (0..255), value=0 | `00000000` | `00` |

Full `ProtocolVersion(1, 0)` encoding: `0100`

---

## CHOICE — tagged union (§23)

A CHOICE with N root alternatives and no extension marker encodes the zero-based index
of the selected alternative as a constrained whole number in the range `0..(N-1)`
(identical formula to ENUMERATED), immediately followed by the encoding of the selected
alternative's value — with no tag, length prefix, or padding between the index and the
payload. An alternative whose type is NULL contributes zero payload bits (per the NULL
rule above); a SEQUENCE-typed alternative encodes exactly as that SEQUENCE would as a
standalone value (per the Type Reference / SEQUENCE rules above).

**Steps:**
1. `index` = zero-based position of the selected alternative in declaration order
2. `range = N − 1`
3. `bit_count = 32 − Integer.numberOfLeadingZeros(range)` (0 if N ≤ 1)
4. Write `index` in exactly `bit_count` bits, MSB first
5. Encode the selected alternative's value using that alternative's own type rules,
   immediately following the index bits (no padding)

**Example** (`Propulsion ::= CHOICE { gasoline GasEngine, electric ElectricMotor, none NULL }`,
N=3, range=2, bit_count=2):

| alternative | index | index bits |
|-------------|-------|------------|
| gasoline    | 0     | `00`       |
| electric    | 1     | `01`       |
| none        | 2     | `10`       |

For `none`, the encoding is just the 2 index bits (`10`) — NULL contributes nothing
further. For `gasoline`/`electric`, the 2 index bits are followed immediately by the
chosen `GasEngine`/`ElectricMotor` SEQUENCE's own field encoding, with no gap.

**`Vehicle` SEQUENCE encoding** (`id INTEGER (0..65535), propulsion Propulsion`, where
`GasEngine ::= SEQUENCE { displacementCc INTEGER (0..8000), cylinders INTEGER (1..16) }`
and `ElectricMotor ::= SEQUENCE { powerKw INTEGER (0..1000), batteryKwh INTEGER (0..500) }`):

`id` is a 16-bit constrained whole number (range 65535). The `propulsion` field is the
2-bit CHOICE index followed by the selected alternative's own field bits — see
`golden-tests/vehicle/*.hex` for exact byte-level values verified against the
`asn1tools` oracle.

---

## SEQUENCE — OPTIONAL components (preamble bitmap, §19)

A SEQUENCE with one or more `OPTIONAL` (or `DEFAULT` — see the next section)
components carries a **preamble**: one presence bit per such component,
written before any field content, in declaration order. Mandatory
components do not get a preamble bit. After the preamble, fields are
encoded in declaration order as usual — mandatory fields are always
encoded; an `OPTIONAL` field is encoded only if its presence bit was `1`.
An absent `OPTIONAL` field contributes nothing beyond its own preamble bit.

**Steps:**
1. Collect the SEQUENCE's `OPTIONAL`/`DEFAULT` components, in declaration
   order.
2. Write one bit per such component: `1` if the field is present
   (non-null), `0` if absent.
3. Encode the fields themselves in declaration order: mandatory fields
   always; `OPTIONAL` fields only if their presence bit was `1` — using
   that field's own type encoding, immediately following the preamble
   with no gap.

**Example** (`Contact ::= SEQUENCE { id INTEGER (0..255), age INTEGER
(0..255) OPTIONAL }`; `id` is an 8-bit constrained whole number, range 255;
`age` is the same, gated by a 1-bit preamble):

| input               | preamble | id bits    | age bits   | hex      |
|---------------------|----------|------------|------------|----------|
| id=1, age=30        | `1`      | `00000001` | `00011110` | `808f00` |
| id=2, age absent    | `0`      | `00000010` | (none)     | `0100`   |

---

## SEQUENCE — DEFAULT components (§11.5)

A `DEFAULT` component shares the same preamble mechanism as `OPTIONAL`
(previous section) — it gets a presence bit in the preamble bitmap — but
the bit means something different: `1` if the value **differs** from the
declared default, `0` if the value **equals** the default. When the bit is
`0`, no value bits are written at all; the decoder reconstructs the field
by substituting the default value.

**Steps:**
1. Collect the SEQUENCE's `OPTIONAL`/`DEFAULT` components, in declaration
   order (same preamble as the previous section).
2. For each `DEFAULT` component, write `1` in the preamble if the value is
   different from the default, `0` if it equals the default.
3. Encode the fields themselves in declaration order: mandatory fields
   always; a `DEFAULT` field only if its preamble bit was `1` — using that
   field's own type encoding.
4. On decode, if a `DEFAULT` field's preamble bit is `0`, no bits are read
   for it; its value is the declared default.

**Example** (`Settings ::= SEQUENCE { id INTEGER (0..255), volume INTEGER
(0..100) DEFAULT 50, muted BOOLEAN DEFAULT FALSE }`; `id` is an 8-bit
constrained whole number; `volume` has range 100, bit_count = 7, gated by a
1-bit preamble; `muted` is a 1-bit boolean, gated by a 1-bit preamble):

| input                          | preamble | id bits    | volume bits | muted bit | hex      |
|---------------------------------|----------|------------|-------------|-----------|----------|
| id=1, volume=50 (=default), muted=false (=default) | `00` | `00000001` | (none) | (none) | `0040`   |
| id=2, volume=80 (≠default), muted=true (≠default)  | `11` | `00000010` | `1010000` | `1` | `c0a840` |

**DEFAULT on `ENUMERATED` and string types** (`UTF8String`/`IA5String`/
`VisibleString`): the mechanism above is unchanged — the only thing that
varies per type is what "equals the default" means:
- `ENUMERATED`: compare the selected value's **zero-based ordinal** to the
  default's ordinal (not the identifier text). The value itself, when
  present, still encodes as the usual ordinal constrained-whole-number
  (§13).
- String types: compare the string **value** (not byte length or any
  other proxy). The value itself, when present, still encodes exactly as
  the type's own unconstrained/SIZE-constrained rules (see the UTF8String/
  IA5String/VisibleString sections above).

**Example** (`Profile ::= SEQUENCE { id INTEGER (0..255), status
ENUMERATED { pending, active, inactive } DEFAULT active, nickname
UTF8String DEFAULT "anonymous" }`; `status` has 3 values, bit_count = 2,
default ordinal = 1 (`active`); `nickname` is unconstrained UTF8String):

| input                                                          | preamble | id bits    | status bits | nickname bits                | hex                |
|------------------------------------------------------------------|----------|------------|-------------|-------------------------------|--------------------|
| id=1, status=active (=default), nickname="anonymous" (=default)  | `00`     | `00000001` | (none)      | (none)                         | `0040`             |
| id=2, status=pending (ordinal 0, ≠default), nickname="Alice" (≠default) | `11` | `00000010` | `00`        | `00000101` + 5 UTF-8 bytes    | `c0805416c6963650` |

---

## SEQUENCE OF — element count + elements (§19, length determinant per §10.7/§10.9)

A `SEQUENCE OF T` value encodes the number of elements as a length determinant,
then each element in declaration order using that element's own type rules — no
tags, lengths, or padding between elements. Three cases, depending on whether a
SIZE constraint is present, exactly mirroring the OCTET STRING/BIT STRING cases
above.

### Unconstrained `SEQUENCE OF T` (no SIZE)

The element count is encoded with the same §10.7 length determinant used by
unconstrained UTF8String/OCTET STRING: 1 byte if count < 128 (high bit 0); 2 bytes
if 128 ≤ count < 16384 (`0x80 | count>>8`, `count&0xFF`).

**Steps:**
1. `n` = element count
2. Write the length determinant for `n` (1 or 2 bytes per §10.7)
3. Encode each of the `n` elements using its own type's encoding, in order

**Example** (`SEQUENCE OF INTEGER (0..255)`, each element an 8-bit constrained whole number):

| value    | n | length byte | element bits            | hex      |
|----------|---|-------------|--------------------------|----------|
| `[]`     | 0 | `00`        | (none)                   | `00`     |
| `[1, 2]` | 2 | `02`        | `00000001` `00000010`    | `020102` |

### `SEQUENCE (SIZE (lb..ub)) OF T` — range-constrained count

The element count is encoded as a constrained whole number — offset from `lb`, in
`bit_count = ceil(log2(ub-lb+1))` bits — then the elements follow with no gap.

**Steps:**
1. `offset = n − lb`
2. `range = ub − lb`
3. `bit_count = 32 − Integer.numberOfLeadingZeros(range)` (0 if `range == 0`, see fixed-size case below)
4. Write `offset` in `bit_count` bits
5. Encode each of the `n` elements using its own type's encoding, in order

**Example** (`SEQUENCE (SIZE (1..4)) OF INTEGER (0..255)`, range=3, bit_count=2):

| value      | n | offset | count bits | element bits                    | hex      |
|------------|---|--------|------------|----------------------------------|----------|
| `[7]`      | 1 | 0      | `00`       | `00000111`                       | `01c0`   |
| `[7, 9]`   | 2 | 1      | `01`       | `00000111` `00001001`            | `41c240` |

### `SEQUENCE (SIZE (n..n)) OF T` — fixed-size

No length determinant is written — the count is fully determined by the constraint.
Just the `n` elements follow, back to back.

**Example** (`SEQUENCE (SIZE (2..2)) OF INTEGER (0..255)`):

| value     | element bits            | hex    |
|-----------|--------------------------|--------|
| `[5, 6]`  | `00000101` `00000110`   | `0506` |

### Element types

The element's own type rules apply unchanged — a `SEQUENCE OF UTF8String` element
is the usual unconstrained/SIZE-constrained UTF8String encoding (its own internal
length determinant, distinct from the outer SEQUENCE OF's element-count determinant);
a `SEQUENCE OF Track` (a named SEQUENCE type) element encodes exactly as that
SEQUENCE's own field encoding, per the Type Reference rule above.

**`Playlist` SEQUENCE encoding** (`tags SEQUENCE OF UTF8String, tracks SEQUENCE
(SIZE (1..64)) OF Track, topThree SEQUENCE (SIZE (3..3)) OF Track`, where `Track
::= SEQUENCE { title UTF8String }`): `tags` uses the unconstrained case above with
UTF8String elements; `tracks` uses the range-constrained case (range=63, bit_count=6)
with `Track` elements; `topThree` uses the fixed-size case (3 elements, no length
bits) with `Track` elements. See `golden-tests/playlist/*.hex` for exact byte-level
values verified against the `asn1tools` oracle.

---

## Value assignments — named constants (no wire-format impact)

A module-level value assignment (e.g. `maxVolume INTEGER ::= 100`) declares a
named constant that may be used afterward in place of a literal number in any
constraint bound — an INTEGER range (`INTEGER (0..maxVolume)`) or a SIZE
constraint (`SEQUENCE (SIZE (0..maxTags)) OF UTF8String`).

This is a **compile-time substitution only** — it has no effect whatsoever on the
UPER bit-level encoding. `INTEGER (0..maxVolume)` where `maxVolume INTEGER ::= 100`
encodes byte-identically to `INTEGER (0..100)` written directly: same range, same
`bit_count`, same offset formula. The name is resolved to its literal value before
any encoding decision is made; nothing downstream of parsing is aware a named
constant was ever used.

**`Settings` SEQUENCE encoding** (`maxVolume INTEGER ::= 100`, `maxTags INTEGER ::=
10`, `Settings ::= SEQUENCE { volume INTEGER (0..maxVolume), tags SEQUENCE (SIZE
(0..maxTags)) OF UTF8String }`): `volume` is an ordinary 7-bit constrained whole
number (range 100, bit_count = 7); `tags` is an ordinary range-constrained
SEQUENCE OF (range 10, bit_count = 4) of unconstrained UTF8String elements — see
the SEQUENCE OF and INTEGER (lb..ub) sections above for the algorithms themselves.
See `golden-tests/limits/*.hex` for exact byte-level values verified against the
`asn1tools` oracle.

---

## Adding new rules

When a new construct is implemented, document it here before moving on to the code
generator. Follow the existing section structure:

1. Construct name, X.691 clause reference
2. Step-by-step algorithm
3. Worked example table with at least the boundary values
