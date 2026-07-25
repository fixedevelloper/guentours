package com.guentours.storage;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    private final MinioProperties properties;

    public MinioConfig(MinioProperties properties) {
        this.properties = properties;
    }

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    /**
     * Initialise le bucket MinIO une fois que le contexte Spring Boot est complètement prêt.
     */
    @Bean
    public ApplicationRunner minioBucketInitializer(MinioClient minioClient) {
        return args -> ensureBucketExists(minioClient);
    }

    private void ensureBucketExists(MinioClient minioClient) {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(properties.getBucket()).build());

            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(properties.getBucket()).build());
                log.info("Created MinIO bucket '{}'", properties.getBucket());
            }

            String publicReadPolicy = """
                    {
                      "Version": "2012-10-17",
                      "Statement": [
                        {
                          "Effect": "Allow",
                          "Principal": {"AWS": ["*"]},
                          "Action": ["s3:GetObject"],
                          "Resource": ["arn:aws:s3:::%s/*"]
                        }
                      ]
                    }
                    """.formatted(properties.getBucket());

            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(properties.getBucket())
                    .config(publicReadPolicy)
                    .build());
        } catch (Exception ex) {
            log.error("Failed to initialize MinIO bucket '{}': {}", properties.getBucket(), ex.getMessage());
        }
    }
}