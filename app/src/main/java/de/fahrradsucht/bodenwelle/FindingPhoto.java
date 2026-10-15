package de.fahrradsucht.bodenwelle;

/** findingId is a current linked file owner; photo_links holds every measurement using the image. */
public record FindingPhoto(long id, long findingId, String filename, int width, int height, long bytes) {}
