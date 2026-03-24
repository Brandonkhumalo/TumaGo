# ── Stage 1: Install dependencies ────────────────────────────────────────
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --only=production=false

# ── Stage 2: Build the Next.js app ──────────────────────────────────────
FROM node:20-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .

# Production env vars baked into the build:
# - NEXT_PUBLIC_API_URL is inlined into client JS bundles (browser uses relative /api/v1)
# - BACKEND_URL configures server-side rewrites to reach the Go gateway via Docker network
ENV NEXT_PUBLIC_API_URL=/api/v1
ENV BACKEND_URL=http://gateway:8080

RUN npm run build

# ── Stage 3: Production runner ──────────────────────────────────────────
FROM node:20-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production

RUN addgroup --system --gid 1001 nodejs && \
    adduser --system --uid 1001 nextjs

# Copy standalone server + static assets
COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs

EXPOSE 3000
ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -qO- http://localhost:3000/ || exit 1

CMD ["node", "server.js"]
