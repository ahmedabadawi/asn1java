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

## Adding new rules

When a new construct is implemented, document it here before moving on to the code
generator. Follow the existing section structure:

1. Construct name, X.691 clause reference
2. Step-by-step algorithm
3. Worked example table with at least the boundary values
