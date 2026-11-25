package com.github.kio7po.comic_tracker.adapter.rest.controller;

import java.util.Optional;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.kio7po.comic_tracker.adapter.rest.security.CoverUrlSigner;
import com.github.kio7po.comic_tracker.adapter.rest.security.CoverUrlSigner.CoverAddress;
import com.github.kio7po.comic_tracker.domain.service.ComicReadingCoverService;

@RestController
@RequestMapping("/api")
public class ComicReadingCoverController {

    private final ComicReadingCoverService comicReadingCoverService;
    private final CoverUrlSigner coverUrlSigner;

    public ComicReadingCoverController(ComicReadingCoverService comicReadingCoverService,
            CoverUrlSigner coverUrlSigner) {
        this.comicReadingCoverService = comicReadingCoverService;
        this.coverUrlSigner = coverUrlSigner;
    }

    // Público a propósito (un <img> no puede enviar el token de sesión): lo protege la firma de la dirección, que
    // solo se entrega en una búsqueda con sesión. Una firma inválida o caducada no es una excepción de dominio sino
    // un acceso no autorizado a esta ruta, y quien la recibe es un <img> que no lee el cuerpo.
    @GetMapping("/covers/{token}")
    public ResponseEntity<byte[]> findCover(@PathVariable String token) {
        Optional<CoverAddress> coverAddress = coverUrlSigner.verify(token);
        if (coverAddress.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return comicReadingCoverService
                .findCover(coverAddress.get().providerKey(), coverAddress.get().externalCoverId())
                .map(image -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(image.getContentType()))
                        .cacheControl(CacheControl.maxAge(coverUrlSigner.minValidity()).cachePrivate())
                        .body(image.getContent()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
