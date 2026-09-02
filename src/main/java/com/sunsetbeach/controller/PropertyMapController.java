package com.sunsetbeach.controller;

import com.sunsetbeach.api.PropertyMapApi;
import com.sunsetbeach.model.PropertyMap;
import com.sunsetbeach.service.PropertyMapService;
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
public class PropertyMapController implements PropertyMapApi {

    private final PropertyMapService propertyMapService;

    public PropertyMapController(PropertyMapService propertyMapService) {
        this.propertyMapService = propertyMapService;
    }

    @Override
    public ResponseEntity<PropertyMap> getPropertyMap() {
        return ResponseEntity.ok(propertyMapService.get());
    }

    @Override
    public ResponseEntity<Resource> getPropertyMapImage() {
        Resource resource = propertyMapService.resolveImage();
        MediaType contentType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(contentType)
                // Short-lived, not immutable like room photos: this URL has no filename in it (see
                // PropertyMapService#resolveImage), so unlike a room photo's randomized-forever path,
                // the same URL legitimately serves different bytes after a manager replaces the plan.
                // The frontend appends ?v=<imageUpdatedAt> to force a fresh fetch on replacement -
                // this header is a courtesy for anything that ever calls this endpoint directly.
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(resource);
    }

    @Override
    public ResponseEntity<PropertyMap> uploadPropertyMapImage(MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(propertyMapService.uploadImage(file));
    }
}
