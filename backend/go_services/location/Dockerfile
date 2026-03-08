FROM golang:1.22-alpine AS builder
WORKDIR /app
COPY go.mod ./
RUN go mod download
COPY . .
RUN go mod tidy
RUN CGO_ENABLED=0 go build -o location-service .

FROM alpine:3.19
RUN apk add --no-cache ca-certificates
COPY --from=builder /app/location-service /location-service
ENTRYPOINT ["/location-service"]
