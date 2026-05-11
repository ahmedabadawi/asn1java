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

| value | n   | encoding  |
|-------|-----|-----------|
| 0     | `01`| `01 00`   |
| 1     | `01`| `01 01`   |
| 2     | `01`| `01 02`   |
| 24    | `01`| `01 18`   |
| 255   | `01`| `01 ff`   |
| 256   | `02`| `02 01 00`|

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

| value | offset | bits (binary) | notes |
|-------|--------|---------------|-------|
| 10    | 0      | `0000`        | lower bound |
| 15    | 5      | `0101`        | |
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

## Adding new rules

When a new construct is implemented, document it here before moving on to the code
generator. Follow the existing section structure:

1. Construct name, X.691 clause reference
2. Step-by-step algorithm
3. Worked example table with at least the boundary values
