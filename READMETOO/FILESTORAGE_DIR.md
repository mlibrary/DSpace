# Now I'm examining the configuration or code around filestorage.dir to understand how the file storage directory is set or used, so I can determine where files are stored and managed.

**What is `filestorage.dir`?**

In DSpace, **`filestorage.dir`** is a configuration property that defines the base directory path where DSpace stores persisted file assets (such as bitstreams / uploaded files).

---

**How It Works and Where It Is Used**

1. **Default Definition in `dspace.cfg`**:
   In `dspace/config/dspace.cfg` (line 22), it defaults to the root DSpace installation directory:
   ```properties
   dspace.dir = /dspace
   filestorage.dir = ${dspace.dir}
   ```

2. **Assetstore Resolution in `assetstore.cfg`**:
   In `dspace/config/modules/assetstore.cfg` (line 9), DSpace uses `filestorage.dir` to construct the actual bitstream storage directory:
   ```properties
   assetstore.dir = ${filestorage.dir}/assetstore
   ```

3. **Bitstream Storage Service (`bitstore.xml`)**:
   In `dspace/config/spring/api/bitstore.xml`, the `localStore` bean receives `assetstore.dir` as its base directory (`baseDir`), which is the filesystem directory where all uploaded document bitstreams and asset files are physically stored and organized into subdirectories.

---

**Context in Your Environment**

- **In Docker Compose (`compose.yml`)**:
  The backend container configures:
  ```yaml
  environment:
    - filestorage__P__dir=/dspace
  ```
  This resolves the assetstore path inside the container to `/dspace/assetstore` (backed by the `dspace-assetstore` Docker volume).

- **In your `dspace/config/local.cfg`**:
  You have configured:
  ```properties
  dspace.dir=/Users/gkostin/GitHub/mlibrary/Dspace/dspace
  filestorage.dir=data
  ```
  With this setting, the assetstore directory resolves to `data/assetstore` (or `${dspace.dir}/assetstore` if set to `${dspace.dir}`).


# Now I'm examining configuration files in the dspace/config area to identify directory or file path settings, aiming to understand how paths are defined and organized there.

**Backend Filesystem Configuration Variables in DSpace**

Beyond `filestorage.dir`, DSpace relies on several configuration variables to manage how the backend interacts with the host filesystem. Organizing these by operational domain allows for optimal resource planning, disk allocation, and backup management.

---

**1. Core Installation & Asset Storage**

These properties define where application code, configuration files, and permanent digital assets reside on the server.

* **`dspace.dir`** (defined in `dspace.cfg` and `application.properties`)
    * **Default**: `/dspace`
    * **Role**: The foundational root directory of the DSpace installation. Almost all other directory configurations resolve relative to `${dspace.dir}` (e.g., `${dspace.dir}/config`, `${dspace.dir}/bin`, `${dspace.dir}/lib`).
* **`filestorage.dir`** (defined in `dspace.cfg`)
    * **Default**: `${dspace.dir}`
    * **Role**: The base parent path for persistent file storage.
* **`assetstore.dir`** (defined in `config/modules/assetstore.cfg`)
    * **Default**: `${filestorage.dir}/assetstore`
    * **Role**: The directory where all uploaded document bitstreams and asset files are stored in directory structures.
    * *Multi-store support*: DSpace supports multiple asset stores on separate filesystems or mount points by defining indexed properties such as `assetstore.dir.1`, `assetstore.dir.2`, etc.

---

**2. Uploads & Temporary Ingestion Buffers**

These settings dictate where temporary files are written during ingestion workflows prior to final processing.

* **`upload.temp.dir`** (defined in `dspace.cfg`)
    * **Default**: `${dspace.dir}/upload`
    * **Role**: Staging directory used by the backend to buffer incoming file uploads before they are ingested into the asset store.
* **`sword-server.dir` / `swordv2-server.dir`** (defined in `config/modules/sword-server.cfg` and `swordv2-server.cfg`)
    * **Default**: `${dspace.dir}/sword` and `${dspace.dir}/swordv2`
    * **Role**: Working directories for deposits made via the SWORD / SWORDv2 protocols.
* **`swordv2-server.unpack.dir`** (defined in `config/modules/swordv2-server.cfg`)
    * **Default**: `${swordv2-server.dir}/unpack`
    * **Role**: Directory where SWORD ZIP packages are extracted and parsed during deposit.

---

**3. Exports & Generated Dissemination Files**

These variables control where generated batch jobs, exports, and public discovery files are written.

* **`csvexport.dir`** (defined in `dspace.cfg`)
    * **Default**: `${dspace.dir}/exports`
    * **Role**: Base directory where metadata CSV batch export files are generated.
* **`org.dspace.app.itemexport.work.dir`** (defined in `dspace.cfg`)
    * **Default**: `${dspace.dir}/exports`
    * **Role**: Working directory where item, collection, and community export archives (AIPs) are assembled and compressed.
* **`org.dspace.app.itemexport.download.dir`** (defined in `dspace.cfg`)
    * **Default**: `${dspace.dir}/exports/download`
    * **Role**: Public download holding area where compressed export archives reside for user retrieval.
* **`sitemap.dir`** (defined in `dspace.cfg`)
    * **Default**: `${dspace.dir}/sitemaps`
    * **Role**: Directory where the automated Sitemap generator outputs `sitemap.xml` and related XML files for search engines.

---

**4. Logging, Analytics & Caching**

These variables configure runtime telemetry, diagnostic logs, and auxiliary datasets.

* **`logging.config`** (defined in `application.properties`)
    * **Default**: `${dspace.dir}/config/log4j2.xml`
    * **Role**: File path pointing to the active Log4j2 configuration file.
* **`log.dir`** (configured inside `log4j2.xml`)
    * **Default**: `${dspace.dir}/log`
    * **Role**: Target filesystem directory where standard application logs (`dspace.log`, `checker.log`, CLI logs) are written and rotated.
* **`usage-statistics.dbfile`** (defined in `config/modules/usage-statistics.cfg`)
    * **Default**: `${dspace.dir}/config/GeoLite2-City.mmdb`
    * **Role**: Filesystem path to the MaxMind GeoIP binary database used for IP lookup and geographic statistics.
* **`spring.cache.jcache.config`** (defined in `application.properties`)
    * **Default**: `classpath:iiif/cache/ehcache.xml`
    * **Role**: Filesystem path or classpath URI to cache configurations (e.g., Ehcache configurations for IIIF image processing).

---

**5. Curation, Batch Tasks & Replication**

* **`curate.taskqueue.dir`** (defined in `config/modules/curate.cfg`)
    * **Default**: `${dspace.dir}/ctqueues`
    * **Role**: Filesystem storage for persistent curation task queues processed by background workers.
* **`curate.script.dir`** (defined in `config/modules/curate.cfg`)
    * **Default**: `${dspace.dir}/ctscripts`
    * **Role**: Directory containing external non-Java executable scripts executed by the Curation framework.
* **`replicate.base.dir` / `replicate.consumer.dir` / `replicate.packager.dir`** (defined in `config/modules/replicate.cfg`)
    * **Role**: Working directories used by DSpace Replication and BagIt packaging tasks.

---

**Summary Table for Disk Provisioning**

| Configuration Variable | Default Location | Primary Function |
| :--- | :--- | :--- |
| `dspace.dir` | `/dspace` | Installation & config root |
| `assetstore.dir` | `${filestorage.dir}/assetstore` | Primary bitstream & document storage |
| `upload.temp.dir` | `${dspace.dir}/upload` | Temporary upload buffering |
| `org.dspace.app.itemexport.work.dir` | `${dspace.dir}/exports` | AIP / item export scratch space |
| `sitemap.dir` | `${dspace.dir}/sitemaps` | Generated search engine sitemaps |
| `curate.taskqueue.dir` | `${dspace.dir}/ctqueues` | Curation queue persistence |
| `usage-statistics.dbfile` | `${dspace.dir}/config/GeoLite2-City.mmdb` | GeoIP lookup database file |
| `log.dir` | `${dspace.dir}/log` | Rolling application logs |