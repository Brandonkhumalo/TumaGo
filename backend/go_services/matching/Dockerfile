FROM golang:1.22-alpine AS builder
WORKDIR /app
COPY go.mod ./
RUN go mod download
COPY . .
RUN go mod tidy
RUN CGO_ENABLED=0 go build -o matching-service .

FROM alpine:3.19
RUN apk add --no-cache ca-certificates
COPY --from=builder /app/matching-service /matching-service
ENTRYPOINT ["/matching-service"]
