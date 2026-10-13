# SentimentScribe deployment and maintenance notes

The architecture and full deployment instructions remain in [AWS_REHOSTING_GUIDE.md](AWS_REHOSTING_GUIDE.md). Initial deployment is complete. This document records the deployment results and the remaining maintenance actions; do not repeat completed provisioning or credential-generation steps on the existing application.

## Confirmed deployment choices

- New AWS account: **`920235987728`**. This replaces the previously supplied account ID; do not use the earlier account.
- Domain: **`sentimentscribe.cloud`**, registered/DNS-hosted at **Porkbun**. Keep DNS there; no domain transfer is needed.
- Database: **start empty**. Do not access the old AWS account.
- Application resources: **`ca-central-1`**. CloudFront ACM certificate: **`us-east-1`**.

## Current checkpoint

**Latest user-reported status, October 12, 2026: AWS is running and working, except for the deferred Spotify credentials.** This repository cleanup did not recheck or change live AWS resources. Historically, AWS confirmed a user-initiated stop of instance `i-09763071d6129c44f` on **October 6, 2026 at 4:32:58 a.m. America/Toronto**; that earlier paused state is not the current availability claim. The encrypted 30 GiB disk and Elastic IP were retained. No DNS changes are needed. Before the pause, registration, login, unlock, encrypted entry save, refresh, fresh-browser server reload/decryption, analysis, and API network/CORS checks passed.

**Your latest decision:** leave only Spotify credentials blank and configure them later. Both Spotify fields are now blank locally and in the running backend; TMDb, PostgreSQL, JWT, and other settings were preserved. Combined music/movie recommendations remain unavailable until Spotify is configured and its access checks pass. No subscription or app-ownership change was made.

**Approved backup completed:** a private database dump was transferred off EC2 and restored successfully in an isolated local PostgreSQL 15 container. Your **$3 USD monthly AWS budget** and email alerts above **$1, $2, and $3** are created and verified. This is not a hard spending cap. Daily backups still need a retention choice. The one-time dump does not protect entries created afterward.

## Completed automatically and verified

The named profile is authenticated. AWS STS confirmed **`920235987728`**, identity **`arn:aws:iam::920235987728:root`**, before resource inventory or creation. For routine administration, a non-root identity is recommended; no long-lived root access keys were created.

| Resource | Verified value |
| --- | --- |
| CloudFormation stack | `sentimentscribe-foundation`, `CREATE_COMPLETE`, termination-protected |
| Ubuntu EC2 | `i-09763071d6129c44f`, `t3.medium`, `ca-central-1a`; user reports running October 12; deployment health checks passed previously |
| Elastic IP | **`16.52.107.103`** |
| EBS | `vol-09382fd8ed24b4d02`, encrypted 30 GiB `gp3`, retained on instance termination |
| Security group | `sg-030811040a851a204`; SSH from `142.198.204.149/32`; public 80/443 only |
| Private ECR | `920235987728.dkr.ecr.ca-central-1.amazonaws.com/sentimentscribe-backend` |
| Private S3 | `sentimentscribe-frontend-920235987728`; all public access blocked |
| EC2 role | `SentimentScribeEc2Role`; instance credentials, not access keys |
| EC2 tooling | Docker, Compose, AWS CLI installed; bootstrap completed |
| CloudFront | `E3LBEOGE327OT5`, `Deployed`, `dgzayw2ozg6wz.cloudfront.net` |
| Frontend certificate | ACM `us-east-1`, `Issued`, attached to `sentimentscribe.cloud` |

These resources remain billable while running; retained EBS/IP/storage charges continue even during an EC2 pause. Spotify recommendations are intentionally deferred. The backend image verified during deployment was **`rehost-20261006024651-f4d7600f`**, with ECR digest `sha256:7911c9ce2ced6fb6a546f8a5a5610dc20e68458b3569f7f4d71cd1d928c9b5e1`. Docker's temporary ECR login credentials were removed after image operations. The backend verification bullets below are historical deployment results, not fresh checks during repository cleanup.

- `https://api.sentimentscribe.cloud/api/health` returns **`{"status":"ok"}`** with valid Caddy HTTPS, through both direct new-IP verification and the normal public hostname.
- PostgreSQL is healthy; Flyway migration 1 succeeded; data is mounted at `/home/ubuntu/sentimentscribe/data/postgres` on EBS.
- The private environment file was transferred over SSH and its hash verified without printing values. EC2 file mode is `600`. The backend receives a production JWT secret of the expected length.
- TMDb API-key authentication passed with HTTP 200.
- **Spotify is intentionally deferred:** the earlier corrected pair authenticated but search returned **403**. At your request, only its two fields were subsequently blanked. The server rejected non-Spotify differences before replacing the private environment file and recreating only the backend. Runtime blank fields, public HTTPS health, and preserved TMDb access were verified. Private rollback copies were retained; no tokens, keys, or response bodies were printed.
- ACM certificate `arn:aws:acm:us-east-1:920235987728:certificate/1a27e6e2-63c6-496d-9502-900b570c165c` is **Issued**, with validation **SUCCESS**. CloudFront is **Deployed**, with `sentimentscribe.cloud` and the correct certificate attached. Private S3 access, default-domain SPA routes, refresh, desktop/mobile rendering, JS/CSS assets, and production-origin API CORS preflight passed. Public root DNS and valid HTTPS are verified after your ALIAS addition; no alias transfer or old-account access was needed.

## Completed manual action: API DNS

You updated the API A record at Porkbun. Both Porkbun's authoritative server (`curitiba.ns.porkbun.com`) and Cloudflare's public resolver (`1.1.1.1`) now return **`16.52.107.103`** for `api.sentimentscribe.cloud`. The agent verified these independently after your confirmation. No further API DNS action is needed.

| Field | Current value |
| --- | --- |
| Type | `A` |
| Host | `api` |
| Answer | `16.52.107.103` |
| TTL requested | `600` |
| Previous public answer, for rollback only | `54.147.219.50` |

The API is now serving over HTTPS. Leave this record in place. Keep nameservers, the root domain, email records, and other DNS records unchanged until the corresponding deployment step.

## Completed manual action: private credential entry

You created the Git-ignored `deploy/ec2/.env.prod` through masked local input. The agent transferred it privately, started the existing Compose services, and verified API HTTPS. Do not recreate the file: the running database and JWT signing depend on the existing random secrets.

1. Open **Start**, type **PowerShell**, and open **Windows PowerShell**. Use your own terminal window, not this chat, for the secret prompts.
2. Run:

   ```powershell
   cd "C:\Users\jay03\Jaynik\Projects\SentimentScribe"
   powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\deploy\aws\configure-secrets.ps1 -ExpectedAccountId 920235987728 -Profile sentimentscribe-new
   ```

   `ExecutionPolicy Bypass` applies only to this child process; it does not change your machine's persistent policy. The script checks AWS STS against the target account before creating the file.

3. At **Spotify Client ID**, paste/type the client ID and press **Enter**.
4. At **Spotify Client Secret**, paste/type the client secret and press **Enter**.
5. At **TMDb API key (v3, not the longer API Read Access Token)**, paste/type the v3 API key and press **Enter**.
6. Input is masked. Wait for **Private .env.prod created and Git-ignore verified**. The script also generates different cryptographically random PostgreSQL and JWT secrets automatically. You do not need to generate them yourself or edit the file.
7. Reply **credentials ready**. Do not send the entered values, a terminal screenshot containing credentials, or the contents of `.env.prod`.

If you do not have one of these credentials, stop before running the script and tell the agent which provider's credentials are missing. Do not enter dummy values; live recommendations must be tested with valid credentials. If the script reports an error, send only the error text, with any credentials removed.

### Where to find existing provider credentials

- **Spotify:** sign in at the [developer dashboard](https://developer.spotify.com/dashboard), open your existing SentimentScribe app, and locate **Client ID** and **Client Secret** in its overview/settings. Reveal the secret using the displayed control if needed, then paste it only into the local masked prompt. Do not rotate or delete the app. Spotify documents these credentials in its [app guide](https://developer.spotify.com/documentation/web-api/concepts/apps).
- **TMDb:** sign in on a desktop browser, open [Account Settings -> API](https://www.themoviedb.org/settings/api), and locate the **API key (v3)**. Use that key, not the longer read-access token. TMDb documents API registration under [Getting Started](https://developer.themoviedb.org/docs/getting-started); its [application authentication guide](https://developer.themoviedb.org/docs/authentication-application) distinguishes the key and token. If no key exists, tell the agent so it can guide the account-specific registration step.

The initial credential-entry steps above are retained for reference only. They are complete. Spotify correction will be a separate targeted action; it must preserve the working database/JWT/TMDb values.

## Completed manual action: frontend certificate DNS

You added the record below. The agent verified all four delegated Porkbun nameservers and public resolver `1.1.1.1` return the exact expected CNAME. ACM issuance is verified; no additional certificate DNS action is needed.

The CloudFront ACM certificate is in **`us-east-1`**, separate from application resources in **`ca-central-1`**. Add this new DNS record at Porkbun. This is certificate validation only; it does not change where the website or API routes.

| Porkbun field | Exact value |
| --- | --- |
| Type | **CNAME** |
| Host | `_27ae848b0731dcd63f9753725b37edd2` |
| Answer/Value | `_e892ecdbaef4b6e453001075a916e2f9.wzccmgtwzk.acm-validations.aws` |
| TTL | `600` |

1. Sign in at [Porkbun](https://porkbun.com/).
2. Click **ACCOUNT -> Domain Management**.
3. Find **sentimentscribe.cloud**, click **Details**, then the edit icon beside **DNS Records**.
4. Click **Add Record**.
5. Select **CNAME**, and enter the **Host**, **Answer/Value**, and **TTL** exactly as shown in the table.
6. Keep both leading underscores. Enter only the short Host value; Porkbun appends `.sentimentscribe.cloud`. Do not add `https://`, an IP address, or the root domain to the Host field.
7. Click **Add**. If this exact host already exists with a different answer, stop and report the conflict instead of replacing it. Leave old certificate-validation records and unrelated DNS records untouched.
8. This action is complete. The agent will wait for ACM issuance before deploying the frontend.

Keep this CNAME permanently: ACM uses it for managed renewal. [AWS ACM DNS validation](https://docs.aws.amazon.com/acm/latest/userguide/dns-validation.html). These screens follow [Porkbun DNS editing](https://kb.porkbun.com/article/68-what-is-dns-and-how-do-i-edit-it).

## Completed local action: correct Spotify credentials

You previously confirmed **Spotify corrected** after running the helper. OAuth then passed, but search was forbidden. You subsequently chose to leave Spotify blank until later; that targeted deferral is complete. The historical steps below are for reference; use the reactivation procedure below when ready.

The initial `invalid_client` issue was corrected using the targeted steps below, which are now historical reference only. Do not re-enter the pair again or rerun `configure-secrets.ps1` to address the current search-access restriction.

1. Open the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard) and sign in.
2. Select your existing SentimentScribe app, open **Settings**, and locate **Client ID** and **Client Secret**. Use **View client secret** if shown. Both values must come from the same app. Do not reset the secret or delete/recreate the app. [Spotify app credential documentation](https://developer.spotify.com/documentation/web-api/concepts/apps).
3. In your own Windows PowerShell window, run:

   ```powershell
   cd "C:\Users\jay03\Jaynik\Projects\SentimentScribe"
   powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\deploy\aws\update-spotify-credentials.ps1 -ExpectedAccountId 920235987728 -Profile sentimentscribe-new
   ```

4. Enter the Spotify Client ID and Client Secret at the masked prompts. Do not paste them into chat.
5. The helper checks the AWS account and verifies Spotify authentication **before** changing the local file. It changes only the two Spotify fields, preserves PostgreSQL/JWT/TMDb settings, and retains a private rollback copy inside Git-ignored `.state`. The running EC2 environment is not changed by this local step.
6. After **Spotify authentication passed**, reply **Spotify corrected**. The agent will compare non-Spotify settings before applying the correction to EC2 and verifying recommendations. If authentication fails, the existing local file remains unchanged; report only the error text, with no secrets.

## Completed manual action: check Spotify app access

1. Open [Spotify Developer Dashboard](https://developer.spotify.com/dashboard), select the existing SentimentScribe app, and open **Settings**.
2. Read **App Status**: **Development Mode** or **Extended Quota Mode**. Do not change app ownership, reset the secret, or recreate the app.
3. For the Spotify account that **owns that app**, open [Account Overview](https://www.spotify.com/account/overview/) and check whether its plan has **active Premium**.
4. Reply with just **app mode** and **Premium yes/no**. Do not send account passwords, provider keys, or screenshots showing secrets. If it is Free, do not buy a subscription yet; first report the result.

You confirmed **Development Mode** and **Spotify Free**. Spotify's Premium requirement applies to existing Development Mode integrations from **March 9, 2026**; its later postponement of some endpoint changes did not postpone Premium. This explains why the app could previously work on Free, while its current owner plan fails the prerequisite. [Spotify's March 9 clarification](https://developer.spotify.com/blog/2026-02-06-update-on-developer-access-and-platform-security), [current quota-mode requirements](https://developer.spotify.com/documentation/web-api/concepts/quota-modes). Adding diary users to a Spotify allowlist or changing redirect URIs is not a verified fix for this application's server-side client-credentials search; do not do either speculatively.

## Deferred action: add Spotify credentials later

No Spotify action is required now. Do not rerun initial `configure-secrets.ps1`, replace the entire `.env.prod`, or regenerate database/JWT secrets.

When you are ready:

1. Check the existing app-owning account's Spotify access requirements below. Adding keys alone will not resolve the confirmed Free Development Mode search restriction.
2. Open [Spotify Developer Dashboard](https://developer.spotify.com/dashboard), select your existing SentimentScribe app, click **Settings**, and locate **Client ID** and **Client Secret**. Use **View client secret** if shown. Do not reset/delete the app. Keep both values out of chat and Git.
3. Open **Start -> Windows PowerShell** on this computer and run:

   ```powershell
   cd "C:\Users\jay03\Jaynik\Projects\SentimentScribe"
   powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\deploy\aws\update-spotify-credentials.ps1 -ExpectedAccountId 920235987728 -Profile sentimentscribe-new
   ```

4. Enter the matching Client ID and Client Secret at the two masked prompts. The helper authenticates before writing and changes only Spotify fields. If AWS login has expired, run `aws login --profile sentimentscribe-new --region ca-central-1`, sign in to account **920235987728**, complete MFA, and rerun the helper.
5. After local authentication succeeds, run:

   ```powershell
   powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\deploy\aws\apply-spotify-credentials.ps1 -ExpectedAccountId 920235987728 -Profile sentimentscribe-new
   ```

6. Wait for backend HTTPS and provider checks. This updates only Spotify fields on EC2 and recreates only the backend. If search fails, report just the error text; do not reset any secrets. A successful OAuth token alone is not a successful search check.
7. Open [SentimentScribe](https://sentimentscribe.cloud), sign in, unlock a diary entry, and click **Get Media Recommendations**. Both songs and movies must load before recommendations can be marked verified.

## Spotify account prerequisite

To restore the existing Spotify integration, the app-owning account needs active Premium. The agent cannot satisfy that requirement through AWS settings and will not purchase a subscription or change ownership on your behalf.

1. This decision is deferred at your request. Before reactivation, decide whether you accept a recurring Spotify subscription for the account that owns the app. Music and the current combined media endpoint cannot be marked working with this Free Development Mode account. A different recommendation design requires separate approval and is not part of this deployment.
2. **Only if you choose to activate Premium**, sign in as the app-owning account at [Spotify Premium](https://www.spotify.com/premium/), select a plan you are eligible for, and review its displayed price, renewal terms, and account identity before confirming any purchase. Do not subscribe a different account by mistake.
3. Verify the same owner's [Account Overview](https://www.spotify.com/account/overview/) shows active Premium, then follow the masked reactivation steps above. Search and full recommendations still require verification; no AWS architecture change is needed.

This is a paid browser/account action under your control. No subscription has been purchased or modified automatically.

## Completed: approved backup and restore test

The initial export was stopped pending specific consent because a database dump contains account data, including password hashes and encrypted diary records. You explicitly approved the private export, local transfer, and isolated restore test. It then completed successfully in account **920235987728** without modifying production data or stopping any production container.

| Verified backup property | Result |
| --- | --- |
| Local private dump | `deploy/aws/.state/sentimentscribe-20261006072226016.dump` |
| EC2 private dump | `/home/ubuntu/.local/share/sentimentscribe-backups/sentimentscribe-20261006072226016.dump` |
| Format / size | PostgreSQL custom format (`PGDMP`), 7,093 bytes |
| Transfer | SSH; matching SHA-256 hashes |
| Restored data | 1 user, 1 encrypted entry, 1 successful migration |
| Local test container | `sentimentscribe-restore-check-20261006072226016`; network isolated, stopped and retained |
| Proof metadata | `deploy/aws/.state/backup-restore.json`; no passwords, diary bodies, or provider keys |

This is a **one-time backup**, not a recurring schedule. Keep the dump and private SSH/environment files secure and Git-ignored; do not upload them to chat or a public repository. Neither private copy nor the stopped restore container was deleted. Recurring AWS backups remain pending your retention choice and cost approval. Monthly cost alerts are configured separately below.

## Information the agent needs before later stages

- Daily AWS backup retention in days.
- Any later EC2 pause must be requested explicitly because it takes the API offline. Starting a stopped instance resumes compute charges. Nothing was stopped by the agent as part of alert setup or repository cleanup.

Spotify setup is intentionally postponed. No DNS, certificate, or API-key action is needed now. The agent will show recurring backup cost implications and request approval before creating the plan.

## Monthly budget and running-cost estimate

On **October 6, 2026**, AWS STS verified account `920235987728`, then the agent created and read back cost budget **`sentimentscribe-monthly`**, limit **$3 USD**, repeating **monthly**. It covers the account across services/regions, including credits, refunds, discounts, and taxes. No unrelated budget was changed. Reproducible configuration is in `deploy/aws/monthly-budget.json`; verification metadata is private local state in `.state/monthly-budget-result.json`.

This is monitoring, **not a spending cap**. No automatic stop action or paid budget report was configured. At your request, the agent added three **actual-spend, absolute-dollar email alerts** above **$1, $2, and $3**, all addressed to your supplied email. Each threshold and recipient was read back from AWS. Delivery to your inbox is not yet tested. Standard budget monitoring and notifications are free. [AWS Budgets pricing](https://aws.amazon.com/aws-cost-management/aws-budgets/pricing/).

The account-guarded helper `deploy/aws/configure-budget-alerts.ps1` verifies the existing $3 budget and ownership tags, creates only missing requested notifications, and refuses to overwrite existing recipients. Its tests verify dollar thresholds rather than percentages and reject malformed addresses. Recipient/request/result metadata stays in Git-ignored `.state`; the email is not hard-coded into the reusable script. Actual alerts are sent once per threshold per monthly budget period, after billing data updates; they are **not real-time**. [AWS alert cadence and behavior](https://docs.aws.amazon.com/cost-management/latest/userguide/budgets-best-practices.html).

The user reports the instance is running again as of **October 12, 2026**. The estimate below is the cost **if kept running continuously**, with no credits/free-tier discounts, using rates verified on October 6. While stopped, EC2 compute charges cease, but the retained disk plus Elastic IP were estimated at **$6.29/month**, with S3/ECR storage additional.

| Resource | Verified rate | Approximate cost per 730-hour month |
| --- | --- | --- |
| Linux `t3.medium`, `ca-central-1` | $0.0464/hour | $33.87 |
| 30 GiB `gp3` EBS, included baseline IOPS/throughput | $0.088/GiB-month | $2.64 |
| One public IPv4 / Elastic IP | $0.005/hour | $3.65 |
| Baseline total | About $1.32/day | **$40.16** |

EC2/EBS rates were queried directly from the official [AWS Price List API](https://docs.aws.amazon.com/awsaccountbilling/latest/aboutv2/price-changes.html) for `ca-central-1`. The IPv4 rate is from [AWS VPC pricing](https://aws.amazon.com/vpc/pricing/). This excludes S3/ECR storage, requests, CloudFront/other transfer, future backups, taxes, and any unrelated resources.

**$3 / $1.32 per day = approximately 2.3 days (55 hours) from zero spend.** This is not a countdown from when the budget was created: it tracks the current calendar month's spending. Existing charges shorten the remaining time; credits or eligible free-tier offers may reduce billed spending. AWS initially reported $0 accrued when the budget was verified, but billing updates are delayed; that figure is not proof of zero usage. The current architecture cannot run continuously for $3/month at standard rates. No resources were stopped, resized, or redesigned to meet that amount.

To view it manually: sign in to the **new account**, use the console search for **Billing and Cost Management**, open **Budgets**, then select **sentimentscribe-monthly**. Check the $3 limit and actual spend. No additional budget creation is needed.

### Why a $3 hard spending cap was not set

The requested **guaranteed $3 cap cannot be configured for this deployment**. AWS has a newer project spend-limit experience, but it is limited-release and requires a Paid Plan. Its minimum limit is **the greater of $20 or AWS's conservative estimate of likely spending**, not $3. It also has special project-pause/reactivation rules, including deletion of paused project data after 90 days without action. No account/project conversion or paid-plan upgrade was attempted. [AWS project spend limits](https://docs.aws.amazon.com/accounts/latest/reference/create-spend-limit.html).

Ordinary AWS Budget actions can stop a specified EC2 instance, but they do not guarantee a $3 account-wide ceiling: billing is delayed, and EBS, Elastic IP, S3, and ECR retention costs continue. At the already verified rates, retaining just the current 30 GiB disk and Elastic IP is about **$6.29/month before credits/tax/other storage**, even with EC2 stopped. No auto-stop was configured under the misleading label of a hard cap. A teardown would require a fresh backup, explicit destructive-action approval, and a restore/redeployment plan. [Budget actions](https://docs.aws.amazon.com/cost-management/latest/userguide/budgets-controls.html), [EC2 stopped-instance billing](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-lifecycle.html).

### Best place to view current and estimated costs

1. Sign in to AWS account **920235987728** and open [Billing and Cost Management](https://console.aws.amazon.com/costmanagement/). Billing totals cover the account across regions; do not restrict the view to the Ohio EC2 page.
2. In the left navigation, click **Cost Explorer**. If a welcome page appears, click **Launch Cost Explorer**. Initial data can take about 24 hours to prepare. Opening it for the first time also sets up AWS's default Cost Anomaly Detection monitor; that is separate from your $1/$2/$3 alerts. [Initial activation](https://docs.aws.amazon.com/cost-management/latest/userguide/ce-enable.html).
3. On the dashboard, look at **Month-to-date costs** and **Forecasted month end costs**. The first is estimated accrued spending; the second projects the end-of-month total. A new account may not yet have sufficient history for a forecast. [Cost Explorer dashboard](https://docs.aws.amazon.com/cost-management/latest/userguide/ce-exploring-data.html), [forecast limitations](https://docs.aws.amazon.com/cost-management/latest/userguide/ce-forecast.html).
4. Click **Explore costs** or open **Cost & Usage**. Set the date range to **This month**, **Granularity** to **Daily**, and **Group by** to **Service**. Leave service/region filters unset initially so you do not hide charges. Review EC2 compute, EC2-Other/EBS, VPC/public IPv4, S3, ECR, and CloudFront as they appear.
5. Return to **Bills**, select the current month, and expand **Charges by service** plus the credit/tax sections. Use this to reconcile gross usage charges with credits and estimated payable charges; a low or zero credited balance does not mean resources have stopped running.
6. Open **Budgets -> sentimentscribe-monthly** to confirm the $3 monthly amount and the three dollar-based alerts. Do not change thresholds to 1%, 2%, and 3%.

Cost Explorer and budget figures are delayed, not a live per-second meter; some billing data arrives later than 24 hours. Use **Daily**, not optional paid hourly/resource-level analysis, for routine checks. Until a forecast is available, the verified full-month baseline above is about **$40.16 before traffic, other storage, taxes, and credits**. [Billing-data delay](https://docs.aws.amazon.com/cost-management/latest/userguide/ce-exploring-data.html), [Cost Explorer pricing](https://aws.amazon.com/aws-cost-management/aws-cost-explorer/pricing/).

The private SSH key is stored at `deploy/aws/.state/sentimentscribe-prod.pem`, protected with a user-only Windows ACL and ignored by Git. Retain it securely; do not paste it into chat. Keep `.env.prod` values in a password manager once created; do not commit the file.

API DNS, certificate validation, and root-domain routing are complete. Keep all DNS records unchanged.

## Completed manual action: connect the website root domain

You confirmed **Website DNS added**. Authoritative Porkbun DNS and public resolver `1.1.1.1` now return CloudFront addresses; valid HTTPS and browser checks passed on the real root domain. The record below is complete and retained for reference. Do not add it again or change the API/certificate records.

| Porkbun field | Exact value |
| --- | --- |
| Type | **ALIAS - CNAME flattening** |
| Host | **Leave completely blank** |
| Answer/Value | `dgzayw2ozg6wz.cloudfront.net` |
| TTL | `600` |

1. Sign in at [Porkbun](https://porkbun.com/), then click **ACCOUNT -> Domain Management**.
2. Find **sentimentscribe.cloud**, click **Details**, then **Edit** beside **DNS Records**.
3. Click **Add Record** and select **ALIAS - CNAME flattening**.
4. Leave **Host** blank. This means `sentimentscribe.cloud`; do not enter `api`, `www`, or `@`.
5. Enter **Answer/Value** `dgzayw2ozg6wz.cloudfront.net` and **TTL** `600`. Do not include `https://`, a path, or an IP address.
6. Click **Add**. If an existing root A/AAAA/ALIAS record conflicts, stop and report its type/value instead of deleting it. Keep nameservers, email records, the `api` A record, and the certificate CNAME untouched.
7. This DNS action and verification are complete; no further DNS step is needed.

Use ALIAS, not a normal root CNAME or URL forwarding. No `www` record is needed for this deployment; the certificate and application origin currently cover the root domain only. [Porkbun root-domain ALIAS instructions](https://kb.porkbun.com/article/85-how-to-connect-your-root-domain-when-your-web-host-wont-provide-an-ip-address).

## Preparation already verified

- Required JWT environment mapping has been added to Docker Compose. Configuration rejects a missing JWT secret and maps a supplied value to the backend.
- Frontend production build uses `https://api.sentimentscribe.cloud`.
- All 37 frontend tests and 12 backend health/API checks passed. A stale test fixture was updated to satisfy the existing 100-character diary-entry minimum.
- Read-only inventory automation is in [`deploy/aws/preflight.ps1`](../deploy/aws/preflight.ps1). It requires an expected account ID and a named profile, checks identity first, and saves ignored local state. It does not retrieve secrets or mutate resources.
- Application region remains `ca-central-1`; CloudFront ACM certificate region remains `us-east-1`.
- `deploy/aws/frontend.yaml` and `deploy/aws/deploy-frontend.ps1` automate the private S3/CloudFront stage. Live deployment and verification passed. The custom-domain change set modified only the distribution with no replacement; no old-account alias move was required. `verify-frontend-browser.js` passed anonymous browser checks at 1440x900 and 390x844, including direct routes and refresh. All 37 frontend tests were rerun successfully.
- Read-only IAM checks confirmed root MFA is enabled and no root access keys exist. The requested $3 USD monthly cost budget and $1/$2/$3 actual-dollar email alerts are created and verified. Inbox delivery is not yet tested. No shutdown actions or hard cap were configured.
- Live browser checks used synthetic account `rehost-check-1791265548412` and one synthetic encrypted entry. Registration, login, unlock, encrypted server save, refresh, fresh-browser server load/decryption, and analysis passed. API calls went to the intended production origin with no network/CORS failures. Recommendations returned HTTP 400 while provider search independently returned HTTP 403; combined recommendations cannot be marked working yet. Existing users and entries were not modified or deleted.
- `backup-database.ps1` passed its wrong-account rejection test. With your explicit approval, the live private dump/hash/isolated restoration checks passed; the test container was stopped and retained. No production data or containers were changed.

AWS deployment, both production domains, authentication, encrypted diary storage/decryption, analysis, a one-time backup/restore, and budget/alert configuration are verified. Spotify is blank by your choice; combined recommendations are not verified or available yet. Recurring backup protection remains pending your settings. A guaranteed $3 spending cap is not available for this setup. No secrets or `.env.prod` were committed.

**Pause/resume reference:** The user reports the application is running again as of October 12. To resume after a future pause, open **EC2 -> Canada (Central) -> Instances**, select `i-09763071d6129c44f`, and choose **Instance state -> Start instance**. Wait for instance checks and verify API HTTPS/container/database health before using the diary again. Do not terminate the instance, delete its disk, or release the Elastic IP to perform a temporary pause.
