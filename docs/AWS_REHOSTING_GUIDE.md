# Rehost SentimentScribe on AWS: start-to-finish runbook

This guide deploys the application from this repository to a **new AWS account**. It uses the existing deployment layout:

> **Existing deployment:** Initial deployment to account `920235987728` is complete. For its resource identifiers, verification history, and the targeted procedure to add Spotify credentials later, use [AWS_DEPLOYMENT_ACTIONS.md](AWS_DEPLOYMENT_ACTIONS.md). Do not repeat initial provisioning or regenerate the existing database/JWT secrets.

| Component | Location |
| --- | --- |
| React/Vite frontend | Private S3 bucket, delivered through CloudFront |
| Spring Boot API | Docker on one Ubuntu EC2 instance |
| HTTPS for the API | Caddy container on that EC2 instance |
| PostgreSQL | Docker container with data on the EC2 EBS volume |
| Backend image | Private Amazon ECR repository |
| DNS | Route 53 public hosted zone, or your current DNS provider |

The commands below use **Canada (Central), `ca-central-1`** for EC2, ECR, and S3. CloudFront requires its public ACM certificate in **US East (N. Virginia), `us-east-1`**. Keep these regions distinct.

> **Data warning:** This procedure creates a new, empty PostgreSQL database. Old users and diary entries are not in this Git repository. To restore them, obtain a PostgreSQL dump or usable backup from the old account before inviting users back. Users also need their original diary passphrases to decrypt restored entries.

> **Domain warning:** You must control the registration/DNS for `sentimentscribe.cloud`. A new AWS account cannot take over a domain registered in an inaccessible old account merely by creating a hosted zone. Resolve registrar access first if necessary.

## Checklist and values to record

Work down the document in order. Record these values as you go; do not put passwords in this table.

| Value | Your value |
| --- | --- |
| AWS account ID (12 digits) | `920235987728` |
| EC2 Elastic IP | `16.52.107.103` |
| ECR registry, `<account-id>.dkr.ecr.ca-central-1.amazonaws.com` | `920235987728.dkr.ecr.ca-central-1.amazonaws.com` |
| Frontend S3 bucket name | `sentimentscribe-frontend-920235987728` |
| CloudFront distribution ID | `E3LBEOGE327OT5` |
| CloudFront domain, `d...cloudfront.net` | `dgzayw2ozg6wz.cloudfront.net` |
| Domain registrar and active DNS provider | Porkbun |

You also need access to this repository, a Windows computer with PowerShell, your Spotify client ID/secret, and a TMDb API key. If you do not have the old third-party credentials, create new ones in those providers' developer dashboards before Step 10. Do not paste any secret into Git, the frontend build, or this document.

> **Current Spotify prerequisite:** Development Mode apps require their owning Spotify account to have active Premium, including existing apps. Valid client credentials alone are insufficient. The user confirmed this app is in Development Mode and its owner is on Spotify Free; OAuth succeeds but search returns 403. Do not rotate keys or change AWS resources to address this provider-account restriction. Any subscription purchase is the user's decision, not an automated deployment step. [Spotify access update, including March 9, 2026 clarification](https://developer.spotify.com/blog/2026-02-06-update-on-developer-access-and-platform-security).

## 1. Create and secure the new AWS account

Skip account creation if you already have an active new account where you can create EC2, ECR, S3, CloudFront, ACM, IAM, and DNS resources.

1. Go to [aws.amazon.com](https://aws.amazon.com/) and choose **Create an AWS Account**. Enter a root email address you control, an account name, payment details, and contact details. Complete email/phone verification and choose a support plan. Sign in to the [AWS Management Console](https://console.aws.amazon.com/). AWS's [account sign-up guide](https://docs.aws.amazon.com/accounts/latest/reference/getting-started.html) covers the current screens.
2. At the top right, open your **account name -> Security credentials**. Under **Multi-factor authentication (MFA)**, choose **Assign MFA device** and complete the passkey, security-key, or authenticator setup. [AWS root MFA steps](https://docs.aws.amazon.com/IAM/latest/UserGuide/enable-mfa-for-root.html)
3. Open the account menu again and copy the **12-digit Account ID** into the table above.
4. In the region selector at the top right of the console, choose **Canada (Central)**. Keep this selected except when Step 12 explicitly tells you to use `us-east-1`.
5. Open **Billing and Cost Management -> Budgets -> Create budget -> Use a template (simplified) -> Monthly cost budget**. Enter a monthly amount you are willing to spend and an email address for alerts; choose **Create budget**. A budget alerts you, but does not automatically stop resources. [AWS budget steps](https://docs.aws.amazon.com/cost-management/latest/userguide/budget-templates.html)

## 2. Establish who controls `sentimentscribe.cloud`

1. Sign in at the company where the domain was purchased. Find the domain's **DNS**, **Nameservers**, or **Manage domain** page. Confirm that the registration has not expired and that you can edit its nameservers. Write down the registrar and current DNS provider.
2. If DNS already lives at an accessible provider and you want to keep it there, you can create the records in Steps 7, 12, and 15 at that provider. Skip the Route 53 hosted-zone creation below. The AWS Console cannot change records held at another provider.
3. To use Route 53 in the new account, open **Route 53 -> Hosted zones -> Create hosted zone**. Enter `sentimentscribe.cloud`, select **Public hosted zone**, and choose **Create hosted zone**.
4. Open the new hosted zone and copy the four values in its **NS** record. At the registrar, replace the domain's current nameservers with these four values. **Before replacing nameservers, recreate any existing email or other important DNS records in the new hosted zone.** Leave the automatically created NS and SOA records alone.
5. Wait for the registrar's change to propagate. In Windows PowerShell, run `Resolve-DnsName sentimentscribe.cloud -Type NS`. The returned nameservers should be the four from the new hosted zone. This can take up to two days. [AWS Route 53 DNS migration guide](https://docs.aws.amazon.com/Route53/latest/DeveloperGuide/migrate-dns-domain-inactive.html)

If the domain was registered through the deactivated AWS account and the registrar settings are inaccessible, stop the domain-specific steps and recover or transfer the registration with AWS/registrar support. You may continue creating EC2 and S3, but `sentimentscribe.cloud` and Caddy HTTPS cannot be completed until you control DNS.

## 3. Create the ECR repository

1. Confirm the console region says **Canada (Central)**.
2. Search for **Elastic Container Registry** in the AWS Console search bar. Open **Private repositories -> Create repository**.
3. Set **Repository name** to `sentimentscribe-backend`. Leave visibility **Private** and other settings at their defaults. Choose **Create**.
4. Open the repository. Its URI begins `<account-id>.dkr.ecr.ca-central-1.amazonaws.com/sentimentscribe-backend`. Record the **registry portion only** (everything before `/sentimentscribe-backend`) in the table. [AWS ECR instructions](https://docs.aws.amazon.com/AmazonECR/latest/userguide/repository-create.html)

## 4. Create the EC2 IAM role

This role lets the instance build, push, and pull its ECR image without an AWS access key stored on disk.

1. Search for **IAM** in the console. Open **Roles -> Create role**.
2. Under **Trusted entity type**, choose **AWS service**; under **Use case**, choose **EC2**. Choose **Next**.
3. Search for and select the policy **AmazonEC2ContainerRegistryPowerUser**. Choose **Next**.
4. Set **Role name** to `SentimentScribeEc2Role`; choose **Create role**. This managed policy can write to ECR repositories. After the first deployment, consider replacing it with repository-scoped permissions or the pull-only policy for normal operation. [AWS ECR policies](https://docs.aws.amazon.com/AmazonECR/latest/userguide/security-iam-awsmanpol.html)

## 5. Launch the Ubuntu EC2 instance

1. Search for **EC2**. In **Instances**, choose **Launch instances**.
2. **Name:** `sentimentscribe-prod`.
3. **Application and OS Images:** choose the official **Ubuntu Server 24.04 LTS**, **64-bit (x86)** AMI. Do not select an ARM AMI; the AWS CLI installation command later in this guide is x86-64.
4. **Instance type:** `t3.medium` is a starting size for the Java API plus PostgreSQL. It is not a promise of free-tier eligibility. Review the cost shown in the console.
5. **Key pair (login):** choose **Create new key pair**. Name it `sentimentscribe-prod`, select **RSA** and **.pem**, create it, and retain the downloaded `.pem` file on your computer. AWS will not let you download that private key again.
6. **Network settings:** choose the default VPC, a public subnet, and **Auto-assign public IP: Enable**. Choose **Create security group**, name it `sentimentscribe-web`, and add these inbound rules:

   | Type | Port | Source |
   | --- | --- | --- |
   | SSH | 22 | My IP |
   | HTTP | 80 | Anywhere-IPv4 (`0.0.0.0/0`) |
   | HTTPS | 443 | Anywhere-IPv4 (`0.0.0.0/0`) |

   Keep outbound traffic allowed. Do not open `5432` (database) or `8080` (Spring Boot) to the internet.
7. **Configure storage:** set the root EBS volume to **30 GiB**, type **gp3**. This is an initial size; monitor disk use after launch.
8. Expand **Advanced details**. Under **IAM instance profile**, select `SentimentScribeEc2Role`.
9. Choose **Launch instance**. Return to **Instances** and wait for **Instance state: Running** and **Status checks: 2/2 checks passed**. [AWS launch parameter reference](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-launch-parameters.html)

## 6. Allocate a stable IP

1. In EC2's left navigation, open **Network & Security -> Elastic IPs -> Allocate Elastic IP address**. Leave **Amazon's pool of IPv4 addresses** selected and choose **Allocate**.
2. Select the allocated address, choose **Actions -> Associate Elastic IP address**, set **Resource type: Instance**, select `sentimentscribe-prod`, and choose **Associate**.
3. Record the Elastic IP. Verify it appears as the instance's **Public IPv4 address**. Elastic IPs/public IPv4 addresses incur charges. [AWS Elastic IP steps](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/working-with-eips.html)

## 7. Create the API DNS record

If you use Route 53:

1. Open **Route 53 -> Hosted zones -> sentimentscribe.cloud -> Create record**.
2. **Record name:** `api`; **Record type:** `A`; **Value:** the Elastic IP; **TTL:** `300` seconds; **Routing policy:** Simple. Leave **Alias** off. Choose **Create records**.

At another DNS provider, create the equivalent `A` record for `api.sentimentscribe.cloud`. Replace any old `api` record rather than leaving two conflicting values.

For Porkbun, use **TTL 600**, its supported minimum, rather than Route 53's 300 above. This provider-specific setting does not change the architecture. [Porkbun TTL guidance](https://kb.porkbun.com/article/33-how-long-will-it-take-for-changes-to-dns-to-show-up)

In Windows PowerShell, run `Resolve-DnsName api.sentimentscribe.cloud -Type A`. Continue when it returns the new Elastic IP. Caddy will use this DNS record plus ports 80/443 to request its HTTPS certificate.

## 8. Connect to the instance from Windows

1. In **EC2 -> Instances**, select `sentimentscribe-prod`. Copy its **Public IPv4 address** (the Elastic IP).
2. On Windows, open PowerShell. Set the key path and IP to your actual values:

   ```powershell
   $Key = "C:\Users\YOUR_WINDOWS_USER\Downloads\sentimentscribe-prod.pem"
   $Ip = "YOUR_ELASTIC_IP"
   ssh -i $Key "ubuntu@$Ip"
   ```

3. Answer `yes` if SSH asks whether to trust the host key. Your prompt should now show `ubuntu@...`.

If SSH times out, check that the instance is in a public subnet, the Elastic IP is associated, port 22 is allowed from your **current** public IP, and your ISP/VPN is not changing that IP. You can also try **EC2 -> Instances -> select instance -> Connect -> EC2 Instance Connect**, but browser-based Instance Connect has its own network prerequisites. [AWS EC2 connection guide](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/connect.html)

**All commands in Steps 9-11 run in the Ubuntu SSH terminal, not Windows PowerShell.**

## 9. Install Git, Docker, Compose, and the AWS CLI on EC2

Run these commands one block at a time. The Docker commands follow [Docker's Ubuntu guide](https://docs.docker.com/engine/install/ubuntu/).

```bash
sudo apt update
sudo apt install -y ca-certificates curl git unzip
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
sudo tee /etc/apt/sources.list.d/docker.sources >/dev/null <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}")
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable --now docker
sudo docker compose version
```

Install the AWS CLI using [AWS's Linux installer](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html):

```bash
cd ~
curl -fsSL https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip -o awscliv2.zip
unzip -q awscliv2.zip
sudo ./aws/install
aws --version
aws sts get-caller-identity --query Account --output text
```

The last command must print the new account's 12-digit ID. If it reports missing credentials, check that `SentimentScribeEc2Role` is attached to the instance under **EC2 -> Instances -> select instance -> Security -> IAM role**. Do **not** run `aws configure` with long-lived access keys on this instance.

## 10. Clone and prepare the backend

In the Ubuntu terminal:

```bash
cd ~
git clone https://github.com/Jaynik-S/SentimentScribe.git sentimentscribe
cd sentimentscribe
```

If GitHub says the repository is private or access is denied, authenticate to GitHub using your normal authorized method, then retry. The guide assumes this repository is the version you want to deploy.

### Apply the required JWT environment fix

The backend container must receive the JWT secret through `deploy/ec2/docker-compose.prod.yml`. This repository now includes the required mapping. If deploying an older checkout that lacks it, follow the edit below. Without that mapping, Spring Boot uses the development default from `application.yml` even if `.env.prod` contains a secret.

Run `nano deploy/ec2/docker-compose.prod.yml`. Find the backend service's `environment:` block. If the following line is already present, do not add it again. Otherwise, add it directly below its `TMDB_API_KEY:` line, preserving the six leading spaces:

```yaml
      SENTIMENTSCRIBE_JWT_SECRET: ${SENTIMENTSCRIBE_JWT_SECRET?set SENTIMENTSCRIBE_JWT_SECRET in .env.prod}
```

In nano, save with **Ctrl+O**, **Enter**, then exit with **Ctrl+X**. Also apply this one-line change to your local repository after the deployment so a later `git pull` cannot remove it. The file to update locally is [`deploy/ec2/docker-compose.prod.yml`](../deploy/ec2/docker-compose.prod.yml). In local PowerShell, after saving that file, run `git add deploy/ec2/docker-compose.prod.yml`, `git commit -m "Pass production JWT secret to backend"`, and `git push`. Only do this if you have permission to push to the repository. The EC2 copy will then match the committed version.

### Create the instance's private environment file

Run `openssl rand -hex 32` **twice**. Record the two different outputs securely: one for PostgreSQL and one for JWT signing. Then:

```bash
cd ~/sentimentscribe/deploy/ec2
nano .env.prod
```

Paste the following and replace every `<...>` value. `ECR_REGISTRY` is the registry portion recorded in Step 3, with no `/sentimentscribe-backend` suffix.

```dotenv
ECR_REGISTRY=<12-digit-account-id>.dkr.ecr.ca-central-1.amazonaws.com
BACKEND_IMAGE_TAG=latest
POSTGRES_USER=diary
POSTGRES_PASSWORD=<first-openssl-value>
SENTIMENTSCRIBE_JWT_SECRET=<second-openssl-value>
SPOTIFY_CLIENT_ID=<your-spotify-client-id>
SPOTIFY_CLIENT_SECRET=<your-spotify-client-secret>
TMDB_API_KEY=<your-tmdb-api-key>
SENTIMENTSCRIBE_CORS_ORIGINS=https://sentimentscribe.cloud
```

Save and exit nano. Run `chmod 600 .env.prod`. Do not run `git add .env.prod`; the file is intentionally ignored by this repository. The secrets are on the EC2 disk, so restrict SSH access and make a secure copy of them outside this instance.

## 11. Build, push, and start the backend

Run in the Ubuntu terminal:

```bash
cd ~/sentimentscribe
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
REGISTRY="$ACCOUNT_ID.dkr.ecr.ca-central-1.amazonaws.com"
sudo docker build -t "$REGISTRY/sentimentscribe-backend:latest" ./backend
aws ecr get-login-password --region ca-central-1 | sudo docker login --username AWS --password-stdin "$REGISTRY"
sudo docker push "$REGISTRY/sentimentscribe-backend:latest"
cd deploy/ec2
sudo docker compose -f docker-compose.prod.yml --env-file .env.prod config --quiet
sudo docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
sudo docker compose -f docker-compose.prod.yml --env-file .env.prod ps
```

The `ps` output should show `postgres`, `backend`, and `caddy` running. Building the backend can take several minutes. The image push follows [AWS ECR's Docker login/push procedure](https://docs.aws.amazon.com/AmazonECR/latest/userguide/getting-started-cli.html).

Check the API in Windows PowerShell:

```powershell
Invoke-RestMethod https://api.sentimentscribe.cloud/api/health
```

Expected output contains `status` equal to `ok`. If Caddy has not obtained a certificate yet, first reconfirm the `api` A record and inbound ports 80/443, then inspect the logs on EC2:

```bash
cd ~/sentimentscribe/deploy/ec2
sudo docker compose -f docker-compose.prod.yml --env-file .env.prod logs --tail=100 backend caddy postgres
```

Do not continue to the frontend until the HTTPS health check works.

## 12. Request the CloudFront certificate

1. In the AWS Console region selector, switch to **US East (N. Virginia)**. This step must use `us-east-1` even though EC2 and S3 use `ca-central-1`. [CloudFront certificate requirement](https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/cnames-and-https-requirements.html)
2. Search for **Certificate Manager**. Choose **Request certificate -> Request a public certificate**.
3. Enter `sentimentscribe.cloud` as the domain name. Select **DNS validation** and request the certificate.
4. Open the certificate with status **Pending validation**. Under **Domains**, choose **Create records in Route 53 -> Create records** if your active public zone is in this account. If DNS is elsewhere, copy the displayed **CNAME name and value** into that DNS provider exactly. Do not create an `A` record for certificate validation.
5. Wait for certificate status **Issued**. If it remains pending, verify the active nameservers and the validation CNAME. [ACM DNS validation procedure](https://docs.aws.amazon.com/acm/latest/userguide/dns-validation.html)

## 13. Build the frontend on your Windows computer

Install current Node.js LTS from [nodejs.org](https://nodejs.org/) if `node --version` and `npm --version` do not work in PowerShell. Open a **new** PowerShell window after installing Node.

From your local copy of this repository, run:

```powershell
cd C:\Users\jay03\Jaynik\Projects\SentimentScribe\frontend
node --version
npm --version
npm ci
$env:VITE_API_BASE_URL = "https://api.sentimentscribe.cloud"
npm run build
Test-Path .\dist\index.html
```

`Test-Path` must print `True`. The frontend's [API request layer](../frontend/src/api/http.ts) uses `VITE_API_BASE_URL` **at build time**. Set this variable again for every future production build; changing an EC2 environment variable will not change an already built frontend.

## 14. Create the private S3 bucket and upload the build

1. Switch the AWS Console region back to **Canada (Central)**.
2. Search for **S3**. Choose **Create bucket**.
3. Select **General purpose**. For the globally unique bucket name, use `sentimentscribe-frontend-<your-12-digit-account-id>` (replace the placeholder). Choose **Canada (Central)** as region.
4. Keep **Block all public access** enabled and **ACLs disabled / bucket owner enforced**. Leave other settings at their defaults and choose **Create bucket**. Do not enable S3 static website hosting; CloudFront will use the private S3 origin.
5. Open the bucket and choose **Upload**. In Windows File Explorer, open `C:\Users\jay03\Jaynik\Projects\SentimentScribe\frontend\dist`. Drag **the contents of `dist`**, including `index.html`, `icon.png`, and the `assets` folder, into S3's upload area. Choose **Upload**.
6. Return to the bucket's **Objects** tab. `index.html` and `assets/` must appear at the **top level**, not inside a `dist/` folder. [AWS S3/CloudFront walkthrough](https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/GettingStarted.SimpleDistribution.html)

## 15. Create the CloudFront distribution

1. Search for **CloudFront**. Choose **Create distribution**.
2. Enter distribution name `sentimentscribe-frontend`; choose **Single website or app** and continue.
3. For **Origin type**, choose **Amazon S3**. Use **Browse S3** to select your frontend bucket. Choose **Use recommended origin settings**, which sets up Origin Access Control (OAC) and lets CloudFront update the bucket policy. Keep the S3 bucket private. Continue through the security screen and choose **Create distribution**. [AWS CloudFront creation steps](https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/GettingStarted.SimpleDistribution.html)
4. Open the new distribution and record its **Distribution ID** and **Distribution domain name** (for example, `d123example.cloudfront.net`). Wait for deployment to finish.
5. On its **General** tab, find **Settings -> Edit**. Set **Default root object** to `index.html` (without a leading slash). Save. [AWS default-root-object steps](https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/DefaultRootObject.html)
6. In the distribution's domain/certificate settings, add **Alternate domain name (CNAME)** `sentimentscribe.cloud` and select the **Issued** ACM certificate from Step 12. Save and wait for deployment. Console labels may show this under **General -> Settings -> Edit** or a **Domains** section.
7. Open the **Error pages** tab. Choose **Create custom error response** for HTTP **403**; set **Customize error response: Yes**, **Response page path: `/index.html`**, **HTTP response code: 200**, and **Error caching minimum TTL: 0**. Create a second equivalent response for HTTP **404**. This supports direct browser visits to React routes such as `/register`. [AWS custom error responses](https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/creating-custom-error-pages.html)
8. Test `https://<your-cloudfront-domain>/` and `https://<your-cloudfront-domain>/register`. The React page should load at both URLs. Authentication requests may fail until the main domain is connected, because the API CORS allowlist intentionally contains only `https://sentimentscribe.cloud`.

### If CloudFront says `CNAMEAlreadyExists`

The old AWS account may still have `sentimentscribe.cloud` attached to its distribution. Do not keep retrying DNS changes; DNS does not release the CloudFront alias. Follow [AWS's cross-account alias move procedure](https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/alternate-domain-names-move-options.html): set up the new distribution and ownership TXT record, then request AWS Support to move the domain if you cannot access or disable the old distribution. This is a possible blocker to using the old hostname, but the new distribution can still serve its `cloudfront.net` domain meanwhile.

## 16. Point `sentimentscribe.cloud` to CloudFront

If Route 53 hosts your active public DNS:

1. Open **Route 53 -> Hosted zones -> sentimentscribe.cloud -> Create record**.
2. Leave **Record name** blank (this means the root domain). Set **Record type: A**, turn **Alias** on, choose **Alias to CloudFront distribution**, and select the distribution from Step 15. Choose **Create records**.
3. If IPv6 is enabled on the CloudFront distribution, create the same root record with **Record type: AAAA** and the same alias target. [AWS Route 53 to CloudFront steps](https://docs.aws.amazon.com/Route53/latest/DeveloperGuide/routing-to-cloudfront-distribution.html)

If another provider hosts DNS, create its supported **ALIAS**, **ANAME**, or flattened-CNAME record for the root domain pointing to the CloudFront domain. A normal CNAME at the root is often unsupported; use your provider's documented root-domain option. Replace old records for the root domain.

Wait until `https://sentimentscribe.cloud` loads the login screen with a valid certificate.

## 17. Run the complete application check

In a browser, perform these checks in order:

1. Open `https://api.sentimentscribe.cloud/api/health`. Expect `{"status":"ok"}`.
2. Open `https://sentimentscribe.cloud`. Expect the login page, not an S3 Access Denied page.
3. Open `https://sentimentscribe.cloud/register` directly in a new tab. Expect the registration page after a refresh.
4. Register a **new test account**. Log in and unlock the diary with a test passphrase.
5. Create and save an entry. Refresh; confirm that the entry is still present.
6. Run analysis and recommendations. If those fail while diary storage works, check the Spotify/TMDb credentials and backend logs.
7. Open browser Developer Tools -> **Network**. Confirm API calls go to `https://api.sentimentscribe.cloud`, return expected statuses, and do not show CORS errors.
8. On EC2, run the `docker compose ... ps` command from Step 11 again. All three services should still be running.

If you have an old PostgreSQL backup, **restore it before inviting former users back** and repeat the checks using an existing account. Do not tell former users to re-register until you have decided whether their old data can be restored.

## 18. Protect the new database

The compose file stores PostgreSQL at `/home/ubuntu/sentimentscribe/data/postgres` on the EC2 instance's root EBS volume. S3's frontend bucket does **not** back up the database. Before storing real diary data:

1. Make an initial PostgreSQL dump on EC2. In the Ubuntu terminal, run:

   ```bash
   sudo docker exec sentimentscribe-postgres pg_dump -U diary -d sentiment_scribe -Fc > ~/sentimentscribe-backup.dump
   test -s ~/sentimentscribe-backup.dump && ls -lh ~/sentimentscribe-backup.dump
   ```

   The second command must show a nonempty file. This export contains user account data and encrypted diary records; handle it as sensitive data.
2. Copy the dump **off the instance**. In a new Windows PowerShell window, using the same key and Elastic IP from Step 8, run:

   ```powershell
   $Key = "C:\Users\YOUR_WINDOWS_USER\Downloads\sentimentscribe-prod.pem"
   $Ip = "YOUR_ELASTIC_IP"
   scp -i $Key "ubuntu@${Ip}:/home/ubuntu/sentimentscribe-backup.dump" "C:\Users\YOUR_WINDOWS_USER\Documents\sentimentscribe-backup.dump"
   ```

   Confirm the destination file exists. Store it in your normal secure backup location, then remove the temporary copy from EC2 with `rm ~/sentimentscribe-backup.dump`. Repeat the export regularly. A one-time dump does not protect later entries.
3. Set up recurring EBS snapshots through **AWS Backup -> Backup plans -> Create backup plan**, choose a daily schedule and a retention period you can afford, then assign the EC2 instance or its EBS volume to the plan. Snapshots complement the PostgreSQL dump; test a restore in a separate environment before relying on either. [AWS EBS snapshot guidance](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/EBSSnapshots.html)
4. Keep the `.env.prod` values in a secure password manager. If they are lost, an EBS snapshot alone may not provide all third-party credentials.
5. Restrict SSH port 22 to your current IP. Keep database port 5432 and application port 8080 closed externally.
6. Monitor EC2 disk space, container status, and AWS costs. The initial single-instance architecture has no automatic failover.

## Updating the deployment later

**Backend:** The automated deployment uploaded backend source as an archive; the EC2 source directory is not a Git checkout. Its ECR repository uses immutable tags. Do not run `git pull` there or overwrite a reused `latest` tag.

After reviewing and committing the intended source changes locally, the account-guarded helper can upload, build, and verify a uniquely tagged image:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\deploy\aws\build-backend.ps1 -ExpectedAccountId 920235987728 -Profile sentimentscribe-new
```

This prepares an image; it does not switch the running backend. Before switching releases, record the current tag for rollback, back up the database, and review the new image's migrations. Change only `BACKEND_IMAGE_TAG` in the existing private local and EC2 `.env.prod` files to the verified new tag, preserving all other values. Then authenticate Docker to ECR, pull that backend image, and recreate only the backend using the existing Compose file. Verify HTTPS health, database health, login, and diary save/reload afterward. Do not recreate PostgreSQL storage or regenerate secrets. The initial `start-backend.ps1` deliberately rejects differing local/remote environment files; it is not an automatic release updater. For Spotify-only changes, use the narrower helpers documented in [AWS_DEPLOYMENT_ACTIONS.md](AWS_DEPLOYMENT_ACTIONS.md), not this release procedure.

**Frontend:** On Windows, repeat `npm ci`, set `VITE_API_BASE_URL`, and run `npm run build` as in Step 13. Upload the new **contents** of `frontend/dist` to the same S3 bucket, replacing existing files. In **CloudFront -> your distribution -> Invalidations -> Create invalidation**, enter `/*` and create it. Wait for the invalidation to complete, then refresh the site. [AWS invalidation guide](https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/Invalidation_Requests.html)

## Quick fault finder

| Symptom | First thing to check |
| --- | --- |
| `api.sentimentscribe.cloud` does not resolve | `api` A record and active domain nameservers |
| API certificate/HTTPS fails | `api` A record points to Elastic IP; EC2 ports 80/443 open; Caddy logs |
| API gives `502` | Backend/postgres container status and logs |
| Frontend gives S3 `Access Denied` | `index.html` is at bucket root; CloudFront OAC/bucket policy |
| `/register` works by clicking but fails on refresh | CloudFront 403/404 custom error responses |
| Frontend calls `localhost:8080` | Rebuild with `VITE_API_BASE_URL` set, upload again, invalidate CloudFront |
| Browser reports CORS error | `SENTIMENTSCRIBE_CORS_ORIGINS` exactly matches the site's HTTPS origin; recreate backend container |
| Login fails or JWT errors | JWT secret line is present in compose and `.env.prod`; inspect backend logs |
| Old domain cannot be added to CloudFront | Old distribution still owns alias; use AWS cross-account move/Support |

No AWS resources are created by this document itself. You create them only by following the steps above in your own account.
