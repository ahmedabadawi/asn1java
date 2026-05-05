FROM python:3.12-slim
RUN pip install --no-cache-dir asn1tools
COPY encode.py /usr/local/bin/encode.py
RUN chmod +x /usr/local/bin/encode.py
WORKDIR /work
ENTRYPOINT ["python3", "/usr/local/bin/encode.py"]
