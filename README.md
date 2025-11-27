# Rediscovering Apollo 11 🚀

## Overview
**Rediscovering Apollo 11** is a Java-based project that leverages **Redis OM Spring**, **vector search**, and **retrieval-augmented generation (RAG)** to explore Apollo 11 mission data. This project integrates structured and unstructured data, including transcripts, photographs, and extracted information, to enable powerful semantic search and querying capabilities.

## Talk
This implementation is the support demo for the [Rediscovering Apollo 11: Using Spring AI + Redis OM Spring to explore the trip to the moon!](https://sessionize.com/s/RaphaelDeLio/rediscovering-apollo-11-using-spring-ai-redis-om-s/112536) talk.

## Slides

The slides can be found at [Speaker Deck](https://speakerdeck.com/raphaeldelio/rediscovering-apollo-11-using-kotlin-spring-ai-plus-redis-om-spring-to-explore-the-mission-to-the-moon)

## How It Works
- The `./src/resources/Apollo11_Data` directory contains data sourced from [Apollo in Real Time](https://apolloinrealtime.org), including utterances, photographs, and the table of contents.
- We use [Redis OM Spring](https://github.com/redis/redis-om-spring), an extension of Spring Data Redis, to efficiently load and manage this data in Redis.
- We also use [Redis Vector Library](https://github.com/redis/redis-vl-java) that gives us different vector search abstractions out of the box. We use it for semantic caching.
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

## 🐳 Running with Docker Compose
To start Redis **with the preloaded RDB file**, run:
```sh
docker-compose up -d
```

The RDB file contains all the data already loaded so that you can simply query it.
Download it from: https://www.dropbox.com/scl/fi/x5hz3pfnizt4tntqe3yor/dump.rdb

