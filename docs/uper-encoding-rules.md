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
