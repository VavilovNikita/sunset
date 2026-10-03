package com.sunsetbeach.controller;

import com.sunsetbeach.api.RestaurantMapApi;
import com.sunsetbeach.model.RestaurantMap;
import com.sunsetbeach.service.RestaurantMapService;
import java.util.concurrent.TimeUnit;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class RestaurantMapController implements RestaurantMapApi {

    private final RestaurantMapService restaurantMapService;

    public RestaurantMapController(RestaurantMapService restaurantMapService) {
        this.restaurantMapService = restaurantMapService;
    }

    @Override
    public ResponseEntity<RestaurantMap> getRestaurantMap() {
        return ResponseEntity.ok(restaurantMapService.get());
    }

    @Override
    public ResponseEntity<Resource> getRestaurantMapImage() {
        Resource resource = restaurantMapService.resolveImage();
        MediaType contentType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(contentType)
                // Same as SpaController#getSpaMapImage: no filename in this URL, so the same URL
                // serves different bytes after a replacement - short-lived, and the frontend
                // appends ?v=<imageUpdatedAt>.
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(resource);
    }

    @Override
    public ResponseEntity<RestaurantMap> uploadRestaurantMapImage(MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(restaurantMapService.uploadImage(file));
    }
}
