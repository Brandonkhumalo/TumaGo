FROM golang:1.22-alpine AS builder
WORKDIR /app
COPY go.mod ./
RUN go mod download
COPY . .
RUN go mod tidy
RUN CGO_ENABLED=0 go build -o matching-service .

FROM alpine:3.19
RUN apk add --no-cache ca-certificates
RUN addgroup -S appuser && adduser -S appuser -G appuser
COPY --from=builder /app/matching-service /matching-service
USER appuser

HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
    CMD wget -qO- http://localhost:8002/health || exit 1

ENTRYPOINT ["/matching-service"]
