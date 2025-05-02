package dev.raphaeldelio.redisapollo.photograph;

public record ImageSearchResult(String imagePath, String description, Double score) {}