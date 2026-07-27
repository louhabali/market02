package buy01.product_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import buy01.product_service.config.FeignConfig;
import java.util.List;

@FeignClient(name = "media-service", url = "${media.service.url}", configuration = FeignConfig.class)
public interface MediaClient {

        @PostMapping(value = "api/media/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        List<String> uploadImages(
                        @RequestPart("images") MultipartFile[] images);

}