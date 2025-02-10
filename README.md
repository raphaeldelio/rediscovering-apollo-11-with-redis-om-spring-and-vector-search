# Rediscovering Apollo 11 🚀

## Overview
**Rediscovering Apollo 11** is a Java-based project that leverages **Redis OM Spring**, **vector search**, and **retrieval-augmented generation (RAG)** to explore Apollo 11 mission data. This project integrates structured and unstructured data, including transcripts, photographs, and extracted information, to enable powerful semantic search and querying capabilities.

## How It Works
- The `./src/resources/Apollo11_Data` directory contains data sourced from [Apollo in Real Time](https://apolloinrealtime.org), including utterances, photographs, and the table of contents.
- We use **Redis OM Spring**, an extension of Spring Data Redis, to efficiently load and manage this data in Redis.
- The RDB is the file that allows us to recreate our Redis database with all the data already loaded in. It can be downloaded from: https://www.dropbox.com/scl/fi/x5hz3pfnizt4tntqe3yor/dump.rdb
- Download the images from (https://www.dropbox.com/scl/fi/qanbqn9084ull141tzqxe/apollo11.zip) and unzip them into `resources/static/images/` which will result in the `resources/static/images/apollo11` directory being created. 

### **Data Processing Pipeline**
1. **Utterance Processing:**
    - Each utterance is loaded into Redis and **vectorized** for similarity search.
2. **Table of Contents Integration:**
    - The table of contents is loaded, and utterances are grouped accordingly.
3. **Summarization & Vectorization:**
    - An **LLM** generates summaries for each group of utterances, which are then vectorized for efficient retrieval.
4. **Question Extraction & Vectorization:**
    - The LLM extracts relevant questions from the utterances and vectorizes them for searchability.
5. **Photograph Processing:**
    - Photographs are indexed by **vectorizing both the images and their descriptions**.

### **Search Capabilities**
We expose **REST controllers** that allow searching across multiple dimensions:
- **Text-based Search:** Retrieve utterances using natural language queries.
- **Question-based Search:** Find answers using extracted and vectorized questions.
- **Summary-based Search:** Query key mission moments through LLM-generated summaries.
- **Image-text Search:** Search images using textual descriptions.
- **Image-based Search:** Find similar images by uploading a query image.

This setup ensures fast, efficient retrieval of Apollo 11 data using **Redis vector search**.

## 🌐 API Endpoints
| Method | Endpoint                | Description                               |
|--------|-------------------------|-------------------------------------------|
| `POST` | `/search-by-text`       | Search mission logs using text similarity |
| `POST` | `/search-by-question`   | Search using vectorized questions         |
| `POST` | `/search-by-summary`    | Retrieve summarized reports               |
| `POST` | `/search-by-image-text` | Find images based on text queries         |
| `POST` | `/search-by-image`      | Search for images using vector embeddings |

## 🐳 Running with Docker Compose
To start Redis **with the preloaded RDB file**, run:
```sh
docker-compose up -d
```

The RDB file contains all the data already loaded so that you can simply query it.
Download it from: https://www.dropbox.com/scl/fi/x5hz3pfnizt4tntqe3yor/dump.rdb

---
🛠 **Developed by:** Raphael De Lio, Developer Advocate  
📢 Powered by **Redis OM Spring & Redis 8** 🚀

