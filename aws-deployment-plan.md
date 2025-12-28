# SentimentScribe — Docker + AWS Deployment Plan (EC2 backend, S3/CloudFront frontend, SSM secrets)

This is a **planning + checklist** document only. It does **not** deploy anything automatically.

**Repo structure assumed:** `backend/` (Spring Boot + Flyway + PostgreSQL), `frontend/` (Vite/React).

**How to use this plan**

1) Follow phases **in order** to avoid rework.  
2) In each phase, do **YOU DO THIS** items manually.  
3) When you’re ready for repo edits, ask Codex to implement the **CODEX DOES THIS** items **one step at a time**.  
4) After each Codex implementation step, Codex must document the exact edits in `aws-deployment-changes.md`.

---

## PHASE 0 — Prerequisites & Terminology (read once)

### Terminology (1–2 lines each)

- **EC2**: A virtual server in AWS (“a computer in the cloud”) where you’ll run Docker containers.
- **Security Group**: A firewall for EC2 that controls which inbound/outbound network traffic is allowed.
- **Key Pair**: SSH credentials for logging into EC2 (private key `.pem` + stored public key).
- **VPC/Subnet**: AWS networking boundaries; a VPC is your private network, a subnet is a slice of it (public subnet = has a route to the internet).
- **EIP (Elastic IP)**: A “static” public IPv4 you can keep even if the instance is replaced; helps DNS stay stable.
- **Docker image vs container**: Image = immutable “app package”; Container = a running instance of an image.
- **S3 bucket**: Object storage for files (your built frontend files will live here).
- **CloudFront distribution**: AWS CDN that serves content globally and provides HTTPS; it reads files from an **origin** (your S3 bucket).
- **Origin**: The source CloudFront fetches from (S3 bucket in this plan).
- **Invalidation**: Tells CloudFront to stop serving cached files and fetch fresh ones (use after redeploys, especially `index.html`).
- **ACM certificate**: AWS-managed TLS cert for HTTPS; **CloudFront requires the cert in `us-east-1`**.
- **Route 53 hosted zone**: A DNS “control panel” in AWS for your domain’s records.
- **DNS records (A/AAAA/CNAME)**: A = IPv4 mapping; AAAA = IPv6 mapping; CNAME = alias one hostname to another.
- **SSM Parameter Store vs Secrets Manager**: Both store config/secrets; Parameter Store is simpler/cheaper for basic use; Secrets Manager has rotation features. This plan uses **SSM Parameter Store**.
- **CORS**: Browser rule that controls which frontend origins are allowed to call your API; your frontend domain must be explicitly allowed in backend config.

### Things you must have before starting

- [ ] **AWS account** created + **billing** enabled (you must add a payment method).
- [ ] **MFA** enabled on your AWS root account (security requirement).
- [ ] Local tools installed: **Docker Desktop**, **Git**, and **Node.js** (for frontend builds).
- [ ] Your Git working tree is clean: no uncommitted work you’d be sad to lose.
- [ ] Optional but recommended: a **domain decision**, e.g. `example.com` with `api.example.com` for backend and `app.example.com` for frontend.
- [ ] Your secrets ready (you will store them in SSM later):
  - Spotify: `SPOTIFY_CLIENT_ID`, `SPOTIFY_CLIENT_SECRET`
  - TMDb: `TMDB_API_KEY`
  - JWT: `SENTIMENTSCRIBE_JWT_SECRET` (generate a long random string)
  - DB creds: `POSTGRES_USER`, `POSTGRES_PASSWORD`

### Assumptions / defaults (change if you prefer)

- **AWS region for most resources:** `us-east-1` (keeps things simple because CloudFront’s ACM cert must be in `us-east-1` anyway).
- **Backend deployment strategy (chosen): Option A — ECR (recommended)**
  - Why: safer than building on the server, supports versioning/rollbacks, avoids SCP of images, and is the normal AWS pattern.
- **Production DB strategy (chosen): Option A — Postgres as a Docker container on the same EC2 (MVP)**
  - Why: simplest operationally for first deployment; you can migrate to RDS later.
- **HTTPS termination strategy (chosen): Option A — Caddy reverse proxy on EC2**
  - Why: easiest “just enough” HTTPS (automatic Let’s Encrypt), no ALB complexity for MVP.

### YOU DO THIS

#### 0.1 If you don’t have an AWS account yet

1) Go to https://aws.amazon.com/ → click **Create an AWS Account**
2) Complete signup, then log in to the AWS Console: https://console.aws.amazon.com/
3) AWS Console → top-right account menu → **Billing and Cost Management**
4) Billing → **Payment preferences** / **Payment methods** → add a payment method

#### 0.2 Secure your AWS account (minimum)

1) AWS Console → top-right account menu → **Security credentials**
2) Under “Multi-factor authentication (MFA)” → **Assign MFA device**
3) Use an authenticator app and complete setup

Recommended (do early; helps later when using AWS CLI):

1) AWS Console → **IAM** → **Users** → **Create user**
2) Username: `yourname-admin`
3) “Provide user access to the AWS Management Console” → **On**
4) Permissions: **Attach policies directly** → check `AdministratorAccess` (tighten later)
5) Complete creation and save the sign-in URL
6) Enable MFA for this IAM user as well

#### 0.3 Local machine readiness

- Install Docker Desktop: https://www.docker.com/products/docker-desktop/
- Install Git: https://git-scm.com/downloads
- Install Node.js (LTS): https://nodejs.org/
- (Recommended) Install AWS CLI v2: https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html
- Confirm repo is clean:
  - `git status` (should show “working tree clean”)

#### 0.4 Decide your domains (recommended)

- Choose:
  - Frontend: `app.example.com`
  - Backend: `api.example.com`
- You can buy from Namecheap / Google Domains successor / Route 53 registration.

### CODEX DOES THIS

No repo changes required in this phase.

### VERIFY

- [ ] You can log into AWS Console (not just the signup screen)
- [ ] Billing is enabled (payment method attached)
- [ ] MFA is enabled for your root account (and IAM admin user, if created)
- [ ] `git status` is clean
- [ ] You have all required secrets ready to store in SSM (Phase 5)

---

## PHASE 1 — Containerize Backend + Compose for Local Prod-Parity

### YOU DO THIS (local)

1) Ensure Docker is running.
2) From repo root, run local parity checks using the compose file(s) that Codex will prepare:
   - Build images: `docker compose build`
   - Start: `docker compose up -d`
   - Watch logs:
     - `docker compose logs -f postgres`
     - `docker compose logs -f backend`
3) Confirm Flyway migrations applied (you should see Flyway logs during backend start).
4) Smoke test the API locally:
   - `http://localhost:8080/api/health` should return `{"status":"ok"}`.

### CODEX DOES THIS (repo changes)

Codex Step 1 — Add production backend image build

- Add `backend/Dockerfile` (multi-stage build) to produce a small runtime image that runs the Spring Boot jar.
- Ensure container uses env vars already supported by `backend/src/main/resources/application*.yml`:
  - DB: `POSTGRES_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
  - JWT: `SENTIMENTSCRIBE_JWT_SECRET`, `SENTIMENTSCRIBE_JWT_ISSUER`, `SENTIMENTSCRIBE_JWT_TTL_SECONDS`
  - APIs: `SPOTIFY_CLIENT_ID`, `SPOTIFY_CLIENT_SECRET`, `TMDB_API_KEY`
  - CORS: `SENTIMENTSCRIBE_CORS_ORIGIN`, `SENTIMENTSCRIBE_CORS_ORIGIN_2` (and possibly a list-friendly variant)

Codex Step 2 — Create/adjust “prod-parity” Compose

- Update `docker-compose.yml` and/or add a new file like `docker-compose.prod.yml` so you can run:
  - backend + postgres with **no code bind-mounts**
  - postgres data persisted via a **named volume**
  - backend reads config via env vars (no secrets hardcoded)
- Make sure Flyway still runs on startup (it’s enabled in `application-postgres.yml`).

### VERIFY

- [ ] `docker compose ps` shows `postgres` healthy and `backend` running.
- [ ] `GET http://localhost:8080/api/health` returns `200`.
- [ ] Stop + start again and confirm DB persists:
  - `docker compose down`
  - `docker compose up -d`
  - Confirm app still starts without re-initializing data unexpectedly.

---

## PHASE 2 — Create AWS Infrastructure (EC2 + Network/Security)

### YOU DO THIS (AWS Console click-by-click)

#### 2.1 Create an IAM role for the EC2 instance (so it can read SSM + pull from ECR)

1) AWS Console → **IAM** → **Roles** → **Create role**
2) Trusted entity type: **AWS service**
3) Use case: **EC2** → click **Next**
4) Permissions: attach these managed policies (search and check the box):
   - `AmazonSSMManagedInstanceCore` (lets you manage the instance via Systems Manager)
   - `AmazonEC2ContainerRegistryReadOnly` (lets the instance pull images from ECR)
5) Click **Next**
6) Role name: `SentimentScribeEc2Role` → **Create role**

Optional “least privilege” later: replace ECR/SSM managed policies with a custom policy restricted to your parameter path and ECR repo.

#### 2.2 Launch the EC2 instance

1) AWS Console → **EC2** → **Instances** → **Launch instances**
2) Name: `sentimentscribe-prod-1`
3) Application and OS Images (AMI):
   - Choose **Ubuntu Server 22.04 LTS** (x86_64)
4) Instance type (recommendation):
   - Start with **t3.small** (2 GB RAM). If NLP workloads feel slow, upgrade to **t3.medium**.
5) Key pair (login):
   - Click **Create new key pair**
   - Name: `sentimentscribe-prod`
   - Type: **RSA**
   - File format: **.pem**
   - Click **Create key pair** (download happens once; store it safely)
6) Network settings → click **Edit**
   - VPC: leave default (unless you know you want custom)
   - Subnet: choose a **public subnet**
   - Auto-assign public IP: **Enable** (you’ll attach an EIP next, but this helps during setup)
   - Firewall (security groups): **Create security group**
     - Name: `sentimentscribe-prod-sg`
     - Description: `SSH restricted + HTTP/HTTPS public`
     - Inbound rules:
       - **SSH** / TCP / **22** / Source: **My IP** (not “Anywhere”)
       - **HTTP** / TCP / **80** / Source: **Anywhere-IPv4** (`0.0.0.0/0`)
       - **HTTPS** / TCP / **443** / Source: **Anywhere-IPv4** (`0.0.0.0/0`)
     - Do **NOT** add 5432 (Postgres) and do **NOT** add 8080 to public inbound rules.
7) Configure storage:
   - Root volume size: **30 GiB** (gp3)
8) Advanced details:
   - IAM instance profile: select **`SentimentScribeEc2Role`**
9) Click **Launch instance**

#### 2.3 (Recommended) Allocate and attach an Elastic IP (EIP)

Why: your public IP won’t change across instance stops/starts and is friendlier for DNS.

1) AWS Console → **EC2** → **Elastic IPs** → **Allocate Elastic IP address** → **Allocate**
2) Select the new EIP → **Actions** → **Associate Elastic IP address**
3) Resource type: **Instance**
4) Instance: select `sentimentscribe-prod-1`
5) Private IP address: leave default → **Associate**

#### 2.4 Find your public address

1) AWS Console → **EC2** → **Instances** → click your instance
2) Copy:
   - **Public IPv4 address** (or your EIP)
   - **Public IPv4 DNS**

### CODEX DOES THIS

No repo changes required in this phase.

### VERIFY

- [ ] Instance state: **Running**
- [ ] Security group inbound rules match exactly (SSH from *your IP* only; 80/443 open; no 5432/8080)
- [ ] You have the `.pem` file saved somewhere safe

---

## PHASE 3 — Install Docker on EC2 + Run Backend Container (via ECR)

### YOU DO THIS (EC2 terminal)

#### 3.1 SSH into EC2

On your local machine (PowerShell), from the folder containing the `.pem`:

1) Fix key file permissions (Windows may not require this; if SSH complains, use WSL or tighten ACLs).
2) SSH:
   - `ssh -i sentimentscribe-prod.pem ubuntu@<YOUR_EIP_OR_PUBLIC_IP>`

If you prefer, you can use AWS Console → EC2 → Instance → **Connect** → **EC2 Instance Connect** (SSH from browser).

#### 3.2 Install Docker Engine + Compose plugin (Ubuntu 22.04)

Run on EC2:

1) `sudo apt-get update`
2) `sudo apt-get install -y ca-certificates curl gnupg`
3) `sudo install -m 0755 -d /etc/apt/keyrings`
4) `curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg`
5) `echo \"deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable\" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null`
6) `sudo apt-get update`
7) `sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin`
8) `sudo systemctl enable --now docker`
9) Add your user to the `docker` group so you don’t need `sudo`:
   - `sudo usermod -aG docker ubuntu`
   - Log out and back in (or run `newgrp docker`)
10) Verify:
   - `docker --version`
   - `docker compose version`

#### 3.3 Create directories on EC2 for deployment

Run on EC2:

- `mkdir -p ~/sentimentscribe/deploy`
- `mkdir -p ~/sentimentscribe/data/postgres`

#### 3.4 Create an ECR repository (once)

1) AWS Console → **ECR** → **Repositories** → **Create repository**
2) Visibility: **Private**
3) Repository name: `sentimentscribe-backend`
4) Click **Create repository**

#### 3.5 Push your backend image to ECR (from your local machine)

You will run AWS CLI commands locally. If you don’t have AWS CLI installed:

- Install: https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html
- Configure: `aws configure` (use an IAM user with limited permissions; avoid daily use of root)

Then:

1) Authenticate Docker to ECR:
   - `aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com`
2) Build the backend image using the production Dockerfile Codex adds:
   - `docker build -t sentimentscribe-backend:prod -f backend/Dockerfile backend`
3) Tag for ECR:
   - `docker tag sentimentscribe-backend:prod <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/sentimentscribe-backend:prod`
4) Push:
   - `docker push <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/sentimentscribe-backend:prod`

#### 3.6 Pull and run on EC2 (initially without HTTPS proxy; HTTPS comes in Phase 6)

On EC2:

1) Install AWS CLI if needed: `sudo apt-get install -y awscli`
2) Confirm the instance has permissions (IAM role attached in Phase 2):
   - `aws sts get-caller-identity`
3) Login to ECR:
   - `aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com`

Do **not** run the backend publicly on 8080 long-term; the final setup will hide it behind Caddy on 80/443.

### CODEX DOES THIS

Codex Step 3 — Add EC2 “prod” Compose + helper scripts

- Add `deploy/ec2/docker-compose.prod.yml` (or similar) containing:
  - `postgres` container with a named volume or host mount for persistence
  - `backend` container pulling from ECR
  - no public port mapping for Postgres
  - backend exposed only to the reverse proxy network (Phase 6)
- Add a small script to fetch SSM parameters and generate an env file (Phase 5), then start `docker compose` safely.

### VERIFY

- [ ] `docker ps` shows expected containers running.
- [ ] `docker compose logs -f backend` shows successful startup and Flyway migration checks.
- [ ] If you temporarily mapped 8080 for smoke testing, `curl http://localhost:8080/api/health` works on EC2.

---

## PHASE 4 — PostgreSQL in Production (MVP: container + volume on EC2)

### YOU DO THIS

#### 4.1 Ensure Postgres persists across container restarts

In your production compose file (Codex will provide), ensure one of these is true:

- A named Docker volume like `sentimentscribe_pgdata:/var/lib/postgresql/data`, OR
- A host directory mount like `~/sentimentscribe/data/postgres:/var/lib/postgresql/data`

#### 4.2 Keep Postgres private

- Do **not** publish `5432` in Docker (`ports:`) on EC2.
- Do **not** open `5432` in the EC2 security group.

#### 4.3 Minimal backup/restore (MVP)

Backup (on EC2; produces a file you can copy off the server):

- `docker exec -t <postgres_container_name> pg_dump -U <POSTGRES_USER> -d <POSTGRES_DB> > ~/sentimentscribe/backups/backup-$(date +%F).sql`

Restore (dangerous; do only when you mean it):

- `cat ~/sentimentscribe/backups/backup-YYYY-MM-DD.sql | docker exec -i <postgres_container_name> psql -U <POSTGRES_USER> -d <POSTGRES_DB>`

#### 4.4 Debug connection (keep it private)

To connect for debugging without exposing the port publicly:

- SSH tunnel from local:
  - `ssh -i sentimentscribe-prod.pem -L 5432:localhost:5432 ubuntu@<EIP>`
- In a second terminal, connect your DB tool to `localhost:5432` (traffic is encrypted via SSH).

### CODEX DOES THIS

Codex Step 4 — Make prod DB settings explicit

- Ensure prod compose defines:
  - `POSTGRES_DB=sentiment_scribe`
  - `POSTGRES_USER` / `POSTGRES_PASSWORD` from env (ultimately SSM)
  - stable container name and a healthcheck
- Ensure backend uses `jdbc:postgresql://postgres:5432/sentiment_scribe` in container networking (not `localhost`).

### VERIFY

- [ ] Restart containers and confirm data persists.
- [ ] Confirm `5432` is not reachable from the public internet (only from inside the instance / SSH tunnel).

---

## PHASE 5 — Configure Secrets via SSM Parameter Store

### YOU DO THIS (AWS Console click-by-click)

#### 5.1 Create SSM parameters (prod)

1) AWS Console → **Systems Manager** → **Parameter Store**
2) Click **Create parameter**
3) Use this naming convention (path-like; easy to scope permissions):
   - `/sentimentscribe/prod/POSTGRES_URL`
   - `/sentimentscribe/prod/POSTGRES_USER`
   - `/sentimentscribe/prod/POSTGRES_PASSWORD`
   - `/sentimentscribe/prod/SENTIMENTSCRIBE_JWT_SECRET`
   - `/sentimentscribe/prod/SPOTIFY_CLIENT_ID`
   - `/sentimentscribe/prod/SPOTIFY_CLIENT_SECRET`
   - `/sentimentscribe/prod/TMDB_API_KEY`
   - `/sentimentscribe/prod/SENTIMENTSCRIBE_CORS_ORIGINS` (single comma-separated string for prod)

For each parameter:

1) Name: paste exact name (example: `/sentimentscribe/prod/POSTGRES_PASSWORD`)
2) Tier: **Standard**
3) Type:
   - Use **SecureString** for secrets (passwords, JWT secret, API keys)
   - Use **String** for non-sensitive (CORS origins), though SecureString is also acceptable
4) Value: paste the secret
5) Click **Create parameter**

**CORS value suggestion** (comma-separated):

- `https://<YOUR_CLOUDFRONT_DOMAIN>,https://app.example.com,http://localhost:3000`

Common mistakes to avoid:

- Putting your raw secrets into `docker-compose.yml` or committing `.env` files.
- Using the wrong region (Parameter Store is region-specific; keep consistent).

#### 5.2 Ensure the EC2 instance can read parameters

If you used `AmazonSSMManagedInstanceCore`, you still need permission to read Parameter Store values.

Simplest approach (works for MVP):

1) AWS Console → **IAM** → **Roles** → `SentimentScribeEc2Role`
2) **Add permissions** → **Attach policies**
3) Attach `AmazonSSMReadOnlyAccess` (broad; tighten later)

Better approach (recommended later): a custom policy allowing `ssm:GetParameter(s)` only for `/sentimentscribe/prod/*`.

### CODEX DOES THIS

Codex Step 5 — Make backend config “SSM-friendly”

- Ensure backend supports a **single env var** like `SENTIMENTSCRIBE_CORS_ORIGINS` (comma-separated) in addition to the existing two-origin setup, so prod can be configured with one parameter.
- Ensure no secrets are hardcoded in repo config files beyond safe dev defaults.

Codex Step 6 — Add EC2 script to render env from SSM

- Add `deploy/ec2/render-env-from-ssm.sh` (or similar) that:
  - fetches each parameter with `aws ssm get-parameter --with-decryption`
  - writes a local `.env.prod` on EC2 with `KEY=value` lines
  - sets strict file permissions (e.g., `chmod 600 .env.prod`)

### VERIFY

On EC2:

- [ ] `aws ssm get-parameter --name /sentimentscribe/prod/TMDB_API_KEY --with-decryption` returns a value (not “AccessDenied”).
- [ ] Generated `.env.prod` contains expected keys (do not paste the contents into chat/logs).

---

## PHASE 6 — Expose Backend Securely (Ports + HTTPS)

### YOU DO THIS

#### 6.1 Decide your backend public URL

Recommended:

- `api.example.com` → points to your EC2 **Elastic IP**

If you don’t have a domain yet, you can temporarily use `https://<EIP>` is **not** ideal (TLS cert won’t match an IP); prefer a domain.

#### 6.2 Create DNS record for the API host

If using Route 53:

1) AWS Console → **Route 53** → **Hosted zones** → your zone
2) **Create record**
3) Record name: `api` (for `api.example.com`)
4) Record type: **A**
5) Value: your **Elastic IP**
6) TTL: 300 → **Create records**

If using an external DNS provider:

- Create an **A record** for `api.example.com` pointing to your Elastic IP.

#### 6.3 Run a reverse proxy that terminates TLS and forwards to the backend container

In this plan, you’ll run **Caddy** as a container:

- Caddy listens on **80/443** on the host.
- Caddy forwards requests to `backend:8080` inside the Docker network.
- The backend container does **not** publish port 8080 to the internet.

### CODEX DOES THIS

Codex Step 7 — Add Caddy reverse proxy configuration for prod

- Add `deploy/ec2/Caddyfile` defining:
  - site: `api.example.com`
  - reverse proxy to `backend:8080`
  - basic security headers (Phase 9) if appropriate at the proxy layer
- Update `deploy/ec2/docker-compose.prod.yml` to include a `caddy` service:
  - image `caddy:2`
  - ports `80:80` and `443:443`
  - volumes for `/data` and `/config` (so certs persist)
  - mount the `Caddyfile`
  - backend only reachable on the internal compose network

### VERIFY

- [ ] From your laptop: `https://api.example.com/api/health` returns `200` and valid JSON.
- [ ] HTTP redirects to HTTPS (try `http://api.example.com/api/health`).
- [ ] EC2 security group inbound rules: only 22 (your IP), 80/443 (world). No 8080.

---

## PHASE 7 — Deploy Frontend to S3 + CloudFront

### YOU DO THIS (AWS Console click-by-click)

#### 7.1 Build the frontend for production (local)

From repo root:

1) `cd frontend`
2) `npm ci`
3) Set the API base URL for the build:
   - PowerShell (one-off): `$env:VITE_API_BASE_URL='https://api.example.com'; npm run build`
   - macOS/Linux: `VITE_API_BASE_URL=https://api.example.com npm run build`
4) Confirm output exists in `frontend/dist/`

#### 7.2 Create an S3 bucket (private) to store the built frontend

1) AWS Console → **S3** → **Buckets** → **Create bucket**
2) Bucket name: `sentimentscribe-frontend-<unique-suffix>` (must be globally unique)
3) Region: `us-east-1` (recommended)
4) **Block Public Access settings for this bucket**: keep **ON** (bucket stays private)
5) Click **Create bucket**

#### 7.3 Create a CloudFront distribution with the S3 bucket as origin (recommended: OAC)

1) AWS Console → **CloudFront** → **Distributions** → **Create distribution**
2) Origin domain: select your S3 bucket (it will look like `sentimentscribe-frontend-...s3.amazonaws.com`)
3) Origin access:
   - Choose **Origin access control settings (recommended)**
   - Click **Create control setting** (name: `sentimentscribe-frontend-oac`) → **Create**
   - Back on the distribution page, select the new OAC
   - Click **Update bucket policy** when CloudFront prompts (this is what allows CloudFront to read private objects)
4) Default cache behavior:
   - Viewer protocol policy: **Redirect HTTP to HTTPS**
   - Allowed HTTP methods: **GET, HEAD**
   - Cache policy: **CachingOptimized** (fine for static assets)
5) Settings:
   - Default root object: `index.html`
6) Create distribution → wait for **Status: Deployed** (can take ~10–30 minutes)

#### 7.4 SPA routing (React) — map 403/404 to `index.html`

1) CloudFront → your distribution → **Error pages** (or **Custom error responses**)
2) Create custom error response:
   - HTTP error code: **403**
   - Customize error response: **Yes**
   - Response page path: `/index.html`
   - HTTP response code: **200**
   - TTL: **0**
3) Repeat for **404**

#### 7.5 Upload `dist/` to S3

Console method (beginner-friendly):

1) S3 → your bucket → **Upload**
2) Upload contents of `frontend/dist/` (upload the files *inside* `dist`, not the folder itself)
3) Click **Upload**

CLI method (faster for repeat deploys; optional):

- `aws s3 sync frontend/dist s3://sentimentscribe-frontend-<unique-suffix> --delete`

#### 7.6 CloudFront invalidation (on updates)

After uploading new files:

1) CloudFront → your distribution → **Invalidations** → **Create invalidation**
2) Path: `/index.html` (and optionally `/*` if you’re unsure)
3) **Create invalidation**

### CODEX DOES THIS

Codex Step 8 — Confirm frontend uses runtime API base

- Ensure the frontend uses `VITE_API_BASE_URL` for API calls (currently in `frontend/src/api/http.ts`) and document the production build command.
- Optional: add a clear `.env.production.example` for the frontend so the required variable is obvious.

### VERIFY

- [ ] Visit the CloudFront domain (found in CloudFront distribution “General”):
  - `https://<DISTRIBUTION_ID>.cloudfront.net`
- [ ] App loads without 403/404 for static assets.
- [ ] Open browser devtools → Network:
  - API calls go to `https://api.example.com/...` (not localhost).

---

## PHASE 8 — Custom Domain + TLS (Optional but Recommended)

This phase covers a nice end state:

- Frontend: `https://app.example.com` (CloudFront + ACM in `us-east-1`)
- Backend: `https://api.example.com` (Caddy + Let’s Encrypt on EC2)

### YOU DO THIS

#### 8.1 Buy a domain (choose one)

Option A: Buy from a registrar (Namecheap, Google Domains successor, etc.)  
Option B: Buy directly in AWS (Route 53 → Domains → Register domain)

#### 8.2 Create a Route 53 hosted zone (if using Route 53 for DNS)

1) AWS Console → **Route 53** → **Hosted zones** → **Create hosted zone**
2) Domain name: `example.com`
3) Type: **Public hosted zone**
4) Create hosted zone

If you bought the domain outside AWS:

1) In Route 53 hosted zone, copy the **NS** records (nameservers)
2) In your registrar’s DNS settings, replace nameservers with Route 53 nameservers
3) Wait for propagation (can take minutes to hours)

#### 8.3 Request ACM certificate for the frontend domain (MUST be in `us-east-1` for CloudFront)

1) Switch region to **us-east-1 (N. Virginia)** in the AWS Console
2) AWS Console → **ACM (Certificate Manager)** → **Request a certificate**
3) Type: **Public certificate**
4) Fully qualified domain name:
   - `app.example.com` (and optionally `example.com` if you want apex)
5) Validation method: **DNS validation**
6) Request
7) On the cert details page → **Create records in Route 53** (if using Route 53)
   - If not using Route 53, ACM will show a CNAME record; create that CNAME in your DNS provider
8) Wait until certificate status is **Issued**

#### 8.4 Attach the custom domain + cert to CloudFront

1) CloudFront → your distribution → **Settings** → **Edit**
2) Alternate domain name (CNAME): add `app.example.com`
3) Custom SSL certificate: select the ACM cert you requested (must be in `us-east-1`)
4) Save changes

#### 8.5 Create DNS record pointing to CloudFront

If using Route 53:

1) Route 53 → Hosted zone → **Create record**
2) Record name: `app`
3) Record type: **A**
4) Toggle **Alias** = On
5) Route traffic to: **Alias to CloudFront distribution**
6) Select your distribution → Create records

If using an external DNS provider:

- For `app.example.com`, create a **CNAME** to your CloudFront domain (looks like `dxxxxx.cloudfront.net`).
- For apex `example.com`, you may need ALIAS/ANAME support; otherwise use `app.example.com` and redirect.

### CODEX DOES THIS

Codex Step 9 — Finalize CORS allowlist for real domains

- Ensure backend CORS allowlist supports:
  - `https://app.example.com`
  - the CloudFront domain `https://dxxxxx.cloudfront.net` (optional once custom domain works)
  - local dev `http://localhost:3000` (dev only)

### VERIFY

- [ ] `https://app.example.com` loads via CloudFront with a valid cert.
- [ ] Browser console shows no CORS errors when calling `https://api.example.com`.
- [ ] `https://api.example.com/api/health` works.

---

## PHASE 9 — Production Hardening “Just Enough”

### YOU DO THIS

#### 9.1 AWS account safety (minimum)

1) AWS Console → **IAM** → **Users** → create an admin IAM user for daily work
2) Enable MFA for the IAM user too
3) AWS Console → **Billing** → **Budgets** → create a monthly cost budget + alerts

#### 9.2 EC2 safety (minimum)

- Keep SSH locked to your IP; update the rule when your IP changes (don’t switch it to “Anywhere”).
- Keep system updated occasionally:
  - `sudo apt-get update && sudo apt-get upgrade -y`

#### 9.3 Container reliability

- Ensure compose uses restart policies:
  - `restart: unless-stopped` for `postgres`, `backend`, `caddy`
- Ensure Docker starts on boot (`systemctl enable docker`) (done in Phase 3).

#### 9.4 Basic monitoring (minimum viable)

What this means: you want a quick way to notice “the server is down” or “disk is full” before users do.

1) **CPU/Network graphs**
   - AWS Console → EC2 → Instances → select your instance → **Monitoring** tab
2) **Disk usage (manual check)**
   - On EC2: `df -h` (watch the root volume and any mounted data directories)
3) **CloudWatch alarm (recommended)**
   - AWS Console → **CloudWatch** → **Alarms** → **Create alarm**
   - Select metric: **EC2** → **Per-Instance Metrics** → pick your instance → `CPUUtilization`
   - Condition: `>= 80` for `5 minutes` (example)
   - Notification: create an **SNS topic** and add your email → confirm the subscription email
4) **Application logs**
   - MVP: use `docker compose logs -f backend` on EC2 when debugging.
   - Next step (optional): send container logs to CloudWatch Logs (more setup, but centralized).

### CODEX DOES THIS

Codex Step 10 — “Just enough” app-level hardening

- **Strict CORS**: production should allow only your frontend origins.
- **Security headers** (pick a minimal set):
  - `Strict-Transport-Security` (HSTS) (enable once HTTPS is stable)
  - `X-Content-Type-Options: nosniff`
  - `Referrer-Policy: no-referrer` (or `strict-origin-when-cross-origin`)
  - `Content-Security-Policy` (optional; can be tricky with SPAs)
- **Reduce error leakage**:
  - avoid returning stack traces
  - ensure prod logging levels are sane
- **Secrets hygiene**:
  - ensure secrets aren’t printed to logs
  - ensure `.env.prod` is not world-readable on EC2

### VERIFY

- [ ] From browser devtools, response headers include basic security headers.
- [ ] No secrets appear in `docker compose logs`.
- [ ] Reboot EC2 and confirm containers return automatically (Phase 10 also checks this).

---

## PHASE 10 — Final Verification Checklist + Future Updates

### YOU DO THIS (final checks)

#### 10.1 End-to-end functionality checks

- [ ] Backend health works publicly over HTTPS:
  - `GET https://api.example.com/api/health` returns `200`
- [ ] Frontend loads over HTTPS:
  - `https://app.example.com` (or CloudFront domain if no custom domain)
- [ ] Frontend can call backend without CORS errors
- [ ] Login/auth flow works (current password gate or future auth)
- [ ] Create/edit/delete entry works end-to-end

#### 10.2 Reliability checks

- [ ] Containers auto-restart on reboot:
  - `sudo reboot`
  - SSH back in
  - `docker ps` shows `postgres`, `backend`, `caddy` up
- [ ] DB persists across restarts:
  - Create a test entry
  - Restart `postgres` container
  - Confirm entry still exists

#### 10.3 “How to deploy updates” (repeatable process)

Backend update (ECR strategy):

1) Local: build new image and tag with a version:
   - `docker build -t sentimentscribe-backend:vX -f backend/Dockerfile backend`
2) Tag + push to ECR:
   - `docker tag sentimentscribe-backend:vX <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/sentimentscribe-backend:vX`
   - `docker push <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/sentimentscribe-backend:vX`
3) EC2: update the image tag in your compose env/config and redeploy:
   - `cd ~/sentimentscribe/deploy`
   - `docker compose pull`
   - `docker compose up -d`
   - `docker compose logs -f backend`

Frontend update (S3/CloudFront):

1) Local: build with correct API URL:
   - `cd frontend`
   - `VITE_API_BASE_URL=https://api.example.com npm run build` (or PowerShell variant)
2) Upload:
   - `aws s3 sync frontend/dist s3://sentimentscribe-frontend-<suffix> --delete`
3) Invalidate:
   - CloudFront invalidation for `/index.html`

### CODEX DOES THIS

No additional repo changes required in this phase.

### VERIFY (quick “green lights”)

- [ ] `https://app.example.com` loads and can fetch data from API
- [ ] `https://api.example.com/api/health` is OK
- [ ] No open inbound SG ports besides 22 (your IP), 80/443
- [ ] SSM parameters exist and instance role can read them
- [ ] Postgres not publicly exposed, and data persists across restarts
