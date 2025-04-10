FROM eclipse-temurin:22-jdk

WORKDIR /app

COPY build/libs/QilletniToolchain.jar .
COPY scripts/qilletni .

RUN chmod +x ./qilletni

WORKDIR /data

ENTRYPOINT ["/app/qilletni"]
