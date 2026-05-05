FROM debian:13-slim AS builder
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    autoconf \
    automake \
    libtool \
    pkg-config \
    ca-certificates \
    git
RUN git clone --depth 1 https://github.com/vlm/asn1c.git asn1c \
    && cd asn1c \
    && autoreconf -iv \
    && ./configure \
    && make && make install

FROM debian:13-slim
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    libc6-dev \
    make \
    python3 \
    xxd \
    && rm -rf /var/lib/apt/lists/*
COPY --from=builder /usr/local/bin/asn1c /usr/local/bin/asn1c
COPY --from=builder /usr/local/share/asn1c /usr/local/share/asn1c

COPY entrypoint.sh /usr/local/bin/entrypoint.sh
COPY json2xer.py /usr/local/bin/json2xer.py
RUN chmod +x /usr/local/bin/entrypoint.sh /usr/local/bin/json2xer.py

WORKDIR /work

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]