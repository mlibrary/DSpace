# Now I'm searching configuration files for API keys, tokens, or secrets to understand how sensitive credentials are stored and managed in the project. This will help me identify where authentication details reside before handling or securing them.

**API Keys, Tokens, and Credentials in DSpace**

In addition to `api.user.key` (used in `OidcAuthenticationBean` for Alma user privilege verification) and `pubmed.apiKey`, DSpace defines several other API keys, tokens, client secrets, and credential properties across its configuration files (`dspace.cfg`, module configuration files in `dspace/config/modules/`, and Spring bean definitions).

---

**1. External Metadata & Importer API Keys**

Configured primarily in `dspace/config/modules/external-providers.cfg` and mapped in `spring-dspace-addon-import-services.xml`:

| Property Name | Service | Purpose / Description |
| :--- | :--- | :--- |
| `ads.key` | NASA ADS | API key for NASA Astrophysics Data System search & live import |
| `scopus.apiKey` | Elsevier Scopus | API key for Elsevier Scopus search & citation import |
| `scopus.instToken` | Elsevier Scopus | Optional institutional token for expanded Scopus API access |
| `wos.apiKey` | Clarivate Web of Science | API key for Web of Science REST API search & import |
| `cinii.appid` | CiNii | Application ID (API key) for Japanese research literature search |
| `epo.consumerKey` | European Patent Office (EPO) | OAuth Consumer Key for Open Patent Services |
| `epo.consumerSecretKey` | European Patent Office (EPO) | OAuth Consumer Secret Key for Open Patent Services |

---

**2. Publication Policy & Translation Services**

Configured in `dspace/config/dspace.cfg` and `dspace/config/modules/translator.cfg`:

| Property Name | Service | Purpose / Description |
| :--- | :--- | :--- |
| `sherpa.romeo.apikey` | SHERPA/RoMEO | API key for querying publisher open access and copyright policies |
| `translator.api.key.microsoft` | Microsoft Azure / Bing | API key for automated metadata translation curation tasks |
| `translator.api.key.google` | Google Translate | Google Cloud Translation API key |

---

**3. Analytics, reCAPTCHA & Research Graph Integrations**

| Property Name | Location | Purpose / Description |
| :--- | :--- | :--- |
| `google.analytics.api-secret` | `dspace.cfg` | GA4 Measurement Protocol API Secret for tracking backend events/downloads |
| `google.recaptcha.key.site` | `dspace.cfg` | Google reCAPTCHA site public key for form validation |
| `google.recaptcha.key.secret` | `dspace.cfg` | Google reCAPTCHA secret key for server-side verification |
| `openaire.token.clientId` | `modules/openaire-client.cfg` | OpenAIRE REST API client ID |
| `openaire.token.clientSecret` | `modules/openaire-client.cfg` | OpenAIRE REST API client secret |
| `orcid.application-client-id` | `modules/orcid.cfg` | ORCID API Member/Public application client ID |
| `orcid.application-client-secret` | `modules/orcid.cfg` | ORCID API application client secret |

---

**4. Authentication, OAuth & JWT Signing Secrets**

| Property Name | Location | Purpose / Description |
| :--- | :--- | :--- |
| `api.user.key` | `local.cfg` | Alma API key for checking user privileges in OIDC authentication |
| `authentication-oidc.client-id` | `modules/authentication-oidc.cfg` | OpenID Connect client ID |
| `authentication-oidc.client-secret` | `modules/authentication-oidc.cfg` | OpenID Connect client secret |
| `jwt.login.token.secret` | `modules/authentication.cfg` | Secret key used to sign REST API authentication JWT tokens |
| `jwt.login.encryption.secret` | `modules/authentication.cfg` | Encryption key for JWE payload encryption (if enabled) |
| `jwt.shortLived.token.secret` | `modules/authentication.cfg` | Secret key for short-lived bitstream download tokens |
| `jwt.shortLived.encryption.secret`| `modules/authentication.cfg` | Encryption key for short-lived bitstream tokens |

---

**5. Cloud Storage & Identifier Services**

| Property Name | Location | Purpose / Description |
| :--- | :--- | :--- |
| `assetstore.s3.awsAccessKey` | `modules/assetstore.cfg` | AWS Access Key ID for Amazon S3 asset storage |
| `assetstore.s3.awsSecretKey` | `modules/assetstore.cfg` | AWS Secret Access Key for Amazon S3 asset storage |
| `identifier.doi.user` / `.password` | `dspace.cfg` | Registration agency credentials for DataCite / CrossRef DOIs |
| `duracloud.username` / `.password` | `modules/duracloud.cfg` | DuraCloud replication storage credentials |
